package com.navin.flintstones.rxwebsocket_app;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.navin.flintstones.rxwebsocket.WebSocketConverter;

import java.lang.reflect.Type;

public final class WebSocketConverterFactory extends WebSocketConverter.Factory {
    public static WebSocketConverterFactory create() {
        return create(new Gson());
    }

    public static WebSocketConverterFactory create(Gson gson) {
        return new WebSocketConverterFactory(gson);
    }

    private final Gson gson;

    private WebSocketConverterFactory(Gson gson) {
        if (gson == null) throw new NullPointerException("gson == null");
        this.gson = gson;
    }

    @Override
    public WebSocketConverter<String, ?> responseBodyConverter(Type type) {
        TypeAdapter<?> adapter = gson.getAdapter(TypeToken.get(type));
        return new GsonResponseConvertor(gson, adapter);
    }

    @Override
    public WebSocketConverter<?, String> requestBodyConverter(Type type) {
        TypeAdapter<?> adapter = gson.getAdapter(TypeToken.get(type));
        return new GsonRequestConvertor<>(gson, adapter);
    }
}
