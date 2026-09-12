package com.unitycubed.tldrapi;

import com.fasterxml.jackson.databind.JsonNode;

/** Result of {@link Tldrapi#customPromptGet}. Includes the full
 *  instruction text plus the judge's verdict JSON. */
public final class CustomPromptDetail {
    private final String id;
    private final String customerId;
    private final String voiceName;
    private final String instruction;
    private final String status;
    private final String voiceReference;
    private final String approvedAlias;
    private final String rejectionReason;
    private final String judgeVerdictJson;
    private final String submittedAt;
    private final String reviewedAt;
    private final JsonNode raw;

    CustomPromptDetail(String id, String customerId, String voiceName, String instruction,
                       String status, String voiceReference, String approvedAlias,
                       String rejectionReason, String judgeVerdictJson, String submittedAt,
                       String reviewedAt, JsonNode raw) {
        this.id = id;
        this.customerId = customerId;
        this.voiceName = voiceName;
        this.instruction = instruction;
        this.status = status;
        this.voiceReference = voiceReference;
        this.approvedAlias = approvedAlias;
        this.rejectionReason = rejectionReason;
        this.judgeVerdictJson = judgeVerdictJson;
        this.submittedAt = submittedAt;
        this.reviewedAt = reviewedAt;
        this.raw = raw;
    }

    public String getId()               { return id; }
    public String getCustomerId()       { return customerId; }
    public String getVoiceName()        { return voiceName; }
    public String getInstruction()      { return instruction; }
    public String getStatus()           { return status; }
    public String getVoiceReference()   { return voiceReference; }
    public String getApprovedAlias()    { return approvedAlias; }
    public String getRejectionReason()  { return rejectionReason; }
    public String getJudgeVerdictJson() { return judgeVerdictJson; }
    public String getSubmittedAt()      { return submittedAt; }
    public String getReviewedAt()       { return reviewedAt; }
    public JsonNode getRaw()            { return raw; }

    static CustomPromptDetail fromResponse(HttpTransport.Response r) throws TldrapiException {
        JsonNode body = r.jsonOrEmpty();
        return new CustomPromptDetail(
            body.path("id").asText(""),
            body.path("customer_id").asText(""),
            body.path("voice_name").asText(""),
            body.path("instruction").asText(""),
            body.path("status").asText(""),
            body.hasNonNull("voice_reference") ? body.path("voice_reference").asText(null) : null,
            body.hasNonNull("approved_alias") ? body.path("approved_alias").asText(null) : null,
            body.hasNonNull("rejection_reason") ? body.path("rejection_reason").asText(null) : null,
            body.path("judge_verdict_json").asText(""),
            body.path("submitted_at").asText(""),
            body.path("reviewed_at").asText(""),
            body
        );
    }
}
