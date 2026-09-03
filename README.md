# DrunkSettingsAndroid

## Why?
Modern phones and their OS are awful, a lot for anal-protected things and compleately no attention to save user's attention, this app tries to add more control to things what importatant to me

## Build
Requires JDK 21 and the Android SDK
```bash
git submodule update --init --recursive

(cd lib/libxposed/api && ./gradlew publishToMavenLocal)
(cd lib/libxposed/service && ./gradlew publishToMavenLocal)

./gradlew :app:assembleRelease -PappVersionName=1.2.3 -PappVersionCode=42
adb install -r app/build/outputs/apk/release/app-release.apk
```
### Signing
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
