# FlashAlpha Java SDK

Java client library for the [FlashAlpha](https://flashalpha.com) options analytics API.
Provides a **live options screener** (filter/rank symbols by GEX, VRP, IV, greeks, harvest
scores, and custom formulas), real-time gamma exposure (GEX), delta exposure (DEX), vanna
exposure (VEX), charm exposure (CHEX), 0DTE analytics, volatility surface, implied
volatility, Black-Scholes greeks, Kelly criterion position sizing, stock quotes, and option
chain data — all from a single, dependency-light Java 11+ package.

> 🔑 **[Get a free API key at flashalpha.com →](https://flashalpha.com)** · 📚 [API documentation](https://flashalpha.com/docs) · 💹 [FlashAlpha options analytics API](https://flashalpha.com)

## Requirements

- Java 11 or later
- Maven 3.6+ or Gradle 7+

## Installation

### Maven

```xml
<dependency>
    <groupId>com.flashalpha</groupId>
    <artifactId>flashalpha</artifactId>
    <version>1.2.1</version>
</dependency>
```

### Gradle (Groovy DSL)

```groovy
implementation 'com.flashalpha:flashalpha:1.2.1'
```

### Gradle (Kotlin DSL)

```kotlin
implementation("com.flashalpha:flashalpha:1.2.1")
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
| `surfaceSvi(symbol)` | SVI-fitted vol surface — calibrated `(a, b, rho, m, sigma)` params, per-expiry forward, ATM total variance and ATM IV | Alpha+ |
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
| `dex(symbol)` | Delta exposure by strike | Basic+ |
| `dex(symbol, expiration)` | Delta exposure filtered by expiration | Basic+ |
| `vex(symbol)` | Vanna exposure by strike | Basic+ |
| `vex(symbol, expiration)` | Vanna exposure filtered by expiration | Basic+ |
| `chex(symbol)` | Charm exposure by strike | Basic+ |
| `chex(symbol, expiration)` | Charm exposure filtered by expiration | Basic+ |
| `exposureLevels(symbol)` | Key support/resistance levels | Any |
| `exposureSummary(symbol)` | Full GEX/DEX/VEX/CHEX + hedging summary | Growth+ |
| `narrative(symbol)` | Verbal narrative analysis of exposure | Growth+ |
| `zeroDte(symbol)` | Real-time 0DTE analytics | Growth+ |
| `zeroDte(symbol, strikeRange)` | 0DTE analytics with custom strike range | Growth+ |
| `zeroDte(symbol, strikeRange, expiry)` | 0DTE analytics for a specific expiry | Growth+ |
| `maxPain(symbol)` | Max pain analysis with dealer alignment, pain curve, pin probability | Basic+ |
| `maxPain(symbol, expiration)` | Max pain for a single expiry | Basic+ |
| `exposureSheet(symbol[, expiration, minOi])` | Unified per-strike sheet — GEX/DEX/VEX/CHEX + DAG, chain totals, Line-in-the-Sand inflection strike, gamma peaks, OPEX / triple-witching flags | Growth+ |
| `exposureTermStructure(symbol)` | Net GEX/DEX/VEX/CHEX aggregated by DTE bucket and rolled up per expiry | Growth+ |
| `exposureBasket(symbols[, weights])` | Weighted cross-symbol exposure basket — aggregate net GEX/DEX/VEX/CHEX across up to 50 symbols | Growth+ |
| `oiDiff(symbol[, topN])` | Day-over-day open-interest deltas, top-N changes by magnitude, call/put aggregate totals | Growth+ |

### Flow (live, simulation-aware) — Growth+ (raw tape, unusual-flow signals, OI simulator state & the full live bundle are Alpha)

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
| `flowSignals(symbol, minScore, intent, structure, windowMinutes, limit, expiry)` | Scored, classified unusual-flow feed (block/sweep, intent, 0-100 score) |
| `flowSignalsSummary(symbol, windowMinutes, expiry)` | Net bullish/bearish + opening/closing premium roll-up + top 10 signals |
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
| `flowStockBars(symbol, resolution[, minutes])` | Per-resolution stock-flow OHLC bars (`1s/1m/5m/15m/30m/1h/4h`) |
| `flowDealerPremium(symbol[, windowMinutes, expiry])` | Full-tape Net Dealer Premium roll-up over a window |
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
| `liquidity(symbol)` | Per-expiry execution / liquidity score (0-100) — ATM bid-ask spread %, OI-weighted spread %, ATM OI depth, best/worst expiry | Growth+ |
| `skewTerm(symbol)` | Skew term structure — ATM IV, 25Δ / 10Δ wing IVs, `skew_25d`, `risk_reversal_25d`, `butterfly_25d`, `tail_convexity` | Growth+ |
| `spotVolCorrelation(symbol)` | Rolling spot-vol correlation (empirical leverage / spot-vol-beta) | Growth+ |
| `dispersion(index, symbols[, weights, horizonDays])` | Implied-vs-realized dispersion / vol-arb between index and constituent basket, per-constituent contribution | Alpha+ |
| `expectedMove(symbol[, expiry])` | Straddle-implied expected move per expiry from ATM IV | Basic+ |
| `realizedVolatility(symbol)` | Range-based realized vol over 10 / 20 / 30-day windows — close-to-close, Parkinson, Garman-Klass, Rogers-Satchell, Yang-Zhang | Alpha+ |
| `volatilityForecast(symbol[, dist])` | Conditional vol forecasts — EWMA (λ=0.94), HAR-RV, GARCH(1,1) MLE with persistence, half-life, and multi-horizon forecast path | Alpha+ |

### Variance risk premium (VRP)

| Method | Description | Plan |
|--------|-------------|------|
| `vrp(symbol)` | Variance risk premium — IV-vs-RV spread, directional skew, GEX-conditioned harvest scores, short-vol strategy scores, term VRP curve | Alpha+ |
| `vrp(symbol, date)` | Point-in-time VRP for a specific historical date | Alpha+ |
| `vrpHistory(symbol[, days])` | Trailing series of daily VRP snapshots | Alpha+ |

### Strategy signals

Ten decision-grade strategy signals sharing the `StrategyDecisionResponse` envelope (recommendation, conviction, rationale, suggested structure). Each has a `*Typed` variant.

| Method | Description | Plan |
|--------|-------------|------|
| `strategyFlowAnomaly(symbol[, expiry])` | Directional flow-anomaly signal | Growth+ |
| `strategyExpiryPositioning(symbol[, expiry, minOpenInterest, wingWidth])` | OPEX pin / expiry-positioning signal | Basic+ |
| `strategyZeroDte(symbol[, expiry, minOpenInterest, wingWidth])` | 0DTE range-compression signal | Growth+ |
| `strategyDealerRegime(symbol[, expiry])` | Dealer gamma-regime signal | Growth+ |
| `strategyVolCarry(symbol[, expiry, minOpenInterest, targetShortDelta, maxWidth, minCredit])` | Vol-carry / VRP harvest signal | Alpha+ |
| `strategyYieldEnhancement(symbol[, expiry, targetDelta, minOpenInterest, structure, excludeEarningsBeforeExpiry])` | Covered-call / cash-secured-put yield signal | Growth+ |
| `strategySurfaceAnomaly(symbol[, expiry])` | SVI surface-anomaly signal | Alpha+ |
| `strategySkew(symbol[, expiry])` | 25-delta skew signal | Growth+ |
| `strategyTermStructure(symbol)` | ATM term-structure signal | Growth+ |
| `strategyTailPricing(symbol[, expiry])` | Downside-tail-pricing signal | Growth+ |

### Earnings

| Method | Description | Plan |
|--------|-------------|------|
| `earningsCalendar([days, symbols, importance])` | Upcoming earnings calendar over a forward window | Growth+ |
| `earningsExpectedMove(symbol)` | Straddle-implied earnings expected move | Growth+ |
| `earningsHistory(symbol[, limit])` | Historical post-earnings move history | Growth+ |
| `earningsIvCrush(symbol)` | Earnings IV-crush profile | Growth+ |
| `earningsVrp(symbol)` | Earnings variance-risk-premium read | Alpha+ |
| `earningsDealerPositioning(symbol)` | Earnings dealer-positioning read | Alpha+ |
| `earningsStrategies(symbol)` | Suggested earnings option structures | Alpha+ |
| `earningsScreener([sort, limit, days, minImportance])` | Rank upcoming earnings (e.g. `vrp_richest`) | Growth+ |

### Structures (multi-leg, pure math)

POST endpoints — no symbol resolution; supply legs via `StructureRequest` / `StructureGreeksRequest` (`StructureLeg.pnlLeg(...)` / `StructureLeg.greeksLeg(...)`).

| Method | Description | Plan |
|--------|-------------|------|
| `structurePnl(request)` | At-expiry P&L curve, breakevens, max profit/loss for an arbitrary multi-leg structure | Basic+ |
| `structureGreeks(request)` | Aggregate Black-Scholes greeks across a multi-leg position (per-leg expiry + IV) | Basic+ |

### Zero-DTE flow

| Method | Description | Plan |
|--------|-------------|------|
| `flowZeroDteSnapshot(symbol)` | Intraday 0DTE flow snapshot + net `flow_direction` | Growth+ |
| `flowZeroDteSeries(symbol[, bar, minutes])` | Intraday 0DTE bucketed-flow time series | Growth+ |
| `flowZeroDteHedgeFlow(symbol[, side, bar, minutes])` | Intraday 0DTE dealer hedge-flow series | Growth+ |
| `flowZeroDteHeatmap(symbol[, metric, mode, bar, minutes])` | Intraday 0DTE strike × time heatmap | Alpha+ |
| `flowZeroDteStrikeFlow(symbol[, bar, minutes])` | Intraday 0DTE per-strike flow | Growth+ |

### Macro and universe

| Method | Description | Plan |
|--------|-------------|------|
| `vixState()` | Composite VIX-state read (level, regime, term structure) | Growth+ |
| `universe([sort, limit])` | Curated tier-1 / tier-2 symbol directory (pre-warmed universe) | Public |

### Screener

| Method | Description | Plan |
|--------|-------------|------|
| `screener(body)` | Live options screener — filter / rank symbols by GEX, VRP, IV, greeks, harvest scores, and custom formulas | Growth+ |

v1.1 adds screenable fields covering the new analytics: `expected_move`, `liquidity_score`, `skew_25d`, `risk_reversal_25d`, `butterfly_25d`, `tail_convexity`, `spot_vol_corr`, `oi_diff_call`, `oi_diff_put`, `vix_state`, plus the per-strike `exposure_sheet` columns — combinable with existing `regime`, `harvest_score`, `dealer_flow_risk`, `vrp_regime`, `atm_iv`, `net_gex`, and `term_state` fields.

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

## Futures (CME equity-index)

FlashAlpha serves the full options-analytics stack for **CME equity-index futures** — **`ES=F`** (E-mini S&P 500) and **`NQ=F`** (E-mini Nasdaq-100). Options-on-futures are priced with **Black-76** (forward-priced) using the correct CME contract multipliers. Everything that works for an equity works for futures: gamma exposure (GEX), DEX, VEX, CHEX, key levels, max pain, the IV surface, exposure summary, narrative, and live flow.

```java
// Gamma exposure for the E-mini S&P 500 future
JsonObject gex = client.gex("ES=F");
System.out.println(gex);
```

Use the `=F` suffix — bare `ES`/`NQ` are equities, not futures. In raw REST paths URL-encode the `=` as `%3D` (e.g. `GET /v1/exposure/gex/ES%3DF`); SDK methods take the plain string `"ES=F"`. Historical replay for futures is coming; live analytics are available now.

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

## What the paid tiers unlock

The free tier covers single-expiry GEX on equities, key levels, the BSM Greeks/IV
calculator and stock quotes. Paid tiers add:

- **DEX, VEX (vanna) and CHEX (charm) exposure, plus max pain** — from the **Basic tier**
  ($79/mo), with ETF and index symbols.
- **Full-chain GEX, 0DTE and flow analytics** — from the **Growth tier** ($299/mo).
- **Point-in-time replay since 2018, SVI vol surfaces, VRP analytics, higher-order Greeks**,
  uncached and unlimited — the **Alpha tier** ($1,499/mo). FlashAlpha is one of the only
  public APIs publishing aggregate vanna and charm exposure across the full universe, with
  no look-ahead and no training-serving skew.

Built for quants, prop desks, and vol funds. See the full picture and get a key:
**[flashalpha.com/for-quant-teams](https://flashalpha.com/for-quant-teams?utm_source=github&utm_medium=readme&utm_campaign=repo-flashalpha-java)**
