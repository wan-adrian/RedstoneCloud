package de.redstonecloud.node.cluster.grpc;

import de.redstonecloud.api.RCGenericProto;
import de.redstonecloud.api.RCNodeProto;
import de.redstonecloud.api.RCNodeServiceGrpc;
import de.redstonecloud.node.cluster.ClusterClient;
import io.grpc.stub.StreamObserver;

public class RCNodeServiceImpl extends RCNodeServiceGrpc.RCNodeServiceImplBase {
    @Override
    public void prepareServer(RCNodeProto.PrepareServer request,
                              StreamObserver<RCGenericProto.GenericResponse> responseObserver) {

        String template = request.getTemplate();


        System.out.println("[prepareServer] template=" + template);

        // Build response (example)
        RCGenericProto.GenericResponse response = RCGenericProto.GenericResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Server prepared")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void startServer(RCNodeProto.StartServer request,
                            StreamObserver<RCGenericProto.GenericResponse> responseObserver) {

        String token = request.getToken();
        String server = request.getServer();

        System.out.println("[startServer] token=" + token + " server=" + server);

        RCGenericProto.GenericResponse response = RCGenericProto.GenericResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Server started")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void stopServer(RCNodeProto.StopServer request,
                           StreamObserver<RCGenericProto.GenericResponse> responseObserver) {

        String token = request.getToken();
        String server = request.getServer();
        boolean kill = request.getKill();

        System.out.println("[stopServer] token=" + token +
                " server=" + server +
                " kill=" + kill);

        RCGenericProto.GenericResponse response = RCGenericProto.GenericResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Server stopped")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void executeCommand(RCNodeProto.ExecuteCommand request,
                               StreamObserver<RCGenericProto.GenericResponse> responseObserver) {

        String token = request.getToken();
        String server = request.getServer();
        String command = request.getCommand();

        System.out.println("[executeCommand] token=" + token +
                " server=" + server +
                " command=" + command);

        RCGenericProto.GenericResponse response = RCGenericProto.GenericResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Command executed")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
