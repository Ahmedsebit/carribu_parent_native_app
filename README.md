# Carribu Parent Native

Separate native Android parent application for the Carribu school transport backend.

- **Application ID:** `com.carribu.parentnative`
- **Display name:** Carribu Parent Native
- **Stack:** Kotlin, Jetpack Compose, Retrofit/OkHttp, Moshi, DataStore, Google Maps Compose, Firebase Messaging
- **SDK:** min 26, target/compile 36
- **Realtime strategy:** lifecycle-aware polling through a replaceable `RealtimeUpdates` interface (10 seconds for location/chat, 20 seconds for alerts)

## Tracking freshness and trip phases

The backend owns stop order, frozen destinations and the current trip phase.
Home ETAs are suppressed during afternoon school boarding. GPS older than two
minutes, more than 30 seconds in the future, or invalidated by an API failure
is shown as last known rather than live; stale ETAs are hidden.

Refresh responses cannot overwrite newer requests, and completed trips leave
the active selection. Parent alerts include drop-off and completion alongside
boarding/arrival. Provider acceptance does not prove handset delivery or parent
read. Deploy backend migration `0016` and configure/register FCM for tray alerts.

## Project setup

1. Open `C:\src\school-transport-app\carribu_parent_native_app` in Android Studio.
2. Copy `local.properties.example` to `local.properties`.
3. Confirm the SDK path:

   ```properties
   sdk.dir=C\:\\Users\\ahmedsebit\\AppData\\Local\\Android\\Sdk
   ```

4. Set the backend URL. It must end in `/api/`:

   ```properties
   API_BASE_URL=https\://carribubackend-production.up.railway.app/api/
   ```

5. For maps, enable **Maps SDK for Android**, restrict the key to
   `com.carribu.parentnative` and the signing certificate, then add:

   ```properties
   MAPS_API_KEY=your_restricted_key
   ```

   The key is passed through a manifest placeholder and is never stored in source control.
   The app builds without it, but Google map tiles cannot load.

## Firebase Cloud Messaging

The only required Firebase credential file is:

```text
app/google-services.json
```

Download it for Android package `com.carribu.parentnative` from Firebase Console. It is
gitignored. The Google Services plugin is applied **only when this file exists**, so builds
without Firebase credentials succeed. When Firebase is configured, the app:

- creates the `carribu_updates` high-importance notification channel;
- requests Android 13+ notification permission;
- includes `CarribuFirebaseMessagingService`;
- sends refreshed tokens to `PUT /api/auth/push-token` as
  `{"pushToken":"…","provider":"fcm"}`.

Without Firebase configuration, token initialization is skipped without crashing. The
backend stores FCM and Expo tokens independently. Native tray delivery becomes active after
`FIREBASE_SERVICE_ACCOUNT_JSON` is configured as a backend deployment secret.

## Build and run

From PowerShell:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME='C:\Users\ahmedsebit\AppData\Local\Android\Sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

Install on a connected device/emulator:

```powershell
.\gradlew.bat :app:installDebug
```

Debug APK:

```text
app\build\outputs\apk\debug\app-debug.apk
```

## Signed release APK

Release signing is read only from environment variables so keystore passwords
are never stored in the project:

```powershell
$env:CARRIBU_RELEASE_KEYSTORE='C:\Users\ahmedsebit\.android\carribu-parent-native-release.jks'
$env:CARRIBU_RELEASE_STORE_PASSWORD='your-store-password'
$env:CARRIBU_RELEASE_KEY_ALIAS='carribu-parent-native'
$env:CARRIBU_RELEASE_KEY_PASSWORD='your-key-password'
.\gradlew.bat :app:assembleRelease
```

Keep the keystore and passwords permanently. Android updates must be signed
with the same key. The generated package is:

```text
app\build\outputs\apk\release\app-release.apk
```

For a local backend from the Android emulator, set
`API_BASE_URL=http\://10.0.2.2\:5000/api/`. Physical devices need the development
computer's LAN address. Cleartext HTTP is enabled for local development.

## Architecture

```text
data/
  api/          Retrofit contract, authenticated OkHttp client, API result/error mapping
  model/        Backend DTOs and pure filter/coordinate/version logic
  repository/   Auth and parent feature repositories
  session/      DataStore session with Android Keystore AES-GCM encryption
realtime/       Polling abstraction, replaceable by Socket.IO later
push/           FCM service, notification rendering, backend token registration
ui/
  auth/         Login, first-time registration, forced password change
  main/         Track, trips/history, alerts, chat/absence, profile
  components/   Shared states/cards and lifecycle polling
  theme/        Carribu Material 3 theme
```

ViewModels expose immutable `StateFlow` state and Compose uses lifecycle-aware collection.
Errors are surfaced in context with retry actions where appropriate.

## Feature/API coverage

| Feature | API |
|---|---|
| Email/phone login and parent-role validation | `POST /api/auth/login` |
| Registration lookup and completion | `POST /api/auth/registration-status`, `/complete-registration` |
| Forced and normal password changes | `PUT /api/auth/change-password` |
| Secure persistent session/logout | Local encrypted DataStore |
| Multiple live buses, ordered stops, route polyline, ETA | `GET /api/location/my-bus` |
| Upcoming trips and completed history with filters | `GET /api/parent/upcoming-trips`, `/trip-history` |
| Alert history | `GET /api/messages/notifications` |
| Conversations, threads, send, unread, drivers | Existing `/api/messages/*` endpoints |
| Multi-child absence report | `POST /api/messages/absence` |
| Profile, schools, children, pickup map/current location | `GET /api/students`, `PUT /api/auth/profile` |
| Published version and external download | `GET /api/app-versions/latest/parent` |

No sample transport, location, trip, conversation, or notification data is generated.
