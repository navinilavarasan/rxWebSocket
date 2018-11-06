package com.navin.flintstones.app;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import com.navin.flintstones.rxwebsocket.RxWebSocket;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

public class MainActivity extends Activity {

    @BindView(R.id.location)
    EditText location;

    @BindView(R.id.send_message)
    EditText sendMessage;

    @BindView(R.id.recd_message)
    TextView recdMessage;

    private RxWebSocket websocket;

    @NonNull
    private final CompositeDisposable mCompositeDisposable = new CompositeDisposable();

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
        mCompositeDisposable.add(websocket.eventStream()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(event -> {
                    if (event instanceof RxWebSocket.Open) {
                        logWithCurrentTime("CONNECTED");
                        logNewLine();
                    } else if (event instanceof RxWebSocket.Closed) {
                        logWithCurrentTime("DISCONNECTED");
                        logNewLine();
                    } else if (event instanceof RxWebSocket.QueuedMessage) {
                        Object message = ((RxWebSocket.QueuedMessage) event).message();
                        if (message == null) {
                            return;
                        }

                        logWithCurrentTime("[MESSAGE QUEUED]:" + message.toString());
                        logNewLine();
                    } else if (event instanceof RxWebSocket.Message) {
                        try {
                            logWithCurrentTime("[DE-SERIALIZED MESSAGE RECEIVED]:" + ((RxWebSocket.Message) event).data(SampleDataModel.class).toString());
                            logWithCurrentTime(String.format(Locale.US, "[DE-SERIALIZED MESSAGE RECEIVED][id]:%d", ((RxWebSocket.Message) event).data(SampleDataModel.class).id()));
                            logWithCurrentTime(String.format("[DE-SERIALIZED MESSAGE RECEIVED][message]:%s", ((RxWebSocket.Message) event).data(SampleDataModel.class).message()));
                            logNewLine();
                        } catch (Throwable throwable) {
                            logWithCurrentTime("[MESSAGE RECEIVED]:" + ((RxWebSocket.Message) event).data());
                            logNewLine();
                        }
                    }
                })
                .subscribe(event -> {
                }, this::logError));
    }

    private void logNewLine() {
        log("\n");
    }

    private void log(String s) {
        StringBuilder stringBuilder = new StringBuilder(recdMessage.getText());
        stringBuilder.append(s);
        recdMessage.setText(stringBuilder);
    }


    private void logError(Throwable throwable) {
        log(String.format("\n[%s]:[ERROR]%s", getCurrentTime(), throwable.getMessage()));
    }

    private void logWithCurrentTime(String text) {
        log(String.format("\n[%s]:%s", getCurrentTime(), text));
    }

    private String getCurrentTime() {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.US);
        return sdf.format(c.getTime());
    }

    @OnClick({R.id.connect})
    void onConnect() {
        openWebsocket();
        mCompositeDisposable.add(websocket.connect()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        event -> Log.d(MainActivity.class.getSimpleName(), event.toString()),
                        this::logError));
    }

    @OnClick({R.id.disconnect})
    void onDisconnect() {
        if (websocket != null) {
            mCompositeDisposable.add(websocket.disconnect(1000, "Disconnect")
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            event -> Log.d(MainActivity.class.getSimpleName(), event.toString()),
                            this::logError));
        }
    }

    @Override
    protected void onDestroy() {
        if (!mCompositeDisposable.isDisposed()) {
            mCompositeDisposable.dispose();
        }

        super.onDestroy();
    }

    @OnClick({R.id.send})
    void onSend() {
        if (websocket != null) {
            mCompositeDisposable.add(websocket
                    .send(sendMessage.getText().toString())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            event -> Log.d(MainActivity.class.getSimpleName(), event.toString()),
                            this::logError));
        }
    }

    @OnClick({R.id.send_sample_obj})
    void onSendObject() {
        if (websocket != null) {
            mCompositeDisposable.add(websocket
                    .send(new SampleDataModel(1, "sample object"))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            event -> Log.d(MainActivity.class.getSimpleName(), event.toString()),
                            this::logError));
        }
    }


    @OnClick({R.id.clear})
    void onClear() {
        recdMessage.setText("");
    }
}
