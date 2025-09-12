@file:JvmName("UrlMatcherJava")
package com.trackingplan.shared

object UrlMatcherJava {
    @JvmStatic
    fun matchProvider(providers: java.util.Map<String, String>, requestUrl: String): String? {
        // Direct call - Kotlin can handle Java Map automatically
        @Suppress("UNCHECKED_CAST")
        return UrlMatcher.matchProvider(providers as Map<String, String>, requestUrl)
    }

}