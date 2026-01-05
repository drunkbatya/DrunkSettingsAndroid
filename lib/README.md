## libxposed

### Compiling local
```bash
# If update
rm -rf ~/.gradle/caches
rm -rf ~/.gradle/daemon

# API
cd libxposed/api
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ANDROID_HOME=/Users/drunkbatya/Library/Android/sdk ./gradlew :api:publishApiPublicationToMavenLocal

# Interface
cd libxposed/service
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ANDROID_HOME=/Users/drunkbatya/Library/Android/sdk ./gradlew :interface:publishInterfacePublicationToMavenLocal

# Service
cd libxposed/service
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ANDROID_HOME=/Users/drunkbatya/Library/Android/sdk ./gradlew :service:publishServicePublicationToMavenLocal
```
