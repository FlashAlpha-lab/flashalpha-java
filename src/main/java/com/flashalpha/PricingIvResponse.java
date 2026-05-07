package com.flashalpha;

import com.google.gson.annotations.SerializedName;

/**
 * Typed response model for {@code GET /v1/pricing/iv} (Free+, live only).
 *
 * <p>Inverts the Black-Scholes-Merton formula to recover the implied
 * volatility consistent with a quoted option price. Echoes the request
 * inputs, then returns {@link #impliedVolatility} as a decimal
 * (e.g. {@code 0.185}) and {@link #impliedVolatilityPct} as the same
 * value scaled to a percent (e.g. {@code 18.5}).
 *
 * <p>About FlashAlpha: real-time options dealer-flow analytics. See
 * <a href="https://lab.flashalpha.com">https://lab.flashalpha.com</a>
 * and <a href="https://flashalpha.com">https://flashalpha.com</a>.
 */
public final class PricingIvResponse {

    /** Echo of the request inputs. */
    @SerializedName("inputs")
    public Inputs inputs;

    /** Implied volatility, decimal (e.g. {@code 0.185} = 18.5%). */
    @SerializedName("implied_volatility")
    public Double impliedVolatility;

    /** Implied volatility, percent (e.g. {@code 18.5}). Same number scaled by 100. */
    @SerializedName("implied_volatility_pct")
    public Double impliedVolatilityPct;

    /** Echo of the {@code /v1/pricing/iv} request inputs. */
    public static final class Inputs {
        /** Underlying spot price. */
        @SerializedName("spot") public Double spot;
        /** Strike price. */
        @SerializedName("strike") public Double strike;
        /** Days to expiration. */
        @SerializedName("dte") public Double dte;
        /** Market option price the IV is solved for. */
        @SerializedName("price") public Double price;
        /** Option type — {@code "call"} or {@code "put"}. */
        @SerializedName("type") public String type;
        /** Annualised risk-free rate (decimal). */
        @SerializedName("risk_free_rate") public Double riskFreeRate;
        /** Annualised continuous dividend yield (decimal). */
        @SerializedName("dividend_yield") public Double dividendYield;
    }
}
