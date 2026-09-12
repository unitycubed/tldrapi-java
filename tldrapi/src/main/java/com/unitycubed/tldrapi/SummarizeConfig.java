package com.unitycubed.tldrapi;

import com.fasterxml.jackson.databind.node.ObjectNode;

/** Optional per-call generation config for
 *  {@link Tldrapi#summarize(String, SummarizeOptions)}. Every field is
 *  optional; omit to use the server's tier default. Shape mirrors
 *  {@code openapi.yaml SummarizeRequest.config}. */
public final class SummarizeConfig {
    private final String modelAlias;
    private final Double temperature;
    private final Double topP;
    private final Integer maxOutputTokens;
    private final Integer maxInputTokens;

    private SummarizeConfig(Builder b) {
        this.modelAlias = b.modelAlias;
        this.temperature = b.temperature;
        this.topP = b.topP;
        this.maxOutputTokens = b.maxOutputTokens;
        this.maxInputTokens = b.maxInputTokens;
    }

    public String getModelAlias()       { return modelAlias; }
    public Double getTemperature()      { return temperature; }
    public Double getTopP()             { return topP; }
    public Integer getMaxOutputTokens() { return maxOutputTokens; }
    public Integer getMaxInputTokens()  { return maxInputTokens; }

    /** True iff every field is null — signals the caller / marshaller that
     *  the whole config block can be dropped instead of sending {@code {}}. */
    public boolean isEmpty() {
        return modelAlias == null && temperature == null && topP == null
            && maxOutputTokens == null && maxInputTokens == null;
    }

    /** Serialize non-null fields into a fresh Jackson {@link ObjectNode}
     *  suitable for embedding as the {@code config} sub-object of a
     *  summarize request body. Returns {@code null} if {@link #isEmpty()}. */
    ObjectNode toJson() {
        if (isEmpty()) return null;
        ObjectNode n = JsonUtil.MAPPER.createObjectNode();
        if (modelAlias != null) n.put("model_alias", modelAlias);
        if (temperature != null) n.put("temperature", temperature);
        if (topP != null) n.put("top_p", topP);
        if (maxOutputTokens != null) n.put("max_output_tokens", maxOutputTokens);
        if (maxInputTokens != null) n.put("max_input_tokens", maxInputTokens);
        return n;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String modelAlias;
        private Double temperature;
        private Double topP;
        private Integer maxOutputTokens;
        private Integer maxInputTokens;

        public Builder modelAlias(String v)       { this.modelAlias = v; return this; }
        public Builder temperature(Double v)      { this.temperature = v; return this; }
        public Builder topP(Double v)             { this.topP = v; return this; }
        public Builder maxOutputTokens(Integer v) { this.maxOutputTokens = v; return this; }
        public Builder maxInputTokens(Integer v)  { this.maxInputTokens = v; return this; }

        public SummarizeConfig build() { return new SummarizeConfig(this); }
    }
}
