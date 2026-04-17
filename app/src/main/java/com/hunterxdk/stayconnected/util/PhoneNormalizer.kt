package com.hunterxdk.stayconnected.util

object PhoneNormalizer {

    /**
     * Strips all non-digit characters and common country-code prefixes (+1, leading 1)
     * from a phone number, returning the last 10 digits for US numbers.
     *
     * Examples:
     *   "+1 (555) 867-5309" → "5558675309"
     *   "15558675309"       → "5558675309"
     *   "555-867-5309"      → "5558675309"
     *   "5558675309"        → "5558675309"
     */
    fun normalize(raw: String): String {
        // Strip everything except digits
        val digitsOnly = raw.filter { it.isDigit() }

        // Remove leading country code for US (+1 / 1 prefix on 11-digit numbers)
        return when {
            digitsOnly.length == 11 && digitsOnly.startsWith("1") ->
                digitsOnly.substring(1)
            digitsOnly.length > 10 ->
                digitsOnly.takeLast(10)
            else ->
                digitsOnly
        }
    }

    /**
     * Returns true if two phone number strings refer to the same number after normalization.
     */
    fun isSameNumber(a: String, b: String): Boolean =
        normalize(a) == normalize(b)
}
