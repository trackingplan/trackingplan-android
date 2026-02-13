# Troubleshooting

## Verify bytecode transformations

The Trackingplan adapter can write a log file during the build with details about which classes and methods were transformed. Logging is disabled by default and must be opted into with `-Ptrackingplan.transformLogging=true`.

1. Get the Gradle version, stop the daemon, and remove the transforms cache so
   that dependency transformations (Firebase, OkHttp, etc.) are re-executed from scratch:

   ```bash
   GRADLE_VERSION=$(./gradlew --version | grep '^Gradle' | awk '{print $2}')
   ./gradlew clean
   ./gradlew --stop
   rm -rf ~/.gradle/caches/$GRADLE_VERSION/transforms
   ```

2. Run a build with the logging flag, bypassing build and configuration caches:

   ```bash
   ./gradlew :app:assembleDebug --no-build-cache --no-configuration-cache -Ptrackingplan.transformLogging=true 2>&1 | grep "Trackingplan log"
   ```

   This prints the log file path, e.g.:

   ```
   Trackingplan log: /Users/you/project/app/build/trackingplan.log
   ```

3. Look for `Apply transform` entries in the log file:

   ```bash
   grep "Apply transform" app/build/trackingplan.log
   ```

   Each line shows a method call that was intercepted and the transformation applied. If no lines appear, the adapter did not find any matching SDK calls to transform.
