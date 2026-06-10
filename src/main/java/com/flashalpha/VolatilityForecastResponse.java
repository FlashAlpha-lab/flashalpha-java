package com.flashalpha;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Typed response for {@code GET /v1/volatility/forecast/{symbol}} (Alpha+) —
 * conditional volatility forecasts from three models: EWMA (RiskMetrics,
 * {@code lambda = 0.94}), HAR-RV (heterogeneous autoregressive realized vol),
 * and GARCH(1,1) fit by maximum likelihood.
 *
 * <p>The GARCH innovation distribution is selectable via the optional
 * {@code dist} query parameter ({@code student_t} default, or
 * {@code gaussian}); {@link Garch.Params#dof} is only populated for
 * {@code student_t}. All vol figures are annualised in percent.
 *
 * <p>Obtain via {@link FlashAlphaClient#volatilityForecastTyped(String)} or
 * {@link FlashAlphaClient#volatilityForecastTyped(String, String)}.
 */
public final class VolatilityForecastResponse {

    /** Resolved, upper-cased underlying symbol. */
    @SerializedName("symbol")
    public String symbol;

    /** ISO 8601 UTC time the response was built. */
    @SerializedName("as_of")
    public String asOf;

    /** EWMA (RiskMetrics) conditional vol. */
    @SerializedName("ewma")
    public Ewma ewma;

    /** HAR-RV forecast and its daily / weekly / monthly components. */
    @SerializedName("har_rv")
    public HarRv harRv;

    /** GARCH(1,1) MLE fit, parameters, diagnostics, and forecast path. */
    @SerializedName("garch")
    public Garch garch;

    /** EWMA (RiskMetrics) exponentially-weighted conditional vol. */
    public static final class Ewma {
        /** Decay parameter (typically {@code 0.94}). */
        @SerializedName("lambda") public Double lambda;
        /** Annualised conditional vol (percent). */
        @SerializedName("vol_annualized") public Double volAnnualized;
        /** Next-day vol forecast (annualised percent). */
        @SerializedName("next_day_forecast") public Double nextDayForecast;
    }

    /** HAR-RV (heterogeneous autoregressive realized vol) forecast. */
    public static final class HarRv {
        /** Annualised conditional vol (percent). */
        @SerializedName("vol_annualized") public Double volAnnualized;
        /** Daily / weekly / monthly realized-vol components. */
        @SerializedName("components") public Components components;
        /** Next-day vol forecast (annualised percent). */
        @SerializedName("next_day_forecast") public Double nextDayForecast;

        /** The three HAR-RV regressor components (annualised percent). */
        public static final class Components {
            @SerializedName("daily") public Double daily;
            @SerializedName("weekly") public Double weekly;
            @SerializedName("monthly") public Double monthly;
        }
    }

    /** GARCH(1,1) maximum-likelihood fit, diagnostics, and forecast path. */
    public static final class Garch {
        /** Model identifier, e.g. {@code "garch_1_1"}. */
        @SerializedName("model") public String model;
        /** Innovation distribution: {@code "student_t"} or {@code "gaussian"}. */
        @SerializedName("distribution") public String distribution;
        /** Estimated model parameters. */
        @SerializedName("params") public Params params;
        /** {@code alpha + beta} — variance persistence. */
        @SerializedName("persistence") public Double persistence;
        /** Long-run (unconditional) annualised vol (percent, nullable). */
        @SerializedName("long_run_vol_annualized") public Double longRunVolAnnualized;
        /** Shock half-life in days. */
        @SerializedName("half_life_days") public Double halfLifeDays;
        /** Whether the MLE optimiser converged. */
        @SerializedName("converged") public Boolean converged;
        /** Forecast path (nullable; null when {@code converged} is false). */
        @SerializedName("forecast") public List<ForecastPoint> forecast;

        /**
         * GARCH(1,1) parameters. {@code dof} (Student-t degrees of freedom)
         * is only populated when {@code distribution == "student_t"}.
         */
        public static final class Params {
            @SerializedName("omega") public Double omega;
            @SerializedName("alpha") public Double alpha;
            @SerializedName("beta") public Double beta;
            /** Student-t degrees of freedom (nullable; student_t only). */
            @SerializedName("dof") public Double dof;
        }

        /** One point on the GARCH forecast path. */
        public static final class ForecastPoint {
            @SerializedName("horizon_days") public Integer horizonDays;
            /** Annualised vol forecast at this horizon (percent). */
            @SerializedName("vol_annualized") public Double volAnnualized;
        }
    }
}
