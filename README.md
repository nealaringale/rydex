# RYDEX 🏍️

RYDEX is a motorcycle trip-planning and riding cockpit for Android.

The MVP follows the flow we designed:

1. Read the rider's live GPS location.
2. Enter a destination.
3. Customize the trip.
4. Add a fuel stop when required, with HIGH / MEDIUM / LOW priority.
5. Add a food stop around the rider's requested time.
6. Check weather at roughly 30 km route intervals and generate raincoat guidance.
7. Preview the whole trip.
8. Start an in-app riding cockpit with the route, live speed, stops and next instruction.

## Project layout

```text
RYDEX/
├── app/                         # Android app
│   └── src/main/java/com/rydex/app/
│       ├── MainActivity.kt      # Home, preferences, trip preview, riding UI
│       ├── RydexViewModel.kt    # GPS + trip state
│       ├── Repository.kt        # API client + demo fallback
│       ├── Models.kt            # API/domain models
│       ├── LocationManager.kt   # live GPS stream
│       └── Polyline.kt          # Google encoded polyline decoder
│
└── server/                     # Node/Express trip planner
    └── src/index.js            # Places + Routes + weather planning
```

## Architecture

- **Android:** Kotlin + Jetpack Compose + Google Maps SDK for Android.
- **Planner backend:** Node.js + Express.
- **Route + places:** Google Places API (New) + Google Routes API.
- **Weather:** Open-Meteo.
- The server keeps the Google web-service key out of the Android APK.

## Google Cloud

Enable these APIs in the Google Cloud project:

- Maps SDK for Android
- Places API (New)
- Routes API

Use an Android-restricted key for the Maps SDK and a separate server-side key for Places/Routes.

Create `local.properties` from `local.properties.example`:

```properties
MAPS_API_KEY=YOUR_ANDROID_RESTRICTED_KEY
RYDEX_BACKEND_URL=http://10.0.2.2:8787/
```

For a real phone, replace the emulator address with the development PC's LAN IP.

## Run the planner server

```bash
cd server
npm install
cp .env.example .env
# add GOOGLE_MAPS_SERVER_KEY to .env
npm start
```

Health check:

```text
GET http://localhost:8787/api/health
```

## Run Android

Open the repository in Android Studio, create `local.properties`, sync Gradle and run the `app` configuration.

The app includes a **demo fallback** so the UI can still be explored if the planner backend is not reachable.

## Current MVP boundary

This repository is intentionally the first planning/cockpit layer. Full production navigation, automatic off-route rerouting, voice guidance, call/audio handling and KTM/helmet integration are next-stage features.
