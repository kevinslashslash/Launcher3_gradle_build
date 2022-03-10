This is [AOSP Launcher3](https://android.googlesource.com/platform/packages/apps/Launcher3/) with minimal modifications to make it easier to build gradle or Android Studio against the Android SDK
This is not interesting to end users, it is meant for testing and as a tool to report bugs in Android related to 3rd party launchers

Build with

```
git clone https://github.com/kevinslashslash/Launcher3_gradle_build.git
cd Launcher3_gradle_build
git submodule init
git submodule update
./gradlew assembleAospWithoutQuickstepDebug
```


