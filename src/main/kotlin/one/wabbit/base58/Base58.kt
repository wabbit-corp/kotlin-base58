package one.wabbit.base58

import kotlin.math.ceil
import kotlin.math.ln

typealias Uuid = java.util.UUID

/**
 * Custom exception for Base58 decoding errors.
 */
class Base58DecodingException(message: String) : Exception(message)

/**
 * Provides Base58 encoding and decoding functionality.
 *
 * Base58 is a binary-to-text encoding scheme that's similar to Base64 but uses a smaller alphabet.
 * It excludes easily-confused characters (0, O, I, l) and non-alphanumeric characters.
 *
 * Example usage:
 * ```
 * val encoded = Base58.encode("Hello, World!".toByteArray())
 * val decoded = Base58.decode(encoded)
 * println(String(decoded)) // Prints: Hello, World!
 *
 * val uuid = Uuid.random()
 * val encodedUUID = Base58.encodeUUID(uuid)
 * val decodedUUID = Base58.decodeUUID(encodedUUID)
 * println(uuid == decodedUUID) // Prints: true
 * ```
 */
object Base58 {
    private const val ENCODED_ZERO = '1'
    private const val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
    private val INDICES = IntArray(128) { ALPHABET.indexOf(it.toChar()) }
    private val K = ln(256.0) / ln(58.0)

    /**
     * Encodes a byte array as a Base58 string.
     *
     * @param input The byte array to encode
     * @return The Base58-encoded string
     * @throws IllegalArgumentException if the input is too large to safely process
     */
    fun encode(input: ByteArray): String {
        if (input.isEmpty()) return ""

        // Check for potential overflow.
        require(input.size <= Int.MAX_VALUE / 2) { "Input too large to safely process" }

        val inputCopy = input.copyOf()
        val inputSize = inputCopy.size

        // Count the leading zeros.
        var zeros = 0
        while (zeros < inputSize && inputCopy[zeros].toInt() == 0) {
            zeros += 1
        }

        val encoded = CharArray(ceil((inputSize - zeros) * K).toInt() + zeros)
        var outputStart = encoded.size
        var inputStart = zeros
        while (inputStart < inputSize) {
            outputStart -= 1
            // Perform the division in-place.
            val remainder = divmod_256_58(inputCopy, inputStart)
            encoded[outputStart] = ALPHABET[remainder.toInt()]
            while (inputStart < inputSize && inputCopy[inputStart].toInt() == 0) {
                inputStart += 1
            }
        }

        // Preserve leading zeros.
        while (outputStart < encoded.size && encoded[outputStart] == ENCODED_ZERO) {
            outputStart += 1
        }
        for (it in 0 until zeros) {
            outputStart -= 1
            encoded[outputStart] = ENCODED_ZERO
        }

        return encoded.concatToString(outputStart, encoded.size)
    }

    /**
     * Decodes a Base58 string into a byte array.
     *
     * @param input The Base58-encoded string to decode
     * @return The decoded byte array
     * @throws Base58DecodingException if the input is not a valid Base58 string
     */
    @Throws(Base58DecodingException::class)
    fun decode(input: String): ByteArray {
        if (input.isEmpty()) return ByteArray(0)

        // Convert the input string to base-58 digits.
        val inputSize = input.length
        val input58 = ByteArray(inputSize)
        for (i in input.indices) {
            val char = input[i]
            val digit = if (char.code < 128) INDICES[char.code] else -1
            if (digit < 0) {
                throw Base58DecodingException("Illegal character '$char' at position $i.")
            }
            input58[i] = digit.toByte()
        }

        // Count the leading zeros.
        var zeros = 0
        while (zeros < inputSize && input58[zeros].toInt() == 0) {
            zeros += 1
        }

        val expectedSize = ceil((inputSize - zeros) / K).toInt() + zeros
        val decoded = ByteArray(expectedSize)
        var outputStart = decoded.size
        var inputStart = zeros
        while (inputStart < inputSize) {
            val remainder = divmod_58_256(input58, inputStart)

            // Skip leading zeros.
            while (inputStart < inputSize && input58[inputStart].toInt() == 0) {
                inputStart += 1
            }

            outputStart -= 1
            decoded[outputStart] = (remainder and 0xFFu).toByte()

            if (inputSize == inputStart && (remainder shr 8) == 0uL) continue

            outputStart -= 1
            decoded[outputStart] = (remainder shr 8).toByte()
        }

        // We may have leading zeros in the decoded array.
        while (outputStart < decoded.size && decoded[outputStart].toInt() == 0) {
            outputStart += 1
        }

        if (outputStart - zeros == 0) return decoded
        return decoded.copyOfRange(outputStart - zeros, decoded.size)
    }

    private fun divmod_256_58(digits: ByteArray, startIndex: Int): UInt {
        val size = digits.size
        var remainder: Long = 0

        val restStart = size - (size - startIndex) % 4

        for (i in startIndex ..< restStart step 4) {
            val d1 = digits[i].toLong() and 0xFF
            val d2 = digits[i + 1].toLong() and 0xFF
            val d3 = digits[i + 2].toLong() and 0xFF
            val d4 = digits[i + 3].toLong() and 0xFF
            val temp = (remainder shl 32) + (d1 shl 24) + (d2 shl 16) + (d3 shl 8) + d4
            val quotient = temp / 58
            digits[i] = (quotient ushr 24).toByte()
            digits[i + 1] = (quotient ushr 16).toByte()
            digits[i + 2] = (quotient ushr 8).toByte()
            digits[i + 3] = quotient.toByte()
            remainder = temp - quotient * 58
        }

        for (i in restStart ..< size) {
            val digit = digits[i].toInt() and 0xFF
            val temp = (remainder shl 8) + digit
            val quotient = temp / 58
            digits[i] = quotient.toByte()
            remainder = temp - quotient * 58
        }

        return remainder.toUInt()
    }

    private const val P58_0: Long = 1L
    private const val P58_1: Long = 58L
    private const val P58_2: Long = 58L * 58
    private const val P58_3: Long = 58L * 58 * 58
    private const val P58_4: Long = 58L * 58 * 58 * 58

    private fun divmod_58_256(digits: ByteArray, startIndex: Int): ULong {
        val size = digits.size
        var remainder: Long = 0

        val restStart = size - (size - startIndex) % 4

        for (i in startIndex ..< restStart step 4) {
            val d1 = digits[i].toLong() and 0xFF
            val d2 = digits[i + 1].toLong() and 0xFF
            val d3 = digits[i + 2].toLong() and 0xFF
            val d4 = digits[i + 3].toLong() and 0xFF

            val temp: Long = remainder * P58_4 + d1 * P58_3 + d2 * P58_2 + d3 * P58_1 + d4 * P58_0
            val quotient = temp ushr 16
            val n1 = quotient / P58_3
            val n2 = (quotient % P58_3) / P58_2
            val n3 = (quotient % P58_2) / P58_1
            val n4 = quotient % P58_1
            digits[i]     = n1.toByte()
            digits[i + 1] = n2.toByte()
            digits[i + 2] = n3.toByte()
            digits[i + 3] = n4.toByte()

            remainder = temp and 0xFFFFL
        }

        for (i in restStart ..< size) {
            val digit = digits[i].toInt() and 0xFF
            val temp = remainder * P58_1 + digit * P58_0
            val quotient = temp ushr 16
            digits[i] = quotient.toByte()
            remainder = temp and 0xFFFFL
        }

        return remainder.toULong()
    }

    /**
     * Encodes a Short as a Base58 string.
     *
     * @param value The Short value to encode
     * @return The Base58-encoded string
     */
    fun encodeShort(value: Short): String {
        val bytes = ByteArray(2)
        bytes[0] = (value.toInt() ushr 8).toByte()
        bytes[1] = value.toByte()
        return encode(bytes)
    }

    /**
     * Decodes a Base58 string into a Short.
     *
     * @param value The Base58-encoded string to decode
     * @return The decoded Short value
     * @throws Base58DecodingException if the input is invalid or not the correct length
     */
    @Throws(Base58DecodingException::class)
    fun decodeShort(value: String): Short {
        val bytes = decode(value)
        if (bytes.size != 2)
            throw Base58DecodingException("Invalid Short length: ${bytes.size}")
        return ((bytes[0].toInt() and 0xFF shl 8) or (bytes[1].toInt() and 0xFF)).toShort()
    }

    /**
     * Encodes an Int as a Base58 string.
     *
     * @param value The Int value to encode
     * @return The Base58-encoded string
     */
    fun encodeInt(value: Int): String {
        val bytes = ByteArray(4)
        bytes[0] = (value ushr 24).toByte()
        bytes[1] = (value ushr 16).toByte()
        bytes[2] = (value ushr 8).toByte()
        bytes[3] = value.toByte()
        return encode(bytes)
    }

    /**
     * Decodes a Base58 string into an Int.
     *
     * @param value The Base58-encoded string to decode
     * @return The decoded Int value
     * @throws Base58DecodingException if the input is invalid or not the correct length
     */
    @Throws(Base58DecodingException::class)
    fun decodeInt(value: String): Int {
        val bytes = decode(value)
        if (bytes.size != 4)
            throw Base58DecodingException("Invalid Int length: ${bytes.size}")
        return (bytes[0].toInt() and 0xFF shl 24) or
               (bytes[1].toInt() and 0xFF shl 16) or
               (bytes[2].toInt() and 0xFF shl 8) or
               (bytes[3].toInt() and 0xFF)
    }

    /**
     * Encodes a Long as a Base58 string.
     *
     * @param value The Long value to encode
     * @return The Base58-encoded string
     */
    fun encodeLong(value: Long): String {
        val bytes = ByteArray(8)
        bytes[0] = (value ushr 56).toByte()
        bytes[1] = (value ushr 48).toByte()
        bytes[2] = (value ushr 40).toByte()
        bytes[3] = (value ushr 32).toByte()
        bytes[4] = (value ushr 24).toByte()
        bytes[5] = (value ushr 16).toByte()
        bytes[6] = (value ushr 8).toByte()
        bytes[7] = value.toByte()
        return encode(bytes)
    }

    /**
     * Decodes a Base58 string into a Long.
     *
     * @param value The Base58-encoded string to decode
     * @return The decoded Long value
     * @throws Base58DecodingException if the input is invalid or not the correct length
     */
    @Throws(Base58DecodingException::class)
    fun decodeLong(value: String): Long {
        val bytes = decode(value)
        if (bytes.size != 8)
            throw Base58DecodingException("Invalid Long length: ${bytes.size}")
        return (bytes[0].toLong() and 0xFF shl 56) or
               (bytes[1].toLong() and 0xFF shl 48) or
               (bytes[2].toLong() and 0xFF shl 40) or
               (bytes[3].toLong() and 0xFF shl 32) or
               (bytes[4].toLong() and 0xFF shl 24) or
               (bytes[5].toLong() and 0xFF shl 16) or
               (bytes[6].toLong() and 0xFF shl 8) or
               (bytes[7].toLong() and 0xFF)
    }

    /**
     * Encodes a UUID as a Base58 string.
     *
     * @param uuid The UUID to encode
     * @return The Base58-encoded string
     */
    fun encodeUUID(uuid: Uuid): String {
        val bytes = ByteArray(16)
        val mostSignificantBits = uuid.mostSignificantBits
        val leastSignificantBits = uuid.leastSignificantBits
        bytes[0] = (mostSignificantBits ushr 56).toByte()
        bytes[1] = (mostSignificantBits ushr 48).toByte()
        bytes[2] = (mostSignificantBits ushr 40).toByte()
        bytes[3] = (mostSignificantBits ushr 32).toByte()
        bytes[4] = (mostSignificantBits ushr 24).toByte()
        bytes[5] = (mostSignificantBits ushr 16).toByte()
        bytes[6] = (mostSignificantBits ushr 8).toByte()
        bytes[7] = mostSignificantBits.toByte()
        bytes[8] = (leastSignificantBits ushr 56).toByte()
        bytes[9] = (leastSignificantBits ushr 48).toByte()
        bytes[10] = (leastSignificantBits ushr 40).toByte()
        bytes[11] = (leastSignificantBits ushr 32).toByte()
        bytes[12] = (leastSignificantBits ushr 24).toByte()
        bytes[13] = (leastSignificantBits ushr 16).toByte()
        bytes[14] = (leastSignificantBits ushr 8).toByte()
        bytes[15] = leastSignificantBits.toByte()
        return encode(bytes)
    }

    /**
     * Decodes a Base58 string into a UUID.
     *
     * @param value The Base58-encoded string to decode
     * @return The decoded UUID
     * @throws Base58DecodingException if the input is invalid or not the correct length
     */
    @Throws(Base58DecodingException::class)
    fun decodeUUID(value: String): Uuid {
        val bytes = decode(value)
        if (bytes.size != 16)
            throw Base58DecodingException("Invalid UUID length: ${bytes.size}")
        val mostSigBits = (bytes[0].toLong() and 0xFF shl 56) or
                          (bytes[1].toLong() and 0xFF shl 48) or
                          (bytes[2].toLong() and 0xFF shl 40) or
                          (bytes[3].toLong() and 0xFF shl 32) or
                          (bytes[4].toLong() and 0xFF shl 24) or
                          (bytes[5].toLong() and 0xFF shl 16) or
                          (bytes[6].toLong() and 0xFF shl 8) or
                          (bytes[7].toLong() and 0xFF)
        val leastSigBits = (bytes[8].toLong() and 0xFF shl 56) or
                           (bytes[9].toLong() and 0xFF shl 48) or
                           (bytes[10].toLong() and 0xFF shl 40) or
                           (bytes[11].toLong() and 0xFF shl 32) or
                           (bytes[12].toLong() and 0xFF shl 24) or
                           (bytes[13].toLong() and 0xFF shl 16) or
                           (bytes[14].toLong() and 0xFF shl 8) or
                           (bytes[15].toLong() and 0xFF)
        return Uuid(mostSigBits, leastSigBits)
    }
}
