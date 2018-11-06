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

