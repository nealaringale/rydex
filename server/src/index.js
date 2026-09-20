import 'dotenv/config';
import express from 'express';
import cors from 'cors';

const app = express();
app.use(cors());
app.use(express.json({ limit: '256kb' }));

const PORT = Number(process.env.PORT || 8787);
const GOOGLE_KEY = process.env.GOOGLE_MAPS_SERVER_KEY;

if (!GOOGLE_KEY) {
  console.warn('GOOGLE_MAPS_SERVER_KEY is not set. /api/plan will return a configuration error.');
}

const GOOGLE_HEADERS = {
  'Content-Type': 'application/json',
  'X-Goog-Api-Key': GOOGLE_KEY || '',
};

app.get('/api/health', (_req, res) => {
  res.json({
    ok: true,
    googleKeyConfigured: Boolean(GOOGLE_KEY),
  });
});

app.post('/api/plan', async (req, res) => {
  try {
    if (!GOOGLE_KEY) {
      return res
        .status(503)
        .json({ error: 'Server Google Maps key is not configured.' });
    }

    const input = normalizePlanRequest(req.body);

    const destination = await searchDestination(
      input.destinationQuery,
      input.origin,
    );

    if (!destination) {
      return res.status(404).json({ error: 'Destination not found.' });
    }

    const baseRoute = await computeRoute(input.origin, destination, []);
    const basePoints = decodePolyline(baseRoute.polyline);
    const candidateStops = [];

    if (input.preferences.needsFuel) {
      const fractions =
        baseRoute.distanceMeters > 400_000 ? [0.34, 0.68] : [0.45];

      for (const fraction of fractions) {
        const point = pointAtDistanceFraction(basePoints, fraction);
        const stop = await findFuelStop(
          point,
          input.preferences.fuelPriority,
        );

        if (stop) {
          candidateStops.push({
            ...stop,
            sortKm: baseRoute.distanceMeters * fraction / 1000,
          });
        }
      }
    }

    if (input.preferences.needsFood) {
      const fraction = foodRouteFraction(
        input.departureTime,
        input.preferences.foodTime,
        baseRoute.durationSeconds,
      );

      const point = pointAtDistanceFraction(basePoints, fraction);
      const stop = await findFoodStop(point);

      if (stop) {
        candidateStops.push({
          ...stop,
          sortKm: baseRoute.distanceMeters * fraction / 1000,
        });
      }
    }

    const stops = dedupeStops(candidateStops).sort(
      (a, b) => a.sortKm - b.sortKm,
    );

    const finalRoute = await computeRoute(
      input.origin,
      destination,
      stops.map((s) => ({ lat: s.lat, lng: s.lng })),
    );

    const finalPoints = decodePolyline(finalRoute.polyline);

    const weather = input.preferences.needsWeather
      ? await buildWeatherCheckpoints(
          finalPoints,
          finalRoute.durationSeconds,
          input.departureTime,
        )
      : [];

    res.json({
      destinationName: destination.name,
      destinationAddress: destination.address,
      distanceMeters: finalRoute.distanceMeters,
      durationSeconds: finalRoute.durationSeconds,
      encodedPolyline: finalRoute.polyline,
      stops: stops.map((s) => ({
        type: s.type,
        name: s.name,
        address: s.address,
        lat: s.lat,
        lng: s.lng,
        rating: s.rating,
        distanceFromRouteMeters: s.distanceFromRouteMeters,
        reason: s.reason,
      })),
      weather,
      steps: finalRoute.steps,
    });
  } catch (error) {
    console.error(error);
    res.status(500).json({
      error: error?.message || 'Planning failed.',
    });
  }
});

async function searchDestination(query, origin) {
  const body = {
    textQuery: query,
    maxResultCount: 1,
    languageCode: 'en',
    locationBias: {
      circle: {
        center: {
          latitude: origin.lat,
          longitude: origin.lng,
        },
        radius: 50_000,
      },
    },
  };

  const data = await googlePost(
    'https://places.googleapis.com/v1/places:searchText',
    body,
    'places.id,places.displayName,places.formattedAddress,places.location',
  );

  const place = data.places?.[0];
  if (!place?.location) return null;

  return {
    id: place.id,
    name: place.displayName?.text || query,
    address: place.formattedAddress || null,
    lat: place.location.latitude,
    lng: place.location.longitude,
  };
}

async function findFuelStop(point, priority) {
  const rank =
    priority === 'LOW'
      ? 'POPULARITY'
      : 'DISTANCE';

  const radius =
    priority === 'HIGH'
      ? 1800
      : priority === 'MEDIUM'
        ? 4000
        : 7000;

  const places = await nearby(
    point,
    ['gas_station'],
    radius,
    rank,
  );

  const place = choosePlace(places, priority);
  if (!place) return null;

  return {
    type: 'fuel',
    id: place.id,
    name: place.name,
    address: place.address,
    lat: place.lat,
    lng: place.lng,
    rating: place.rating,
    distanceFromRouteMeters: place.distanceFromRouteMeters,
    reason: `Fuel priority: ${priority}`,
  };
}

async function findFoodStop(point) {
  const places = await nearby(
    point,
    ['restaurant', 'cafe', 'meal_takeaway'],
    5000,
    'POPULARITY',
  );

  const place = choosePlace(places, 'LOW');
  if (!place) return null;

  return {
    type: 'food',
    id: place.id,
    name: place.name,
    address: place.address,
    lat: place.lat,
    lng: place.lng,
    rating: place.rating,
    distanceFromRouteMeters: place.distanceFromRouteMeters,
    reason: 'Chosen near your preferred food time',
  };
}

async function nearby(point, includedTypes, radius, rankPreference) {
  const body = {
    includedTypes,
    maxResultCount: 10,
    rankPreference,
    locationRestriction: {
      circle: {
        center: {
          latitude: point.lat,
          longitude: point.lng,
        },
        radius,
      },
    },
    languageCode: 'en',
  };

  const data = await googlePost(
    'https://places.googleapis.com/v1/places:searchNearby',
    body,
    'places.id,places.displayName,places.formattedAddress,places.location,places.rating,places.userRatingCount,places.businessStatus',
  );

  return (data.places || [])
    .map((place) => ({
      id: place.id,
      name: place.displayName?.text || 'Place',
      address: place.formattedAddress || null,
      lat: place.location?.latitude,
      lng: place.location?.longitude,
      rating: place.rating ?? null,
      userRatingCount: place.userRatingCount ?? 0,
      businessStatus: place.businessStatus || null,
      distanceFromRouteMeters: place.location
        ? haversineMeters(
            point.lat,
            point.lng,
            place.location.latitude,
            place.location.longitude,
          )
        : null,
    }))
    .filter(
      (place) =>
        Number.isFinite(place.lat) &&
        Number.isFinite(place.lng),
    );
}

function choosePlace(places, priority) {
  const open = places.filter(
    (place) => place.businessStatus !== 'CLOSED_PERMANENTLY',
  );

  const candidates = open.length ? open : places;
  if (!candidates.length) return null;

  return [...candidates].sort((a, b) => {
    const da = a.distanceFromRouteMeters ?? 999999;
    const db = b.distanceFromRouteMeters ?? 999999;
    const ra = a.rating ?? 0;
    const rb = b.rating ?? 0;
    const ca = a.userRatingCount ?? 0;
    const cb = b.userRatingCount ?? 0;

    if (priority === 'HIGH') return da - db;

    if (priority === 'LOW') {
      return (rb - ra) || (cb - ca) || (da - db);
    }

    return (
      (db * 0.65 + (5 - rb) * 500) -
      (da * 0.65 + (5 - ra) * 500)
    );
  })[0];
}

async function computeRoute(origin, destination, stops) {
  const body = {
    origin: {
      location: {
        latLng: {
          latitude: origin.lat,
          longitude: origin.lng,
        },
      },
    },
    destination: destination.id
      ? { placeId: destination.id }
      : {
          location: {
            latLng: {
              latitude: destination.lat,
              longitude: destination.lng,
            },
          },
        },
    intermediates: stops.map((stop) => ({
      location: {
        latLng: {
          latitude: stop.lat,
          longitude: stop.lng,
        },
      },
    })),
    travelMode: 'TWO_WHEELER',
    routingPreference: 'TRAFFIC_AWARE',
    polylineQuality: 'HIGH_QUALITY',
    polylineEncoding: 'ENCODED_POLYLINE',
    computeAlternativeRoutes: false,
    languageCode: 'en-US',
    units: 'METRIC',
  };

  let data;

  try {
    data = await googlePost(
      'https://routes.googleapis.com/directions/v2:computeRoutes',
      body,
      'routes.distanceMeters,routes.duration,routes.polyline.encodedPolyline,routes.legs.steps.navigationInstruction,routes.legs.steps.startLocation,routes.legs.steps.endLocation',
    );
  } catch (error) {
    const message = String(error?.message || '');

    if (
      message.includes('TWO_WHEELER') ||
      message.includes('travelMode')
    ) {
      body.travelMode = 'DRIVE';

      data = await googlePost(
        'https://routes.googleapis.com/directions/v2:computeRoutes',
        body,
        'routes.distanceMeters,routes.duration,routes.polyline.encodedPolyline,routes.legs.steps.navigationInstruction,routes.legs.steps.startLocation,routes.legs.steps.endLocation',
      );
    } else {
      throw error;
    }
  }

  const route = data.routes?.[0];

  if (!route?.polyline?.encodedPolyline) {
    throw new Error('Google Routes returned no route.');
  }

  const steps = (route.legs || []).flatMap((leg) =>
    (leg.steps || []).map((step) => ({
      instruction:
        step.navigationInstruction?.instructions || 'Continue',
      maneuver:
        step.navigationInstruction?.maneuver || null,
      lat:
        step.startLocation?.latLng?.latitude ??
        step.endLocation?.latLng?.latitude ??
        0,
      lng:
        step.startLocation?.latLng?.longitude ??
        step.endLocation?.latLng?.longitude ??
        0,
    })),
  );

  return {
    distanceMeters: route.distanceMeters,
    durationSeconds: parseDurationSeconds(route.duration),
    polyline: route.polyline.encodedPolyline,
    steps,
  };
}

async function buildWeatherCheckpoints(
  points,
  durationSeconds,
  departureIso,
) {
  if (!points.length || durationSeconds <= 0) return [];

  const distances = cumulativeDistances(points);
  const total = distances.at(-1) || 0;
  const checkpoints = [];

  for (
    let km = 30;
    km < total / 1000 - 12;
    km += 30
  ) {
    const fraction = (km * 1000) / total;
    const point = pointAtDistance(
      points,
      distances,
      fraction,
    );

    const arrival = new Date(
      new Date(departureIso).getTime() +
        durationSeconds * 1000 * fraction,
    );

    checkpoints.push({
      kmFromStart: km,
      point,
      arrivalTime: arrival,
    });
  }

  return Promise.all(
    checkpoints.map(async (checkpoint) => {
      const weather = await fetchWeather(
        checkpoint.point.lat,
        checkpoint.point.lng,
        checkpoint.arrivalTime,
      );

      return {
        kmFromStart: Math.round(checkpoint.kmFromStart * 10) / 10,
        arrivalTime: checkpoint.arrivalTime.toISOString(),
        weatherCode: weather.weatherCode,
        precipitationProbability:
          weather.precipitationProbability,
        rainMm: weather.rainMm,
        recommendation: weatherRecommendation(weather),
      };
    }),
  );
}

async function fetchWeather(lat, lng, arrival) {
  const url = new URL(
    'https://api.open-meteo.com/v1/forecast',
  );

  url.searchParams.set('latitude', lat);
  url.searchParams.set('longitude', lng);
  url.searchParams.set(
    'hourly',
    'precipitation_probability,rain,showers,weather_code',
  );
  url.searchParams.set('timezone', 'UTC');
  url.searchParams.set('forecast_days', '3');

  const response = await fetch(url);

  if (!response.ok) {
    throw new Error(
      `Weather request failed: ${response.status}`,
    );
  }

  const data = await response.json();
  const times = data.hourly?.time || [];

  let best = 0;
  let bestDiff = Infinity;

  for (let i = 0; i < times.length; i++) {
    const diff = Math.abs(
      Date.parse(times[i]) - arrival.getTime(),
    );

    if (diff < bestDiff) {
      bestDiff = diff;
      best = i;
    }
  }

  return {
    weatherCode:
      data.hourly?.weather_code?.[best] ?? null,
    precipitationProbability:
      data.hourly?.precipitation_probability?.[best] ?? null,
    rainMm:
      data.hourly?.rain?.[best] ??
      data.hourly?.showers?.[best] ??
      0,
  };
}

function weatherRecommendation(weather) {
  const probability =
    weather.precipitationProbability ?? 0;
  const rain = weather.rainMm ?? 0;
  const code = weather.weatherCode ?? 0;

  const rainyCode =
    (code >= 51 && code <= 67) ||
    (code >= 80 && code <= 82) ||
    code === 95 ||
    code === 96 ||
    code === 99;

  if (
    probability >= 65 ||
    rain >= 1.5 ||
    rainyCode
  ) {
    return 'Rain likely — keep your raincoat ready.';
  }

  if (probability >= 35 || rain > 0.2) {
    return 'Possible showers — raincoat recommended.';
  }

  return 'No significant rain expected.';
}

function foodRouteFraction(
  departureIso,
  foodTime,
  durationSeconds,
) {
  if (!foodTime || durationSeconds <= 0) return 0.5;

  const [hours, minutes] = String(foodTime)
    .split(':')
    .map(Number);

  if (
    !Number.isFinite(hours) ||
    !Number.isFinite(minutes)
  ) {
    return 0.5;
  }

  const departure = new Date(departureIso);
  const preferred = new Date(departure);

  preferred.setHours(hours, minutes, 0, 0);

  if (preferred.getTime() < departure.getTime()) {
    preferred.setDate(preferred.getDate() + 1);
  }

  const minutesFromDeparture =
    (preferred.getTime() - departure.getTime()) / 60000;

  return clamp(
    (minutesFromDeparture * 60) / durationSeconds,
    0.1,
    0.9,
  );
}

function pointAtDistance(points, cumulative, fraction) {
  const total = cumulative.at(-1) || 1;
  const target = total * clamp(fraction, 0, 1);

  let index = 0;

  while (
    index < cumulative.length - 1 &&
    cumulative[index + 1] < target
  ) {
    index++;
  }

  const a = points[index];
  const b = points[Math.min(index + 1, points.length - 1)];
  const span =
    cumulative[Math.min(index + 1, cumulative.length - 1)] -
      cumulative[index] || 1;

  const t = (target - cumulative[index]) / span;

  return {
    lat: a.lat + (b.lat - a.lat) * t,
    lng: a.lng + (b.lng - a.lng) * t,
  };
}

function pointAtDistanceFraction(points, fraction) {
  return pointAtDistance(
    points,
    cumulativeDistances(points),
    fraction,
  );
}

function cumulativeDistances(points) {
  const cumulative = [0];

  for (let i = 1; i < points.length; i++) {
    cumulative.push(
      cumulative[i - 1] +
        haversineMeters(
          points[i - 1].lat,
          points[i - 1].lng,
          points[i].lat,
          points[i].lng,
        ),
    );
  }

  return cumulative;
}

function decodePolyline(encoded) {
  const points = [];
  let index = 0;
  let lat = 0;
  let lng = 0;

  while (index < encoded.length) {
    let shift = 0;
    let result = 0;
    let byte;

    do {
      byte = encoded.charCodeAt(index++) - 63;
      result |= (byte & 31) << shift;
      shift += 5;
    } while (byte >= 32);

    lat +=
      (result & 1)
        ? ~(result >> 1)
        : result >> 1;

    shift = 0;
    result = 0;

    do {
      byte = encoded.charCodeAt(index++) - 63;
      result |= (byte & 31) << shift;
      shift += 5;
    } while (byte >= 32);

    lng +=
      (result & 1)
        ? ~(result >> 1)
        : result >> 1;

    points.push({
      lat: lat / 1e5,
      lng: lng / 1e5,
    });
  }

  return points;
}

function dedupeStops(stops) {
  const seen = new Set();

  return stops.filter((stop) => {
    const key =
      `${stop.type}:${stop.id || `${stop.lat.toFixed(4)},${stop.lng.toFixed(4)}`}`;

    if (seen.has(key)) return false;

    seen.add(key);
    return true;
  });
}

function parseDurationSeconds(duration) {
  if (!duration) return 0;
  return Math.round(
    Number(String(duration).replace('s', '')),
  );
}

function normalizePlanRequest(body) {
  if (
    body?.origin?.lat == null ||
    body?.origin?.lng == null ||
    !body?.destinationQuery
  ) {
    throw new Error(
      'origin.lat, origin.lng and destinationQuery are required',
    );
  }

  return {
    origin: {
      lat: Number(body.origin.lat),
      lng: Number(body.origin.lng),
    },
    destinationQuery: String(body.destinationQuery),
    departureTime:
      body.departureTime ||
      new Date().toISOString(),
    preferences: {
      needsFuel: Boolean(
        body.preferences?.needsFuel,
      ),
      fuelPriority: [
        'HIGH',
        'MEDIUM',
        'LOW',
      ].includes(
        body.preferences?.fuelPriority,
      )
        ? body.preferences.fuelPriority
        : 'HIGH',
      needsFood: Boolean(
        body.preferences?.needsFood,
      ),
      foodTime:
        body.preferences?.foodTime || null,
      needsWeather:
        body.preferences?.needsWeather !== false,
    },
  };
}

async function googlePost(url, body, fieldMask) {
  const response = await fetch(url, {
    method: 'POST',
    headers: {
      ...GOOGLE_HEADERS,
      'X-Goog-FieldMask': fieldMask,
    },
    body: JSON.stringify(body),
  });

  const responseText = await response.text();

  let data;
  try {
    data = JSON.parse(responseText);
  } catch {
    data = { error: responseText };
  }

  if (!response.ok) {
    const message =
      data?.error?.message ||
      data?.error ||
      `Google request failed with ${response.status}`;

    throw new Error(String(message));
  }

  return data;
}

function haversineMeters(
  aLat,
  aLng,
  bLat,
  bLng,
) {
  const earthRadius = 6371000;

  const lat1 = (aLat * Math.PI) / 180;
  const lat2 = (bLat * Math.PI) / 180;
  const dLat =
    ((bLat - aLat) * Math.PI) / 180;
  const dLng =
    ((bLng - aLng) * Math.PI) / 180;

  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(lat1) *
      Math.cos(lat2) *
      Math.sin(dLng / 2) ** 2;

  return (
    2 *
    earthRadius *
    Math.asin(Math.sqrt(a))
  );
}

function clamp(value, min, max) {
  return Math.min(max, Math.max(min, value));
}

app.listen(PORT, '0.0.0.0', () => {
  console.log(`RYDEX planner server listening on port ${PORT}`);
});
