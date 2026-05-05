# API Reference

`kotlin-base58` exposes one exception type and one utility object.

Generate exact signatures locally with:

```bash
./gradlew dokkaGeneratePublicationHtml
```

## Public Surface

- `Base58DecodingException`: thrown by decoding functions for malformed input or wrong decoded
  lengths.
- `Base58.alphabet`: the `const val` digit alphabet used by this implementation.
- `Base58.encode(input)`: encodes arbitrary bytes.
- `Base58.decode(input)`: decodes arbitrary Base58 text.
- `Base58.encodeShort` and `Base58.decodeShort`: encode and decode fixed-width two-byte values.
- `Base58.encodeInt` and `Base58.decodeInt`: encode and decode fixed-width four-byte values.
- `Base58.encodeLong` and `Base58.decodeLong`: encode and decode fixed-width eight-byte values.
- `Base58.encodeUuid` and `Base58.decodeUuid`: encode and decode fixed-width 16-byte UUID values
  using Kotlin's experimental `kotlin.uuid.Uuid` type.

Typed integer helpers use big-endian two's-complement binary representations. They reject decoded
byte arrays whose length does not match the target type.
