package com.unitycubed.tldrapi;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Result of {@link Tldrapi#ratesHistory()} — a chronological list of
 *  credit-cost changes per tier, plus a summary count. */
public final class RatesHistory {
    private final List<Change> history;
    private final long rangeDays;
    private final long totalChanges;
    private final JsonNode raw;

    RatesHistory(List<Change> history, long rangeDays, long totalChanges, JsonNode raw) {
        this.history = Collections.unmodifiableList(history);
        this.rangeDays = rangeDays;
        this.totalChanges = totalChanges;
        this.raw = raw;
    }

    public List<Change> getHistory()   { return history; }
    public long         getRangeDays() { return rangeDays; }
    public long         getTotalChanges() { return totalChanges; }
    public JsonNode     getRaw()       { return raw; }

    static RatesHistory fromResponse(HttpTransport.Response r) throws TldrapiException {
        JsonNode body = r.jsonOrEmpty();
        List<Change> list = new ArrayList<>();
        if (body.path("history").isArray()) {
            for (JsonNode row : body.path("history")) {
                list.add(new Change(
                    row.path("changed_at").asText(""),
                    row.path("tier").asText(""),
                    row.path("credits_before").asLong(0),
                    row.path("credits_after").asLong(0),
                    row.path("reason").asText(""),
                    row.path("operator").asText("")
                ));
            }
        }
        long total = body.path("total_changes").asLong(0);
        if (total == 0) total = list.size();
        return new RatesHistory(list, body.path("range_days").asLong(30), total, body);
    }

    /** One row of {@link RatesHistory#getHistory()}. */
    public static final class Change {
        private final String changedAt;
        private final String tier;
        private final long   creditsBefore;
        private final long   creditsAfter;
        private final String reason;
        private final String operator;

        Change(String changedAt, String tier, long creditsBefore, long creditsAfter,
               String reason, String operator) {
            this.changedAt = changedAt;
            this.tier = tier;
            this.creditsBefore = creditsBefore;
            this.creditsAfter = creditsAfter;
            this.reason = reason;
            this.operator = operator;
        }

        public String getChangedAt()     { return changedAt; }
        public String getTier()          { return tier; }
        public long   getCreditsBefore() { return creditsBefore; }
        public long   getCreditsAfter()  { return creditsAfter; }
        public String getReason()        { return reason; }
        public String getOperator()      { return operator; }
    }
}
