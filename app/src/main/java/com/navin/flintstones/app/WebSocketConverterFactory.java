/*
 * Copyright 2018 Alireza Eskandarpour
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navin.flintstones.app;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.navin.flintstones.rxwebsocket.WebSocketConverter;

import java.lang.reflect.Type;

public final class WebSocketConverterFactory extends WebSocketConverter.Factory {
    private final Gson gson;

    private WebSocketConverterFactory(Gson gson) {
        if (gson == null) throw new NullPointerException("gson == null");
        this.gson = gson;
    }

    public static WebSocketConverterFactory create() {
        return create(new Gson());
    }

    public static WebSocketConverterFactory create(Gson gson) {
        return new WebSocketConverterFactory(gson);
    }

    @Override
    public WebSocketConverter<String, ?> responseBodyConverter(Type type) {
        TypeAdapter<?> adapter = gson.getAdapter(TypeToken.get(type));
        return new GsonResponseConverter<>(gson, adapter);
    }

    @Override
    public WebSocketConverter<?, String> requestBodyConverter(Type type) {
        TypeAdapter<?> adapter = gson.getAdapter(TypeToken.get(type));
        return new GsonRequestConverter<>(gson, adapter);
    }
}
