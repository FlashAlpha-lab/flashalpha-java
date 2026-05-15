# FlashAlpha Java SDK

Java client library for the [FlashAlpha](https://flashalpha.com) options analytics API.
Provides a **live options screener** (filter/rank symbols by GEX, VRP, IV, greeks, harvest
scores, and custom formulas), real-time gamma exposure (GEX), delta exposure (DEX), vanna
exposure (VEX), charm exposure (CHEX), 0DTE analytics, volatility surface, implied
volatility, Black-Scholes greeks, Kelly criterion position sizing, stock quotes, and option
chain data — all from a single, dependency-light Java 11+ package.

## Requirements

- Java 11 or later
- Maven 3.6+ or Gradle 7+

## Installation

### Maven

```xml
<dependency>
    <groupId>com.flashalpha</groupId>
    <artifactId>flashalpha</artifactId>
    <version>0.3.7</version>
</dependency>
```

### Gradle (Groovy DSL)

```groovy
implementation 'com.flashalpha:flashalpha:0.3.7'
```

### Gradle (Kotlin DSL)

```kotlin
implementation("com.flashalpha:flashalpha:0.3.7")
```

## Quick start

```java
import com.flashalpha.FlashAlphaClient;
import com.google.gson.JsonObject;

public class Example {
    public static void main(String[] args) {
        FlashAlphaClient client = new FlashAlphaClient(System.getenv("FLASHALPHA_API_KEY"));

        // Gamma exposure for SPY
        JsonObject gex = client.gex("SPY");
        System.out.println(gex);

        // Live stock quote
        JsonObject quote = client.stockQuote("AAPL");
        System.out.println(quote.get("last").getAsDouble());

        // Black-Scholes greeks
        JsonObject greeks = client.greeks(450.0, 455.0, 5.0, 0.20, "call", null, null);
        System.out.println(greeks);

        // Implied volatility from market price
        JsonObject iv = client.iv(450.0, 455.0, 5.0, 3.50, "call", null, null);
        System.out.println(iv);

        // Live options screener — harvestable VRP setups
        java.util.Map<String, Object> filters = java.util.Map.of(
            "op", "and",
            "conditions", java.util.List.of(
                java.util.Map.of("field", "regime", "operator", "eq", "value", "positive_gamma"),
                java.util.Map.of("field", "harvest_score", "operator", "gte", "value", 65)
            )
        );
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("filters", filters);
        body.put("sort", java.util.List.of(java.util.Map.of("field", "harvest_score", "direction", "desc")));
        body.put("select", java.util.List.of("symbol", "price", "harvest_score", "dealer_flow_risk"));
        JsonObject screen = client.screener(body);
        System.out.println(screen);
    }
}
```

## Authentication

All endpoints (except `/health` and `/v1/surface/{symbol}`) require an API key, which
is passed in the `X-Api-Key` HTTP request header. Obtain your key at
[flashalpha.com](https://flashalpha.com).

Set it as an environment variable and read it at runtime — never hard-code secrets in
source files.

## Error handling

All errors extend `FlashAlphaException` (a `RuntimeException`). Catch the specific
subclass you care about:

```java
import com.flashalpha.*;

try {
    JsonObject gex = client.gex("SPY");
} catch (AuthenticationException e) {
    // HTTP 401 — invalid or missing API key
    System.err.println("Bad key: " + e.getMessage());
} catch (TierRestrictedException e) {
    // HTTP 403 — endpoint requires a higher plan
    System.err.printf("Need %s, have %s%n", e.getRequiredPlan(), e.getCurrentPlan());
} catch (NotFoundException e) {
    // HTTP 404 — symbol or resource not found
} catch (RateLimitException e) {
    // HTTP 429 — slow down; retry after e.getRetryAfter() seconds
    Thread.sleep(e.getRetryAfter() * 1000L);
} catch (ServerException e) {
    // HTTP 5xx — server-side error
} catch (FlashAlphaException e) {
    // Catch-all for any other API error
    System.err.println("Status " + e.getStatusCode() + ": " + e.getMessage());
}
```

## API methods

All methods return `com.google.gson.JsonObject`.

### Market data

| Method | Description | Plan |
|--------|-------------|------|
| `stockQuote(ticker)` | Live stock quote (bid/ask/mid/last) | Any |
| `optionQuote(ticker)` | Option quotes with greeks for all contracts | Growth+ |
| `optionQuote(ticker, expiry, strike, type)` | Filtered option quotes | Growth+ |
| `surface(symbol)` | Volatility surface grid | Public |
| `stockSummary(symbol)` | Comprehensive stock summary | Any |

### Historical data

| Method | Description | Plan |
|--------|-------------|------|
| `historicalStockQuote(ticker, date, time)` | Minute-by-minute historical stock quotes | Any |
| `historicalOptionQuote(ticker, date, time, expiry, strike, type)` | Historical option quotes | Growth+ |

### Exposure analytics

| Method | Description | Plan |
|--------|-------------|------|
| `gex(symbol)` | Gamma exposure by strike | Any |
| `gex(symbol, expiration, minOi)` | Gamma exposure with filters | Any |
| `dex(symbol)` | Delta exposure by strike | Any |
| `dex(symbol, expiration)` | Delta exposure filtered by expiration | Any |
| `vex(symbol)` | Vanna exposure by strike | Any |
| `vex(symbol, expiration)` | Vanna exposure filtered by expiration | Any |
| `chex(symbol)` | Charm exposure by strike | Any |
| `chex(symbol, expiration)` | Charm exposure filtered by expiration | Any |
| `exposureLevels(symbol)` | Key support/resistance levels | Any |
| `exposureSummary(symbol)` | Full GEX/DEX/VEX/CHEX + hedging summary | Growth+ |
| `narrative(symbol)` | Verbal narrative analysis of exposure | Growth+ |
| `zeroDte(symbol)` | Real-time 0DTE analytics | Growth+ |
| `zeroDte(symbol, strikeRange)` | 0DTE analytics with custom strike range | Growth+ |
| `maxPain(symbol)` | Max pain analysis with dealer alignment, pain curve, pin probability | Growth+ |
| `maxPain(symbol, expiration)` | Max pain for a single expiry | Growth+ |

### Flow (live, simulation-aware) — requires the Alpha plan

Each method has a strongly-typed `*Typed` variant (e.g. `flowLevelsTyped`).

| Method | Description |
|--------|-------------|
| `flowLevels(symbol[, expiry])` | Live gamma flip / call & put walls / max pain |
| `flowPinRisk(symbol[, expiry])` | 0DTE pin-risk score + component breakdown |
| `flowSummary(symbol[, expiry])` | At-a-glance flow direction + headline GEX shift |
| `flowOi(symbol[, expiry])` | Open-interest simulator state (official vs intraday) |
| `flowGex(symbol[, expiry])` | Live (flow-adjusted) GEX + per-strike profile |
| `flowDex(symbol[, expiry])` | Live (flow-adjusted) DEX + per-strike profile |
| `flowDealerRisk(symbol[, expiry])` | Settled-vs-live dealer GEX/DEX + flow adjustment |
| `flowLive(symbol[, expiry])` | Everything-at-once live flow bundle |
| `flowOptionRecent(symbol, limit, expiry)` | Recent option trades, newest-first |
| `flowOptionSummary(symbol, expiry)` | Per-underlying option-flow aggregates |
| `flowOptionBlocks(symbol, minSize, expiry)` | Large option prints (`size >= minSize`) |
| `flowOptionHistory(symbol, minutes, expiry)` | Per-minute option-flow buckets |
| `flowOptionCumulative(symbol, minutes, expiry)` | Cumulative option net-flow series |
| `flowStockRecent(symbol, limit)` | Recent stock trades, newest-first |
| `flowStockSummary(symbol)` | Per-symbol stock-flow aggregates |
| `flowStockBlocks(symbol, minSize)` | Large stock prints (`size >= minSize`) |
| `flowStockHistory(symbol, minutes)` | Per-minute stock-flow buckets w/ OHLC |
| `flowStockCumulative(symbol, minutes)` | Cumulative stock net-flow series |
| `flowOptionsLeaderboard(n, windowMinutes)` | Cross-symbol option-flow leaderboard |
| `flowOptionsOutliers(limit, minTrades, windowMinutes)` | Cross-symbol option-flow outliers |
| `flowStocksLeaderboard(n, windowMinutes)` | Cross-symbol stock-flow leaderboard |
| `flowStocksOutliers(limit, minTrades, windowMinutes)` | Cross-symbol stock-flow outliers |

### Pricing and position sizing

| Method | Description | Plan |
|--------|-------------|------|
| `greeks(spot, strike, dte, sigma, type, r, q)` | Full BSM greeks (1st/2nd/3rd order) | Any |
| `iv(spot, strike, dte, price, type, r, q)` | Implied volatility from market price | Any |
| `kelly(spot, strike, dte, sigma, premium, mu, type, r, q)` | Kelly criterion position sizing | Growth+ |

### Volatility analytics

| Method | Description | Plan |
|--------|-------------|------|
| `volatility(symbol)` | Term structure, skew, realized vs implied | Growth+ |
| `advVolatility(symbol)` | SVI, variance surface, arbitrage detection | Alpha+ |

### Reference data

| Method | Description | Plan |
|--------|-------------|------|
| `tickers()` | All available stock tickers | Any |
| `options(ticker)` | Option chain metadata (expirations + strikes) | Any |
| `symbols()` | Currently queried symbols with live data | Any |

### Account and system

| Method | Description | Plan |
|--------|-------------|------|
| `account()` | Account info and quota usage | Any |
| `health()` | API health check | Public |

## Running tests

```bash
# Unit tests only (no API key required)
mvn test -Dtest=ClientTest

# Integration tests (requires live API key)
export FLASHALPHA_API_KEY=your_key_here
mvn test -Dtest=IntegrationTest

# All tests
mvn test
```

## Building from source

```bash
git clone https://github.com/FlashAlpha-lab/flashalpha-java.git
cd flashalpha-java
mvn package
```

## License

MIT. See [LICENSE](LICENSE).

## Other SDKs

| Language | Package | Repository |
|----------|---------|------------|
| Python | `pip install flashalpha` | [flashalpha-python](https://github.com/FlashAlpha-lab/flashalpha-python) |
| JavaScript | `npm i flashalpha` | [flashalpha-js](https://github.com/FlashAlpha-lab/flashalpha-js) |
| .NET | `dotnet add package FlashAlpha` | [flashalpha-dotnet](https://github.com/FlashAlpha-lab/flashalpha-dotnet) |
| Go | `go get github.com/FlashAlpha-lab/flashalpha-go` | [flashalpha-go](https://github.com/FlashAlpha-lab/flashalpha-go) |
| MCP | Claude / LLM tool server | [flashalpha-mcp](https://github.com/FlashAlpha-lab/flashalpha-mcp) |

## Links

- [FlashAlpha](https://flashalpha.com) — API keys, docs, pricing
- [API Documentation](https://flashalpha.com/docs)
- [Examples](https://github.com/FlashAlpha-lab/flashalpha-examples) — runnable tutorials
- [GEX Explained](https://github.com/FlashAlpha-lab/gex-explained) — gamma exposure theory and code
- [0DTE Options Analytics](https://github.com/FlashAlpha-lab/0dte-options-analytics) — 0DTE pin risk, expected move, dealer hedging
- [Volatility Surface Python](https://github.com/FlashAlpha-lab/volatility-surface-python) — SVI calibration, variance swap, skew analysis
- [Awesome Options Analytics](https://github.com/FlashAlpha-lab/awesome-options-analytics) — curated resource list
