package com.unitycubed.tldrapi;

import java.net.http.HttpClient;

/**
 * Immutable configuration bundle passed from the {@link Tldrapi.Builder}
 * to the client's internal transport. Consumers should never construct
 * this directly — go through {@link Tldrapi#builder()}.
 */
final class TldrapiOptions {
    private final String rapidApiKey;
    private final String rapidApiHost;
    private final String baseUrl;
    private final int timeoutSeconds;
    private final int retries;
    private final String userAgent;
    private final HttpClient httpClient;

    TldrapiOptions(String rapidApiKey, String rapidApiHost, String baseUrl,
                   int timeoutSeconds, int retries, String userAgent,
                   HttpClient httpClient) {
        this.rapidApiKey = rapidApiKey;
        this.rapidApiHost = rapidApiHost;
        this.baseUrl = baseUrl;
        this.timeoutSeconds = timeoutSeconds;
        this.retries = retries;
        this.userAgent = userAgent;
        this.httpClient = httpClient;
    }

    String getRapidApiKey()   { return rapidApiKey; }
    String getRapidApiHost()  { return rapidApiHost; }
    String getBaseUrl()       { return baseUrl; }
    int    getTimeoutSeconds(){ return timeoutSeconds; }
    int    getRetries()       { return retries; }
    String getUserAgent()     { return userAgent; }
    HttpClient getHttpClient(){ return httpClient; }
}
