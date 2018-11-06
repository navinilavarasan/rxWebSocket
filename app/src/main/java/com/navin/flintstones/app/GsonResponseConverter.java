package com.navin.flintstones.app;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.navin.flintstones.rxwebsocket.WebSocketConverter;

import java.io.IOException;
import java.io.StringReader;

public class GsonResponseConverter<T> implements WebSocketConverter<String, T> {
    private final Gson gson;
    private final TypeAdapter<T> adapter;

    GsonResponseConverter(Gson gson, TypeAdapter<T> adapter) {
        this.gson = gson;
        this.adapter = adapter;
    }

    @Override
    public T convert(String value) throws IOException {
        try (JsonReader jsonReader = gson.newJsonReader(new StringReader(value))) {
            return adapter.read(jsonReader);
        }
    }
}

