package de.redstonecloud.cloud.cluster.grpc;

import de.redstonecloud.api.RCGenericProto;
import de.redstonecloud.api.RCNodeProto;
import de.redstonecloud.cloud.cluster.ClusterManager;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.CompletableFuture;

public class RCNode {
    private String nodeId;

    public RCNode(String nodeId) {
        this.nodeId = nodeId;
    }

    public RCGenericProto.GenericResponse prepareServer(String template, String name) {
        RCNodeProto.PrepareServer request = RCNodeProto.PrepareServer.newBuilder()
                .setTemplate(template)
                .setName(name)
                .build();

        CompletableFuture<RCGenericProto.GenericResponse> response = new CompletableFuture<>();

        ClusterManager.getInstance().getNodeStub(nodeId).prepareServer(request, new StreamObserver<RCGenericProto.GenericResponse>() {
            @Override
            public void onNext(RCGenericProto.GenericResponse value) {
                response.complete(value);
            }

            @Override
            public void onError(Throwable t) {
                // Handle error
            }

            @Override
            public void onCompleted() {
                // Completed
            }
        });

        return response.join();
    }

    public RCGenericProto.GenericResponse startServer(String server) {
        RCNodeProto.StartServer request = RCNodeProto.StartServer.newBuilder()
                .setServer(server)
                .build();

        CompletableFuture<RCGenericProto.GenericResponse> response = new CompletableFuture<>();

        ClusterManager.getInstance().getNodeStub(nodeId).startServer(request, new StreamObserver<RCGenericProto.GenericResponse>() {
            @Override
            public void onNext(RCGenericProto.GenericResponse value) {
                response.complete(value);
            }

            @Override
            public void onError(Throwable t) {
                // Handle error
            }

            @Override
            public void onCompleted() {
                // Completed
            }
        });

        return response.join();
    }

    public RCGenericProto.GenericResponse stopServer(String server, boolean kill) {
        RCNodeProto.StopServer request = RCNodeProto.StopServer.newBuilder()
                .setServer(server)
                .setKill(kill)
                .build();

        CompletableFuture<RCGenericProto.GenericResponse> response = new CompletableFuture<>();

        ClusterManager.getInstance().getNodeStub(nodeId).stopServer(request, new StreamObserver<RCGenericProto.GenericResponse>() {
            @Override
            public void onNext(RCGenericProto.GenericResponse value) {
                response.complete(value);
            }

            @Override
            public void onError(Throwable t) {
                // Handle error
            }

            @Override
            public void onCompleted() {
                // Completed
            }
        });

        return response.join();
    }

    public RCGenericProto.GenericResponse executeCommand(String server, String command) {
        RCNodeProto.ExecuteCommand request = RCNodeProto.ExecuteCommand.newBuilder()
                .setServer(server)
                .setCommand(command)
                .build();

        CompletableFuture<RCGenericProto.GenericResponse> response = new CompletableFuture<>();

        ClusterManager.getInstance().getNodeStub(nodeId).executeCommand(request, new StreamObserver<RCGenericProto.GenericResponse>() {
            @Override
            public void onNext(RCGenericProto.GenericResponse value) {
                response.complete(value);
            }

            @Override
            public void onError(Throwable t) {
                // Handle error
            }

            @Override
            public void onCompleted() {
                // Completed
            }
        });

        return response.join();
    }
}
