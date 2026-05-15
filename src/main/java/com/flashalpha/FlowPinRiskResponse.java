package com.flashalpha;

import com.google.gson.annotations.SerializedName;

/**
 * Typed response for {@code GET /v1/flow/pin-risk/{symbol}} (Alpha+). A 0-100 composite pin-risk score plus the magnet strike and breakdown.
 */
public final class FlowPinRiskResponse {

    /** Underlying ticker echoed from the request path. */
    @SerializedName("symbol")
    public String symbol;

    /** Timestamp this snapshot was computed for (ISO-8601 UTC). */
    @SerializedName("as_of")
    public String asOf;

    /** Spot mid at the snapshot time. */
    @SerializedName("underlying_price")
    public Double underlyingPrice;

    /** Expiration filter echoed back, or null. */
    @SerializedName("expiry")
    public String expiry;

    /** Composite 0-100 pin-risk score (higher = stronger pin pull). */
    @SerializedName("live_pin_risk")
    public Integer livePinRisk;

    /** Pin magnet strike (argmax|net gamma|); null when no dominant strike. */
    @SerializedName("magnet_strike")
    public Double magnetStrike;

    /** Signed % distance from spot to the magnet strike. */
    @SerializedName("distance_to_magnet_pct")
    public Double distanceToMagnetPct;

    /** Hours remaining until the regular-session cash close. */
    @SerializedName("time_to_close_hours")
    public Double timeToCloseHours;

    /** Component scores behind {@link #livePinRisk}. */
    @SerializedName("breakdown")
    public Breakdown breakdown;


    /** Component scores (0-100) behind the {@code live_pin_risk} headline. */
    public static final class Breakdown {
        /** Open-interest concentration around the magnet strike. */
        @SerializedName("oi_score") public Integer oiScore;
        /** How close spot is to the magnet strike. */
        @SerializedName("proximity_score") public Integer proximityScore;
        /** Time-to-close weighting (pin pressure rises into the cash close). */
        @SerializedName("time_score") public Integer timeScore;
        /** Dealer-gamma intensity at the magnet strike. */
        @SerializedName("gamma_score") public Integer gammaScore;
    }
}
