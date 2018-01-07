# rxWebSocket

rxWebSocket is a simple reactive extension of OkHttp Websocket interface with support for Convertor Factories and Interceptors.
<br><i>The library contains only 3 files and is meant to be very simple.</i>

# Usage

<b> To Create a WebSocket with no convertors: </b>
```
websocket = new RxWebsocket.Builder()
                .build("wss://echo.websocket.org");
```

<b> To Create a WebSocket with convertors(See sample application to add a simple Gson convertor or write your own):</b>
```
websocket = new RxWebsocket.Builder()
                .addConverterFactory(//YOUR OWN CONVERTOR)
		.addReceiveInterceptor(data -> //Intercept the received data)
                .build("wss://echo.websocket.org");
```
		
<b> To Connect to the websocket:</b>
```
websocket.connect()
         .observeOn(AndroidSchedulers.mainThread())
         .subscribe(
	  	this::logEvent,
                this::logError
	   );
```
	   
<b> To Connect and Send data on a connected socket:</b>
```
websocket.connect()
         .flatMap(open -> open.client().send("Hello"))
         .observeOn(AndroidSchedulers.mainThread())
         .subscribe(
         	this::logEvent,
                this::logError
	  );
```

<b> To Connect and Listen data on a connected socket:</b>
```
websocket.connect()
         .flatMapPublisher(open -> open.client().listen())
         .observeOn(AndroidSchedulers.mainThread())
         .subscribe(
         	this::logEvent,
                this::logError
	  );
```

# Download

<b>Step 1. Add the JitPack repository to your build file</b>
Add it in your root build.gradle at the end of repositories:

	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}

<b>Step 2. Add the dependency</b>

	dependencies {
	        implementation 'com.github.navinilavarasan:rxWebSocket:v1.01'
	}


<b> RELEASE </b><br>
[![](https://jitpack.io/v/navinilavarasan/rxWebSocket.svg)](https://jitpack.io/#navinilavarasan/rxWebSocket)

<br>
[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-rxWebsocket-brightgreen.svg?style=flat)](https://android-arsenal.com/details/1/6630)
