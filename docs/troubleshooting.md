# Troubleshooting

## Invalid Character Errors

Base58 excludes `0`, `O`, `I`, and `l`. Decoding any character outside the ASCII Base58 alphabet,
including non-ASCII characters, throws `Base58DecodingException`.

```kotlin
Base58.decode("0OIl") // throws
```

## Typed Decode Length Errors

Typed decoders require the fixed-width byte count for the target type.

```kotlin
Base58.decodeInt("j") // throws: "j" decodes to one byte, not four
```

Use `Base58.decode` when you need variable-length bytes, or use the matching typed encoder and
decoder pair for fixed-width values.

## Base58Check Values

This library implements plain Base58. It does not validate, add, or remove Base58Check checksums.

If a decoded value has protocol-specific prefix, version, or checksum bytes, decode the Base58 text
first and then validate those bytes in protocol-specific code.

## Empty Input

Encoding an empty `ByteArray` returns an empty string. Decoding an empty string returns an empty
`ByteArray`.
