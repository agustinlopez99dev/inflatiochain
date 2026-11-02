# InflatioChain

REST API that measures **how much a cryptocurrency's circulating supply has inflated over the last year**. Java / Spring Boot rewrite of the original Python (Flask) project.

For a given CoinGecko coin id it returns market data plus:

- the oldest circulating supply available (≈ 1 year ago), derived as `market_cap / price`
- `circulating_emission_percentage = (current_supply - old_supply) / old_supply * 100`

## Stack

Java 21 · Spring Boot 4 · Maven · Spring Cache + Caffeine · `RestClient` · springdoc (Swagger UI) · JUnit 5 + MockWebServer

## Run

```bash
./mvnw spring-boot:run
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health

## Endpoints

| Method | Path | Description | Cache |
|---|---|---|---|
| GET | `/api/v1/coins/list` | Top coins by market cap (`id`, `name`, `symbol`, `image`, `market_cap_rank`) | 1 day |
| GET | `/api/v1/coin/{coinId}` | Market data, price changes, ATH and supply inflation | 5 min per coin |

Errors use RFC 9457 `ProblemDetail`: `400` malformed id, `404` unknown coin, `502` CoinGecko unavailable or rate limited.

The `/coin/{coinId}` JSON keeps the same snake_case contract as the Python version. Fields CoinGecko does not provide (e.g. no history) are `null` instead of failing the request.

## Configuration

See [application.properties](src/main/resources/application.properties). Notable keys:

| Key | Default | Meaning |
|---|---|---|
| `coingecko.api-key` | empty | Optional CoinGecko demo key (sent as `x-cg-demo-api-key`) |
| `coingecko.list-pages` | `1` | Pages of 250 coins for the list (free tier: keep at 1) |
| `inflatiochain.cache.summary-ttl` | `5m` | TTL per coin summary |
| `inflatiochain.cache.summary-max-size` | `500` | Max cached coin summaries |
| `inflatiochain.cache.coin-list-ttl` | `1d` | TTL of the coin list |

## Test

```bash
./mvnw test
```

Integration tests run against a `MockWebServer` standing in for CoinGecko, including a check that a repeated request is served from cache.

## Differences from the Python version

- Cache is Caffeine with real TTL and size eviction instead of a hand-rolled `OrderedDict`.
- Inflation calculation is a pure, unit-tested class (`InflationCalculator`); it no longer crashes when history is missing.
- Input validation and proper HTTP status codes (the Flask version returned `200` with an `error` body).
- API only; the original single-page frontend is not ported.

Data: [CoinGecko](https://www.coingecko.com/) public API.
