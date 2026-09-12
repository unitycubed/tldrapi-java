package com.unitycubed.tldrapi;

import com.fasterxml.jackson.databind.JsonNode;

/** Result of {@link Tldrapi#rates()} — credits charged per call at each
 *  quality tier. Fields align with {@code openapi.yaml RatesResponse}.
 *  Pre-1.0 releases parsed the tier ints from top-level keys and used
 *  {@code updated_at}; the server never emitted those. The tier fields
 *  on this class are convenience aliases populated from
 *  {@code credits_per_call} so pre-1.0 call sites keep compiling.
 *  {@code updatedAt} is kept as a deprecated alias for
 *  {@code creditCostsUpdatedAt}. */
public final class Rates {
    private final int quick;
    private final int standard;
    private final int deep;
    private final int premium;
    private final int ultra;
    private final String creditCostsUpdatedAt;
    private final String historyUrl;
    private final JsonNode raw;

    Rates(int quick, int standard, int deep, int premium, int ultra,
          String creditCostsUpdatedAt, String historyUrl, JsonNode raw) {
        this.quick = quick;
        this.standard = standard;
        this.deep = deep;
        this.premium = premium;
        this.ultra = ultra;
        this.creditCostsUpdatedAt = creditCostsUpdatedAt;
        this.historyUrl = historyUrl;
        this.raw = raw;
    }

    public int getQuick()    { return quick; }
    public int getStandard() { return standard; }
    public int getDeep()     { return deep; }
    public int getPremium()  { return premium; }
    public int getUltra()    { return ultra; }
    public String getCreditCostsUpdatedAt() { return creditCostsUpdatedAt; }
    /** Deprecated alias for {@link #getCreditCostsUpdatedAt()}. */
    @Deprecated
    public String getUpdatedAt() { return creditCostsUpdatedAt; }
    public String getHistoryUrl() { return historyUrl; }
    public JsonNode getRaw() { return raw; }

    static Rates fromResponse(HttpTransport.Response r) throws TldrapiException {
        JsonNode body = r.jsonOrEmpty();
        JsonNode cpc = body.path("credits_per_call");
        return new Rates(
            cpc.path("quick").asInt(1),
            cpc.path("standard").asInt(5),
            cpc.path("deep").asInt(30),
            cpc.path("premium").asInt(110),
            cpc.path("ultra").asInt(400),
            body.path("credit_costs_updated_at").asText(null),
            body.path("history_url").asText(""),
            body
        );
    }
}
