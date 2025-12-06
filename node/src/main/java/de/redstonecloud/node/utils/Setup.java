package de.redstonecloud.node.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.redstonecloud.node.config.NodeConfig;
import de.redstonecloud.shared.config.SnakeYamlConfig;
import de.redstonecloud.shared.files.TypeConfig;
import eu.okaeri.configs.ConfigManager;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Scanner;

@Log4j2
public class Setup {
    private Scanner scanner = new Scanner(System.in);

    private String DEF_IP = "127.0.0.1";
    private int DEF_REDISPORT = 6379;
    private int DEF_REDISDB = 0;
    private int DEF_CLUSTERPORT = 6380;

    public void run() {
        log.info("RedstoneCloud Node - Welcome");

        log.info("===== Redis Setup =====");

        log.info("Please enter the ip address of your redis instance [{}]", DEF_IP);
        String redisIp = awaitStringAnswer(DEF_IP);

        log.info("Please enter the port of your redis instance [{}]", DEF_REDISPORT);
        int redisPort = awaitIntAnswer(DEF_REDISPORT);

        log.info("Please enter the db id your master instance uses [{}]", DEF_REDISDB);
        int redisDb = awaitIntAnswer(DEF_REDISDB);

        log.info("===== RC Master Setup =====");

        log.info("Please enter the ip address of your RedstoneCloud master instance [{}]", DEF_IP);
        String clusterIp = awaitStringAnswer(DEF_IP);

        log.info("Please enter the port of your RedstoneCloud master instance [{}]", DEF_CLUSTERPORT);
        int clusterPort = awaitIntAnswer(DEF_CLUSTERPORT);

        log.info("===== RC Node Setup =====");

        log.info("Please enter the id of this node (you can find this in your master config)");
        String nodeId = awaitStringAnswer("");
        if(nodeId.isEmpty()) {
            log.info("Please enter a valid node id. Exiting setup.");
            System.exit(0);
        }

        log.info("Please enter the IP Address of this node (must be reachable from master instance) [{}]", DEF_IP);
        String nodeIp = awaitStringAnswer(DEF_IP);

        //summary
        log.info("===== Setup Summary =====");

        log.info("Redis Server: {}:{}", redisIp, redisPort);
        log.info("Redis DB-ID: {}", redisDb);

        log.info("RC Master: {}:{}", clusterIp, clusterPort);

        log.info("Node-ID: {}", nodeId);
        log.info("IP of this node: {}", nodeIp);

        log.info("Is this information correct? (Y/n) [y]");
        boolean correct = awaitBooleanAnswer(true);

        if (!correct) {
            log.info("Setup aborted. Please run the setup again to correct the information.");
            System.exit(0);
        }

        log.info("Setting up RedstoneCloud Node. This may take a moment...");

        File configFile = new File("./config.yml");
        NodeConfig config = ConfigManager.create(NodeConfig.class, it -> {
            it.withConfigurer(new SnakeYamlConfig());
            it.withBindFile(configFile);
            it.withRemoveOrphans(true);
            it.saveDefaults();
            it.load(true);
        });

        config.redis().ip(redisIp);
        config.redis().port(redisPort);
        config.redis().dbId(redisDb);

        config.master().ip(clusterIp);
        config.master().port(clusterPort);

        config.node().id(nodeId);
        config.node().address(nodeIp);


        config.save();

        log.info("Config has been saved.");

        log.info("RC Node will automatically fetch type & template information from master instance.");
        log.info("However, you still have to manually create all template files. You can just move them into the nodes templates folder.");

        log.info("Setup is complete! Press any key to boot into RedstoneCloud Node...");
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
}