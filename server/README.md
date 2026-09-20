# RYDEX Planner Server

The server keeps Google Places and Routes web-service credentials off the Android client.

## Setup

```bash
cd server
npm install
cp .env.example .env
# edit .env and set GOOGLE_MAPS_SERVER_KEY
npm run check
npm start
```

## API

- `GET /api/health`
- `POST /api/plan`

The `/api/plan` request contains the rider's origin, destination query, departure time and fuel/food/weather preferences. The response contains the final route, inserted stops, weather checkpoints and navigation steps.

For the Android emulator, use `http://10.0.2.2:8787/` as the backend URL. For a physical phone, use the development computer's LAN IP.
