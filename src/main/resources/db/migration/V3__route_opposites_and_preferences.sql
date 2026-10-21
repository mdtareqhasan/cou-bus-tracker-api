-- V3: opposite-route pairing + per-user route preferences.
--
-- Two features ride on this migration:
--
-- 1. routes.opposite_route_id — links the two physical directions of one
--    corridor ("Kandirpar -> Campus" <-> "Campus -> Kandirpar") so the app can
--    render a pair as one card. Self-referential FK, ON DELETE SET NULL (the
--    service refuses to delete an in-use route anyway; a clean delete just
--    drops the pairing).
--
--    Guardrails in the schema, not just the service:
--      * CHECK  — no route may be its own opposite
--      * UNIQUE — a route may be the opposite of at most one other route
--        (Postgres allows many NULLs in a unique index, so unpaired routes
--        are unaffected)
--
-- 2. user_route_preferences — server-side pins + per-route schedule-alert
--    opt-in, keyed by (user_type, user_id, route_id). Storing these on the
--    server (instead of device-local) means a rider's pins follow them across
--    phones, and gives the push dispatcher a per-route audience to target.

-- ---------------------------------------------------------------------------
-- Schema
-- ---------------------------------------------------------------------------

ALTER TABLE routes ADD COLUMN opposite_route_id BIGINT REFERENCES routes(id) ON DELETE SET NULL;

ALTER TABLE routes ADD CONSTRAINT chk_routes_no_self_opposite
    CHECK (opposite_route_id IS NULL OR opposite_route_id <> id);

CREATE UNIQUE INDEX uk_routes_opposite ON routes(opposite_route_id);

CREATE TABLE user_route_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_type VARCHAR(20) NOT NULL CHECK (user_type IN ('STUDENT', 'TEACHER')),
    user_id BIGINT NOT NULL,
    route_id BIGINT NOT NULL REFERENCES routes(id) ON DELETE CASCADE,
    is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    schedule_alerts_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_route_pref UNIQUE (user_type, user_id, route_id)
);

-- The dispatcher looks up a route's alert subscribers on every push.
CREATE INDEX idx_user_route_pref_route_alerts
    ON user_route_preferences(route_id, schedule_alerts_enabled);

-- ---------------------------------------------------------------------------
-- Data fix-up: split mixed-direction routes into one row per direction.
--
-- Historically a route row carried BOTH directions ("Kandirpar -> Campus"
-- held the 07:00 UP trip and the 16:00 DOWN trip), which makes "the route's
-- schedules" ambiguous and blocks pair-aware UI. The invariant we enforce
-- here: one routes row = one physical direction of travel.
--
-- The fix is a swap between each route and its reverse:
--   * the route's DOWN schedules move to the reverse route, and
--   * the reverse route's UP schedules move back to the route.
-- Direction labels (UP = toward campus, DOWN = away) stay attached to the
-- trip, so after the swap every row holds only trips travelling along its
-- own name.
--
-- Only routes with BOTH directions are touched. The reverse route is adopted
-- if a route with the reversed name already exists (matched whitespace-
-- insensitively, e.g. an admin-typed double space doesn't defeat the match),
-- otherwise it is created. Every decision is logged via RAISE NOTICE — run
-- with `docker logs cou-bus-tracker-app` in mind; anything "left for manual
-- review" needs an admin to look, nothing is silently half-fixed.
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    src RECORD;
    rev_id BIGINT;
    rev_opposite BIGINT;
    rev_en TEXT;
    rev_bn TEXT;
    rev_norm TEXT;
    moved_down BIGINT;
    moved_up BIGINT;
BEGIN
    FOR src IN
        SELECT r.id, r.name_en, r.name_bn
        FROM routes r
        WHERE r.opposite_route_id IS NULL
          AND EXISTS (SELECT 1 FROM schedules s WHERE s.route_id = r.id AND s.direction = 'DOWN')
          AND EXISTS (SELECT 1 FROM schedules s WHERE s.route_id = r.id AND s.direction = 'UP')
        ORDER BY r.id
    LOOP
        -- The FOR cursor was materialized before any pairing happened, so a
        -- route that has meanwhile become another route's reverse is already
        -- fixed up — skip it rather than swapping the schedules back.
        IF EXISTS (SELECT 1 FROM routes r WHERE r.id = src.id AND r.opposite_route_id IS NOT NULL) THEN
            CONTINUE;
        END IF;

        -- Reverse the stop list: "A -> B -> C" becomes "C -> B -> A".
        SELECT string_agg(part, ' → ' ORDER BY ord DESC) INTO rev_en
        FROM unnest(string_to_array(src.name_en, ' → ')) WITH ORDINALITY AS t(part, ord);

        SELECT string_agg(part, ' → ' ORDER BY ord DESC) INTO rev_bn
        FROM unnest(string_to_array(src.name_bn, ' → ')) WITH ORDINALITY AS t(part, ord);

        rev_norm := regexp_replace(btrim(rev_en), '\s+', ' ', 'g');
        IF rev_norm = regexp_replace(btrim(src.name_en), '\s+', ' ', 'g') THEN
            RAISE NOTICE 'route % (%): name reverses to itself — leaving mixed directions for manual review',
                src.id, src.name_en;
            CONTINUE;
        END IF;

        SELECT r.id, r.opposite_route_id INTO rev_id, rev_opposite
        FROM routes r
        WHERE r.id <> src.id
          AND (regexp_replace(btrim(r.name_en), '\s+', ' ', 'g') = rev_norm
               OR regexp_replace(btrim(r.name_bn), '\s+', ' ', 'g')
                    = regexp_replace(btrim(rev_bn), '\s+', ' ', 'g'))
        ORDER BY r.id
        LIMIT 1;

        IF NOT FOUND THEN
            INSERT INTO routes (name_bn, name_en) VALUES (rev_bn, rev_en) RETURNING id INTO rev_id;
            rev_opposite := NULL;
            RAISE NOTICE 'route % (%): created reverse route % (%)',
                src.id, src.name_en, rev_id, rev_en;
        ELSE
            RAISE NOTICE 'route % (%): adopting existing route % as its reverse',
                src.id, src.name_en, rev_id;
        END IF;

        IF rev_opposite IS NOT NULL AND rev_opposite <> src.id THEN
            RAISE NOTICE 'route %: reverse route % is already paired with route % — schedules split, pairing skipped',
                src.id, rev_id, rev_opposite;
        END IF;

        UPDATE schedules SET route_id = rev_id WHERE route_id = src.id AND direction = 'DOWN';
        GET DIAGNOSTICS moved_down = ROW_COUNT;

        UPDATE schedules SET route_id = src.id WHERE route_id = rev_id AND direction = 'UP';
        GET DIAGNOSTICS moved_up = ROW_COUNT;

        RAISE NOTICE 'route % (%): moved % DOWN schedule(s) to route %, % UP schedule(s) back from route %',
            src.id, src.name_en, moved_down, rev_id, moved_up, rev_id;

        UPDATE routes SET opposite_route_id = rev_id WHERE id = src.id;
        IF rev_opposite IS NULL THEN
            UPDATE routes SET opposite_route_id = src.id WHERE id = rev_id;
        END IF;
    END LOOP;
END $$;
