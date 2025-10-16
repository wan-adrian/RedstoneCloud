package de.redstonecloud.cloud.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.config.CloudConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Scanner;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Slf4j
public class Utils {
    public static String[] dropFirstString(String[] input) {
        String[] anstring = new String[input.length - 1];
        System.arraycopy(input, 1, anstring, 0, input.length - 1);
        return anstring;
    }

    public static String readFileFromResources(String filename) throws IOException {
        // Use getResourceAsStream to read the file from the JAR or file system
        try (InputStream inputStream = RedstoneCloud.class.getClassLoader().getResourceAsStream(filename)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("File not found! " + filename);
            }
            // Use a Scanner to read the InputStream as a String
            try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
                return scanner.useDelimiter("\\A").next(); // Read entire file as a single string
            }
        }
    }

    public static URL getResourceFile(String filename) {
        return Utils.class.getClassLoader().getResource(filename);
    }

    public static void copyFolderFromCurrentJar(String folderInJar, File destDir) throws IOException, URISyntaxException {
        URL jarUrl = RedstoneCloud.class.getProtectionDomain().getCodeSource().getLocation();
        File jarFile = new File(jarUrl.toURI());

        if (jarFile.isFile()) {
            // If the application is running from a JAR file
            try (JarFile jar = new JarFile(jarFile)) {
                Enumeration<JarEntry> entries = jar.entries();

                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String entryName = entry.getName();

                    // Check if the entry is part of the folder you want to copy
                    if (entryName.startsWith(folderInJar)) {
                        File destFile = new File(destDir, entryName.substring(folderInJar.length()));
                        if (entry.isDirectory()) {
                            // Create the directory
                            FileUtils.forceMkdir(destFile);
                        } else {
                            // Copy the file
                            try (InputStream is = jar.getInputStream(entry)) {
                                FileUtils.copyInputStreamToFile(is, destFile);
                            }
                        }
                    }
                }
            }
        } else {
            throw new IllegalStateException("Not running from a JAR file");
        }
    }

    public static void setup() {
        Scanner input = new Scanner(System.in);
        Gson gson = new Gson();

        RedisConfig redisConfig = setupRedis(input);
        createBaseStructure();

        JsonObject supportedSoftware = loadSupportedSoftware(gson);
        boolean setupProxy = setupProxyInstance(input, gson, supportedSoftware);
        boolean setupServer = setupServerInstance(input, gson, supportedSoftware);

        copyCloudFiles();
        showSummary(redisConfig, setupProxy, setupServer);
        finalizeSetup(redisConfig);
        waitForUserToStart();
    }

    private static class RedisConfig {
        boolean useBuiltIn;
        String bind;
        int port;
        boolean downloadUpdate;

        RedisConfig(boolean useBuiltIn, String bind, int port, boolean downloadUpdate) {
            this.useBuiltIn = useBuiltIn;
            this.bind = bind;
            this.port = port;
            this.downloadUpdate = downloadUpdate;
        }
    }

    private static RedisConfig setupRedis(Scanner input) {
        boolean redis = true;
        int intRedisPort = 6379;
        String redisBind = "127.0.0.1";
        boolean downloadRedis = true;

        System.out.println("RedstoneCloud comes with a built-in redis instance. Would you like to use it? [y/n] (default: y): ");
        String result = input.nextLine();
        if (result.toLowerCase().contains("n")) {
            redis = false;
        }

        System.out.println("Please provide an IP redis should bind to [127.0.0.1, 0.0.0.0] (default: 127.0.0.1): ");
        String bindInput = input.nextLine();
        if (!bindInput.isEmpty()) {
            redisBind = bindInput;
        }

        System.out.println("Please provide a redis port you want to use [default: 6379]: ");
        try {
            String portInput = input.nextLine();
            if (!portInput.isEmpty()) {
                intRedisPort = Integer.parseInt(portInput);
            }
        } catch (Exception e) {
            log.warn("Provided invalid port, using default port.");
        }

        return new RedisConfig(redis, redisBind, intRedisPort, downloadRedis);
    }

    private static void createBaseStructure() {
        System.out.println("Settings completed. Generating basic file structure...");
        createBaseFolders();
        System.out.println("Basic folders generated. Starting server config...");
    }

    private static JsonObject loadSupportedSoftware(Gson gson) {
        try {
            JsonObject supportedSoftware = gson.fromJson(Utils.readFileFromResources("supportedSoftware.json"), JsonObject.class);

            if (supportedSoftware == null) {
                System.err.println("Output of supportedSoftware.json is null, shutting down...");
                System.exit(0);
            }

            return supportedSoftware;
        } catch (Exception e) {
            System.err.println("Error while reading supportedSoftware.json, shutting down...");
            e.printStackTrace();
            System.exit(0);
            return null; // unreachable, but required
        }
    }

    private static boolean setupProxyInstance(Scanner input, Gson gson, JsonObject supportedSoftware) {
        System.out.println("Would you like to setup a proxy instance? [y/n] (default: y): ");
        String result = input.nextLine();

        if (result.toLowerCase().contains("n")) {
            return false;
        }

        String software = selectSoftware(input, supportedSoftware, "proxy");
        installSoftware(gson, software, "Proxy", "proxy.jar");

        return true;
    }

    private static boolean setupServerInstance(Scanner input, Gson gson, JsonObject supportedSoftware) {
        System.out.println("Would you like to setup a server instance? [y/n] (default: y): ");
        String result = input.nextLine();

        if (result.toLowerCase().contains("n")) {
            return false;
        }

        String software = selectSoftware(input, supportedSoftware, "server");
        installSoftware(gson, software, "Lobby", "server.jar");

        return true;
    }

    private static String selectSoftware(Scanner input, JsonObject supportedSoftware, String type) {
        System.out.println("Please select a " + type +" software you want to use " + supportedSoftware.get(type).getAsJsonArray().toString().replace("\"", ""));

        String result = input.nextLine();
        String finalResult = result.toUpperCase();

        boolean isValid = supportedSoftware.get(type).getAsJsonArray().asList().stream()
                .anyMatch(item -> item.getAsString().equalsIgnoreCase(finalResult));

        if (!isValid) {
            log.warn("{} software {} is unknown.", type, result);
            System.exit(0);
        }

        return finalResult;
    }

    private static void installSoftware(Gson gson, String software, String templateName, String jarName) {
        System.out.println("Generating structure for " + software + "...");

        try {
            JsonObject settings = gson.fromJson(Utils.readFileFromResources("templates/" + software + "/settings.json"), JsonObject.class);

            copyTemplateFiles(software, templateName);
            System.out.println("Copied important files, downloading software...");

            downloadSoftware(software, templateName, jarName);
            System.out.println("Downloaded software successfully.");

            installCloudBridge(software, templateName, settings);
            System.out.println(templateName + " installed successfully.\n");

        } catch (Exception e) {
            System.err.println("Cannot setup " + templateName + ", shutting down...");
            e.printStackTrace();
            System.exit(0);
        }
    }

    private static void copyTemplateFiles(String software, String templateName) throws IOException, URISyntaxException {
        FileUtils.copyURLToFile(Utils.getResourceFile("templates/" + software + "/template_cfg.json"),
                new File("./template_configs/" + templateName + ".json"));
        FileUtils.copyURLToFile(Utils.getResourceFile("templates/" + software + "/type.json"),
                new File("./types/" + software + ".json"));
        Utils.copyFolderFromCurrentJar("templates/" + software + "/files",
                new File("./templates/" + templateName + "/"));
    }

    private static void downloadSoftware(String software, String templateName, String jarName) throws IOException {
        String downloadUrl = Utils.readFileFromResources("templates/" + software + "/download_url.txt");
        FileUtils.copyURLToFile(URI.create(downloadUrl).toURL(),
                new File("./templates/" + templateName + "/" + jarName));
    }

    private static void installCloudBridge(String software, String templateName, JsonObject settings) throws IOException {
        System.out.println("Installing CloudBridge on " + templateName + "...");

        String bridgeUrl = Utils.readFileFromResources("templates/" + software + "/download_url_bridge.txt");
        String pluginDir = settings.get("pluginDir").getAsString();

        FileUtils.copyURLToFile(URI.create(bridgeUrl).toURL(),
                new File("./templates/" + templateName + "/" + pluginDir + "/CloudBridge.jar"));

        System.out.println("Installed CloudBridge.");
    }

    private static void copyCloudFiles() {
        System.out.println("Copying cloud setup files...");

        try {
            FileUtils.copyURLToFile(Utils.getResourceFile("cloud.json"), new File("./cloud.json"));
            FileUtils.copyURLToFile(Utils.getResourceFile("language.json"), new File("./language.json"));
            System.out.println("Copied cloud files.");
        } catch (IOException e) {
            System.err.println("Copying cloud files failed, shutting down...");
            e.printStackTrace();
            System.exit(0);
        }
    }

    private static void showSummary(RedisConfig redisConfig, boolean setupProxy, boolean setupServer) {
        System.out.println("");
        System.out.println("Cloud setup completed.");
        System.out.println("====================");
        System.out.println("Built-in redis: " + redisConfig.useBuiltIn);
        System.out.println("Built-in redis port: " + redisConfig.port);
        System.out.println("Updated built-in redis: " + redisConfig.downloadUpdate);
        System.out.println("Setup proxy: " + setupProxy);
        System.out.println("Setup server: " + setupServer);
        System.out.println("====================");
    }

    private static void finalizeSetup(RedisConfig redisConfig) {
        try {
            JsonObject cfgFile = CloudConfig.getCfg();
            cfgFile.addProperty("redis_port", redisConfig.port);
            cfgFile.addProperty("redis_bind", redisConfig.bind);
            cfgFile.addProperty("custom_redis", !redisConfig.useBuiltIn);

            Files.writeString(Paths.get(RedstoneCloud.workingDir + "/cloud.json"), cfgFile.toString());
            Files.writeString(Paths.get(RedstoneCloud.workingDir + "/.cloud.setup"),
                    "Cloud is set up. Do not delete this file or the setup will start again.");

            CloudConfig.getCfg(true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void waitForUserToStart() {
        System.out.println("");
        System.out.println("Please press Enter to start the cloud.");

        try {
            System.in.read(new byte[2]);
        } catch (IOException e) {
            System.err.println("Error waiting for input");
            e.printStackTrace();
        }
    }

    public static void createBaseFolders() {
        String[] dirs = {"./servers", "./templates", "./tmp", "./logs", "./plugins", "./template_configs", "./types"};

        for (String dir : dirs) {
            File f = new File(dir);
            if (!f.exists()) {
                f.mkdir();
            }
        }
    }
}
