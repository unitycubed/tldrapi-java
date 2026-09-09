# tldrapi-java

Official Java SDK for [TLDRapi](https://tldrapi.com) — summarize any content, in one API call.

## Install

Coming soon on Maven Central. For now, clone this repo and build with Gradle:

```bash
git clone https://github.com/unitycubed/tldrapi-java
cd tldrapi-java/tldrapi
./gradlew build
```

## Get your app's RapidAPI key

1. Sign in at [rapidapi.com](https://rapidapi.com)
2. Subscribe to the [TLDRapi Summarizer](https://rapidapi.com/thunderAPIs256/api/tldrapi-summarizer) listing (start with **BASIC** — free)
3. Go to **Console** (top nav) → **Applications** → **Add App** (or open an existing one)
4. In the App → **Authorizations** tab → click the copy icon next to your Authorization Key

That's the app's `X-RapidAPI-Key`. Pass it to the SDK constructor.

*Legacy path (deprecated): upper-right (?) → Legacy Developer Dashboard → Add New App → Authorization tab. The new Console path above is simpler.*

The Authorization Key field is the same value in both places — RapidAPI just labels it differently depending on which interface you use:

**New Console:**

![RapidAPI Console — Authorization Method labeled "RAPIDAPI"](https://raw.githubusercontent.com/unitycubed/tldrapi-docs/main/img/rapidapi-key-label-console.png)

**Legacy Developer Dashboard:**

![RapidAPI Legacy Developer Dashboard — Authorization Method labeled "API key"](https://raw.githubusercontent.com/unitycubed/tldrapi-docs/main/img/rapidapi-key-label-legacy.png)



## Quickstart

```java
import com.tldrapi.TldrapiClient;

TldrapiClient client = new TldrapiClient(System.getenv("TLDRAPI_KEY"));
String summary = client.summarize("Long article text here...", "standard");
System.out.println(summary);
```

## Docs

- Full API docs: https://tldrapi.com
- RapidAPI listing: https://rapidapi.com/UnityCubed/api/tldrapi-summarizer

## License

Released under the MIT License — see [LICENSE](LICENSE).

Copyright (c) 2026 Ehren Biglari / Unity Cubed.
