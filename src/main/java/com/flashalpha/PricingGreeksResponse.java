package com.flashalpha;

import com.google.gson.annotations.SerializedName;

/**
 * Typed response model for {@code GET /v1/pricing/greeks}.
 *
 * <p>Full-stack Black-Scholes greeks — first, second, and third order
 * — plus the additional {@code lambda} (option elasticity) and
 * {@code veta} (vega decay) sensitivities. FlashAlpha exposes the
 * complete derivatives table so callers can build their own
 * second-order risk views (vanna / charm / vomma / speed / zomma /
 * color / ultima) without re-deriving the math client-side.
 *
 * <p>About FlashAlpha: real-time options dealer-flow analytics. See
 * <a href="https://lab.flashalpha.com">https://lab.flashalpha.com</a>
 * and <a href="https://flashalpha.com">https://flashalpha.com</a>.
 */
public final class PricingGreeksResponse {

    /** Echo of the request inputs. Useful for round-trip logging. */
    @SerializedName("inputs")
    public Inputs inputs;

    /** Black-Scholes theoretical option price. */
    @SerializedName("theoretical_price")
    public Double theoreticalPrice;

    /** First-order greeks (delta, gamma, theta, vega, rho). */
    @SerializedName("first_order")
    public FirstOrder firstOrder;

    /** Second-order greeks (vanna, charm, vomma, dual delta). */
    @SerializedName("second_order")
    public SecondOrder secondOrder;

    /** Third-order greeks (speed, zomma, color, ultima). */
    @SerializedName("third_order")
    public ThirdOrder thirdOrder;

    /** Additional sensitivities (lambda, veta). */
    @SerializedName("additional")
    public Additional additional;

    /** Echo of the pricing inputs. */
    public static final class Inputs {
        /** Underlying spot price. */
        @SerializedName("spot") public Double spot;
        /** Strike price. */
        @SerializedName("strike") public Double strike;
        /** Days to expiration. */
        @SerializedName("dte") public Double dte;
        /** Volatility input (decimal, e.g. {@code 0.20} = 20%). */
        @SerializedName("sigma") public Double sigma;
        /** Option type — {@code "call"} or {@code "put"}. */
        @SerializedName("type") public String type;
        /** Annualised risk-free rate (decimal). */
        @SerializedName("risk_free_rate") public Double riskFreeRate;
        /** Annualised continuous dividend yield (decimal). */
        @SerializedName("dividend_yield") public Double dividendYield;
    }

    /**
     * First-order greeks — direct sensitivities of option value to a
     * single input.
     *
     * <ul>
     *   <li>{@code delta}  = ∂V/∂S</li>
     *   <li>{@code gamma}  = ∂²V/∂S²</li>
     *   <li>{@code theta}  = ∂V/∂t  (per calendar day)</li>
     *   <li>{@code vega}   = ∂V/∂σ  (per 1% volatility change)</li>
     *   <li>{@code rho}    = ∂V/∂r</li>
     * </ul>
     */
    public static final class FirstOrder {
        @SerializedName("delta") public Double delta;
        @SerializedName("gamma") public Double gamma;
        /** Theta per calendar day (so {@code -0.05} ≈ option loses 5¢ overnight). */
        @SerializedName("theta") public Double theta;
        /** Vega per 1% absolute change in volatility. */
        @SerializedName("vega") public Double vega;
        @SerializedName("rho") public Double rho;
    }

    /**
     * Second-order greeks — cross-sensitivities used heavily in
     * dealer-flow analytics.
     *
     * <ul>
     *   <li>{@code vanna}      = ∂²V/∂S∂σ — delta sensitivity to vol moves</li>
     *   <li>{@code charm}      = ∂²V/∂S∂t — delta drift per day (per calendar day)</li>
     *   <li>{@code vomma}      = ∂²V/∂σ²  — vega sensitivity to vol moves (volga)</li>
     *   <li>{@code dual_delta} = ∂V/∂K   — sensitivity to strike (used in skew construction)</li>
     * </ul>
     */
    public static final class SecondOrder {
        @SerializedName("vanna") public Double vanna;
        /** Charm per calendar day. */
        @SerializedName("charm") public Double charm;
        /** Also known as <i>volga</i>. */
        @SerializedName("vomma") public Double vomma;
        @SerializedName("dual_delta") public Double dualDelta;
    }

    /**
     * Third-order greeks — used by quants who care about smile dynamics
     * and gamma stability.
     *
     * <ul>
     *   <li>{@code speed}  = ∂³V/∂S³ — gamma sensitivity to spot</li>
     *   <li>{@code zomma}  = ∂³V/∂S²∂σ — gamma sensitivity to vol</li>
     *   <li>{@code color}  = ∂³V/∂S²∂t — gamma decay per day</li>
     *   <li>{@code ultima} = ∂³V/∂σ³ — vomma sensitivity to vol</li>
     * </ul>
     */
    public static final class ThirdOrder {
        @SerializedName("speed") public Double speed;
        @SerializedName("zomma") public Double zomma;
        /** Color per calendar day. */
        @SerializedName("color") public Double color;
        @SerializedName("ultima") public Double ultima;
    }

    /** Additional sensitivities. */
    public static final class Additional {
        /**
         * Option elasticity: {@code (delta * spot) / option_price}. Measures
         * the leverage of the option vs the underlying. {@code null} when
         * the theoretical price is ≤ 0 (division by zero / negative).
         */
        @SerializedName("lambda") public Double lambda;

        /**
         * Vega decay: ∂vega/∂t — how vega changes per day. Most
         * relevant for long-dated vol books.
         */
        @SerializedName("veta") public Double veta;
    }
}
