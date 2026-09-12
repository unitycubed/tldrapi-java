package com.unitycubed.tldrapi;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Per-call options for {@link Tldrapi#summarize(String, SummarizeOptions)}.
 * Use the fluent {@link #builder()} — every field is optional.
 *
 * <p>Fields:
 * <ul>
 *   <li>{@code tier} — quality tier: quick|standard|deep|premium|ultra.
 *       Omitted → server picks the default for your plan.</li>
 *   <li>{@code sessionId} — pin subsequent calls to the same session so
 *       state (model + tunables) stays stable across a document.</li>
 *   <li>{@code modelAlias} — explicit model_configs alias to force one
 *       specific provider/model. Rarely needed; tier is preferred.</li>
 *   <li>{@code allowOverage} — set to true to permit charges beyond
 *       your plan's included credits (billed at the per-tier overage
 *       rate). Off by default; a 402 fires instead.</li>
 *   <li>{@code extraHeaders} — extra HTTP headers to attach. Mostly for
 *       tracing / experimentation; production code rarely needs it.</li>
 *   <li>{@code perCallTimeoutSeconds} — override the client-level timeout
 *       for this one call. 0 → use the client default.</li>
 * </ul>
 */
public final class SummarizeOptions {
    private final String tier;
    private final String sessionId;
    private final String modelAlias;
    private final SummarizeConfig config;
    private final boolean allowOverage;
    private final Map<String, String> extraHeaders;
    private final int perCallTimeoutSeconds;

    private SummarizeOptions(Builder b) {
        this.tier = b.tier;
        this.sessionId = b.sessionId;
        this.modelAlias = b.modelAlias;
        this.config = b.config;
        this.allowOverage = b.allowOverage;
        this.extraHeaders = b.extraHeaders == null
            ? Collections.emptyMap()
            : Collections.unmodifiableMap(new HashMap<>(b.extraHeaders));
        this.perCallTimeoutSeconds = b.perCallTimeoutSeconds;
    }

    public String  getTier()                  { return tier; }
    public String  getSessionId()             { return sessionId; }
    public String  getModelAlias()            { return modelAlias; }
    public SummarizeConfig getConfig()        { return config; }
    public boolean isAllowOverage()           { return allowOverage; }
    public Map<String,String> getExtraHeaders(){ return extraHeaders; }
    public int     getPerCallTimeoutSeconds() { return perCallTimeoutSeconds; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String tier;
        private String sessionId;
        private String modelAlias;
        private SummarizeConfig config;
        private boolean allowOverage;
        private Map<String, String> extraHeaders;
        private int perCallTimeoutSeconds;

        public Builder tier(String v)         { this.tier = v; return this; }
        public Builder sessionId(String v)    { this.sessionId = v; return this; }
        public Builder modelAlias(String v)   { this.modelAlias = v; return this; }
        /** Optional per-call generation overrides. Matches
         *  openapi.yaml SummarizeRequest.config. */
        public Builder config(SummarizeConfig v)   { this.config = v; return this; }
        public Builder allowOverage(boolean v){ this.allowOverage = v; return this; }
        public Builder extraHeaders(Map<String,String> v) { this.extraHeaders = v; return this; }
        public Builder perCallTimeoutSeconds(int v){ this.perCallTimeoutSeconds = v; return this; }

        public SummarizeOptions build() { return new SummarizeOptions(this); }
    }
}
