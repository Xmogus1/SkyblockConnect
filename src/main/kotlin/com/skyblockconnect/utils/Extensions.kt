package com.skyblockconnect.utils

private val SPACE_CAPS_REGEX_1 = Regex("(?<=[a-z])(?=[A-Z])")
private val SPACE_CAPS_REGEX_2 = Regex("(?<=[A-Z])(?=[A-Z][a-z])")

fun String.startsWithOneOf(vararg prefixes: String): Boolean = prefixes.any { startsWith(it) }

fun String.endsWithOneOf(vararg suffixes: String): Boolean = suffixes.any { endsWith(it) }

fun <T> T.equalsOneOf(vararg others: T): Boolean = others.contains(this)

fun String.containsOneOf(vararg elements: CharSequence): Boolean = elements.any { contains(it) }

fun <T> Collection<T>.containsOneOf(vararg elements: T): Boolean = elements.any { contains(it) }

fun <T> Array<T>.containsOneOf(vararg elements: T): Boolean = elements.any { contains(it) }

fun String.spaceCaps(): String = replace(SPACE_CAPS_REGEX_1, " ").replace(SPACE_CAPS_REGEX_2, " ").trim()

fun String.uppercaseFirst(): String = replaceFirstChar { it.uppercase() }

fun String.remove(vararg patterns: String): String = patterns.fold(this) { acc, s -> acc.replace(s, "") }

fun String.remove(vararg patterns: Regex): String = patterns.fold(this) { acc, r -> acc.replace(r, "") }

inline fun <T> catch(block: () -> T) = try {
    block()
}
catch (_: Throwable) {
    null
}
