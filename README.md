# kotlin-base58

`kotlin-base58` is a JVM library for encoding binary data as compact, human-friendly Base58 strings and decoding those strings back to bytes or typed values.

It is designed for cases where hexadecimal is too long and Base64 is too punctuation-heavy, especially for IDs that humans may need to read, paste, or type.

## Why Base58

The Base58 alphabet removes visually ambiguous characters:

- `0`
- `O`
- `I`
- `l`

That makes it useful for invitation codes, external IDs, short opaque tokens, and UUID-like values that should stay copyable without looking noisy.

This library implements plain Base58 encoding. It does not implement Base58Check or any checksum-bearing Bitcoin-specific format.

## Installation

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("one.wabbit:kotlin-base58:1.1.1")
}
```

## Byte Array Example

```kotlin
import one.wabbit.base58.Base58

val payload = "Hello, world!".encodeToByteArray()
val encoded = Base58.encode(payload)
val decoded = Base58.decode(encoded).decodeToString()

check(encoded == "72k1xXWG59wUsYv7h2")
check(decoded == "Hello, world!")
```

## Primitive Helpers

The library also supports typed helpers for fixed-width values:

```kotlin
import one.wabbit.base58.Base58

val orderId = 42
val encodedOrderId = Base58.encodeInt(orderId)
val decodedOrderId = Base58.decodeInt(encodedOrderId)

check(decodedOrderId == orderId)
```

These helpers use big-endian byte order, which keeps the encoding deterministic across JVMs and interoperable with non-Kotlin implementations that use the same convention.

## Uuid Example

```kotlin
import one.wabbit.base58.Base58
import kotlin.uuid.Uuid

val sessionId = Uuid.parse("123e4567-e89b-12d3-a456-426614174000")
val encodedSessionId = Base58.encodeUuid(sessionId)
val decodedSessionId = Base58.decodeUuid(encodedSessionId)

check(decodedSessionId == sessionId)
```

## Leading Zeros

Leading zero bytes are preserved. That matters when you are encoding binary identifiers instead of text:

```kotlin
import one.wabbit.base58.Base58

val bytes = byteArrayOf(0, 0, 1, 2, 3)
val encoded = Base58.encode(bytes)
val decoded = Base58.decode(encoded)

check(decoded.contentEquals(bytes))
```

## Error Handling

`Base58.decode` and the typed decode helpers throw `Base58DecodingException` when:

- the input contains characters outside the Base58 alphabet
- the decoded byte length does not match the requested target type

For example, `decodeUuid` rejects values that do not decode to exactly 16 bytes.

## API Reference

Published API docs are available at:

- [https://wabbit-corp.github.io/kotlin-base58/](https://wabbit-corp.github.io/kotlin-base58/)

## Licensing

This project is licensed under the GNU Affero General Public License v3.0 (AGPL-3.0) for open source use.

For commercial use, contact Wabbit Consulting Corporation at `wabbit@wabbit.one`.

## Contributing

Before contributions can be merged, contributors need to agree to the repository CLA.
