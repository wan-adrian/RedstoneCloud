package de.redstonecloud.cloud.cluster.grpc;

import de.redstonecloud.api.RCBootProto;
import de.redstonecloud.api.RCBootServiceGrpc;
import de.redstonecloud.api.RCGenericProto;
import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.cluster.ClusterManager;
import de.redstonecloud.cloud.cluster.ClusterNode;
import de.redstonecloud.cloud.config.entires.RedisSettings;
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
        if (!clusterManager.isAllowedNode(request.getNodeId())) {
            log.warn("Rejected node login for unknown node id {}", request.getNodeId());
            RCBootProto.LoginResponse response = RCBootProto.LoginResponse.newBuilder()
                    .setStatus(RCBootProto.Status.FAILURE)
                    .setMessage("Unknown node id")
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return;
        }

        String token;
        if (clusterManager.hasNode(request.getNodeId())) {
            log.info("Node {} reconnected!", request.getNodeId());
            ClusterNode node = clusterManager.getNodeById(request.getNodeId());
            token = node.getToken();
            node.setAddress(request.getHostname());
            node.setShuttingDown(false);
            node.touch();
        } else {
            log.info("Node {} connected", request.getNodeId());
            token = "session-" + request.getNodeId() + "-" + System.currentTimeMillis();

            String name = clusterManager.getNodeNameById(request.getNodeId());
            ClusterNode node = new ClusterNode(name, request.getNodeId(), token, null, false, request.getHostname());
            node.touch();
            clusterManager.addNode(node);
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

        RedisSettings redis = RedstoneCloud.getConfig().redis();

        RCBootProto.LoginResponse response = RCBootProto.LoginResponse.newBuilder()
                .setStatus(RCBootProto.Status.SUCCESS)
                .setToken(token)
                .addAllTypes(types)
                .addAllTemplates(templates)
                .setRedisIp(redis.connectIp())
                .setRedisPort(redis.port())
                .setRedisDb(redis.dbId())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
