/*
 *  Copyright (C) Allegion - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *
 */

package com.navin.flintstones.rxwebsocket;

import java.lang.reflect.Type;

public interface WebSocketConverter<F, T> {
    T convert(F value) throws Throwable;

    /**
     * Creates converter instances based on a type and target usage.
     */
    abstract class Factory {

        public WebSocketConverter<String, ?> responseBodyConverter(Type type) {
            return null;
        }

        public WebSocketConverter<?, String> requestBodyConverter(Type type) {
            return null;
        }
    }
}
