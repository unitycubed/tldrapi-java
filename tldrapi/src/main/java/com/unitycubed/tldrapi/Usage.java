package com.unitycubed.tldrapi;

import com.fasterxml.jackson.databind.JsonNode;

/** Per-call token counts + model, from the {@code usage} field of a
 *  summarize response. Values default to zero when absent. */
public final class Usage {
    private final int inputTokens;
    private final int outputTokens;
    private final double totalCost;
    private final String modelUsed;

    Usage(int inputTokens, int outputTokens, double totalCost, String modelUsed) {
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.totalCost = totalCost;
        this.modelUsed = modelUsed;
    }

    public int    getInputTokens()  { return inputTokens; }
    public int    getOutputTokens() { return outputTokens; }
    public double getTotalCost()    { return totalCost; }
    public String getModelUsed()    { return modelUsed; }

    static Usage fromNode(JsonNode u) {
        if (u == null || u.isMissingNode() || u.isNull()) {
            return new Usage(0, 0, 0.0, "");
        }
        return new Usage(
            u.path("input_tokens").asInt(0),
            u.path("output_tokens").asInt(0),
            u.path("total_cost").asDouble(0.0),
            u.path("model_used").asText("")
        );
    }
}
