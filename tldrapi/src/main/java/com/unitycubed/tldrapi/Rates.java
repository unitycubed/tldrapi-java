package com.unitycubed.tldrapi;

import com.fasterxml.jackson.databind.JsonNode;

/** Result of {@link Tldrapi#rates()} — credits charged per call at
 *  each quality tier. Useful for surfacing the cost of a call before
 *  making it. Fields default to the launch-day rate table if the
 *  server response is missing keys, so a partial response is still
 *  usable. */
public final class Rates {
    private final int quick;
    private final int standard;
    private final int deep;
    private final int premium;
    private final int ultra;
    private final String updatedAt;
    private final JsonNode raw;

    Rates(int quick, int standard, int deep, int premium, int ultra,
          String updatedAt, JsonNode raw) {
        this.quick = quick;
        this.standard = standard;
        this.deep = deep;
        this.premium = premium;
        this.ultra = ultra;
        this.updatedAt = updatedAt;
        this.raw = raw;
    }

    public int getQuick()    { return quick; }
    public int getStandard() { return standard; }
    public int getDeep()     { return deep; }
    public int getPremium()  { return premium; }
    public int getUltra()    { return ultra; }
    public String getUpdatedAt() { return updatedAt; }
    public JsonNode getRaw() { return raw; }

    static Rates fromResponse(HttpTransport.Response r) throws TldrapiException {
        JsonNode body = r.jsonOrEmpty();
        return new Rates(
            body.path("quick").asInt(1),
            body.path("standard").asInt(5),
            body.path("deep").asInt(30),
            body.path("premium").asInt(110),
            body.path("ultra").asInt(400),
            body.path("updated_at").asText(null),
            body
        );
    }
}
