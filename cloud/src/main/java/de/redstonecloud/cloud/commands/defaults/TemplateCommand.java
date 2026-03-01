package de.redstonecloud.cloud.commands.defaults;

import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.commands.Command;
import de.redstonecloud.shared.commands.CommandCompletion;
import de.redstonecloud.shared.commands.CommandExecution;
import de.redstonecloud.shared.config.SnakeYamlConfig;
import de.redstonecloud.shared.files.TemplateConfig;
import de.redstonecloud.shared.files.template.TemplateBehavior;
import de.redstonecloud.shared.files.template.TemplateInfo;
import de.redstonecloud.shared.server.Template;
import de.redstonecloud.shared.utils.Directories;
import eu.okaeri.configs.ConfigManager;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

@Log4j2
public class TemplateCommand extends Command {
    public TemplateCommand(String cmd) {
        super(cmd);
        CommandCompletion completion = CommandCompletion.root();
        completion.add(CommandCompletion.literal("list"));
        completion.add(CommandCompletion.literal("create")
                .then(CommandCompletion.param(CommandCompletion.ParamType.ANY, "name"))
                .then(CommandCompletion.param(CommandCompletion.ParamType.TYPE, "type")));
        completion.add(CommandCompletion.literal("delete")
                .then(CommandCompletion.param(CommandCompletion.ParamType.TEMPLATE, "template")));
        completion.add(CommandCompletion.literal("show")
                .then(CommandCompletion.param(CommandCompletion.ParamType.TEMPLATE, "template")));
        completion.add(CommandCompletion.literal("set")
                .then(CommandCompletion.param(CommandCompletion.ParamType.TEMPLATE, "template"))
                .then(CommandCompletion.param(CommandCompletion.ParamType.ANY, "field"))
                .then(CommandCompletion.param(CommandCompletion.ParamType.ANY, "value")));

        setCompletions(completion);
    }

    @Override
    public void onCommand(CommandExecution execution) {
        List<String> args = execution.positionals();
        if (args.isEmpty()) {
            showUsage();
            return;
        }

        String action = args.get(0).toLowerCase(Locale.ROOT);
        switch (action) {
            case "list" -> listTemplates();
            case "create" -> createTemplate(args);
            case "delete" -> deleteTemplate(args);
            case "set" -> setTemplate(args);
            case "show" -> showTemplate(args);
            default -> showUsage();
        }
    }

    private void showUsage() {
        log.info("Usage:");
        log.info("  template list");
        log.info("  template create <name> <type>");
        log.info("  template delete <name>");
        log.info("  template show <name>");
        log.info("  template set <name> <field> <value>");
        log.info("Fields: type, node, static, seperator, maxPlayers, minServers, maxServers, bootMillis, shutdownMillis, autoStop");
    }

    private void listTemplates() {
        if (RedstoneCloud.getInstance().getServerManager().getTemplates().isEmpty()) {
            log.info("No templates configured.");
            return;
        }
        log.info("Templates:");
        for (Template template : RedstoneCloud.getInstance().getServerManager().getTemplates().values()) {
            log.info("- {} | type={} | min={} max={} | node={}",
                    template.getName(),
                    template.getType().name(),
                    template.getMinServers(),
                    template.getMaxServers(),
                    template.getNodes().isEmpty() ? "" : template.getNodes().getFirst());
        }
    }

    private void createTemplate(List<String> args) {
        if (args.size() < 3) {
            log.error("Usage: template create <name> <type>");
            return;
        }

        String name = args.get(1).trim();
        String type = args.get(2).trim();
        if (name.isEmpty() || type.isEmpty()) {
            log.error("Name and type cannot be empty.");
            return;
        }

        Path file = Directories.TEMPLATE_CONFIGS_DIR.toPath().resolve(name + ".yml");
        if (Files.exists(file)) {
            log.error("Template '{}' already exists.", name);
            return;
        }

        TemplateConfig cfg = loadTemplateConfig(file.toFile());
        TemplateInfo info = cfg.info();
        TemplateBehavior behavior = cfg.behavior();
        info.name(name);
        info.type(type);

        cfg.save();
        RedstoneCloud.getInstance().getServerManager().reloadTemplates();
        log.info("Template '{}' created.", name);
    }

    private void deleteTemplate(List<String> args) {
        if (args.size() < 2) {
            log.error("Usage: template delete <name>");
            return;
        }

        String name = args.get(1).trim();
        if (name.isEmpty()) {
            log.error("Template name cannot be empty.");
            return;
        }

        Path file = Directories.TEMPLATE_CONFIGS_DIR.toPath().resolve(name + ".yml");
        if (!Files.exists(file)) {
            log.error("Template '{}' not found.", name);
            return;
        }

        try {
            Files.delete(file);
            RedstoneCloud.getInstance().getServerManager().reloadTemplates();
            log.info("Template '{}' deleted.", name);
        } catch (Exception e) {
            log.error("Failed to delete template '{}'", name, e);
        }
    }

    private void showTemplate(List<String> args) {
        if (args.size() < 2) {
            log.error("Usage: template show <name>");
            return;
        }

        String name = args.get(1).trim();
        Path file = Directories.TEMPLATE_CONFIGS_DIR.toPath().resolve(name + ".yml");
        if (!Files.exists(file)) {
            log.error("Template '{}' not found.", name);
            return;
        }

        try {
            log.info("Template file: {}", file.toAbsolutePath());
            log.info(Files.readString(file, StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Failed to read template '{}'.", name, e);
        }
    }

    private void setTemplate(List<String> args) {
        if (args.size() < 4) {
            log.error("Usage: template set <name> <field> <value>");
            return;
        }

        String name = args.get(1).trim();
        String field = args.get(2).trim().toLowerCase(Locale.ROOT);
        String value = args.get(3).trim();

        if (name.isEmpty() || field.isEmpty()) {
            log.error("Template name and field cannot be empty.");
            return;
        }

        Path file = Directories.TEMPLATE_CONFIGS_DIR.toPath().resolve(name + ".yml");
        if (!Files.exists(file)) {
            log.error("Template '{}' not found.", name);
            return;
        }

        TemplateConfig cfg = loadTemplateConfig(file.toFile());
        TemplateInfo info = cfg.info();
        TemplateBehavior behavior = cfg.behavior();

        switch (field) {
            case "type" -> info.type(value);
            case "node" -> info.node(value);
            case "static" -> info.isStatic(parseBool(value));
            case "seperator" -> info.seperator(value);
            case "maxplayers" -> behavior.maxPlayers(parseInt(value, behavior.maxPlayers()));
            case "minservers" -> behavior.minServers(parseInt(value, behavior.minServers()));
            case "maxservers" -> behavior.maxServers(parseInt(value, behavior.maxServers()));
            case "bootmillis" -> behavior.bootMillis(parseInt(value, behavior.bootMillis()));
            case "shutdownmillis" -> behavior.shutdownMillis(parseInt(value, behavior.shutdownMillis()));
            case "autostop" -> behavior.autoStop(parseBool(value));
            default -> {
                log.error("Unknown field: {}", field);
                showUsage();
                return;
            }
        }

        cfg.save();
        RedstoneCloud.getInstance().getServerManager().reloadTemplates();
        log.info("Template '{}' updated.", name);
    }

    private TemplateConfig loadTemplateConfig(File file) {
        TemplateConfig cfg = ConfigManager.create(TemplateConfig.class, it -> {
            it.withConfigurer(new SnakeYamlConfig());
            it.withBindFile(file);
            it.withRemoveOrphans(true);
            it.saveDefaults();
            it.load(true);
        });

        if (cfg.info() == null) {
            setField(cfg, "info", new TemplateInfo());
        }
        if (cfg.behavior() == null) {
            setField(cfg, "behavior", new TemplateBehavior());
        }

        return cfg;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to set " + fieldName, e);
        }
    }

    private boolean parseBool(String raw) {
        return "true".equalsIgnoreCase(raw) || "1".equals(raw) || "yes".equalsIgnoreCase(raw);
    }

    private int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
