# Kinnect - Assignment 03

Project: `i230507_i230017`  
Android package: `com.kinnect.i230507_i230017`

The starter was cloned from `mudassirs52/Kinnect_smd_sqlite`. The original Assignment 2 project at `C:\Users\shaha\AndroidStudioProjects\i230507` was inspected for configuration and left unchanged.

## Run

1. Open this folder in Android Studio and let Gradle sync. The Gradle daemon uses JDK 17; XAMPP supplies PHP and MySQL.
2. Run `powershell -File backend\Start-LocalDemo.ps1` from this project directory. See `backend/README.md` for the Apache/XAMPP alternative.
3. Run the app on an emulator. `local.properties` defaults to `KINNECT_API_URL=http://10.0.2.2:8088/`. For physical devices, replace the host with your laptop's LAN IP, keep both devices on the same Wi-Fi and rebuild.
4. Sign up with two separate accounts, finish profile setup, search for the other user, send and confirm a friend request, and open a chat from Friends.

First-time profile setup requires a connection so username uniqueness can be checked. A valid stored token allows later offline entry. Logout requires a connection so the server token can be revoked before clearing local data.

## SQLite and synchronization

`OfflineStore.kt` follows the lecture's SQLiteOpenHelper/SQLiteDatabase/ContentValues/Cursor approach. SQLite stores users, friends, posts, stories, comments, messages, response snapshots and `pending_actions`. The UI gets the cached response first, then a server refresh. Database and network work run off the UI thread.

Mutations are written to SQLite before their callbacks report success. Selected attachment bytes are copied into app-private persistent storage before queueing, so temporary gallery URIs can expire safely. `PendingProjection.kt` overlays the queued actions on cached responses. WorkManager waits for a connected network and sends actions in insertion order using exponential retry. Successful responses are cached before the corresponding action is acknowledged. Server rejections are retained as failed actions rather than silently discarded. Post/comment local identifiers are reconciled before dependent actions are sent.

`SyncWorker.kt` sends the real HTTP requests. The starter's fake login token and sync-success placeholders were removed. Server UUID reservations and domain mutations commit together, so retrying after a lost response does not repeat the action.

Server timestamps are milliseconds. The server enforces message ownership and the five-minute change window. Expired stories are filtered on the server and when reading local cached stories. Images are cached privately after viewing, and received chat attachments are downloaded to the private media cache. Cached video/file attachments open through FileProvider while offline; queued outgoing files remain available until logout.

## Configuration

The working copy has the original Android Firebase configuration in an ignored `app/google-services.json`, and Agora settings in ignored `api/config.local.php`. No Agora certificate is compiled into the app. The Android configuration belongs to the old Firebase Android registration; register the new package in the same Firebase project and replace that JSON for final delivery.

PHP push sending needs a **Firebase service account** stored outside the web root. The Android google-services.json cannot authorize the server. Configure its path and run `backend/Start-Notifications.ps1` as described in the backend guide.

## Validation and remaining demo work

`backend/test-results.json` records the API checks actually executed, including duplicate retries, authorization, message edit expiry, multipart images, story expiry/close-friend visibility and signed Agora token generation. Run `python backend\test_api.py` to repeat them against the local demo database.

Two Espresso tests cover rejected login without a fabricated session and durable queued messaging across activity recreation. Run `gradlew.bat connectedDebugAndroidTest` with an emulator or phone connected. Instrumented tests replace this app's local test session/cache, so use a test installation.

The session's emulator could not start because hardware acceleration was unavailable to the test process. A live two-device demonstration, push delivery, and actual Agora audio/video still require device verification. Android 14+ uses native screenshot reporting. Older Android versions request media access and use screenshot filename observation, which depends on the device saving screenshots with a recognizable filename. Exact visual matching requires the missing `Kinnect_UI.pdf`; the starter's XML layouts were retained.

The new private GitHub repository, genuine commits by both members and the final two-device live demonstration remain team submission steps. Existing repository history was preserved; no remote push or fabricated teammate commits were made.

## Submission

Export through Android Studio **File > Export > Export to ZIP File**, using `i230507_i230017_Assignment03.zip`. Keep the project root named `i230507_i230017`. Exclude local.properties, api/config.local.php, service-account credentials, private signing keys, uploads, build output and runtime database files. Configure the backend separately on the demo machine.

References: [Android WorkManager](https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/manage-work), [FCM HTTP v1](https://firebase.google.com/docs/cloud-messaging/send/v1-api), [Agora token authentication](https://docs.agora.io/en/realtime-media/rtc/build/authenticate-users/authentication-workflow).
