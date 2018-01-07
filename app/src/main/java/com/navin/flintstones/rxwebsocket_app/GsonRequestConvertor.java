package com.navin.flintstones.rxwebsocket_app;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.navin.flintstones.rxwebsocket.WebSocketConverter;

import java.io.IOException;
import java.nio.charset.Charset;

public class GsonRequestConvertor<T> implements WebSocketConverter<T, String> {
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private final Gson gson;
    private final TypeAdapter<T> adapter;

    GsonRequestConvertor(Gson gson, TypeAdapter<T> adapter) {
        this.gson = gson;
        this.adapter = adapter;
    }

    @Override
    public String convert(T value) throws IOException {
        return adapter.toJson(value);
    }
}
