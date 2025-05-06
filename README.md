# kotlin-base58

A Kotlin library for encoding and decoding data using Base58.

## Overview

Base58 is a binary-to-text encoding scheme that represents binary data using an alphabet of 58 characters. It excludes easily confused characters like `0`, `O`, `I`, and `l` to improve human readability and prevent errors when manually typing the encoded data.

This library provides a simple API for encoding byte arrays, primitives like `Short`, `Int`, `Long`, and `UUID` to Base58 strings, and decoding Base58 strings back to the original data types.

## Installation

Add the following dependency to your project:

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.wabbit-corp:kotlin-base58:1.0.0")
}
```

## Usage
Import the `Base58` object to access the encoding and decoding functions:
```kotlin
import one.wabbit.base58.Base58

// Encode a byte array
val bytes = "Hello, world!".toByteArray()
val encoded = Base58.encode(bytes)
println(encoded) // Prints: "72k1xXWG59wUsYv7h2"

// Decode a Base58 string
val decoded = Base58.decode("72k1xXWG59wUsYv7h2")
println(String(decoded)) // Prints: Hello, world!

// Encode and decode primitives
val encodedInt = Base58.encodeInt(42)
val decodedInt = Base58.decodeInt(encodedInt)
println(decodedInt) // Prints: 42

val uuid = UUID.randomUUID()
val encodedUUID = Base58.encodeUUID(uuid)
val decodedUUID = Base58.decodeUUID(encodedUUID)
println(uuid == decodedUUID) // Prints: true
```

## Error Handling
The `decode` functions throw a `Base58DecodingException` if the input string contains invalid characters or the decoded data does not match the expected type (e.g., wrong length for `UUID`).

## Performance
The library is optimized for performance and can encode and decode large amounts of data quickly.

## Licensing

This project is licensed under the GNU Affero General Public License v3.0 (AGPL-3.0) for open source use.

For commercial use, please contact Wabbit Consulting Corporation (at wabbit@wabbit.one) for licensing terms.

## Contributing

Before we can accept your contributions, we kindly ask you to agree to our Contributor License Agreement (CLA).
