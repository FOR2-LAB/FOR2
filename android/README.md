Android Kotlin starter (skeleton)

What is included
- Foreground Location Service (Kotlin) stub using FusedLocationProvider (requires Google Play services)
- MainActivity with map view placeholder and a STOP button to dismiss alarms
- WebSocket and MQTT publisher stubs (no external libs added to keep skeleton minimal)

How to open
- Open the `android` folder in Android Studio.
- Add required dependencies and map API keys (if you want a real map view).

Notes
- Permissions (ACCESS_FINE_LOCATION, FOREGROUND_SERVICE) must be requested at runtime for Android 6.0+.
- This is a starting skeleton; integrate Map SDKs (Google Maps / Mapbox) and MQTT libraries (Paho/Async) as needed.

WebSocket notes

- The service opens an OkHttp WebSocket to `ws://10.0.2.2:8080` by default. When running on the Android emulator, `10.0.2.2` maps to the host machine. Change `wsUrl` in `LocationForegroundService.kt` to point at your backend.
- After adding dependencies, open the `android` folder in Android Studio and let it sync Gradle.
