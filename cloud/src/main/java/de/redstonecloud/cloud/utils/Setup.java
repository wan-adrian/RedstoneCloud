package de.redstonecloud.cloud.utils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.redstonecloud.cloud.config.CloudConfig;
import de.redstonecloud.cloud.config.types.Node;
import de.redstonecloud.shared.config.SnakeYamlConfig;
import de.redstonecloud.shared.files.TypeConfig;
import eu.okaeri.configs.ConfigManager;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

@Log4j2
public class Setup {
    private Scanner scanner = new Scanner(System.in);

    private boolean DEF_INTREDIS = true;
    private String DEF_REDISIP = "127.0.0.1";
    private int DEF_REDISPORT = 6379;
    private boolean DEF_USECLUSTER = false;
    private int DEF_CLUSTERPORT = 6380;

    private JsonObject supportedSoftware = Utils.loadSupportedSoftware(new Gson());

    public void run() {
        log.info("Welcome!!");
        log.info("RedstoneCloud can use its own redis instance to manage multiple servers.");
        log.info("If you want, you can also run your own redis server (recommended for bigger networks).");
        log.info("Do you want to use the internal redis server? (Y/n) [y]");

        boolean intRedis = awaitBooleanAnswer(DEF_INTREDIS);
        String redisIP;
        int redisPort;

        if (intRedis) {
            log.info("You have chosen to use the internal redis server.");
            log.info("On what port should the redis server listen? [6379]");

            redisIP = "0.0.0.0";
            redisPort = awaitIntAnswer(DEF_REDISPORT);
        } else {
            log.info("You have chosen to use your own redis server.");
            log.info("Whats the IP Address of your redis server? [127.0.0.1]");

            redisIP = awaitStringAnswer(DEF_REDISIP);
            log.info("On what port is your redis server listening? [6379]");

            redisPort = awaitIntAnswer(DEF_REDISPORT);
        }

        log.info("Redis is set up.");

        //TODO: Add once clustering is stable
        boolean useCluster = false;
        String firstNode = null;
        int clusterPort = -1;
        /*
        log.info("RedstoneCloud supports clustering. Do you want to enable clustering? (Y/n) [n]");
        boolean useCluster = awaitBooleanAnswer(DEF_USECLUSTER);
        int clusterPort = -1;
        String firstNode = null;

        if (useCluster) {
            log.info("You have chosen to enable clustering.");
            log.info("On what port should the cluster communication listen? [6380]");

            clusterPort = awaitIntAnswer(DEF_CLUSTERPORT);
            log.info("Cluster is set up and will use port {}", clusterPort);

            log.info("To set up a cluster, make sure that all instances use the same redis server and cluster port.");
            log.info("Also make sure that the cluster port is open for communication between the instances.");
            log.info("Whats the name of the first node in the cluster? [Node-1]");

            firstNode = awaitStringAnswer("Node-1");
        } else {
            log.info("You have chosen to disable clustering.");
        }

        log.info("Clustering is set up.");
         */

        log.info("Do you want to set up a proxy server now? (Y/n) [n]");
        boolean setupProxy = awaitBooleanAnswer(false);
        String proxySoftware = null;

        if (setupProxy) {
            List<JsonElement> jsonTypes = supportedSoftware.get("proxy").getAsJsonArray().asList();
            List<String> types = jsonTypes.stream().map(JsonElement::getAsString).toList();
            log.info("Please select a software for your proxy server: ({})", types);

            String chosenType = awaitStringAnswer("--");
            if (!types.contains(chosenType)) {
                log.info("Invalid software selected. Aborting proxy setup.");
            } else {
                proxySoftware = chosenType;
                log.info("You have chosen to set up a {} proxy server.", chosenType);
            }
        }

        log.info("Do you want to set up a default server (lobby) now? (Y/n) [n]");
        boolean setupServer = awaitBooleanAnswer(false);
        String serverSoftware = null;

        if (setupServer) {
            List<JsonElement> jsonTypes = supportedSoftware.get("server").getAsJsonArray().asList();
            List<String> types = jsonTypes.stream().map(JsonElement::getAsString).toList();
            log.info("Please select a software for your default server: ({})", types);

            String chosenType = awaitStringAnswer("--");
            if (!types.contains(chosenType)) {
                log.info("Invalid software selected. Aborting default server setup.");
            } else {
                serverSoftware = chosenType;
                log.info("You have chosen to set up a {} default server.", chosenType);
            }
        }

        log.info("Setup is complete!");

        //summary
        log.info("Summary:");
        log.info("Redis Server: {}:{}", redisIP, redisPort);
        log.info("Using internal Redis: {}", intRedis);
        //log.info("Using Clustering: {}", useCluster);
        if (useCluster) {
            log.info("First Cluster Node: {}", firstNode);
        }
        if (setupProxy) {
            log.info("Proxy Server has been set up.");
        }
        if (proxySoftware != null) {
            log.info("Proxy Software: {}", proxySoftware);
        }
        if (serverSoftware != null) {
            log.info("Default Server Software: {}", serverSoftware);
        }

        log.info("Is this information correct? (Y/n) [y]");
        boolean correct = awaitBooleanAnswer(true);

        if (!correct) {
            log.info("Setup aborted. Please run the setup again to correct the information.");
            System.exit(0);
        }

        log.info("Setting up RedstoneCloud. This may take a moment...");

        File configFile = new File("./config.yml");
        CloudConfig config = ConfigManager.create(CloudConfig.class, it -> {
            it.withConfigurer(new SnakeYamlConfig());
            it.withBindFile(configFile);
            it.withRemoveOrphans(true);
            it.saveDefaults();
            it.load(true);
        });

        config.redis().ip(redisIP);
        config.redis().port(redisPort);
        config.redis().internalInstance(intRedis);
        config.redis().dbId(0);

        if (useCluster) {
            config.cluster().port(clusterPort);
            Node node = new Node();
            node.name(firstNode);
            node.id(UUID.randomUUID().toString());

            config.cluster().nodes(List.of(
                    node
            ));
        }

        config.save();

        log.info("Config has been saved.");

        if (setupProxy && proxySoftware != null) {
            log.info("Setting up default proxy server...");
            installSoftware(new Gson(), proxySoftware, "Proxy", "proxy.jar");
        }

        if (setupServer && serverSoftware != null) {
            log.info("Setting up default server...");
            installSoftware(new Gson(), serverSoftware, "Lobby", "server.jar");
        }

        log.info("Setup is complete! Press any key to boot into RedstoneCloud...");
        scanner.nextLine();
    }
    
    private boolean awaitBooleanAnswer(boolean fallback) {
        try {
            String input = scanner.nextLine().trim().toLowerCase();
            if (input.isEmpty() || input.equals("y") || input.equals("yes")) {
                return true;
            } else if (input.equals("n") || input.equals("no")) {
                return false;
            } else {
                log.info("Invalid input. Using default option.");
                return fallback;
            }
        } catch (Exception e) {
            log.info("Error reading input. Using default option.");
            return fallback;
        }
    }

    private int awaitIntAnswer(int fallback) {
        try {
            String input = scanner.nextLine().trim().toLowerCase();
            if (input.isEmpty()) {
                return fallback;
            } else {
                return Integer.parseInt(input);
            }
        } catch (Exception e) {
            log.info("Error reading input. Using default option.");
            return fallback;
        }
    }

    private String awaitStringAnswer(String fallback) {
        try {
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                return fallback;
            } else {
                return input;
            }
        } catch (Exception e) {
            log.info("Error reading input. Using default option.");
            return fallback;
        }
    }

    private static void installSoftware(Gson gson, String software, String templateName, String jarName) {
        log.info("Generating structure for " + software + "...");

        try {
            JsonObject settings = gson.fromJson(Utils.readFileFromResources("templates/" + software + "/settings.json"), JsonObject.class);
            copyTemplateFiles(software, templateName);

            TypeConfig typeConfig = ConfigManager.create(TypeConfig.class, it -> {
                it.withConfigurer(new SnakeYamlConfig());
                it.withBindFile(new File("./types/" + software + ".yml"));
                it.load(true);
            });

            log.info("Copied important files, downloading software...");

            String downloadUrl = typeConfig.downloads().software();
            downloadSoftware(downloadUrl, templateName, jarName);
            log.info("Downloaded software successfully.");

            String downloadUrlBridge = typeConfig.downloads().bridge();
            installCloudBridge(downloadUrlBridge, templateName, settings);
            log.info(templateName + " installed successfully.\n");

        } catch (Exception e) {
            log.error("Cannot setup " + templateName.toLowerCase() + ", shutting down...");
            e.printStackTrace();
        }
    }

    private static void copyTemplateFiles(String software, String templateName) throws IOException, URISyntaxException {
        FileUtils.copyURLToFile(Utils.getResourceFile("templates/" + software + "/template_cfg.yml"),
                new File("./template_configs/" + templateName + ".yml"));
        FileUtils.copyURLToFile(Utils.getResourceFile("templates/" + software + "/type.yml"),
                new File("./types/" + software + ".yml"));
        Utils.copyFolderFromCurrentJar("templates/" + software + "/files",
                new File("./templates/" + templateName + "/"));
    }

    private static void downloadSoftware(String downloadUrl, String templateName, String jarName) throws IOException {
        FileUtils.copyURLToFile(URI.create(downloadUrl).toURL(),
                new File("./templates/" + templateName + "/" + jarName));
    }

    private static void installCloudBridge(String bridgeUrl, String templateName, JsonObject settings) throws IOException {
        log.info("Installing CloudBridge on " + templateName + "...");

        String pluginDir = settings.get("pluginDir").getAsString();

        FileUtils.copyURLToFile(URI.create(bridgeUrl).toURL(),
                new File("./templates/" + templateName + "/" + pluginDir + "/CloudBridge.jar"));

        log.info("Installed CloudBridge.");
    }
}