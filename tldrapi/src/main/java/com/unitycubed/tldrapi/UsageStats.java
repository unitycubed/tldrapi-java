package com.unitycubed.tldrapi;

import com.fasterxml.jackson.databind.JsonNode;

/** Result of {@link Tldrapi#usage()} — aggregate usage stats across
 *  the current billing period. */
public final class UsageStats {
    private final String period;
    private final int calls;
    private final int creditsCharged;
    private final int creditsRemaining;
    private final JsonNode raw;

    UsageStats(String period, int calls, int creditsCharged, int creditsRemaining, JsonNode raw) {
        this.period = period;
        this.calls = calls;
        this.creditsCharged = creditsCharged;
        this.creditsRemaining = creditsRemaining;
        this.raw = raw;
    }

    public String getPeriod()          { return period; }
    public int    getCalls()           { return calls; }
    public int    getCreditsCharged()  { return creditsCharged; }
    public int    getCreditsRemaining(){ return creditsRemaining; }
    public JsonNode getRaw()           { return raw; }

    static UsageStats fromResponse(HttpTransport.Response r) throws TldrapiException {
        JsonNode body = r.jsonOrEmpty();
        return new UsageStats(
            body.path("period").asText(""),
            body.path("calls").asInt(0),
            body.path("credits_charged").asInt(0),
            body.path("credits_remaining").asInt(0),
            body
        );
    }
}
