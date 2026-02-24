package de.redstonecloud.cloud.commands.defaults;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.commands.Command;
import de.redstonecloud.shared.commands.CommandCompletion;
import de.redstonecloud.shared.commands.CommandArgs;
import de.redstonecloud.shared.server.Template;
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
        CommandCompletion.Node template = CommandCompletion.param(CommandCompletion.ParamType.TEMPLATE);
        CommandCompletion.Node all = CommandCompletion.literal("all");
        CommandCompletion.Flag reboot = CommandCompletion.flagSwitch("--reboot");

        CommandCompletion completion = CommandCompletion.anyOrder(template, reboot);
        completion.add(all);
        all.then(CommandCompletion.literal("--reboot"));
        setCompletions(completion);
    }

    @Override
    protected void onCommand(String[] args) {
        if (args.length < 1) {
            log.warn("Usage: update <templateName|all> [--reboot]");
            return;
        }

        CommandArgs parsed = parseArgs(args);
        String templateName = parsed.positionals().isEmpty() ? null : parsed.positionals().getFirst();
        boolean reboot = parsed.hasFlag("reboot");

        var serverManager = RedstoneCloud.getInstance().getServerManager();

        if (templateName == null || templateName.isBlank()) {
            log.warn("Usage: update <templateName|all> [--reboot]");
            return;
        }

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

}
