# Module kotlin-base58

`kotlin-base58` is a small JVM library for turning binary values into human-friendly Base58 strings and back again.

It is useful when you want identifiers that are shorter and easier to copy than hexadecimal while avoiding visually ambiguous characters such as `0`, `O`, `I`, and `l`.

## What It Supports

- arbitrary `ByteArray` values
- `Short`, `Int`, and `Long`
- `Uuid`

This library implements plain Base58 encoding and decoding. It does **not** implement checksum-bearing variants such as Bitcoin Base58Check.

## Installation

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("one.wabbit:kotlin-base58:1.1.1")
}
```

## Quick Start

```kotlin
import one.wabbit.base58.Base58

val payload = "Hello, world!".encodeToByteArray()
val encoded = Base58.encode(payload)
val decoded = Base58.decode(encoded).decodeToString()

check(encoded == "72k1xXWG59wUsYv7h2")
check(decoded == "Hello, world!")
```

## Primitive and Uuid Helpers

```kotlin
import one.wabbit.base58.Base58
import kotlin.uuid.Uuid

val userId = 42
val compactUserId = Base58.encodeInt(userId)
check(Base58.decodeInt(compactUserId) == userId)

val sessionId = Uuid.parse("123e4567-e89b-12d3-a456-426614174000")
val compactSessionId = Base58.encodeUuid(sessionId)
check(Base58.decodeUuid(compactSessionId) == sessionId)
```

These typed helpers use fixed-width big-endian binary representations. They are not compact numeric Base58 helpers, so `decodeInt("j")` does not decode to `42`.

## Leading Zeros Are Preserved

Base58 often appears in contexts where binary data may contain leading zero bytes. Those zeros are preserved by this implementation:

```kotlin
import one.wabbit.base58.Base58

val bytes = byteArrayOf(0, 0, 1, 2, 3)
val encoded = Base58.encode(bytes)
val decoded = Base58.decode(encoded)

check(decoded.contentEquals(bytes))
```

## Error Handling

`Base58.decode*` functions throw `Base58DecodingException` when:

- the input contains characters outside the Base58 alphabet
- a typed decode receives the wrong decoded byte length, such as trying to decode a non-UUID string with `decodeUuid`

## API Notes

- Integer helpers use big-endian byte order.
- Empty input encodes to an empty string.
- Decoding an empty string returns an empty `ByteArray`.
