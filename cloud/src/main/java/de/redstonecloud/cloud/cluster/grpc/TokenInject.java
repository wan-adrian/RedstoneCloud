package de.redstonecloud.cloud.cluster.grpc;

import io.grpc.*;

public class TokenInject implements ClientInterceptor {
    private final String token;

    public TokenInject(String token) {
        this.token = token;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {

        return new ForwardingClientCall.SimpleForwardingClientCall<>(
                next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                headers.put(Metadata.Key.of("auth-token", Metadata.ASCII_STRING_MARSHALLER), token);
                super.start(responseListener, headers);
            }
        };
    }
}

