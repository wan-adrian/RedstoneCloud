package de.redstonecloud.cloud.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.shared.server.Server;
import de.redstonecloud.shared.server.Template;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Scanner;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Utils {
    public static String readFileFromResources(String filename) throws IOException {
        try (InputStream inputStream = RedstoneCloud.class.getClassLoader().getResourceAsStream(filename)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("File not found! " + filename);
            }
            try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8)) {
                return scanner.useDelimiter("\\A").next();
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
            try (JarFile jar = new JarFile(jarFile)) {
                Enumeration<JarEntry> entries = jar.entries();

                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String entryName = entry.getName();

                    if (entryName.startsWith(folderInJar)) {
                        File destFile = new File(destDir, entryName.substring(folderInJar.length()));
                        if (entry.isDirectory()) {
                            FileUtils.forceMkdir(destFile);
                        } else {
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

    public static void updateSoftware(String templateName, String software, String jarName, boolean reboot) {
        System.out.println("Updating " + templateName + "...");
        try {
            JsonObject typeData = new Gson().fromJson(Utils.readFileFromResources("templates/" + software + "/type.json"), JsonObject.class);
            String downloadUrl = typeData.get("download_url").getAsString();

            FileUtils.copyURLToFile(URI.create(downloadUrl).toURL(),
                    new File("./templates/" + templateName + "/" + jarName));
            System.out.println(templateName + " updated successfully.");

            if (reboot) {
                Template template = RedstoneCloud.getInstance().getServerManager().getTemplate(templateName);
                Server[] servers = RedstoneCloud.getInstance().getServerManager().getServersByTemplate(template);
                Arrays.stream(servers).forEach(Server::stop);
            }

        } catch (IOException e) {
            System.err.println("Cannot update " + templateName + ", shutting down...");
            e.printStackTrace();
            System.exit(0);
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

    public static JsonObject loadSupportedSoftware(Gson gson) {
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
            return null;
        }
    }
}
