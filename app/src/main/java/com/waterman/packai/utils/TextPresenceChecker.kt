package com.waterman.packai.utils

import android.util.Log

data class PresenceResult(
    val isValid: Boolean,
    val commonMatchedText: String = ""
)

object TextPresenceChecker {

    private val normalizeCache = object : android.util.LruCache<String, String>(50) {}
    private val normalizeTokenCache = object : android.util.LruCache<String, String>(50) {}

    fun check(extracted: String, actual: String): PresenceResult {
        if (extracted.isBlank() || actual.isBlank()) {
            log("❌ Invalid — Input is blank")
            return PresenceResult(isValid = false)
        }

        // For L1 & L3: remove spaces, slashes only
        val normExtracted = getCachedNormalize(extracted)
        val normActual = getCachedNormalize(actual)

        // For L2: remove spaces, slashes, dots, dashes too
        val deepNormExtracted = getCachedDeepNormalize(extracted)
        val deepNormActual = getCachedDeepNormalize(actual)

        if (normExtracted.isEmpty() || normActual.isEmpty()) {
            log("❌ Invalid — Normalized string is empty")
            return PresenceResult(isValid = false)
        }

        log("Extracted : $normExtracted")
        log("Actual    : $normActual")

        // Level 1: Direct substring — spaces/slash removed only
        val l1 = level1DirectMatch(normExtracted, normActual)
        if (l1 != null) return l1

        // Level 2: Token match — fully cleaned, split by delimiters
        val l2 = level2TokenMatch(extracted, deepNormActual)
        if (l2 != null) return l2

        // Level 3: Fuzzy sliding window
        val l3 = level3BoundedFuzzy(normExtracted, normActual)
        if (l3 != null) return l3

        log("❌ Invalid — All levels failed")
        return PresenceResult(isValid = false)
    }

    // ─── Level 1: Direct substring ─────────────────────────────────────────────
    private fun level1DirectMatch(normExtracted: String, normActual: String): PresenceResult? {
        val idx = normActual.indexOf(normExtracted)
        if (idx == -1) {
            log("L1 MISS")
            return null
        }
        val common = normActual.substring(idx, idx + normExtracted.length)
        log("L1 HIT ✅ | Common: '$common'")
        return PresenceResult(isValid = true, commonMatchedText = common)
    }

    // ─── Level 2: Token match ──────────────────────────────────────────────────
    private fun level2TokenMatch(originalExtracted: String, deepNormActual: String): PresenceResult? {
        // Split original text by delimiters FIRST, then normalize each token
        val tokens = originalExtracted
            .split("-", ".", "_", " ", "/")
            .map { it.lowercase().replace("/", "").replace(" ", "").trim() }
            .filter { it.length >= 2 }

        log("L2 Tokens : $tokens")

        if (tokens.isEmpty()) {
            log("L2 MISS — no valid tokens")
            return null
        }

        val matchedTokens = mutableListOf<String>()
        val missedTokens = mutableListOf<String>()

        /*tokens.forEach { token ->
            if (deepNormActual.contains(token)) {
                matchedTokens.add(token)
                log("L2 Token HIT ✅ : '$token'")
            } else {
                missedTokens.add(token)
                log("L2 Token MISS ❌ : '$token'")
            }
        }*/
        tokens.forEach { token ->
            val matched = if (token.all { it.isDigit() }) {
                // Check isolated first
                val isolatedMatch = Regex("""(?<!\d)${Regex.escape(token)}(?!\d)""")
                    .containsMatchIn(deepNormActual)
                // PATCH: also check if token appears as part of combined slash-pair (e.g "4710")
                val pairedMatch = deepNormActual.contains(token)
                        && tokens.filter { it.all(Char::isDigit) }
                    .any { other ->
                        other != token &&
                                deepNormActual.contains("${token}${other}") ||
                                deepNormActual.contains("${other}${token}")
                    }
                isolatedMatch || pairedMatch
            } else {
                deepNormActual.contains(token)
            }

            if (matched) {
                matchedTokens.add(token)
                log("L2 Token HIT ✅ : '$token'")
            } else {
                missedTokens.add(token)
                log("L2 Token MISS ❌ : '$token'")
            }
        }


        val ratio = matchedTokens.size.toFloat() / tokens.size
        log("L2 Ratio: ${"%.2f".format(ratio)} | Matched: $matchedTokens | Missed: $missedTokens")

        if (ratio < 0.75f) {
            log("L2 MISS")
            return null
        }

        val common = matchedTokens.joinToString("")
        log("L2 HIT ✅ | Common: '$common'")
        return PresenceResult(isValid = true, commonMatchedText = common)
    }

    // ─── Level 3: Bounded fuzzy ────────────────────────────────────────────────
    private fun level3BoundedFuzzy(
        normExtracted: String,
        normActual: String,
        threshold: Float = 0.75f
    ): PresenceResult? {
        if (normExtracted.length > normActual.length) {
            log("L3 SKIP — extracted longer than actual")
            return null
        }

        val windowSize = normExtracted.length
        var bestScore = 0f
        var bestWindow = ""
        var bestIndex = -1

        for (i in 0..(normActual.length - windowSize)) {
            val window = normActual.substring(i, i + windowSize)
            if (!quickPreFilter(normExtracted, window, threshold)) continue
            val score = levenshteinSimilarity(normExtracted, window)
            if (score > bestScore) {
                bestScore = score
                bestWindow = window
                bestIndex = i
            }
            if (bestScore >= threshold) {
                log("L3 Early exit at index $i")
                break
            }
        }

        log("L3 Best: '$bestWindow' | Score: ${"%.2f".format(bestScore)} | Position: $bestIndex")

        if (bestScore < threshold) {
            log("L3 MISS")
            return null
        }

        // ── PATCH: numeric pair must match exactly ───────────────────────
        val numericRe = Regex("""\d{2,4}""")
        val extractedNums = numericRe.findAll(normExtracted).map { it.value }.toList()
        val windowNums    = numericRe.findAll(bestWindow).map { it.value }.toList()
        if (extractedNums.isNotEmpty() && windowNums.isNotEmpty()) {
            val extractedKey = extractedNums.take(2).joinToString("")  // e.g. "4918"
            val windowKey    = windowNums.take(2).joinToString("")     // e.g. "4710"
            if (extractedKey != windowKey) {
                log("L3 PATCH MISS ❌ — numeric mismatch: '$extractedKey' vs '$windowKey'")
                return null
            }
        }

        val common = longestCommonSubstringFast(normExtracted, bestWindow)
        log("L3 HIT ✅ | Common: '$common'")
        return PresenceResult(isValid = true, commonMatchedText = common)
    }

    // ─── Pre-filter ────────────────────────────────────────────────────────────
    private fun quickPreFilter(s1: String, s2: String, threshold: Float): Boolean {
        val freq1 = IntArray(36)
        val freq2 = IntArray(36)
        s1.forEach { c ->
            when {
                c in 'a'..'z' -> freq1[c - 'a']++
                c in '0'..'9' -> freq1[26 + (c - '0')]++
            }
        }
        s2.forEach { c ->
            when {
                c in 'a'..'z' -> freq2[c - 'a']++
                c in '0'..'9' -> freq2[26 + (c - '0')]++
            }
        }
        var common = 0
        for (i in 0..35) common += minOf(freq1[i], freq2[i])
        val overlap = common.toFloat() / s1.length.coerceAtLeast(1)
        return overlap >= (threshold - 0.2f)
    }

    // ─── LCS ───────────────────────────────────────────────────────────────────
    private fun longestCommonSubstringFast(s1: String, s2: String): String {
        if (s1.isEmpty() || s2.isEmpty()) return ""
        var longestMatch = ""
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                if (s1[i - 1] == s2[j - 1]) {
                    dp[i][j] = dp[i - 1][j - 1] + 1
                    if (dp[i][j] > longestMatch.length) {
                        longestMatch = s1.substring(i - dp[i][j], i)
                    }
                }
            }
        }
        return longestMatch
    }

    // ─── Levenshtein ───────────────────────────────────────────────────────────
    private fun levenshteinSimilarity(s1: String, s2: String): Float {
        val maxLen = maxOf(s1.length, s2.length)
        if (maxLen == 0) return 1f
        return 1f - levenshteinDistance(s1, s2).toFloat() / maxLen
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length
        if (len1 > len2) return levenshteinDistance(s2, s1)
        var prev = IntArray(len1 + 1) { it }
        var curr = IntArray(len1 + 1)
        for (j in 1..len2) {
            curr[0] = j
            for (i in 1..len1) {
                curr[i] = if (s1[i - 1] == s2[j - 1]) prev[i - 1]
                else 1 + minOf(prev[i], curr[i - 1], prev[i - 1])
            }
            val tmp = prev; prev = curr; curr = tmp
        }
        return prev[len1]
    }

    // ─── Normalize ─────────────────────────────────────────────────────────────
    // L1 & L3: remove spaces and slashes only
    private fun normalize(text: String): String {
        return text.lowercase().replace("/", "").replace(" ", "").trim()
    }

    // L2: remove spaces, slashes, dots, dashes
    private fun deepNormalize(text: String): String {
        return text.lowercase().replace("/", "").replace(" ", "").replace(".", "").replace("-", "").trim()
    }

    private fun getCachedNormalize(text: String): String {
        return normalizeCache.get(text) ?: normalize(text).also { normalizeCache.put(text, it) }
    }

    private fun getCachedDeepNormalize(text: String): String {
        return normalizeTokenCache.get(text) ?: deepNormalize(text).also { normalizeTokenCache.put(text, it) }
    }

    // ─── Log only in debug ─────────────────────────────────────────────────────
    private fun log(msg: String) {
        Log.d("TPC", msg)
    }
}