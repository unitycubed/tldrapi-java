package com.unitycubed.tldrapi;

/** Credits snapshot pulled from response headers (X-Credits-Charged,
 *  X-Credits-Remaining, X-Credits-Tier). Values are the raw header
 *  strings — callers parse to numeric types as needed. */
public final class Credits {
    private final String charged;
    private final String remaining;
    private final String tier;

    Credits(String charged, String remaining, String tier) {
        this.charged = charged;
        this.remaining = remaining;
        this.tier = tier;
    }

    public String getCharged()   { return charged; }
    public String getRemaining() { return remaining; }
    public String getTier()      { return tier; }

    static Credits fromHeaders(HttpTransport.Response r) {
        return new Credits(
            r.headerFirst("x-credits-charged"),
            r.headerFirst("x-credits-remaining"),
            r.headerFirst("x-credits-tier")
        );
    }
}
