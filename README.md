Picorama Companion App
======================

Android app for easily picking gallery items to send to your [Picorama](https://github.com/foxxyz/picorama) instance.

Supports sending to up to 3 Picorama instances.

Requirements
------------

These can be easily installed using the Android SDK Manager.

 * OpenJDK 8
 * [Android SDK Platform 28](https://developer.android.com/studio/install)
 * [Android SDK Build-Tools 28.0.3](https://developer.android.com/studio/install)

Ensure `ANDROID_SDK_ROOT` is set correctly before building.

Building
--------

### Using IDE

Load the directory in Android Studio and build.

### Manual

 1. Clone repository: `git clone https://github.com/foxxyz/picorama-companion.git`
 2. Enter directory: `cd picorama-companion`
 3. Build: `./gradlew build`
 
 The built APK will be in `app/build/outputs/apk/release/`

Before Using
------------

Once installed, make sure to set it up to work with your Picorama instance:

 1. Open the app
 2. Open settings by clicking the gear in the top left
 3. Set the URL to your Picorama instance
 4. Set the auth code for your Picorama instance

Usage
-----

While you can select photos from the app itself and post them there, the Companion App includes a share widget that allows you to share directly from the Android Gallery (or Google Photos):

 1. Open your Gallery or Photos app
 2. Select an image to share
 3. Tap the "Share" icon
 4. Select "Picorize" under "Share to Apps"
 5. This opens the Companion App with the selected photo
