package de.redstonecloud.node.cluster;

import de.redstonecloud.api.RCClusteringProto;
import de.redstonecloud.api.RCGenericProto;
import de.redstonecloud.node.server.ServerManager;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InboundHandler implements StreamObserver<RCClusteringProto.Payload> {

    @Override
    public void onNext(RCClusteringProto.Payload msg) {

        switch (msg.getPayloadCase()) {

            case PREPARESERVER -> {
                log.info("Master → prepareServer {}", msg.getPrepareServer().getName());
                ServerManager.getInstance().prepareServer(msg.getPrepareServer().getTemplate(), msg.getPrepareServer().getName());
            }

            case STARTSERVER -> {
                log.info("Master → startServer {}", msg.getStartServer().getServer());
            }

            case STOPSERVER -> {
                log.info("Master → stopServer {}", msg.getStopServer().getServer());
            }

            case EXECUTECOMMAND -> {
                log.info("Master → command {}", msg.getExecuteCommand().getCommand());
            }

            case RESPONSE -> {
                log.info("Master → response {}", msg.getResponse().getMessage());
            }

            case SERVERSTATUSCHANGE -> {
                log.info("Master → serverStatusChange {}: {}",
                        msg.getServerStatusChange().getServer(),
                        msg.getServerStatusChange().getStatus());
            }

            case TYPECHANGES -> {
                log.info("Master → typeChanges");
                ServerManager.getInstance().reloadServerTypes(
                        msg.getTypeChanges().getTypesList().stream().map(RCGenericProto.Type::getConfig).toList()
                );
            }

            case TEMPLATECHANGES -> {
                log.info("Master → templateChanges");
                ServerManager.getInstance().reloadTemplates(
                        msg.getTemplateChanges().getTemplatesList().stream().map(RCGenericProto.Template::getData).toList()
                );
            }

            default -> log.warn("Master → Unknown message: {}", msg.getPayloadCase());
        }
    }

    @Override
    public void onError(Throwable t) {
        log.error("Stream error: {}", t.getMessage());
        ClusterClient.getInstance().setStreamActive(false);
    }

    @Override
    public void onCompleted() {
        log.warn("Master closed stream.");
        ClusterClient.getInstance().setStreamActive(false);
    }
}
