package de.redstonecloud.cloud.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.config.CloudConfig;
import de.redstonecloud.cloud.logger.Logger;
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
        Logger l = Logger.getInstance();
        Scanner input = new Scanner(System.in);
        Gson gson = new Gson();

        // Setup Redis configuration
        RedisConfig redisConfig = setupRedis(l, input);

        // Create base structure
        createBaseStructure(l);

        // Load supported software
        JsonObject supportedSoftware = loadSupportedSoftware(l, gson);

        // Setup proxy and server
        boolean setupProxy = setupProxyInstance(l, input, gson, supportedSoftware);
        boolean setupServer = setupServerInstance(l, input, gson, supportedSoftware);

        // Copy cloud files
        copyCloudFiles(l);

        // Show summary and finalize setup
        showSummary(l, redisConfig, setupProxy, setupServer);
        finalizeSetup(l, redisConfig);

        waitForUserToStart(l);
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

    private static RedisConfig setupRedis(Logger l, Scanner input) {
        boolean redis = true;
        int intRedisPort = 6379;
        String redisBind = "127.0.0.1";
        boolean downloadRedis = true;

        l.setup("RC Setup", "§cRedstoneCloud comes with a built-in redis instance. Would you like to use it? §3[y/n] §a(default: y): ");
        String result = input.nextLine();
        if (result.toLowerCase().contains("n")) {
            redis = false;
        }

        l.setup("RC Setup", "§cPlease provide an ip redis should bind to. §3[127.0.0.1, 0.0.0.0] §a(default: 127.0.0.1): ");
        String bindInput = input.nextLine();
        if (!bindInput.isEmpty()) {
            redisBind = bindInput;
        }

        l.setup("RC Setup", "§cPlease provide a redis port you want to use. §3[number] §a(default: 6379): ");
        try {
            String portInput = input.nextLine();
            if (!portInput.isEmpty()) {
                intRedisPort = Integer.parseInt(portInput);
            }
        } catch (Exception e) {
            l.setup("RC Setup", "§eProvided invalid port, using default port.", true);
        }

        return new RedisConfig(redis, redisBind, intRedisPort, downloadRedis);
    }

    private static void createBaseStructure(Logger l) {
        l.setup("RC Setup", "§eSettings completed. Generating basic file structure...", true);
        RedstoneCloud.createBaseFolders();
        l.setup("RC Setup", "§eBasic folders generated. Starting server config...", true);
    }

    private static JsonObject loadSupportedSoftware(Logger l, Gson gson) {
        try {
            JsonObject supportedSoftware = gson.fromJson(Utils.readFileFromResources("supportedSoftware.json"), JsonObject.class);

            if (supportedSoftware == null) {
                l.setup("RC Setup", "§4Output of supportedSoftware.json is null, shutting down...", true);
                System.exit(0);
            }

            return supportedSoftware;
        } catch (Exception e) {
            e.printStackTrace();
            l.setup("RC Setup", "§4Error while reading supportedSoftware.json, shutting down...", true);
            System.exit(0);
            return null;
        }
    }

    private static boolean setupProxyInstance(Logger l, Scanner input, Gson gson, JsonObject supportedSoftware) {
        l.setup("RC Setup", "§cWould you like to setup a proxy instance? §3[y/n] §a(default: y): ");
        String result = input.nextLine();

        if (result.toLowerCase().contains("n")) {
            return false;
        }

        String software = selectSoftware(l, input, supportedSoftware, "proxy");
        installSoftware(l, gson, software, "Proxy", "proxy.jar");

        return true;
    }

    private static boolean setupServerInstance(Logger l, Scanner input, Gson gson, JsonObject supportedSoftware) {
        l.setup("RC Setup", "§cWould you like to setup a server instance? §3[y/n] §a(default: y): ");
        String result = input.nextLine();

        if (result.toLowerCase().contains("n")) {
            return false;
        }

        String software = selectSoftware(l, input, supportedSoftware, "server");
        installSoftware(l, gson, software, "Lobby", "server.jar");

        return true;
    }

    private static String selectSoftware(Logger l, Scanner input, JsonObject supportedSoftware, String type) {
        l.setup("RC Setup", "§cPlease select a " + type + " software you want to use §3" +
                supportedSoftware.get(type).getAsJsonArray().toString().replace("\"", "") + " ");

        String result = input.nextLine();
        String finalResult = result.toUpperCase();

        boolean isValid = supportedSoftware.get(type).getAsJsonArray().asList().stream()
                .anyMatch(item -> item.getAsString().equalsIgnoreCase(finalResult));

        if (!isValid) {
            l.setup("RC Setup", "§e" + type + " software " + result + " is unknown.", true);
            System.exit(0);
        }

        return finalResult;
    }

    private static void installSoftware(Logger l, Gson gson, String software, String templateName, String jarName) {
        l.setup("RC Setup", "§eGenerating structure for " + software + "...", true);

        try {
            JsonObject settings = gson.fromJson(Utils.readFileFromResources("templates/" + software + "/settings.json"), JsonObject.class);

            // Copy template files
            copyTemplateFiles(software, templateName);

            l.setup("RC Setup", "§eCopied important files, downloading software...", true);

            // Download main software
            downloadSoftware(software, templateName, jarName);

            l.setup("RC Setup", "§eDownloaded software successfully.", true);

            // Install CloudBridge
            installCloudBridge(l, software, templateName, settings);

            l.setup("RC Setup", "§e" + templateName + " installed successfully. \n", true);

        } catch (Exception e) {
            e.printStackTrace();
            l.setup("RC Setup", "§4Cannot setup " + templateName.toLowerCase() + ", shutting down...", true);
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

    private static void installCloudBridge(Logger l, String software, String templateName, JsonObject settings) throws IOException {
        l.setup("RC Setup", "§eInstalling CloudBridge on " + templateName + "...", true);

        String bridgeUrl = Utils.readFileFromResources("templates/" + software + "/download_url_bridge.txt");
        String pluginDir = settings.get("pluginDir").getAsString();

        FileUtils.copyURLToFile(URI.create(bridgeUrl).toURL(),
                new File("./templates/" + templateName + "/" + pluginDir + "/CloudBridge.jar"));

        l.setup("RC Setup", "§eInstalled CloudBridge", true);
    }

    private static void copyCloudFiles(Logger l) {
        l.setup("RC Setup", "§eCopying cloud setup files...", true);

        try {
            FileUtils.copyURLToFile(Utils.getResourceFile("cloud.json"), new File("./cloud.json"));
            FileUtils.copyURLToFile(Utils.getResourceFile("language.json"), new File("./language.json"));
            l.setup("RC Setup", "§eCopied cloud files.", true);
        } catch (IOException e) {
            e.printStackTrace();
            l.setup("RC Setup", "§4Copying cloud files failed, shutting down...", true);
            System.exit(0);
        }
    }

    private static void showSummary(Logger l, RedisConfig redisConfig, boolean setupProxy, boolean setupServer) {
        l.setup("RC Setup", "", true);
        l.setup("RC Setup", "", true);
        l.setup("RC Setup", "§eCloud setup completed.", true);
        l.setup("RC Setup", "====================", true);
        l.setup("RC Setup", "Built-in redis: " + redisConfig.useBuiltIn, true);
        l.setup("RC Setup", "Built-in redis port: " + redisConfig.port, true);
        l.setup("RC Setup", "Updated built-in redis: " + redisConfig.downloadUpdate, true);
        l.setup("RC Setup", "Setup proxy: " + setupProxy, true);
        l.setup("RC Setup", "Setup server: " + setupServer, true);
        l.setup("RC Setup", "====================", true);
    }

    private static void finalizeSetup(Logger l, RedisConfig redisConfig) {
        try {
            JsonObject cfgFile = CloudConfig.getCfg();
            cfgFile.addProperty("redis_port", redisConfig.port);
            cfgFile.addProperty("redis_bind", redisConfig.bind);
            cfgFile.addProperty("custom_redis", !redisConfig.useBuiltIn);

            Files.writeString(Paths.get(RedstoneCloud.workingDir + "/cloud.json"), cfgFile.toString());
            Files.writeString(Paths.get(RedstoneCloud.workingDir + "/.cloud.setup"),
                    "Cloud is set up. Do not delete this file or the setup will start again.");

            CloudConfig.getCfg(true); // Reload config for future uses
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void waitForUserToStart(Logger l) {
        l.setup("RC Setup", "", true);
        l.setup("RC Setup", "§cPlease press Enter to start the cloud.", true);

        try {
            System.in.read(new byte[2]);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
