# InflatioChain: improvements spec

Goal: make the repo a strong **Java portfolio piece** (interview-ready): clean architecture, tests, resilience to the CoinGecko free tier, visible polish.

## 1. Data quality fields (flat, backward compatible)

Keep every existing snake_case field of `GET /api/v1/coin/{id}`. Add:

| Field | Type | Meaning |
|---|---|---|
| `supply_estimated` | boolean | Always `true` when `old_circulating_supply` was derived from `market_cap / price` |
| `window_days` | long/null | Real days of history used (same as `days_since_old_data`, kept explicit for clients) |
| `stale` | boolean | `true` when served from the long-lived fallback cache after an upstream failure |

- `historical_note` text should state the method ("Estimated as market_cap / price").
- Update `CoinSummary`, `CoinService`, README, and the integration test contract assertions.

## 2. Stale fallback on CoinGecko failure

- Add a second Caffeine cache `coinSummaryStale` (**last known good**), TTL **7 days**, same max size as `coinSummary`. Configurable: `inflatiochain.cache.stale-ttl` (default 7d).
- On every successful build of a summary, write it to both caches.
- On `UpstreamException` (429, 5xx, network) in `getCoinSummary`: if a stale entry exists return it with `stale=true` (`last_updated` keeps the original build time); otherwise propagate the 502.
- `CoinNotFoundException` (404) must **not** fall back to stale.
- Implement explicitly in `CoinService` (not only via `@Cacheable`), since the fallback needs control flow. Consider extracting a `SummaryCache` collaborator to keep `CoinService` small.
- Same fallback for `coins/list`.
- Tests: upstream returns 429 after a successful call and cache cleared only on the fresh cache -> response 200 with `stale=true`; no stale entry -> 502; 404 -> 404.

## 3. Cache stampede + prewarming

- Concurrent misses for the same coin must trigger one upstream pair of calls: use `@Cacheable(sync = true)` or a per-key lock around the build.
- Scheduled prewarm job (`@Scheduled`, `@EnableScheduling`) refreshing the **top N** coins from the cached coin list, **throttled** to respect the free tier (~8 req/min; each coin = 2 calls, so roughly one coin every 15 s).
  - Properties: `inflatiochain.prewarm.enabled` (default `true`), `top-n` (default `15`, intended range 10-20), `interval` (default aligned to the summary TTL, 5m).
  - Auto-suspend for a cool-down (e.g. 2 min) after any `UpstreamException` with 429, then resume.
  - Disabled in tests (`inflatiochain.prewarm.enabled=false` in test properties).
- Tests: concurrency test (N threads, same coin, request count == 2); prewarm unit test with a fake clock/sleeper (no real sleeping).

## 4. Own rate limiting (Bucket4j)

- Servlet filter on `/api/**`, key = client IP (respect `X-Forwarded-For` only when configured), **60 requests/minute**.
- Over limit: `429` as `ProblemDetail` with `Retry-After` header.
- Properties: `inflatiochain.rate-limit.requests-per-minute` (default 60), `enabled`.
- Evict idle buckets (Caffeine with `expireAfterAccess`) so the map cannot grow unbounded.
- Tests: 61st request in a minute -> 429 with `Retry-After`; different IP unaffected.

## 5. Minimal static frontend

- Scope: **bare minimum demo**: search input (datalist from `/api/v1/coins/list`) and one card showing name, price, market cap, **inflation %**, old vs current supply, and a small badge when `stale` or `supply_estimated`.
- Plain HTML/CSS/JS in `src/main/resources/static/` (no Thymeleaf).
- Do not port the original bugs: no use of an undefined `data`, a single fetch per selection (no duplicate refetch on the 5-minute timer), handle `404`/`502`/`429` with an inline message instead of `alert`.
- Default coin: `bitcoin` on load.

## 6. Housekeeping (portfolio polish)

- Fix `README.md`: it currently says API only; update for static demo page, stale/prewarm/rate-limit behaviour and new properties.
- Remove leftover noise: `Mockito self-attaching` warning (add Mockito agent to surefire `argLine` if cheap), keep `./mvnw test` output clean.
- Springdoc: disable or document `/v3/api-docs` exposure via property for production.
- Out of scope (decided): Docker, GitHub Actions CI, JPA/database persistence, React frontend, Redis, API v2.

## Open questions / risks

- CoinGecko free tier limits change; limits above are assumptions. Keep every number configurable.
- Stale data for **price** can mislead after days; the `stale` flag and `last_updated` are the mitigation. Revisit the 7-day TTL if clients treat price as live.
- `Retry-After` for the prewarm cool-down is a fixed guess, not read from CoinGecko headers; consider honouring the upstream `Retry-After` if present.
- Behind a proxy, IP-based limiting needs a trusted `X-Forwarded-For` policy to avoid spoofing.

## Suggested order

1. Quality fields (small, low risk)
2. Stale fallback + tests
3. Stampede protection
4. Prewarm job
5. Bucket4j filter
6. Static demo page
7. README/housekeeping
