package com.flashalpha;

import com.google.gson.annotations.SerializedName;

/**
 * Typed response model for {@code GET /v1/pricing/kelly} (Growth+, live only).
 *
 * <p>Kelly-criterion position sizing for a single option position, with
 * the underlying assumed log-normal at the option's expiry. Returns
 * three sizing variants (full, half, quarter Kelly), an analysis block
 * with expected ROI, probability of profit and ITM, max loss and
 * breakeven, and a plain-English {@link #recommendation} string.
 *
 * <p>About FlashAlpha: real-time options dealer-flow analytics. See
 * <a href="https://lab.flashalpha.com">https://lab.flashalpha.com</a>
 * and <a href="https://flashalpha.com">https://flashalpha.com</a>.
 */
public final class PricingKellyResponse {

    /** Echo of the request inputs. */
    @SerializedName("inputs")
    public Inputs inputs;

    /** Position sizing — Kelly fraction and the half / quarter risk-scaled variants. */
    @SerializedName("sizing")
    public Sizing sizing;

    /** Expected payoff, ROI, probabilities, max loss and breakeven. */
    @SerializedName("analysis")
    public Analysis analysis;

    /** Plain-English recommendation summary — safe to surface verbatim. */
    @SerializedName("recommendation")
    public String recommendation;

    /** Echo of the {@code /v1/pricing/kelly} request inputs. */
    public static final class Inputs {
        /** Underlying spot price. */
        @SerializedName("spot") public Double spot;
        /** Strike price. */
        @SerializedName("strike") public Double strike;
        /** Days to expiration. */
        @SerializedName("dte") public Double dte;
        /** Annualised implied volatility (decimal). */
        @SerializedName("sigma") public Double sigma;
        /** Premium paid for the option. */
        @SerializedName("premium") public Double premium;
        /** Expected annualised drift of the underlying (decimal). */
        @SerializedName("mu") public Double mu;
        /** Option type — {@code "call"} or {@code "put"}. */
        @SerializedName("type") public String type;
        /** Annualised risk-free rate (decimal). */
        @SerializedName("risk_free_rate") public Double riskFreeRate;
        /** Annualised continuous dividend yield (decimal). */
        @SerializedName("dividend_yield") public Double dividendYield;
    }

    /** Kelly sizing — three risk-scaled variants of the optimal position fraction. */
    public static final class Sizing {
        /** Full Kelly position fraction (decimal of bankroll). */
        @SerializedName("kelly_fraction") public Double kellyFraction;
        /** {@code kelly_fraction / 2}. */
        @SerializedName("half_kelly") public Double halfKelly;
        /** {@code kelly_fraction / 4}. */
        @SerializedName("quarter_kelly") public Double quarterKelly;
        /** Full Kelly as a percent (e.g. {@code 12.5} = 12.5% of bankroll). */
        @SerializedName("kelly_pct") public Double kellyPct;
        /** Half Kelly as a percent. */
        @SerializedName("half_kelly_pct") public Double halfKellyPct;
    }

    /** Expected-value, probability and risk metrics for the position. */
    public static final class Analysis {
        /** Expected return on investment (decimal). */
        @SerializedName("expected_roi") public Double expectedRoi;
        /** Expected ROI as a percent. */
        @SerializedName("expected_roi_pct") public Double expectedRoiPct;
        /** Expected payoff in currency units (signed). */
        @SerializedName("expected_payoff") public Double expectedPayoff;
        /** Probability the trade is profitable at expiry (decimal). */
        @SerializedName("probability_of_profit") public Double probabilityOfProfit;
        /** Probability of profit as a percent. */
        @SerializedName("probability_of_profit_pct") public Double probabilityOfProfitPct;
        /** Probability the option finishes ITM (decimal). */
        @SerializedName("probability_itm") public Double probabilityItm;
        /** Probability ITM as a percent. */
        @SerializedName("probability_itm_pct") public Double probabilityItmPct;
        /** Maximum possible loss (typically the premium for long options). */
        @SerializedName("max_loss") public Double maxLoss;
        /** Breakeven underlying price at expiry. */
        @SerializedName("breakeven") public Double breakeven;
        /** Expected log-growth rate of bankroll under repeated identical bets. */
        @SerializedName("expected_growth_rate") public Double expectedGrowthRate;
    }
}
