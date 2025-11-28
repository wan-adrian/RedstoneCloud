package de.redstonecloud.node.cluster.grpc;

import de.redstonecloud.api.RCGenericProto;
import de.redstonecloud.api.RCMasterProto;
import de.redstonecloud.api.RCMasterServiceGrpc;
import de.redstonecloud.node.cluster.ClusterClient;
import io.grpc.stub.StreamObserver;

public class RCMaster {
    public static void serverDied(String serverName) {
        RCMasterProto.ServerDied serverDied = RCMasterProto.ServerDied.newBuilder()
                .setServer(serverName)
                .build();

        RCMasterServiceGrpc.newStub(ClusterClient.getInstance().getChannel()).serverDied(serverDied, new StreamObserver<RCGenericProto.Empty>() {
            @Override
            public void onNext(RCGenericProto.Empty empty) {

            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onCompleted() {

            }
        });
    }
}
