Chrono.vision Companion App
===========================

Android app for easily picking gallery items to send to your CV instance.

Requirements
------------

These can be easily installed using the Android SDK Manager.

 * OpenJDK 8
 * [Android SDK Platform 28](https://developer.android.com/studio/install)
 * [Android SDK Build-Tools 28.0.3](https://developer.android.com/studio/install)

Ensure `ANDROID_SDK_ROOT` is set correctly before building.

Building
--------

 1. Clone repository: `git clone https://github.com/foxxyz/chrono.vision-app.git`
 2. Enter directory: `cd chrono.vision-app`
 3. Build: `./gradlew build`
 
 The built APK will be in `app/build/outputs/apk/release/`

Before Using
------------

Once installed, make sure to set it up to work with your CV instance:

 1. Open the app
 2. Open settings by clicking the gear in the top left
 3. Set the URL to your CV instance
 4. Set the auth code for your CV instance

Usage
-----

While you can select photos from the app itself and post them there, the Companion App includes a share widget that allows you to share directly from the Android Gallery (or Google Photos):

 1. Open your Gallery or Photos app
 2. Select an image to share
 3. Tap the "Share" icon
 4. Select "Chrono.vise" under "Share to Apps"
 5. This should open the Companion App from where you can post directly
