# User Guide

`kotlin-base58` encodes bytes with the Bitcoin Base58 alphabet:
`123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz`.

The alphabet omits `0`, `O`, `I`, and `l`, which makes encoded values easier to read and copy in
human-facing identifiers.

## Bytes

Use `Base58.encode` and `Base58.decode` for arbitrary binary payloads:

```kotlin
import one.wabbit.base58.Base58

val bytes = byteArrayOf(0, 0, 1, 2, 3)
val encoded = Base58.encode(bytes)
val decoded = Base58.decode(encoded)

check(decoded.contentEquals(bytes))
```

Leading zero bytes are preserved as leading `1` characters.

## Typed Values

The typed helpers serialize fixed-width binary values before Base58 encoding:

```kotlin
import one.wabbit.base58.Base58

val encoded = Base58.encodeInt(42)

check(encoded == "111j")
check(Base58.decodeInt(encoded) == 42)
```

These helpers are not compact numeric encoders. `decodeInt("j")` fails because `"j"` decodes to a
single byte, not the four bytes required for an `Int`.

## UUIDs

UUIDs are encoded as 16 bytes: most significant bits followed by least significant bits, each in
big-endian order.

```kotlin
import one.wabbit.base58.Base58
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
fun example() {
    val id = Uuid.parse("123e4567-e89b-12d3-a456-426614174000")
    val encoded = Base58.encodeUuid(id)

    check(Base58.decodeUuid(encoded) == id)
}
```

Because `kotlin.uuid.Uuid` is experimental, Kotlin call sites may need the same opt-in annotation.

## Plain Base58

This library does not add checksums or version bytes. If a protocol expects Base58Check or another
checksum-bearing format, apply that framing outside this library before calling `Base58.encode`.
