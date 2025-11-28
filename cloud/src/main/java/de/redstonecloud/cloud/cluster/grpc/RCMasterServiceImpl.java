package de.redstonecloud.cloud.cluster.grpc;

import de.redstonecloud.api.RCGenericProto.Empty;
import de.redstonecloud.api.RCGenericProto;
import de.redstonecloud.api.RCMasterProto;
import de.redstonecloud.api.RCMasterServiceGrpc;
import io.grpc.stub.StreamObserver;

public class RCMasterServiceImpl extends RCMasterServiceGrpc.RCMasterServiceImplBase {
    @Override
    public void serverDied(RCMasterProto.ServerDied request,
                           StreamObserver<RCGenericProto.Empty> responseObserver) {

        String token = request.getToken();
        String server = request.getServer();

        System.out.println("Server died: " + server + " (token: " + token + ")");

        new RCNode("testnode1").prepareServer("t", "a");

        // respond with empty
        responseObserver.onNext(RCGenericProto.Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void serverStatusChange(RCMasterProto.ServerStatusChange request,
                                   StreamObserver<Empty> responseObserver) {

        String token = request.getToken();
        String server = request.getServer();
        String status = request.getStatus();

        System.out.println("Server status change: " + server + " -> " + status + " (token: " + token + ")");

        // respond with empty
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}
