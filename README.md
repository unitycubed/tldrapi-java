> ### ⚠️ Service notice
>
> **The RapidAPI listing that backs this SDK is temporarily unavailable while we work through a launch-day issue. Please check back in a few days.**

# TLDRapi Java SDK

Official Java client for the [TLDRapi](https://tldrapi.com) text-summarization API.

- **Java 11+** (uses `java.net.http.HttpClient` — no Apache HttpClient or OkHttp)
- **One dependency**: Jackson (`com.fasterxml.jackson.core:jackson-databind`)
- **Thread-safe**: construct once, share the client
- **Typed exceptions** for every failure mode the server distinguishes

## Install

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>com.unitycubed</groupId>
    <artifactId>tldrapi</artifactId>
    <version>0.1.0</version>
</dependency>
```

Or Gradle:

```groovy
implementation 'com.unitycubed:tldrapi:0.1.0'
```

## Quick start

Subscribe to TLDRapi on RapidAPI, copy your `X-RapidAPI-Key`, then:

```java
import com.unitycubed.tldrapi.Tldrapi;
import com.unitycubed.tldrapi.SummarizeOptions;
import com.unitycubed.tldrapi.SummarizeResult;

public class Example {
    public static void main(String[] args) throws Exception {
        Tldrapi client = Tldrapi.builder()
            .rapidApiKey(System.getenv("TLDRAPI_RAPIDAPI_KEY"))
            .build();

        SummarizeResult r = client.summarize(
            "Long article text goes here...",
            SummarizeOptions.builder().tier("standard").build());

        System.out.println(r.getSummary());
        System.out.println("cost: $" + r.getUsage().getTotalCost());
        System.out.println("credits remaining: " + r.getCredits().getRemaining());
    }
}
```

## Quality levels

| tier       | max chunk tokens | best for                                       |
|------------|-----------------:|------------------------------------------------|
| `quick`    |            4,000 | short summaries, high throughput, free tier    |
| `standard` |           16,000 | most articles + long blog posts                |
| `deep`     |           32,000 | long documents where accuracy matters          |
| `premium`  |           64,000 | research papers, contracts, dense material     |
| `ultra`    |          100,000 | maximum quality; slowest; use sparingly        |

Free tier is `quick`-only. Paid plans unlock the rest.

Credit cost scales with input size (v2.1):
`cost = 1 + Σ over chunks of (base × ceil(chunk_tokens / 1000))`.
Base costs and chunk caps are dynamic — fetch the current schedule
with `client.rates()` or from `GET /rates`.

## Advanced quality controls (v-session129+)

Server-side new features can be reached from Java via
`SummarizeOptions.builder().extraHeaders(map)`:

- `X-Quality: <preset>` — one of 30 named presets (compound
  `{minimal|brief|balanced|thorough|detailed|complete}-{quick|standard|
  deep|premium|ultra}`, e.g. `"thorough-standard"`). Five short names
  (`quick`/`standard`/`deep`/`premium`/`ultra`) are SCORECARD-validated
  highlighted anchors.
- `X-Optional-Quality: <llm>` / `X-Optional-Extractive-Lvl: <ret>` /
  `X-Optional-Strategy: <strategy>` — override any subset of the 3 axes.
- `X-Allow-Downgrade: true` — opt-in permissive paid-tier downgrade.
- `X-Async: true` — async submit; the response is 202 with an
  `X-Paid-Request-Id` header. Poll `GET /paid/result/{id}` on the
  `Retry-After` interval until 200.

```java
Map<String,String> h = new HashMap<>();
h.put("X-Optional-Quality", "premium");
h.put("X-Optional-Extractive-Lvl", "brief");
h.put("X-Allow-Downgrade", "true");
SummarizeResult r = client.summarize(text,
    SummarizeOptions.builder().extraHeaders(h).build());
```

Native `submitAsync` / `getResult` / `waitForResult` methods land in
the next SDK release; today, use raw `HttpClient.send()` against
`/paid/result/{id}` for polling.

## Session continuity

Pin a series of related calls to the same server session so tunables + model choice stay stable:

```java
SummarizeResult first = client.summarize(chapterOne);

SummarizeResult second = client.summarize(chapterTwo,
    SummarizeOptions.builder().sessionId(first.getSessionId()).build());
```

## Handling errors

Every failure the client raises subclasses `TldrapiException`:

```java
import com.unitycubed.tldrapi.TldrapiException;

try {
    SummarizeResult r = client.summarize(text);
    // ...
} catch (TldrapiException.Authentication e) {
    // 401/403: bad or missing key
} catch (TldrapiException.RateLimit e) {
    Thread.sleep(e.getRetryAfterSeconds() * 1000L);
    // retry manually — the SDK deliberately does not auto-retry 429
} catch (TldrapiException.InsufficientCredits e) {
    // 402: surface the upgrade path from e.getResponseBody()
} catch (TldrapiException.LanguageNotSupported e) {
    // 400: only English supported at launch
} catch (TldrapiException.Server e) {
    // 5xx after retries exhausted
} catch (TldrapiException e) {
    // catch-all: anything else the SDK produced
}
```

## Configuration

All builder options with defaults:

```java
Tldrapi client = Tldrapi.builder()
    .rapidApiKey("...")                    // required
    .rapidApiHost("tldrapi.p.rapidapi.com") // default
    .baseUrl(null)                          // defaults to https://<rapidApiHost>
    .timeoutSeconds(60)                     // per-request timeout
    .retries(3)                             // 5xx + transport retries; 0 disables
    .userAgent(null)                        // defaults to "tldrapi-java/<version>"
    .httpClient(null)                       // inject your own HttpClient if needed
    .build();
```

Per-call options:

```java
SummarizeOptions opts = SummarizeOptions.builder()
    .tier("deep")                    // quick|standard|deep|premium|ultra
    .sessionId("...")                // pin to a prior session
    .modelAlias("openai-gpt-4o")     // force a specific model (rarely needed)
    .allowOverage(false)             // permit charges beyond included credits
    .perCallTimeoutSeconds(120)      // override client timeout for this call
    .build();
```

## Building from source

```bash
cd sdks/java/tldrapi
mvn clean install
```

Tests: `mvn test`.

## Publishing to Maven Central

See `runbooks/publish-java-sdk-maven-central.md` in the repo root. Requires a Sonatype OSSRH account and a GPG signing key on public keyservers.

## License

MIT. See `LICENSE`.
