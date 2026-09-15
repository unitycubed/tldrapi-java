# TLDRapi Java SDK

Official Java client for [TLDRapi](https://tldrapi.com) — turn any
content into a clean summary in one API call.

- **Free tier** — 100 credits per month, no card, no trial expiry
- **20+ input formats** — text, HTML, Markdown, PDF (with OCR), .docx,
  .doc, .odt, .rtf, .epub, JSON, YAML, CSV, transcripts
- **5 quality tiers** — pick latency vs. depth per call
- **Custom voice styles** — 20+ built-in voices; paid tiers can define
  their own with plain-English instructions
- **Multi-provider routing** — automatic failover across Anthropic,
  OpenAI, Groq, Gemini, and OpenRouter
- **Refunds you don't have to ask for** — every summary is judge-scored
  and mis-summaries are auto-refunded
- **Java 11+**, single dep (Jackson), thread-safe, no OkHttp / Apache
  HttpClient required — uses stdlib `java.net.http.HttpClient`

## Install

Maven:

```xml
<dependency>
    <groupId>com.unitycubed</groupId>
    <artifactId>tldrapi</artifactId>
    <version>0.1.0</version>
</dependency>
```

Gradle:

```groovy
implementation 'com.unitycubed:tldrapi:0.1.0'
```

## Table of contents

- [Getting your free key](#getting-your-free-key)
- [Hello world](#hello-world)
- [Examples gallery](#examples-gallery)
  - [Summarize an article by URL](#summarize-an-article-by-url)
  - [Pin a session across many summaries](#pin-a-session-across-many-summaries)
  - [Handle a rate-limit with backoff](#handle-a-rate-limit-with-backoff)
  - [Show live credit balance to your user](#show-live-credit-balance-to-your-user)
  - [Advanced quality controls — 3 axes, 30 named presets](#advanced-quality-controls)
- [Async submit + poll](#async-submit--poll)
- [Error handling](#error-handling)
- [Configuration](#configuration)
- [License](#license)

## Getting your free key

1. Sign in at [rapidapi.com](https://rapidapi.com)
2. Subscribe to the [TLDRapi Summarizer](https://rapidapi.com/thunderAPIs256/api/tldrapi-summarizer)
   listing — choose **BASIC (Free)**
3. Open the listing → **Console** → **Applications** → **Add App**
4. In the App → **Authorizations** tab → copy the Authorization Key

Pass it to the builder as `rapidApiKey`. Everything on the free tier
works exactly like paid tiers — same endpoints, same response shape,
same SDK — just with a 100-credit monthly cap.

## Hello world

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
            "Some long article body here...");

        System.out.println(r.getSummary());
        System.out.println("credits remaining: " + r.getCredits().getRemaining());
    }
}
```

## Examples gallery

### Summarize an article by URL

TLDRapi accepts URLs directly — the server fetches, extracts main
content, strips nav/ads, and summarizes.

```java
SummarizeResult r = client.summarize(
    "https://arxiv.org/abs/1706.03762",
    SummarizeOptions.builder().tier("deep").build());
System.out.println(r.getSummary());
```

Works with HTML pages, news sites, GitHub READMEs, blog posts, and
academic PDFs served over HTTP.

### Pin a session across many summaries

```java
SummarizeResult r1 = client.summarize("Doc 1");

SummarizeResult r2 = client.summarize("Doc 2",
    SummarizeOptions.builder().sessionId(r1.getSessionId()).build());

SummarizeResult r3 = client.summarize("Doc 3",
    SummarizeOptions.builder().sessionId(r1.getSessionId()).build());
```

Useful when you want consistent voice across a run — chapters of the same book, articles in a series, tickets in the same support thread.

### Handle a rate-limit with backoff

```java
for (int attempt = 0; attempt < 3; attempt++) {
    try {
        SummarizeResult r = client.summarize(text,
            SummarizeOptions.builder().tier("deep").build());
        System.out.println(r.getSummary());
        break;
    } catch (TldrapiException.RateLimit e) {
        long ms = Math.max(e.getRetryAfterSeconds(), 60) * 1000L;
        Thread.sleep(ms);
    }
}
```

### Show live credit balance to your user

```java
UsageStats u = client.usage();
System.out.println("You have " + u.getCreditsRemaining() +
                   " credits left (" + u.getPlan() + ")");

SummarizeResult r = client.summarize(text);
System.out.println("That call cost " + r.getCredits().getCharged() +
                   " credits. Remaining: " + r.getCredits().getRemaining());
```

### Advanced quality controls

Every summarize call has three orthogonal knobs. You can send zero of
them (defaults are fine), or a named preset, or set 1-3 optional axes
via `extraHeaders`, or combine — axes override the preset and the
server returns `X-Quality-Warning`.

**30 named presets** arranged as a 1D spectrum across the underlying 3D
quality space (LLM x retention x strategy). The 5 bolded rows are the
main anchors; each also accepts a short alias equal to its LLM tier
name (`quick` / `standard` / `deep` / `premium` / `ultra`).

| #  | Preset                | What it delivers                                                                                       |
|---:|-----------------------|--------------------------------------------------------------------------------------------------------|
|  1 | `minimal-quick`       | Cheapest and fastest. Headline-length blurb from a small chunk. Title-level takeaway.                  |
|  2 | **`brief-quick`**     | 3-sentence recap with the fastest LLM. Previews and low-latency feed cards.                            |
|  3 | `minimal-standard`    | Headline blurb with the mid-tier LLM's fluency; still very cheap.                                      |
|  4 | `balanced-quick`      | 3-5 sentences from the fast LLM; slightly deeper than `brief-quick`.                                   |
|  5 | `brief-standard`      | 3-sentence recap with smoother phrasing than `brief-quick`.                                            |
|  6 | **`balanced-standard`** | Balanced coverage without run-ons. The general default for most articles.                            |
|  7 | `thorough-quick`      | Paragraph-length from the fast LLM; retains the top 2-3 supporting facts.                              |
|  8 | `minimal-deep`        | Headline output with the deeper LLM's coherence; frugal way to buy fluency without length.             |
|  9 | `brief-deep`          | 3-sentence recap with deeper-model reasoning.                                                          |
| 10 | **`thorough-deep`**   | Preserves specific dates, names, secondary facts. Research papers, meeting transcripts, long articles. |
| 11 | `detailed-quick`      | Longer paragraph from the fast LLM; more supporting facts, still light on nuance.                      |
| 12 | `thorough-standard`   | Retains dates and names on Standard-class content; great for meeting-transcript recaps.                |
| 13 | `complete-quick`      | Maximum retention the Quick LLM can produce; nearing Standard breadth but Quick tone.                  |
| 14 | `balanced-deep`       | 4-6 sentences with deep-model narrative flow.                                                          |
| 15 | `detailed-standard`   | Full-paragraph, entity-preserving; approaches Deep on retention.                                       |
| 16 | `complete-standard`   | Maximum Standard retention; substantial output length.                                                 |
| 17 | `detailed-deep`       | Heavy retention with deep-model reasoning; picks up minor arguments.                                   |
| 18 | `complete-deep`       | Maximum Deep retention; edging into Premium coverage.                                                  |
| 19 | `minimal-premium`     | Very short output with premium-model tone; premium quality at bargain length.                          |
| 20 | `brief-premium`       | 3-sentence recap with high-fidelity entity handling.                                                   |
| 21 | **`detailed-premium`** | Entity preservation, edge cases, atmospheric detail. Substantial documents and long-form reports.     |
| 22 | `balanced-premium`    | Moderate-length premium coverage; smoother than Deep, more concise than `detailed-premium`.            |
| 23 | `thorough-premium`    | Heavy retention with premium reasoning.                                                                |
| 24 | `minimal-ultra`       | Single-shot on the full document, minimum output length. Ultra fidelity, tiny output.                  |
| 25 | `brief-ultra`         | Full-context single-shot, 3-sentence output. Ideal for research-grade preview blurbs.                  |
| 26 | **`complete-ultra`**  | Single-shot on the full document, maximum retention, no chunking artifacts. Book-length manuscripts, long-form technical documentation. |
| 27 | `complete-premium`    | Maximum Premium retention; almost every noteworthy fact.                                               |
| 28 | `balanced-ultra`      | Full-context single-shot, balanced-length output.                                                      |
| 29 | `thorough-ultra`      | Full-context, retains most secondary facts.                                                            |
| 30 | `detailed-ultra`      | Full-context, near-maximum retention; the top rung of the spectrum.                                    |


**Three optional axis overrides**, sent as headers via `extraHeaders`:

- `X-Optional-Quality` — LLM tier: `quick | standard | deep | premium | ultra`
- `X-Optional-Extractive-Lvl` — retention level: `minimal | brief | balanced | thorough | detailed | complete`
- `X-Optional-Strategy` — inference strategy: `contextual-compression | premium-single-shot | hierarchical-merge`

```java
// named preset
client.summarize(text,
    SummarizeOptions.builder().tier("thorough-quick").build());

// preset + one axis override (axes win, warning header returned)
Map<String,String> h = new HashMap<>();
h.put("X-Optional-Extractive-Lvl", "brief");
client.summarize(text,
    SummarizeOptions.builder().tier("premium").extraHeaders(h).build());

// all three axes, no preset
Map<String,String> all = new HashMap<>();
all.put("X-Optional-Quality", "ultra");
all.put("X-Optional-Extractive-Lvl", "complete");
all.put("X-Optional-Strategy", "premium-single-shot");
client.summarize(text,
    SummarizeOptions.builder().extraHeaders(all).build());

// opt into permissive downgrade on paid-tier
Map<String,String> pd = new HashMap<>();
pd.put("X-Allow-Downgrade", "true");
client.summarize(text,
    SummarizeOptions.builder().tier("premium").extraHeaders(pd).build());
```

Native builder methods for the 3 axes + `allowDowngrade` land in the
next SDK release. Use `extraHeaders` in the meantime.

## Paid-tier quality guarantees

Default = strict wait for the tier's canonical primary model. Opt into
permissive fallback with `X-Allow-Downgrade: true` — the worker walks
DOWN the ladder (premium → deep → standard → quick) and returns
whichever tier's primary is available. Response carries
`X-Quality-Actual` and `X-Original-Tier` when a downgrade happened,
and the credit-cost delta is automatically refunded.

## Async submit + poll

Reachable today via `extraHeaders` + a manual poll:

```java
// Submit
Map<String,String> h = new HashMap<>();
h.put("X-Async", "true");
SummarizeResult sub = client.summarize(text,
    SummarizeOptions.builder().tier("ultra").extraHeaders(h).build());
String requestId = sub.getHeader("X-Paid-Request-Id");

// Poll GET /paid/result/{id} — 200 done, 202 queued, 410 expired
```

Native `submitAsync` / `getResult` / `waitForResult` methods land in
the next SDK release.

## Error handling

Every failure the client raises subclasses `TldrapiException`:

```java
import com.unitycubed.tldrapi.TldrapiException;

try {
    SummarizeResult r = client.summarize(text);
} catch (TldrapiException.Authentication e) {
    // 401/403: bad or missing key
} catch (TldrapiException.RateLimit e) {
    Thread.sleep(e.getRetryAfterSeconds() * 1000L);
} catch (TldrapiException.InsufficientCredits e) {
    // 402: surface the top-up path from e.getResponseBody()
} catch (TldrapiException.LanguageNotSupported e) {
    // 400: English-only at launch; cross-lingual coming Month 2-3
} catch (TldrapiException.Server e) {
    // 5xx after retries exhausted
} catch (TldrapiException e) {
    // catch-all
}
```

Every exception carries `getStatusCode()`, `getRequestId()` (attach
when reporting bugs), and `getResponseBody()`.

## Configuration

```java
Tldrapi client = Tldrapi.builder()
    .rapidApiKey("...")                                 // required
    .rapidApiHost("tldrapi-summarizer.p.rapidapi.com")  // default
    .baseUrl(null)                                      // defaults to https://<rapidApiHost>
    .timeoutSeconds(60)                                 // per-request
    .retries(3)                                         // 5xx + transport retries; 0 disables
    .userAgent(null)                                    // defaults to "tldrapi-java/<version>"
    .httpClient(null)                                   // inject your own HttpClient if needed
    .build();
```

Per-call options:

```java
SummarizeOptions opts = SummarizeOptions.builder()
    .tier("deep")                    // one of the 30 presets
    .sessionId("...")                // pin to a prior session
    .modelAlias("openai-gpt-4o")     // force a specific model (rarely needed)
    .allowOverage(false)             // permit charges beyond included credits
    .perCallTimeoutSeconds(120)      // override client timeout for this call
    .extraHeaders(headers)           // any X-* headers, e.g. X-Allow-Downgrade
    .build();
```

## Building from source

```bash
cd sdks/java/tldrapi
mvn clean install
```

Tests: `mvn test`.

## License

MIT. See `LICENSE`.

Copyright (c) 2026 Ehren Biglari / Unity Cubed.
