// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared

/**
 * Test logger for capturing and verifying log messages in tests.
 * Works across both shared module tests and platform SDK tests.
 */
class TestLogger(private val maxSize: Int = 20) : Logger {
    private val messages = mutableListOf<LogMessage>()
    private val expectations = mutableListOf<Expectation>()
    private val matches = mutableListOf<LogMessage>()

    data class LogMessage(val level: LogLevel, val message: String)

    override fun v(msg: String) = append(LogMessage(LogLevel.VERBOSE, msg))
    override fun d(msg: String) = append(LogMessage(LogLevel.DEBUG, msg))
    override fun i(msg: String) = append(LogMessage(LogLevel.INFO, msg))
    override fun w(msg: String) = append(LogMessage(LogLevel.WARN, msg))
    override fun e(msg: String) = append(LogMessage(LogLevel.ERROR, msg))

    private fun append(logMessage: LogMessage) {
        if (messages.size >= maxSize) {
            messages.removeAt(0)
        }
        messages.add(logMessage)

        if (expectations.isNotEmpty() && expectations[0].matches(logMessage.message)) {
            matches.add(logMessage)
            expectations.removeAt(0)
        }
    }

    fun reset() {
        messages.clear()
        expectations.clear()
        matches.clear()
    }

    fun containsExactMessage(msg: String): Boolean {
        return messages.any { it.message == msg }
    }

    fun expectExactMessage(message: String) {
        expectations.add(Expectation(MatchOperator.EXACT, message, emptyList()))
    }

    fun expectMessageStartsWith(prefix: String) {
        expectations.add(Expectation(MatchOperator.STARTS_WITH, prefix, emptyList()))
    }

    fun expectMessageStartingWithAndContaining(prefix: String, contains: List<String>) {
        expectations.add(Expectation(MatchOperator.STARTS_WITH_AND_CONTAINS, prefix, contains))
    }

    /**
     * Asserts that all expectations were matched.
     * Throws Exception if expectations don't match.
     *
     * Note: Uses @Throws so Swift can catch the exception properly.
     */
    @Throws(Exception::class)
    fun assertExpectationsMatch() {
        // Since logging is synchronous (android.util.Log and NSLog),
        // all expectations should be matched immediately by the time this is called
        if (expectations.isEmpty() && matches.isNotEmpty()) {
            matches.clear()
            return
        }

        if (expectations.isNotEmpty()) {
            val missing = expectations.joinToString("\n") { "  - ${it.message}" }
            throw Exception("Missing expected log messages:\n$missing")
        }
    }

    private enum class MatchOperator {
        EXACT, STARTS_WITH, STARTS_WITH_AND_CONTAINS
    }

    private data class Expectation(
        val operator: MatchOperator,
        val message: String,
        val contains: List<String>
    ) {
        fun matches(msg: String): Boolean = when (operator) {
            MatchOperator.EXACT -> msg == message
            MatchOperator.STARTS_WITH -> msg.startsWith(message)
            MatchOperator.STARTS_WITH_AND_CONTAINS ->
                msg.startsWith(message) && contains.all { msg.contains(it) }
        }
    }
}
