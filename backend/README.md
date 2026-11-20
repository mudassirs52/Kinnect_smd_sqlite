# Local PHP/MySQL setup

Run from the project directory in PowerShell:

```powershell
powershell -File backend\Start-LocalDemo.ps1
```

The script uses XAMPP's installed binaries, creates a separate MySQL instance on port 3307, imports the schema, and serves `api/` on port 8088. It does not replace or modify XAMPP's existing databases. Keep the window open. An already running demo instance is reused.

Alternatively, start MySQL and Apache from XAMPP, import `schema.sql` in phpMyAdmin, copy `api/` to a new htdocs folder and set the DSN in `api/config.local.php`. Update `KINNECT_API_URL` in Android's `local.properties` to match that folder. The example configuration uses port 3306; the standalone demo uses 3307.

Android emulator: `http://10.0.2.2:8088/`. Two physical devices: use this laptop's LAN IP instead, on the same Wi-Fi, and rebuild after changing `KINNECT_API_URL`. Allow the PHP server through Windows Firewall if prompted. Use HTTPS when hosting outside your local demo.

## Authentication and idempotency

`signup.php` and `login.php` accept form fields and return `token`, `expires_at` (milliseconds), and `user`. Send `Authorization: Bearer <token>` on authenticated requests. Passwords use PHP `password_hash` and sessions store only SHA-256 hashes of random tokens.

Every mutation except heartbeat/logout requires a UUID `action_uuid`. Reuse exactly the same UUID and fields when retrying an operation. `processed_actions` records the domain mutation and response in the same MySQL transaction. Replaying a completed UUID returns its original response. Reusing a UUID for a different endpoint returns 409.

Media endpoints accept multipart form data. Posts use `files[0]`, `files[1]`, etc.; other uploads use `file`. Files are validated by their actual MIME, given random names and stored in `api/uploads/`. Maximum size is 25 MB per attachment; posts allow 10 images.

## Calls

Set `agora_app_id` and `agora_certificate` in the ignored `api/config.local.php`. The working copy is already configured from the original Assignment 2 project. These values are intentionally absent from the source ZIP. A call gets a unique channel; only its two participants can obtain a signed token from `agora_token.php`. The certificate is never embedded in Android's BuildConfig.

Agora's PHP token generator is vendored in `api/agora/` from [AgoraIO/Tools](https://github.com/AgoraIO/Tools/tree/master/DynamicKey/AgoraDynamicKey/php/src), with its license.

## FCM

The Android client configuration was copied to the ignored `app/google-services.json`. Firebase is used only for messaging; user data and authentication use PHP/MySQL. The Android Firebase registration currently comes from Assignment 2. For submission, register `com.kinnect.i230507_i230017` in that Firebase project and replace the local JSON with its downloaded configuration.

The PHP sender additionally needs a Firebase **service account** JSON, which is different from Android's google-services.json. Store that private file outside the web root and set `fcm_service_account` to its absolute path in `api/config.local.php`, or set `GOOGLE_APPLICATION_CREDENTIALS`. Then run:

```powershell
powershell -File backend\Start-Notifications.ps1
```

New messages, friend requests and screenshot reports create notification records transactionally. The dispatcher sends them using the FCM HTTP v1 API and retains failed deliveries for retry. Calls use authenticated polling while the app is in the foreground.

## Verification

`test_api.py` creates disposable test accounts in this assignment database and tests the API. It moves only its own message/story fixtures backwards in time to test expiration. Default ports are 8088 and 3307. Run it after starting the server:

```powershell
python backend\test_api.py
```

Set `KINNECT_URL`, `KINNECT_MYSQL_BIN`, `KINNECT_MYSQL_PORT` or `KINNECT_MYSQL_USER` to override defaults. `test-results.json` records the checks actually run. Push delivery and a live Agora connection require device testing; generating a signed token is not proof of a completed call.
