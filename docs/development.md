# Development

`kotlin-base58` is a Kotlin Multiplatform library. In the monorepo workspace, build and test it
through the `dev` tooling:

```bash
./dev build kotlin-base58
```

From the project directory, or when working from a standalone checkout, run Gradle directly:

```bash
./gradlew build
```

## Documentation Standards

Public KDoc should state:

- whether a helper works on arbitrary bytes or a fixed-width typed representation
- byte order for typed helpers
- exact failure conditions for decoding functions

Hand-written docs should describe behavior and examples. Generated Dokka output remains the source
of truth for exact signatures and platform availability.
