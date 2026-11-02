# InflatioChain

REST API that measures **how much a cryptocurrency's circulating supply has inflated over the last year**. Java / Spring Boot rewrite of the original Python (Flask) project.

For a given CoinGecko coin id it returns market data plus:

- the oldest circulating supply available (≈ 1 year ago), derived as `market_cap / price`
- `circulating_emission_percentage = (current_supply - old_supply) / old_supply * 100`

## Stack

Java 21 · Spring Boot 4 · Maven · Caffeine · Bucket4j · `RestClient` · springdoc (Swagger UI) · JUnit 5 + Mockito + MockWebServer

## Run

```bash
./mvnw spring-boot:run
```

- Demo page: http://localhost:8080/
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health

## Endpoints

| Method | Path | Description | Cache |
|---|---|---|---|
| GET | `/api/v1/coins/list` | Top coins by market cap (`id`, `name`, `symbol`, `image`, `market_cap_rank`) | 1 day |
| GET | `/api/v1/coin/{coinId}` | Market data, price changes, ATH and supply inflation | 5 min per coin |

Errors use RFC 9457 `ProblemDetail`: `400` malformed id, `404` unknown coin, `429` you exceeded our rate limit (with `Retry-After`), `503` (with `Retry-After`) CoinGecko's rate limit was hit and no stale copy exists, `502` CoinGecko failed in any other way and no stale copy exists.

The `/coin/{coinId}` JSON keeps the same snake_case contract as the Python version. Fields CoinGecko does not provide (e.g. no history) are `null` instead of failing the request.

Besides the original fields, the summary carries data-quality flags:

| Field | Meaning |
|---|---|
| `supply_estimated` | Old supply was derived as `market_cap / price`, not read directly |
| `window_days` | Real days of history behind the `old_*` values |
| `stale` | Served from the last-known-good copy because CoinGecko failed |

## Resilience

- **Stale fallback:** every successful result is also kept for 7 days. If CoinGecko fails (429, 5xx, network) that copy is returned with `stale: true`. A 404 is never masked.
- **No cache stampede:** concurrent misses for the same coin share a single load (2 CoinGecko calls total).
- **Prewarm:** a scheduled job refreshes the top N coins one at a time, throttled for the free tier, and pauses after a 429.
- **Own rate limit:** 60 requests/minute per IP on `/api/**` (Bucket4j). Enable `trust-forwarded-for` only behind a trusted proxy.

## Configuration

See [application.properties](src/main/resources/application.properties). Notable keys:

| Key | Default | Meaning |
|---|---|---|
| `coingecko.api-key` | empty | Optional CoinGecko demo key (sent as `x-cg-demo-api-key`) |
| `coingecko.list-pages` | `1` | Pages of 250 coins for the list (free tier: keep at 1) |
| `inflatiochain.cache.summary-ttl` | `5m` | TTL per coin summary |
| `inflatiochain.cache.summary-max-size` | `500` | Max cached coin summaries |
| `inflatiochain.cache.coin-list-ttl` | `1d` | TTL of the coin list |
| `inflatiochain.cache.stale-ttl` | `7d` | How long last-known-good copies are kept |
| `inflatiochain.prewarm.enabled` / `top-n` / `throttle` / `cooldown` | `true` / `5` / `15s` / `2m` | Background refresh of the top coins |
| `inflatiochain.rate-limit.requests-per-minute` | `60` | Per-IP limit on `/api/**` |
| `inflatiochain.rate-limit.trust-forwarded-for` | `false` | Use `X-Forwarded-For` as client IP |

## Test

```bash
./mvnw test
```

Integration tests run against a `MockWebServer` standing in for CoinGecko and cover caching, stale fallback, concurrent misses, and rate limiting. The prewarm job is unit tested with a fake clock and sleeper.

## Differences from the Python version

- Cache is Caffeine with real TTL and size eviction instead of a hand-rolled `OrderedDict`, plus stale fallback, prewarm and per-IP rate limiting.
- Inflation calculation is a pure, unit-tested class (`InflationCalculator`); it no longer crashes when history is missing.
- Input validation and proper HTTP status codes (the Flask version returned `200` with an `error` body).
- The original frontend is replaced by a minimal static demo page (`src/main/resources/static`).

Data: [CoinGecko](https://www.coingecko.com/) public API.
