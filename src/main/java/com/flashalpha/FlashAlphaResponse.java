package com.flashalpha;

import com.google.gson.annotations.SerializedName;

/**
 * Base for every typed response model. Carries the envelope the API returns on all
 * successful responses.
 *
 * <p>Gson reflects over the full class hierarchy, so inherited fields bind exactly as
 * declared ones do and the wire shape is unchanged. Both members are objects rather
 * than primitives, so a response predating the envelope leaves them null instead of
 * failing to parse.
 *
 * <p>Endpoints that return a bare JSON array cannot carry an envelope in the body and
 * send the same information in the {@code X-Data-As-Of} and {@code X-Endpoint-Version}
 * response headers instead.
 */
public abstract class FlashAlphaResponse {

    /** Identifies the deployment that produced this response. */
    @SerializedName("endpoint_version")
    public String endpointVersion;

    /** Per-feed freshness of the data behind this response. See {@link DataAsOf}. */
    @SerializedName("data_as_of")
    public DataAsOf dataAsOf;
}
