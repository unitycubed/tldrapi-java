package com.unitycubed.tldrapi;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Result of {@link Tldrapi#usageRange(String, String)} — per-day
 *  credit usage between two YYYY-MM-DD dates. */
public final class UsageRange {
    private final String from;
    private final String to;
    private final long creditsUsed;
    private final List<Day> daily;
    private final JsonNode raw;

    UsageRange(String from, String to, long creditsUsed, List<Day> daily, JsonNode raw) {
        this.from = from;
        this.to = to;
        this.creditsUsed = creditsUsed;
        this.daily = Collections.unmodifiableList(daily);
        this.raw = raw;
    }

    public String   getFrom()        { return from; }
    public String   getTo()          { return to; }
    public long     getCreditsUsed() { return creditsUsed; }
    public List<Day> getDaily()      { return daily; }
    public JsonNode getRaw()         { return raw; }

    static UsageRange fromResponse(HttpTransport.Response r) throws TldrapiException {
        JsonNode body = r.jsonOrEmpty();
        List<Day> list = new ArrayList<>();
        if (body.path("daily").isArray()) {
            for (JsonNode d : body.path("daily")) {
                list.add(new Day(
                    d.path("date").asText(""),
                    d.path("credits_used").asLong(0),
                    d.path("call_count").asLong(0)
                ));
            }
        }
        return new UsageRange(
            body.path("from").asText(""),
            body.path("to").asText(""),
            body.path("credits_used").asLong(0),
            list, body
        );
    }

    /** One day-row inside a {@link UsageRange}. */
    public static final class Day {
        private final String date;
        private final long creditsUsed;
        private final long callCount;

        Day(String date, long creditsUsed, long callCount) {
            this.date = date;
            this.creditsUsed = creditsUsed;
            this.callCount = callCount;
        }

        public String getDate()        { return date; }
        public long   getCreditsUsed() { return creditsUsed; }
        public long   getCallCount()   { return callCount; }
    }
}
