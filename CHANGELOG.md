# Changelog

All notable changes to the FlashAlpha Java SDK are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/), and this
project adheres to [Semantic Versioning](https://semver.org/).

## 1.1.0 - 2026-06-08

Major endpoint-parity release. Adds whole new analytics families and back-compatible
parameter extensions. No existing public method signatures changed.

### Added

- **Strategy Signals (×10)** — one named method per discretionary options strategy,
  all returning the shared `StrategyDecisionResponse` decision envelope (decision,
  0-100 score, ranked best structures, why / avoid-if rationale, risk flags, data
  quality): `strategyFlowAnomaly`, `strategyExpiryPositioning`, `strategyZeroDte`,
  `strategyDealerRegime`, `strategyVolCarry`, `strategyYieldEnhancement`,
  `strategySurfaceAnomaly`, `strategySkew`, `strategyTermStructure`,
  `strategyTailPricing` (each with a `*Typed` variant).
- **Earnings analytics (×8)** — `earningsCalendar`, `earningsExpectedMove`,
  `earningsHistory`, `earningsIvCrush`, `earningsVrp`, `earningsDealerPositioning`,
  `earningsStrategies`, `earningsScreener`.
- **Multi-leg Structures (×2, POST, pure-math)** — `structurePnl` /
  `structurePnlTyped` (at-expiry P&L curve, breakevens, max profit/loss) and
  `structureGreeks` / `structureGreeksTyped` (aggregate position Greeks). New
  request/response types: `StructureLeg`, `StructureRequest`,
  `StructureGreeksRequest`, `StructurePnlResponse`, `StructureGreeksResponse`.
- **Zero-DTE Flow (×5)** — intraday 0DTE flow: `flowZeroDteSnapshot`,
  `flowZeroDteSeries`, `flowZeroDteHedgeFlow`, `flowZeroDteHeatmap`,
  `flowZeroDteStrikeFlow`.
- **Dispersion / vol-arb** — `dispersion` (implied-vs-realized correlation between an
  index and a constituent basket).
- **Liquidity** — `liquidity` / `liquidityTyped` (per-expiry options execution score)
  via `LiquidityResponse`.
- **Skew term structure** — `skewTerm` / `skewTermTyped` (25Δ / 10Δ skew, risk
  reversal, butterfly, tail convexity) via `SkewTermResponse`.
- **Spot-vol correlation** — `spotVolCorrelation`.
- **VIX state** — `vixState` (composite macro VIX regime read).
- **Universe** — `universe` (curated tier-1 / tier-2 pre-warmed symbol directory).
- **SVI surface** — `surfaceSvi` / `surfaceSviTyped` via `SurfaceSviResponse`.
- **Expected move** — `expectedMove` / `expectedMoveTyped` via `ExpectedMoveResponse`.
- **VRP history** — `vrpHistory` (trailing daily VRP series).
- **Exposure sheet / term-structure / basket / OI-diff** — `exposureSheet`,
  `exposureTermStructure`, `exposureBasket`, `oiDiff` (each with a `*Typed`
  variant) via `ExposureSheetResponse`, `ExposureTermStructureResponse`,
  `ExposureBasketResponse`, `OiDiffResponse`.
- **Dealer premium** — `flowDealerPremium` (full-tape Net Dealer Premium roll-up).
- **Flow stock bars** — `flowStockBars` (per-resolution stock-flow OHLC bars).
- **Screener fields** — `screenerFields` (selectable / filterable field catalogue).

### Changed

- `zeroDte` gained a back-compatible overload accepting an `expiry`:
  `zeroDte(symbol, strikeRange, expiry)` (and `zeroDteTyped(symbol, strikeRange, expiry)`).
  The existing `zeroDte(symbol)` and `zeroDte(symbol, strikeRange)` overloads are
  unchanged.
- `vrp` gained a back-compatible overload accepting a point-in-time `date`:
  `vrp(symbol, date)` (and `vrpTyped(symbol, date)`). The existing `vrp(symbol)`
  overload is unchanged.

## 1.0.1 - 2025-05-21

### Added

- Live Flow API tier — 24 simulation-aware `flow*` endpoints plus scored flow
  signals (`flowSignals`, `flowSignalsSummary`).

## 1.0.0

- Initial general-availability release.
