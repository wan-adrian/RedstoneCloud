package de.redstonecloud.node.cluster;

import de.redstonecloud.node.cluster.grpc.RCNodeServiceImpl;
import de.redstonecloud.node.cluster.grpc.TokenCheck;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

@Log4j2
@Getter
public class NodeServer {
    private Server server;
    private static NodeServer INSTANCE = null;
    @Setter
    private String expectedToken = null;
    //random port between 49152 and 65535
    private int port = ThreadLocalRandom.current().nextInt(49152, 65536);

    public static NodeServer getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new NodeServer();
        }
        return INSTANCE;
    }

    private NodeServer() {}

    public void start() {
        try {
            server = ServerBuilder.forPort(port)
                    .addService(new RCNodeServiceImpl())
                    .intercept(new TokenCheck(expectedToken))
                    .build()
                    .start();

            log.info("Node server is listening on {}", port);

        } catch (IOException e) {
            log.error("Failed to start gRPC server", e);
        }
    }
}
