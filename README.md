# DrunkSettingsAndroid

## Why?
Modern phones and their OS are awful, a lot for anal-protected things and compleately no attention to save user's attention, this app tries to add more control to things what importatant to me

## Features
This app **allows you** to:
- Set a per-minute notification limit, per-app or globaly
- Disable music fade-out when notification occurs
- Disable screenshot protection ([FLAG_SECURE](https://developer.android.com/security/fraud-prevention/activities#flag_secure))
- Disable screenshot protection in apps which detects new screenshot file in gallery by name
- Disable wake-on-fingerprint
- Disable headphones signal pin
- Disable notification status detection in apps
- Togle torch on power button long-press

## Usage
### Requirements
- Android with root
- [JingMatrix/Vector](https://github.com/JingMatrix/Vector)
### Steps
- Download latest apk from [Github Releses](https://github.com/drunkbatya/DrunkSettingsAndroid/releases)
- Install and activate all scopes in Vector
- Reboot

## Build
### Compile
Requires JDK 21 and the Android SDK
```bash
git submodule update --init --recursive

(cd lib/libxposed/api && ./gradlew publishToMavenLocal)
(cd lib/libxposed/service && ./gradlew publishToMavenLocal)

./gradlew :app:assembleRelease -PappVersionName=1.2.3 -PappVersionCode=42
adb install -r app/build/outputs/apk/release/app-release.apk
```
### Sign
Keystore needs to be generated once
```bash
STOREPASS="$(openssl rand -base64 10)";
keytool -genkeypair -v \
    -keystore release.keystore \
    -alias drunksettings \
    -keyalg RSA -keysize 2048 \
    -validity 10000 \
    -storepass "$STOREPASS" \
    -keypass "$STOREPASS" \
    -dname "CN=DrunkBatya, O=DrunkSettings, C=EU"
KEYSTORE_BASE64=$(base64 -w0 release.keystore)
```

Set this github envs
`KEYSTORE_BASE64` - base64 -w0 release.keystore
`KEYSTORE_PASSWORD` - store password
``
