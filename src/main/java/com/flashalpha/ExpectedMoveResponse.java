package com.flashalpha;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Typed response for {@code GET /v1/expected-move/{symbol}} (Basic+) —
 * straddle-implied expected move per expiry, derived from ATM implied
 * volatility. Generalizes the earnings-only expected move to any upcoming
 * expiry.
 *
 * <p>Top-level keys are snake_case; the items inside {@code expected_moves} use
 * camelCase, mirrored verbatim by {@link ExpectedMove}.
 *
 * <p>Obtain via {@link FlashAlphaClient#expectedMoveTyped(String)} or
 * {@link FlashAlphaClient#expectedMoveTyped(String, String)}.
 */
public final class ExpectedMoveResponse {

    /** Resolved, upper-cased underlying symbol. */
    @SerializedName("symbol")
    public String symbol;

    /** Spot used for the bounds. */
    @SerializedName("underlying_price")
    public Double underlyingPrice;

    /** ISO 8601 UTC time the response was built. */
    @SerializedName("as_of")
    public String asOf;

    /** Per-expiry expected moves, ordered by expiry. */
    @SerializedName("expected_moves")
    public List<ExpectedMove> expectedMoves;

    /** One expiry's straddle-implied move (camelCase keys, per the API). */
    public static final class ExpectedMove {
        @SerializedName("expiry") public String expiry;
        @SerializedName("daysToExpiry") public Integer daysToExpiry;
        /** ATM implied vol as a decimal (nullable when no ATM IV derivable). */
        @SerializedName("atmIv") public Double atmIv;
        /** 1-sigma move in price terms. */
        @SerializedName("expectedMove") public Double expectedMove;
        /** Expected move as a percentage of spot. */
        @SerializedName("expectedMovePct") public Double expectedMovePct;
        @SerializedName("lowerBound") public Double lowerBound;
        @SerializedName("upperBound") public Double upperBound;
    }
}
