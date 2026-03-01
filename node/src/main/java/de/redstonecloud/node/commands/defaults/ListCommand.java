package de.redstonecloud.node.commands.defaults;

import de.redstonecloud.node.RedstoneNode;
import de.redstonecloud.node.commands.Command;
import de.redstonecloud.shared.commands.CommandCompletion;
import de.redstonecloud.shared.commands.CommandExecution;
import de.redstonecloud.shared.server.Server;

import java.util.Comparator;

public class ListCommand extends Command {
    public ListCommand(String cmd) {
        super(cmd);
        setCompletions(CommandCompletion.root());
    }

    @Override
    public void onCommand(CommandExecution execution) {
        System.out.format("+---------+---------+-----------+----------+%n");
        System.out.format("| Name    | Type    |  Status   | Port     |%n");
        System.out.format("+---------+---------+-----------+----------+%n");
        String leftAlignment = "| %-7s | %-7s | %-9s | %-7s |%n";
        for (Server server : RedstoneNode.getInstance().getServerManager().getServers().values().stream().sorted(Comparator.comparing(a -> a.getName())).toList()) {
            System.out.format(leftAlignment, server.getName(), server.getType().name() != null ? server.getType().name() : "null", server.getStatus().name() != null ? server.getStatus().name() : "null", String.valueOf(server.getPort()));
            System.out.format("+---------+---------+-----------+----------+%n");
        }
    }
}
