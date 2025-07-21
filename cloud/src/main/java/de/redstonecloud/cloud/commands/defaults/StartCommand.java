package de.redstonecloud.cloud.commands.defaults;

import de.redstonecloud.api.util.EmptyArrays;
import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.commands.Command;
import de.redstonecloud.cloud.server.Template;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class StartCommand extends Command {
    public int argCount = 1;

    public StartCommand(String cmd) {
        super(cmd);
    }

    @Override
    protected void onCommand(String[] args) {
        if (args.length == 0) {
            log.error("Usage: start <template> [count]");
            return;
        }

        Template template = RedstoneCloud.getInstance().getServerManager().getTemplate(args[0]);
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
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("--id") || args[i].equalsIgnoreCase("-i")) {
                if (i + 1 < args.length) {
                    newId = Integer.parseInt(args[i + 1]);
                }
            }

            if (args[i].equalsIgnoreCase("--amount") || args[i].equalsIgnoreCase("-a")) {
                if (i + 1 < args.length) {
                    amount = Integer.parseInt(args[i + 1]);
                }
            }
        }

        if (amount != 1 && newId == -1) {
            for (int i = 0; i < amount; i++) {
                RedstoneCloud.getInstance().getServerManager().startServer(template);
            }
        } else RedstoneCloud.getInstance().getServerManager().startServer(template, newId);
        log.info("Successfully started server using template " + template.getName());
    }

    @Override
    public String[] getArgs() {
        return getServer().getServerManager().getTemplates().keySet().toArray(EmptyArrays.STRING);
    }
}