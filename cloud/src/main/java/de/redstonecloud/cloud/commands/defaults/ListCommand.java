package de.redstonecloud.cloud.commands.defaults;

import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.commands.Command;
import de.redstonecloud.shared.server.Server;

import java.util.Comparator;

public class ListCommand extends Command {
    public ListCommand(String cmd) {
        super(cmd);
    }

    @Override
    protected void onCommand(String[] args) {
        System.out.format("+---------+---------+-----------+----------+------------------+%n");
        System.out.format("| Name    | Type    |  Status   | Port     | NodeId           |%n");
        System.out.format("+---------+---------+-----------+----------+------------------+%n");
        String leftAlignment = "| %-7s | %-7s | %-9s | %-8s | %-17s |%n";
        for (Server server : RedstoneCloud.getInstance().getServerManager().getServers().values().stream().sorted(Comparator.comparing(a -> a.getName())).toList()) {
            System.out.format(leftAlignment, server.getName(), server.getType().name() != null ? server.getType().name() : "null", server.getStatus().name() != null ? server.getStatus().name() : "null", String.valueOf(server.getPort()), server.getNodeId());
            System.out.format("+---------+---------+-----------+----------+------------------+%n");
        }
    }
}
