
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = 'https://api.kubijatra.com';
const TOKEN = __ENV.API_TOKEN || '';
const AUTH_ROLE = (__ENV.AUTH_ROLE || '').toLowerCase();

export const options = {
  // Start with a small test while investigating timeouts.
  vus: 5,
  duration: '30s',

  thresholds: {
    http_req_failed: ['rate<0.02'],
    http_req_duration: ['p(95)<800'],
  },
};

const commonHeaders = {
  Accept: 'application/json',
};

const authenticatedHeaders = {
  ...commonHeaders,
  Authorization: `Bearer ${TOKEN}`,
};

function get(path, name, headers = commonHeaders) {
  const response = http.get(`${BASE_URL}${path}`, {
    headers,
    tags: { endpoint: name },
    timeout: '15s',
  });

  const passed = check(response, {
    [`${name}: HTTP 200`]: (r) => r.status === 200,
  });

  if (!passed) {
    console.error(
      `${name}: status=${response.status}, ` +
      `error=${response.error || 'none'}, ` +
      `duration=${response.timings.duration}ms`
    );
  }

  return response;
}

function randomItem(items) {
  return items[Math.floor(Math.random() * items.length)];
}

export default function () {
  // Public GET endpoints only; no admin routes.
  const publicEndpoints = [
    ['/api/health', 'health'],
    ['/api/config', 'config'],
    ['/api/buses', 'buses'],
    ['/api/routes', 'routes'],
    ['/api/academic/departments', 'departments'],
    ['/api/academic/sessions', 'sessions'],
    ['/api/academic/offices', 'offices'],
    ['/api/academic/units', 'units'],
  ];

  const selected = randomItem(publicEndpoints);
  const response = get(selected[0], selected[1]);

  // Sometimes fetch details for a random bus.
  if (selected[1] === 'buses' && Math.random() < 0.35) {
    try {
      const buses = response.json();

      if (Array.isArray(buses) && buses.length > 0) {
        const bus = randomItem(buses);

        if (bus.id !== undefined && bus.id !== null) {
          get(`/api/buses/${encodeURIComponent(bus.id)}`, 'bus-details');
        }
      }
    } catch (_) {
      console.error('Could not parse /api/buses response as JSON.');
    }
  }

  // Only run protected requests when a token is supplied.
  // Set AUTH_ROLE=student or AUTH_ROLE=teacher to test the matching profile.
  if (TOKEN && Math.random() < 0.5) {
    const protectedEndpoints = [
      ['/api/notices/active', 'active-notices'],
    ];

    if (AUTH_ROLE === 'student') {
      protectedEndpoints.push([
        '/api/auth/student/me',
        'student-profile',
      ]);
    } else if (AUTH_ROLE === 'teacher') {
      protectedEndpoints.push([
        '/api/auth/teacher/me',
        'teacher-profile',
      ]);
    }

    const selectedProtected = randomItem(protectedEndpoints);

    get(
      selectedProtected[0],
      selectedProtected[1],
      authenticatedHeaders,
    );
  }

  sleep(Math.random() * 1.0 + 0.2);
}
