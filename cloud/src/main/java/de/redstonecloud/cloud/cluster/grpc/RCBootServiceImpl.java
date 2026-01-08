package de.redstonecloud.cloud.cluster.grpc;

import de.redstonecloud.api.RCBootProto;
import de.redstonecloud.api.RCBootServiceGrpc;
import de.redstonecloud.api.RCGenericProto;
import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.cluster.ClusterManager;
import de.redstonecloud.cloud.cluster.ClusterNode;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.log4j.Log4j2;

import java.util.List;

@Log4j2
public class RCBootServiceImpl extends RCBootServiceGrpc.RCBootServiceImplBase {

    @Override
    public void login(RCBootProto.AuthRequest request,
                      StreamObserver<RCBootProto.LoginResponse> responseObserver) {

        log.info("Node login attempt: {}", request.getNodeId());

        ClusterManager clusterManager = ClusterManager.getInstance();
        String token;
        if (clusterManager.hasNode(request.getNodeId())) {
            log.info("Node {} reconnected!", request.getNodeId());
            token = clusterManager.getNodeById(request.getNodeId()).getToken();
        } else {
            log.info("Node {} connected", request.getNodeId());
            token = "session-" + request.getNodeId() + "-" + System.currentTimeMillis();

            //TODO: add name
            clusterManager.addNode(new ClusterNode("Name", request.getNodeId(), token, null, false, request.getHostname()));
        }

        List<RCGenericProto.Type> types = RedstoneCloud.getInstance().getServerManager().getTypes().clone().values().stream().map(type -> RCGenericProto.Type.newBuilder()
                .setName(type.name())
                .setConfig(type.raw())
                .build()
        ).toList();

        List<RCGenericProto.Template> templates = RedstoneCloud.getInstance().getServerManager().getTemplatesForNode(request.getNodeId()).stream().map(template -> RCGenericProto.Template.newBuilder()
                .setName(template.getName())
                .setData(template.getRaw())
                .build()
        ).toList();

        RCBootProto.LoginResponse response = RCBootProto.LoginResponse.newBuilder()
                .setStatus(RCBootProto.Status.SUCCESS)
                .setToken(token)
                .addAllTypes(types)
                .addAllTemplates(templates)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
