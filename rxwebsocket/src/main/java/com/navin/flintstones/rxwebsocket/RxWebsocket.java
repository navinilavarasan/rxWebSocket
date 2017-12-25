package com.navin.flintstones.rxwebsocket;

import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.processors.PublishProcessor;

/**
 * Created by navin ilavarasan on 25/12/17.
 */
public class RxWebsocket {

    interface Event {
        RxWebsocket client();
    }

    public class Open implements Event {
        @Override
        public RxWebsocket client() {
            return RxWebsocket.this;
        }
    }

    public class Message implements Event {
        @Override
        public RxWebsocket client() {
            return RxWebsocket.this;
        }
    }


    public class Closed implements Event {
        @Override
        public RxWebsocket client() {
            return RxWebsocket.this;
        }
    }

    private PublishProcessor<Event> eventStream = PublishProcessor.create();

    public Single<Open> connect() {
        return eventStream()
                .doOnSubscribe(d -> doConnect())
                .ofType(Open.class)
                .firstOrError();
    }

    public Flowable<Message> listen() {
        return eventStream()
                .ofType(Message.class);
    }

    public Flowable<Closed> disconnect() {
        return eventStream()
                .doOnSubscribe(d -> doDiconnect())
                .ofType(Closed.class);
    }

    public Flowable<Event> eventStream() {
        return eventStream;
    }

    private void doConnect() {

    }

    private void doDiconnect() {

    }
}
