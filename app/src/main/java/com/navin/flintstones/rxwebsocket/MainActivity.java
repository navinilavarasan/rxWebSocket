package com.navin.flintstones.rxwebsocket;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.EditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class MainActivity extends Activity {

    @BindView(R.id.location)
    EditText location;

    @BindView(R.id.send_message)
    EditText sendMessage;

    @BindView(R.id.recd_message)
    EditText recdMessage;

    private RxWebsocket websocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        ButterKnife.bind(this);
    }

    private void openWebsocket() {
        websocket = new RxWebsocket.Builder().build(location.getText().toString());
        logEvents();
    }

    private void logEvents() {
        websocket.eventStream()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(event -> {
                    if (event instanceof RxWebsocket.Open) {
                        log("CONNECTED");
                    } else if (event instanceof RxWebsocket.Closed) {
                        log("DISCONNECTED");
                    } else if (event instanceof RxWebsocket.QueuedMessage) {
                        log("[MESSAGE QUEUED]:" + ((RxWebsocket.QueuedMessage) event).message().toString());
                    } else if (event instanceof RxWebsocket.Message) {
                        log("[MESSAGE RECEIVED]:" + ((RxWebsocket.Message) event).message().toString());
                    }
                })
                .subscribe();
    }


    private void logError(Throwable throwable) {
        recdMessage.setText(recdMessage.getText() + String.format("\n[%s]:[ERROR]%s", getCurrentTime(), throwable.getMessage()));
    }

    private void log(String text) {
        recdMessage.setText(recdMessage.getText() + String.format("\n[%s]:%s", getCurrentTime(), text));
    }

    private String getCurrentTime() {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(c.getTime());
    }

    @OnClick({R.id.connect})
    void onConnect() {
        openWebsocket();
        websocket.connect()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        event -> Log.d(MainActivity.class.getSimpleName(), event.toString()),
                        this::logError);
    }

    @OnClick({R.id.disconnect})
    void onDisconnect() {
        if (websocket != null) {
            websocket.disconnect(1000, "Disconnect")
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            event -> Log.d(MainActivity.class.getSimpleName(), event.toString()),
                            this::logError);
        }
    }

    @OnClick({R.id.send})
    void onSend() {
        if (websocket != null) {
            websocket
                    .send(sendMessage.getText().toString())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            event -> Log.d(MainActivity.class.getSimpleName(), event.toString()),
                            this::logError);
        }
    }
}
