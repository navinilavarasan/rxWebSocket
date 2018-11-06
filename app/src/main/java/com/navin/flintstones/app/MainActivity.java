package com.navin.flintstones.app;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import com.navin.flintstones.rxwebsocket.RxWebSocket;

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
    TextView recdMessage;

    private RxWebSocket websocket;

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
        websocket = new RxWebSocket.Builder()
                .addConverterFactory(WebSocketConverterFactory.create())
                .addReceiveInterceptor(data -> "INTERCEPTED:" + data)
                .build(location.getText().toString());
        logEvents();
    }

    private void logEvents() {
        websocket.eventStream()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(event -> {
                    if (event instanceof RxWebSocket.Open) {
                        log("CONNECTED");
                        logNewLine();
                    } else if (event instanceof RxWebSocket.Closed) {
                        log("DISCONNECTED");
                        logNewLine();
                    } else if (event instanceof RxWebSocket.QueuedMessage) {
                        log("[MESSAGE QUEUED]:" + ((RxWebSocket.QueuedMessage) event).message().toString());
                        logNewLine();
                    } else if (event instanceof RxWebSocket.Message) {
                        try {
                            log("[DE-SERIALIZED MESSAGE RECEIVED]:" + ((RxWebSocket.Message) event).data(SampleDataModel.class).toString());
                            log(String.format("[DE-SERIALIZED MESSAGE RECEIVED][id]:%d", ((RxWebSocket.Message) event).data(SampleDataModel.class).id()));
                            log(String.format("[DE-SERIALIZED MESSAGE RECEIVED][message]:%s", ((RxWebSocket.Message) event).data(SampleDataModel.class).message()));
                            logNewLine();
                        } catch (Throwable throwable) {
                            log("[MESSAGE RECEIVED]:" + ((RxWebSocket.Message) event).data().toString());
                            logNewLine();
                        }
                    }
                })
                .subscribe(event -> {
                }, this::logError);
    }

    private void logNewLine() {
        recdMessage.setText(recdMessage.getText() + "\n");
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

    @OnClick({R.id.send_sample_obj})
    void onSendObject() {
        if (websocket != null) {
            websocket
                    .send(new SampleDataModel(1, "sample object"))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            event -> Log.d(MainActivity.class.getSimpleName(), event.toString()),
                            this::logError);
        }
    }


    @OnClick({R.id.clear})
    void onClear() {
        recdMessage.setText("");
    }
}
