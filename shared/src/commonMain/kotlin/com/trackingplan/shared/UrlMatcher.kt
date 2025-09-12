package com.trackingplan.shared

object UrlMatcher {
    private val regexCache = mutableMapOf<String, Regex?>()
    private val failedPatterns = mutableSetOf<String>()
    
    fun matchProvider(providers: Map<String, String>, requestUrl: String): String? {
        for ((pattern, providerName) in providers) {
            val matches = when {
                pattern.startsWith("regex:") -> {
                    val regexPattern = pattern.substring(6)
                    tryRegexMatch(regexPattern, requestUrl)
                }
                pattern.contains('*') -> {
                    val regexPattern = convertWildcardToRegex(pattern)
                    tryRegexMatch(regexPattern, requestUrl)
                }
                else -> {
                    requestUrl.contains(pattern)
                }
            }
            
            if (matches) return providerName
        }
        return null
    }
    
    private fun tryRegexMatch(regexPattern: String, requestUrl: String): Boolean {
        return if (failedPatterns.contains(regexPattern)) {
            false
        } else {
            try {
                val compiledRegex = regexCache.getOrPut(regexPattern) {
                    Regex(regexPattern)
                }
                compiledRegex?.containsMatchIn(requestUrl) ?: false
            } catch (e: Exception) {
                failedPatterns.add(regexPattern)
                false
            }
        }
    }
    
    @Suppress("unused") // Only used for testing
    fun clearRegexCache() {
        regexCache.clear()
        failedPatterns.clear()
    }
    
    private fun convertWildcardToRegex(pattern: String): String {
        return pattern
            .replace(".", "\\.")
            .replace("*", ".*")
    }
}