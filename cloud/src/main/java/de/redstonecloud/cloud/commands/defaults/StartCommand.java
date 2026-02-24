package de.redstonecloud.cloud.commands.defaults;

import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.commands.Command;
import de.redstonecloud.shared.commands.CommandCompletion;
import de.redstonecloud.shared.commands.CommandArgs;
import de.redstonecloud.shared.server.Template;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class StartCommand extends Command {
    public StartCommand(String cmd) {
        super(cmd);
        CommandCompletion.Node template = CommandCompletion.param(CommandCompletion.ParamType.TEMPLATE);
        CommandCompletion.Flag id = CommandCompletion.flag(CommandCompletion.ParamType.ANY, "--id", "-i");
        CommandCompletion.Flag amount = CommandCompletion.flag(CommandCompletion.ParamType.ANY, "--amount", "-a");
        setCompletions(CommandCompletion.anyOrder(template, id, amount).restrictRootTo(template));
    }

    @Override
    protected void onCommand(String[] args) {
        if (args.length == 0) {
            log.error("Usage: start <template> [count]");
            return;
        }

        CommandArgs parsed = parseArgs(args);
        String templateName = parsed.positionals().isEmpty() ? null : parsed.positionals().getFirst();
        if (templateName == null || templateName.isBlank()) {
            log.error("Usage: start <template> [--id <id>] [--amount <n>]");
            return;
        }

        Template template = RedstoneCloud.getInstance().getServerManager().getTemplate(templateName);
        if (template == null) {
            log.error("Template not found.");
            return;
        }

        //if args has --id or -i flag, next arg is the id, can be at args 1 or 2
        //command: start <template> [count]
        //command: start <template> [count] --id <id>
        //command: start <template> --id <id> [count]
        int newId = -1;
        int amount = 1;
        String idRaw = parsed.flagValue("id");
        if (idRaw == null) {
            idRaw = parsed.flagValue("i");
        }
        if (idRaw != null) {
            newId = parseIntSafe(idRaw, -1);
        }

        String amountRaw = parsed.flagValue("amount");
        if (amountRaw == null) {
            amountRaw = parsed.flagValue("a");
        }
        if (amountRaw != null) {
            amount = parseIntSafe(amountRaw, 1);
        }

        if (amount != 1 && newId == -1) {
            for (int i = 0; i < amount; i++) {
                RedstoneCloud.getInstance().getServerManager().startServer(template);
            }
        } else RedstoneCloud.getInstance().getServerManager().startServer(template, newId);
        log.info("Successfully started server using template " + template.getName());
    }

    private int parseIntSafe(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
