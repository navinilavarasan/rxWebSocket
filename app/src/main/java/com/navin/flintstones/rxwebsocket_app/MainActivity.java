package com.navin.flintstones.rxwebsocket_app;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import com.navin.flintstones.rxwebsocket.RxWebsocket;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.internal.functions.Functions;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import static io.reactivex.internal.functions.Functions.emptyConsumer;
import static io.reactivex.schedulers.Schedulers.io;

public class MainActivity extends Activity {

    @BindView(R.id.location)
    EditText location;

    @BindView(R.id.send_message)
    EditText sendMessage;

    @BindView(R.id.recd_message)
    TextView recdMessage;

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
        OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder();

        okHttpClientBuilder.addInterceptor(chain -> {
            Request original = chain.request();
            Request.Builder requestBuilder = original.newBuilder();
            requestBuilder.addHeader("Authorization", "Bearer ")
                          .build();

            return chain.proceed(requestBuilder.build());
        });

        websocket = new RxWebsocket.Builder()
            .addConverterFactory(WebSocketConverterFactory.create())
            .addReceiveInterceptor(data -> "INTERCEPTED:" + data)
            .build(okHttpClientBuilder.build(), location.getText().toString());
        logEvents();
    }

    private void logEvents() {
        websocket.eventStream()
                 .observeOn(AndroidSchedulers.mainThread())
                 .doOnNext(event -> {
                     if (event instanceof RxWebsocket.Open) {
                         log("CONNECTED");
                         logNewLine();
                     } else if (event instanceof RxWebsocket.Closed) {
                         log("DISCONNECTED");
                         logNewLine();
                     } else if (event instanceof RxWebsocket.QueuedMessage) {
                         log("[MESSAGE QUEUED]:" + ((RxWebsocket.QueuedMessage) event).message().toString());
                         logNewLine();
                     } else if (event instanceof RxWebsocket.Message) {
                         try {
                             log("[DE-SERIALIZED MESSAGE RECEIVED]:" + ((RxWebsocket.Message) event).data(SampleDataModel.class).toString());
                             log(String.format("[DE-SERIALIZED MESSAGE RECEIVED][id]:%d", ((RxWebsocket.Message) event).data(SampleDataModel.class).id()));
                             log(String.format(
                                 "[DE-SERIALIZED MESSAGE RECEIVED][message]:%s",
                                 ((RxWebsocket.Message) event).data(SampleDataModel.class).message()
                             ));
                             logNewLine();
                         } catch (Throwable throwable) {
                             log("[MESSAGE RECEIVED]:" + ((RxWebsocket.Message) event).data().toString());
                             logNewLine();
                         }
                     }
                 })
                 .subscribeOn(Schedulers.io())
                 .subscribe(emptyConsumer(), this::logError);
    }

    private void logNewLine() {
        recdMessage.setText(String.format("%s\n", recdMessage.getText()));
    }


    private void logError(Throwable throwable) {
        recdMessage.setText(String.format("%s%s", recdMessage.getText(), String.format("\n[%s]:[ERROR]%s", getCurrentTime(), throwable.getMessage())));
    }

    private void log(String text) {
        recdMessage.setText(String.format("%s%s", recdMessage.getText(), String.format("\n[%s]:%s", getCurrentTime(), text)));
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
                     this::logError
                 );
    }

    @OnClick({R.id.disconnect})
    void onDisconnect() {
        if (websocket != null) {
            websocket.disconnect(1000, "Disconnect")
                     .observeOn(AndroidSchedulers.mainThread())
                     .subscribe(
                         event -> Log.d(MainActivity.class.getSimpleName(), event.toString()),
                         this::logError
                     );
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
                    this::logError
                );
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
                    this::logError
                );
        }
    }


    @OnClick({R.id.clear})
    void onClear() {
        recdMessage.setText("");
    }
}
