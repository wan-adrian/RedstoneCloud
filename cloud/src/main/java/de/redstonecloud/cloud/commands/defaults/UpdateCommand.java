package de.redstonecloud.cloud.commands.defaults;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.commands.Command;
import de.redstonecloud.cloud.server.Template;
import de.redstonecloud.cloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

@Slf4j
public class UpdateCommand extends Command {

    public UpdateCommand(String cmd) {
        super(cmd);
    }

    @Override
    protected void onCommand(String[] args) {
        if (args.length < 1) {
            log.warn("Usage: update <templateName|all> [--reboot]");
            return;
        }

        String templateName = args[0];
        boolean reboot = args.length > 1 && args[1].equalsIgnoreCase("--reboot");

        var serverManager = RedstoneCloud.getInstance().getServerManager();

        if ("all".equalsIgnoreCase(templateName)) {
            log.info("Updating all templates{}...",
                    reboot ? " with reboot" : "");

            serverManager.getTemplates().keySet().forEach(name -> {
                try {
                    updateTemplate(name, reboot);
                } catch (Exception e) {
                    log.error("Failed to update template '{}'", name, e);
                }
            });

            return;
        }

        if (!serverManager.getTemplates().containsKey(templateName)) {
            log.error("Template '{}' does not exist.", templateName);
            return;
        }

        try {
            updateTemplate(templateName, reboot);
        } catch (Exception e) {
            log.error("Failed to update template '{}'", templateName, e);
        }
    }

    private void updateTemplate(String templateName, boolean reboot) throws Exception {
        Template template = RedstoneCloud.getInstance().getServerManager().getTemplate(templateName);
        File cfgFile = new File("./template_configs/" + templateName + ".json");

        if (!cfgFile.exists()) {
            log.error("template_cfg.json for template '{}' not found.", templateName);
            return;
        }

        String json = FileUtils.readFileToString(cfgFile, StandardCharsets.UTF_8);
        JsonObject cfg = JsonParser.parseString(json).getAsJsonObject();

        String type = cfg.get("type").getAsString().toUpperCase();
        String jarName = template.getType().isProxy() ? "proxy.jar" : "server.jar";

        Utils.updateSoftware(templateName, type, jarName, reboot);
    }

    @Override
    public String[] getArgs() {
        return Stream.concat(
                getServer().getServerManager().getTemplates().keySet().stream(),
                Stream.of("all")
        ).toArray(String[]::new);
    }
}
