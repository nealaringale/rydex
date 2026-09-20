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

## Build

GitHub Actions builds a debug APK from `main` on every push.

## Project layout

```text
RYDEX/
├── app/                         # Android app
│   └── src/main/java/com/rydex/app/
│       ├── MainActivity.kt
│       ├── RydexViewModel.kt
│       ├── Repository.kt
│       ├── Models.kt
│       ├── LocationManager.kt
│       └── Polyline.kt
└── server/                      # Node/Express trip planner
    └── src/index.js
```

## Architecture

- Android: Kotlin + Jetpack Compose + Google Maps SDK for Android.
- Planner backend: Node.js + Express.
- Route + places: Google Places API (New) + Google Routes API.
- Weather: Open-Meteo.
- Google web-service credentials stay on the server.

## Google setup

Enable Maps SDK for Android, Places API (New), and Routes API in Google Cloud.

Create `local.properties` from `local.properties.example` and provide an Android-restricted Maps key.

For the backend, create `server/.env` and set `GOOGLE_MAPS_SERVER_KEY`.

The app has a demo fallback when the planner backend is unavailable.

## Current MVP boundary

Production turn-by-turn navigation, automatic off-route rerouting, voice guidance, call/audio handling, and KTM/helmet integration are next-stage features.
