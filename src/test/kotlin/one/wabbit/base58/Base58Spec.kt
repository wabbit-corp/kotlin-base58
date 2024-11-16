package one.wabbit.base58

import kotlin.random.Random
import kotlin.test.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class Base58Spec {
//    private fun SplittableRandom.nextBytes(size: Int): ByteArray {
//        val bytes = ByteArray(size)
//        nextBytes(bytes)
//        return bytes
//    }

    // Tests from https://github.com/bitcoin/bitcoin/blob/master/src/test/data/base58_encode_decode.json
    @OptIn(ExperimentalStdlibApi::class)
    val TEST_VECTORS = listOf(
        "00eb15231dfceb60925886b67d065299925915aeb172c06647".hexToByteArray() to "1NS17iag9jJgTHD1VXjvLCEnZuQ3rJDE9L",
        "73696d706c792061206c6f6e6720737472696e67".hexToByteArray() to "2cFupjhnEsSn59qHXstmK2ffpLv2",
        "ecac89cad93923c02321".hexToByteArray() to "EJDM8drfXA6uyA",
        "00000000000000000000".hexToByteArray() to "1111111111",
        "bf4f89001e670274dd".hexToByteArray() to "3SEo3LWLoPntC",
        "516b6fcd0f".hexToByteArray() to "ABnLTmg",
        "572e4794".hexToByteArray() to "3EFU7m",
        "10c8511e".hexToByteArray() to "Rt5zm",
        "626262".hexToByteArray() to "a3gV",
        "636363".hexToByteArray() to "aPEr",
        "61".hexToByteArray() to "2g",
        "".hexToByteArray() to "",
        "Hello, world!".encodeToByteArray() to "72k1xXWG59wUsYv7h2",
    )

    @Test fun encodingKnownValues() {
        for ((decoded, encoded) in TEST_VECTORS) {
            assertEquals(encoded, Base58.encode(decoded))
            assertContentEquals(decoded, Base58.decode(encoded))
        }
    }

    @Test fun encodingInt16() {
        for (i in 1..100) {
            val value = Random.nextInt().toShort()
            val encoded = Base58.encodeShort(value)
            val decoded = Base58.decodeShort(encoded)
            assertEquals(value, decoded)
        }
    }

    @Test fun encodingInt32() {
        for (i in 1..100) {
            val value = Random.nextInt()
            val encoded = Base58.encodeInt(value)
            val decoded = Base58.decodeInt(encoded)
            assertEquals(value, decoded)
        }
    }

    @Test fun encodingInt64() {
        for (i in 1..100) {
            val value = Random.nextLong()
            val encoded = Base58.encodeLong(value)
            val decoded = Base58.decodeLong(encoded)
            assertEquals(value, decoded)
        }
    }

    @ExperimentalUuidApi
    @Test fun encodingUUID() {
        for (i in 1..100) {
            val value = Uuid.fromLongs(Random.nextLong(), Random.nextLong())
            val encoded = Base58.encodeUUID(value)
            val decoded = Base58.decodeUUID(encoded)
            assertEquals(value, decoded)
        }
    }

    @Test fun encodingByteArraysOfDifferentSizes() {
        // Test with various input sizes
        val inputSizes = listOf(0, 1, 10, 100, 1000)

        for (size in inputSizes) {
            val input = Random.nextBytes(size)
            val encoded = Base58.encode(input)
            val decoded = Base58.decode(encoded)

            assertEquals(input.size, decoded.size, "Input and decoded output should have the same size for input of size $size")
            assertTrue(input.contentEquals(decoded), "Decoding the encoded input should match the original input for size $size")
        }
    }

//    @Ignore
//    @Test fun `profiler test`() {
//        val rng = SplittableRandom(0x42)
//
//        // Test with various input sizes
//        val inputSizes = listOf(0, 1, 10, 100, 1000, 10000)
//
//        File("base58_sizes1.csv").printWriter().use { out ->
//            out.println("size,encoded_size")
//
//            for (it in 1..1000) {
//                val size = rng.nextInt(2000)
//                val input = rng.nextBytes(size)
//                val encoded = Base58.encode(input)
//                val decoded = Base58.decode(encoded)
//                if (it % 100 == 0) println("$size,${encoded.length}")
//                out.println("$size,${encoded.length}")
//            }
//
//            for (size in inputSizes) {
//                for (it in 1..100) {
//                    val input = rng.nextBytes(size)
//                    val encoded = Base58.encode(input)
//                    val decoded = Base58.decode(encoded)
//                    if (it % 100 == 0) println("$size,${encoded.length}")
//                    out.println("$size,${encoded.length}")
//                }
//            }
//        }
//    }

    @Test fun encodingByteArrayWithLeadingZeros() {
        val input = byteArrayOf(0, 0, 1, 2, 3)
        val encoded = Base58.encode(input)
        val decoded = Base58.decode(encoded)

        assertTrue(input.contentEquals(decoded), "Decoding the encoded input with leading zeros should match the original input")
    }

    @Test fun encodingEmptyByteArray() {
        val input = ByteArray(0)
        val encoded = Base58.encode(input)
        val decoded = Base58.decode(encoded)
        assertEquals("", encoded, "Encoding an empty input should result in an empty output")

        assertTrue(input.contentEquals(decoded), "Encoding and decoding an empty input should result in an empty output")
    }

    @Test fun encodingLargeByteArrays() {
        val input = Random.nextBytes(10000)
        val encoded = Base58.encode(input)
        val decoded = Base58.decode(encoded)

        assertTrue(input.contentEquals(decoded), "Encoding and decoding a large input should match the original input")
    }
}
