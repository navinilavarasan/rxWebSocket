package com.navin.flintstones.rxwebsocket;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.annotations.NonNull;
import io.reactivex.processors.BehaviorProcessor;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class RxWebsocket {

    private Request request;

    private List<WebSocketConverter.Factory> converterFactories  = new ArrayList<>();
    private List<WebSocketInterceptor>       receiveInterceptors = new ArrayList<>();

    @Nullable
    private WebSocket originalWebsocket;

    private boolean userRequestedClose = false;

    private OkHttpClient okHttpClient;

    public interface Event {
        RxWebsocket client();
    }

    public class Open implements Event {
        private final Maybe<Response> response;

        public Open(Response response) {
            this.response = Maybe.just(response);
        }

        public Open() {
            this.response = Maybe.empty();
        }

        @Nullable
        public Response response() {
            return response.blockingGet();
        }

        @Override
        public RxWebsocket client() {
            return RxWebsocket.this;
        }
    }

    public class Message implements Event {
        private final String     message;
        private final ByteString messageBytes;

        public Message(String message) {
            this.message = message;
            this.messageBytes = null;
        }

        public Message(ByteString messageBytes) {
            this.messageBytes = messageBytes;
            this.message = null;
        }

        @Nullable
        public String data() {
            String interceptedMessage = message;
            for (WebSocketInterceptor interceptor : receiveInterceptors) {
                interceptedMessage = interceptor.intercept(interceptedMessage);
            }
            return interceptedMessage;
        }

        @Nullable
        public ByteString dataBytes() {
            return messageBytes;
        }

        @NonNull
        private String dataOrDataBytesAsString() {
            if (data() == null && dataBytes() == null) {
                return "";
            }
            if (dataBytes() == null) {
                return data();
            }

            if (data() == null) {
                return dataBytes() == null ? "" : dataBytes().utf8();
            }

            return "";
        }

        public <T> T data(Class<? extends T> type) throws Throwable {
            WebSocketConverter<String, T> converter = responseConverter(type);
            if (converter != null) {
                return converter.convert(dataOrDataBytesAsString());
            } else {
                throw new Exception("No converters available to convert the enqueued object");
            }
        }

        @Override
        public RxWebsocket client() {
            return RxWebsocket.this;
        }
    }

    public class QueuedMessage<T> implements Event {
        private final T message;

        public QueuedMessage(T message) {
            this.message = message;
        }


        @Nullable
        public T message() {
            return message;
        }

        @Override
        public RxWebsocket client() {
            return RxWebsocket.this;
        }
    }


    public class Closed extends Throwable implements Event {
        public static final int    INTERNAL_ERROR = 500;
        private final       String reason;
        private final       int    code;

        public Closed(int code, String reason) {
            this.code = code;
            this.reason = reason;
        }

        public int code() {
            return code;
        }

        public String reason() {
            return reason;
        }

        @Override
        public String getMessage() {
            return reason();
        }

        @Override
        public RxWebsocket client() {
            return RxWebsocket.this;
        }
    }

    private PublishProcessor<Event> eventStream = PublishProcessor.create();

    public Single<Open> connect() {
        return eventStream()
            .subscribeOn(Schedulers.io())
            .doOnSubscribe(d -> doConnect())
            .ofType(Open.class)
            .firstOrError();
    }

    public Flowable<Message> listen() {
        return eventStream()
            .subscribeOn(Schedulers.io())
            .ofType(Message.class);
    }

    public Single<QueuedMessage> send(byte[] message) {
        return eventStream()
            .subscribeOn(Schedulers.io())
            .doOnSubscribe(d -> doQueueMessage(message))
            .ofType(QueuedMessage.class)
            .firstOrError();
    }

    public <T> Single<QueuedMessage> send(final T message) {
        return eventStream()
            .subscribeOn(Schedulers.io())
            .doOnSubscribe(d -> doQueueMessage(message))
            .ofType(QueuedMessage.class)
            .firstOrError();
    }

    public Single<Closed> disconnect(int code, String reason) {
        return eventStream()
            .subscribeOn(Schedulers.io())
            .doOnSubscribe(d -> doDisconnect(code, reason))
            .ofType(Closed.class)
            .firstOrError();
    }

    public Flowable<Event> eventStream() {
        return eventStream;
    }

    private void doConnect() {
        if (originalWebsocket != null) {
            if (eventStream.hasSubscribers()) {
                eventStream.onNext(new Open());
            }
            return;
        }
        if (okHttpClient == null) {
            okHttpClient = new OkHttpClient.Builder().build();
        }
        okHttpClient.newWebSocket(request, webSocketListener());
    }

    private void doDisconnect(int code, String reason) {
        requireNotNull(originalWebsocket, "Expected an open websocket");
        userRequestedClose = true;
        if (originalWebsocket != null) {
            originalWebsocket.close(code, reason);
        }
    }

    private void doQueueMessage(byte[] message) {
        requireNotNull(originalWebsocket, "Expected an open websocket");
        requireNotNull(message, "Expected a non null message");
        if (originalWebsocket.send(ByteString.of(message))) {
            if (eventStream.hasSubscribers()) {
                eventStream.onNext(new QueuedMessage(ByteString.of(message)));
            }
        }
    }

    private <T> void doQueueMessage(T message) {
        requireNotNull(originalWebsocket, "Expected an open websocket");
        requireNotNull(message, "Expected a non null message");

        WebSocketConverter<T, String> converter = requestConverter(message.getClass());
        if (converter != null) {
            try {
                if (originalWebsocket.send(converter.convert(message))) {
                    if (eventStream.hasSubscribers()) {
                        eventStream.onNext(new QueuedMessage(message));
                    }
                }
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        } else if (message instanceof String) {
            if (originalWebsocket.send((String) message)) {
                if (eventStream.hasSubscribers()) {
                    eventStream.onNext(new QueuedMessage(message));
                }
            }
        }
    }

    private void setClient(WebSocket originalWebsocket) {
        this.originalWebsocket = originalWebsocket;
        userRequestedClose = false;
    }

    private WebSocketListener webSocketListener() {
        return new WebSocketListener() {

            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                super.onOpen(webSocket, response);

                setClient(webSocket);

                if (eventStream.hasSubscribers()) {
                    eventStream.onNext(new Open(response));
                }
            }

            @Override
            public void onMessage(WebSocket webSocket, String message) {
                super.onMessage(webSocket, message);
                if (eventStream.hasSubscribers()) {
                    eventStream.onNext(new Message(message));
                }
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString messageBytes) {
                super.onMessage(webSocket, messageBytes);
                if (eventStream.hasSubscribers()) {
                    eventStream.onNext(new Message(messageBytes));
                }
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                super.onClosed(webSocket, code, reason);
                if (userRequestedClose) {
                    if (eventStream.hasSubscribers()) {
                        eventStream.onNext(new Closed(code, reason));
                        eventStream.onComplete();
                    }
                } else {
                    if (eventStream.hasSubscribers()) {
                        eventStream.onError(new Closed(code, reason));
                    }
                }
                setClient(null);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, @Nullable Response response) {
                super.onFailure(webSocket, t, response);
                if (eventStream.hasSubscribers()) {
                    eventStream.onError(t);
                }
                setClient(null);
            }
        };
    }

    private <T> WebSocketConverter<String, T> responseConverter(final Type type) {
        for (WebSocketConverter.Factory converterFactory : converterFactories) {
            WebSocketConverter<String, ?> converter =
                converterFactory.responseBodyConverter(type);
            if (converter != null) {
                return (WebSocketConverter<String, T>) converter;
            }
        }
        return null;
    }

    private <T> WebSocketConverter<T, String> requestConverter(final Type type) {
        for (WebSocketConverter.Factory converterFactory : converterFactories) {
            WebSocketConverter<?, String> converter =
                converterFactory.requestBodyConverter(type);
            if (converter != null) {
                return (WebSocketConverter<T, String>) converter;
            }
        }
        return null;
    }

    private static <T> T requireNotNull(T object, String message) {
        if (object == null) {
            throw new IllegalStateException(message);
        }
        return object;
    }

    /**
     * Builder class for creating rx websockets.
     */
    public static class Builder {
        private List<WebSocketConverter.Factory> converterFactories  = new ArrayList<>();
        private List<WebSocketInterceptor>       receiveInterceptors = new ArrayList<>();

        @NonNull
        public Builder addConverterFactory(WebSocketConverter.Factory factory) {
            if (factory != null) {
                converterFactories.add(factory);
            }
            return this;
        }

        @NonNull
        public Builder addReceiveInterceptor(WebSocketInterceptor receiveInterceptor) {
            receiveInterceptors.add(receiveInterceptor);
            return this;
        }

        @NonNull
        public RxWebsocket build(@NonNull OkHttpClient okHttpClient, @NonNull Request request) {
            if (request == null) {
                throw new IllegalStateException("Request cannot be null");
            }

            RxWebsocket rxWebsocket = new RxWebsocket();
            rxWebsocket.request = request;
            rxWebsocket.converterFactories = converterFactories;
            rxWebsocket.receiveInterceptors = receiveInterceptors;
            rxWebsocket.okHttpClient = okHttpClient;
            return rxWebsocket;
        }

        @NonNull
        public RxWebsocket build(@NonNull OkHttpClient okHttpClient, @NonNull String wssUrl) {
            if (wssUrl == null || wssUrl.isEmpty()) {
                throw new IllegalStateException("Websocket address cannot be null or empty");
            }

            RxWebsocket rxWebsocket = new RxWebsocket();
            rxWebsocket.converterFactories = converterFactories;
            rxWebsocket.receiveInterceptors = receiveInterceptors;
            rxWebsocket.request = new Request.Builder().url(wssUrl).get().build();
            rxWebsocket.okHttpClient = okHttpClient;
            return rxWebsocket;
        }
    }
}
