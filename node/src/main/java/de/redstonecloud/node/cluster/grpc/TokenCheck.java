package de.redstonecloud.node.cluster.grpc;

import io.grpc.*;

public class TokenCheck implements ServerInterceptor {

    private final String expectedToken;

    public TokenCheck(String expectedToken) {
        this.expectedToken = expectedToken;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String token = headers.get(Metadata.Key.of("auth-token", Metadata.ASCII_STRING_MARSHALLER));

        if (token == null || !token.equals(expectedToken)) {
            call.close(Status.UNAUTHENTICATED.withDescription("Invalid auth token"), headers);
            return new ServerCall.Listener<>() {}; // stop processing
        }

        // token is valid → continue normally
        return next.startCall(call, headers);
    }
}
