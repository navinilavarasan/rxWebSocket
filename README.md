rxWebSocket [![](https://img.shields.io/badge/Android%20Arsenal-rxWebsocket-brightgreen.svg?style=flat)](https://android-arsenal.com/details/1/6630) [![Codacy Badge](https://api.codacy.com/project/badge/grade/e93c6273804a4dbc91a298a66fed99be)](https://www.codacy.com/app/aesshoferi/EasyIntro)
===========
rxWebSocket is a simple reactive extension of OkHttp Websocket interface with support for Converter Factories and Interceptors.

## Download
**Step 1. Add the JitPack repository to your build file**
Add it in your root build.gradle at the end of repositories:

```gradle
allprojects {
    repositories {
    ...
    maven { url 'https://jitpack.io' }
    }
}
```

**Step 2. Add the dependency**
```gradle
dependencies {
    implementation 'io.github.meness:rxWebSocket:v1.02'
}
```
## Usage
**To Create a WebSocket with no converters:**
```java
websocket = new RxWebsocket.Builder()
            .build("wss://echo.websocket.org");
```

**To Create a WebSocket with converters(See sample application to add a simple Gson converter or write your own):**
```java
websocket = new RxWebsocket.Builder()
            .addConverterFactory() //Your converter
            .addReceiveInterceptor(data -> {}) //Intercept the received data
            .build("wss://echo.websocket.org");
```

**To Connect to the websocket:**
```java
websocket.connect()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::logEvent, this::logError);
```

**To Connect and Send data on a connected socket:**
```java
websocket.connect()
         .flatMap(open -> open.client().send("Hello"))
         .observeOn(AndroidSchedulers.mainThread())
         .subscribe(this::logEvent, this::logError);
```

**To Connect and Listen data on a connected socket:**
```java
websocket.connect()
         .flatMapPublisher(open -> open.client().listen())
         .observeOn(AndroidSchedulers.mainThread())
         .subscribe(this::logEvent, this::logError);
```

## License
    Copyright 2018 Alireza Eskandarpour

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.