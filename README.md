# tldrapi-java

Official Java SDK for [TLDRapi](https://tldrapi.com) — summarize any content, in one API call.

## Install

Coming soon on Maven Central. For now, clone this repo and build with Gradle:

```bash
git clone https://github.com/unitycubed/tldrapi-java
cd tldrapi-java/tldrapi
./gradlew build
```

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
