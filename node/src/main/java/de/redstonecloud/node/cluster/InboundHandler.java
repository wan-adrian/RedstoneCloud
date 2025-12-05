package de.redstonecloud.node.cluster;

import de.redstonecloud.api.RCClusteringProto;
import de.redstonecloud.api.RCGenericProto;
import de.redstonecloud.api.components.ServerStatus;
import de.redstonecloud.node.server.NodeServerManager;
import de.redstonecloud.shared.server.Server;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class InboundHandler implements StreamObserver<RCClusteringProto.Payload> {

    @Override
    public void onNext(RCClusteringProto.Payload msg) {

        switch (msg.getPayloadCase()) {

            case PREPARESERVER -> {
                log.info("Preparing server {} from template {}",
                        msg.getPrepareServer().getName(),
                        msg.getPrepareServer().getTemplate());

                Map<String, String> env = msg.getPrepareServer().getEnvList().stream()
                        .collect(
                                java.util.stream.Collectors.toMap(
                                        RCGenericProto.KeyValuePair::getKey,
                                        RCGenericProto.KeyValuePair::getValue
                                )
                        );

                NodeServerManager.getInstance().prepareServer(msg.getPrepareServer().getTemplate(), msg.getPrepareServer().getName(), env);
            }

            case STARTSERVER -> {
                log.info("Starting server {}", msg.getStartServer().getServer());
                NodeServerManager.getInstance().start(NodeServerManager.getInstance().getServer(msg.getStartServer().getServer()));
            }

            case STOPSERVER -> {
                boolean kill = msg.getStopServer().getKill();
                log.info("Stopping server {} (kill={})", msg.getStopServer().getServer(), kill);
                Server server = NodeServerManager.getInstance().getServer(msg.getStopServer().getServer());
                if(server == null) return;

                if(kill)
                    server.kill();
                else
                    server.stop();
            }


            case EXECUTECOMMAND -> {
                log.info("Executing command on server {}: {}",
                        msg.getExecuteCommand().getServer(),
                        msg.getExecuteCommand().getCommand());

                Server server = NodeServerManager.getInstance().getServer(msg.getExecuteCommand().getServer());
                if(server == null) return;
                server.writeConsole(msg.getExecuteCommand().getCommand());
            }

            case RESPONSE -> {
                log.info("Master → response {}", msg.getResponse().getMessage());
            }

            case SERVERSTATUSCHANGE -> {
                String serverName = msg.getServerStatusChange().getServer();
                String status = msg.getServerStatusChange().getStatus();
                log.info("{} changed status to {}", serverName, status);
                Server server = NodeServerManager.getInstance().getServer(serverName);
                if(server == null) return;
                server.setStatusLocally(ServerStatus.valueOf(status));
            }

            case TYPECHANGES -> {
                log.info("Master → typeChanges");
                NodeServerManager.getInstance().reloadServerTypes(
                        msg.getTypeChanges().getTypesList().stream().map(RCGenericProto.Type::getConfig).toList()
                );
            }

            case TEMPLATECHANGES -> {
                log.info("Master → templateChanges");
                NodeServerManager.getInstance().reloadTemplates(
                        msg.getTemplateChanges().getTemplatesList().stream().map(RCGenericProto.Template::getData).toList()
                );
            }

            default -> log.warn("Master → Unknown message: {}", msg.getPayloadCase());
        }
    }

    @Override
    public void onError(Throwable t) {
        log.error("Stream error: {}", t.getMessage());
    }

    @Override
    public void onCompleted() {
        log.warn("Master closed stream.");
        ClusterClient.getInstance().setStreamActive(false);
    }
}
