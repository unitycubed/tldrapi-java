package com.unitycubed.tldrapi;

import com.fasterxml.jackson.databind.JsonNode;

/** Result of {@link Tldrapi#usage()} — aggregate usage stats. Fields
 *  align with {@code openapi.yaml UsageResponse}. Pre-1.0 SDK exposed
 *  {@code period/calls/creditsCharged/creditsRemaining} which the server
 *  has never returned — those keys are gone. */
public final class UsageStats {
    private final long usageCount;
    private final long successfulRequests;
    private final long failedRequests;
    private final double averageResponseTimeMs;
    private final JsonNode endpointsUsed;
    private final double errorRate;
    private final String plan;
    private final UsageLimits limits;
    private final JsonNode raw;

    UsageStats(long usageCount, long successfulRequests, long failedRequests,
               double averageResponseTimeMs, JsonNode endpointsUsed, double errorRate,
               String plan, UsageLimits limits, JsonNode raw) {
        this.usageCount = usageCount;
        this.successfulRequests = successfulRequests;
        this.failedRequests = failedRequests;
        this.averageResponseTimeMs = averageResponseTimeMs;
        this.endpointsUsed = endpointsUsed;
        this.errorRate = errorRate;
        this.plan = plan;
        this.limits = limits;
        this.raw = raw;
    }

    public long getUsageCount()             { return usageCount; }
    public long getSuccessfulRequests()     { return successfulRequests; }
    public long getFailedRequests()         { return failedRequests; }
    public double getAverageResponseTimeMs(){ return averageResponseTimeMs; }
    public JsonNode getEndpointsUsed()      { return endpointsUsed; }
    public double getErrorRate()            { return errorRate; }
    public String getPlan()                 { return plan; }
    public UsageLimits getLimits()          { return limits; }
    public JsonNode getRaw()                { return raw; }

    static UsageStats fromResponse(HttpTransport.Response r) throws TldrapiException {
        JsonNode body = r.jsonOrEmpty();
        JsonNode limitsNode = body.path("limits");
        UsageLimits limits = new UsageLimits(
            limitsNode.hasNonNull("per_minute") ? Long.valueOf(limitsNode.path("per_minute").asLong()) : null,
            limitsNode.hasNonNull("daily")      ? Long.valueOf(limitsNode.path("daily").asLong())      : null,
            limitsNode.hasNonNull("credits")    ? Long.valueOf(limitsNode.path("credits").asLong())    : null,
            limitsNode.hasNonNull("concurrent") ? Long.valueOf(limitsNode.path("concurrent").asLong()) : null
        );
        return new UsageStats(
            body.path("usage_count").asLong(0),
            body.path("successful_requests").asLong(0),
            body.path("failed_requests").asLong(0),
            body.path("average_response_time_ms").asDouble(0.0),
            body.path("endpoints_used"),
            body.path("error_rate").asDouble(0.0),
            body.path("plan").asText(""),
            limits,
            body
        );
    }

    /** Plan-limit sub-object of UsageStats — populated when the server
     *  reports the limit, {@code null} otherwise. Shape mirrors
     *  {@code openapi.yaml UsageResponse.limits}. */
    public static final class UsageLimits {
        private final Long perMinute;
        private final Long daily;
        private final Long credits;
        private final Long concurrent;

        UsageLimits(Long perMinute, Long daily, Long credits, Long concurrent) {
            this.perMinute = perMinute;
            this.daily = daily;
            this.credits = credits;
            this.concurrent = concurrent;
        }

        public Long getPerMinute()  { return perMinute; }
        public Long getDaily()      { return daily; }
        public Long getCredits()    { return credits; }
        public Long getConcurrent() { return concurrent; }
    }
}
