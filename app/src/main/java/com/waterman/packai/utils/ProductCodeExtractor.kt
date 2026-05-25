import android.util.Log

/**
 * ProductCodeExtractor
 * INPUT  : raw OCR / pasted text (String)
 * OUTPUT : extracted product codes (String, newline-separated)
 * LOGCAT : filter tag "PCE" in Android Studio
 *
 * USAGE:
 *   val result = ProductCodeExtractor.extract(rawText)
 */
object ProductCodeExtractor {

    private const val TAG = "PCE"

    private data class Match(val code: String, val source: String, val confidence: Int)

    // ── OCR fixes — ORDER IS CRITICAL ─────────────────────────────────────
    //
    //  "COh"       → "COR"       letter mis-read
    //  "cORA"      → "CORA"      letter mis-read
    //  "CoRA"      → "CORA"      mixed-case mis-read
    //  "Typa"      → "Type"      letter mis-read
    //  "Iype"      → "Type"      I/T mis-read
    //  "ype"       → "Type"      T dropped entirely
    //  "45/ 2"     → "45/2"      stray space inside slash
    //  "45112-"    → "45/12-"    OCR reads "/" as "1" (5-digit run)
    //  "4512-"     → "45/12-"    OCR drops "/" (4-digit run)
    //  "49 16-"    → "49/16-"    OCR reads "/" as space (split digits)
    //  "45/12- T"  → "45/12-T"   stray space after dash
    //  "RLX-s-"    → "RLX-S-"    lowercase between dashes
    //  "COR45"     → "COR 45"    missing space letters→digits
    //
    private fun applyOcrFixes(text: String): String {
        return text
            .replace(Regex("""\bCOh\b"""),"COR")
            .replace(Regex("""\bcORA\b"""),"CORA")
            .replace(Regex("""\bCoRA\b"""),"CORA")
            .replace(Regex("""\bTypa\b"""),"Type")
            .replace(Regex("""\bIype\b"""),"Type")
            .replace(Regex("""\bype\b"""), "Type")
            .replace(Regex("""(\d)\s*/\s*(\d)"""),"$1/$2")
            .replace(Regex("""(?<!\d)(\d{2})\d?(\d{2})(?=-)"""),"$1/$2")
            .replace(Regex("""([A-Z]{2,6}\s+)(\d{1,4})\s+(\d{1,4})(?=-)"""),"$1$2/$3")
            .replace(Regex("""-\s+([A-Z0-9])"""),"-$1")
            .replace(Regex("""(?<=-)[a-z](?=-)""")) { it.value.uppercase() }
            .replace(Regex("""([A-Z]{2,6})(\d)"""),"$1 $2")
            .replace(Regex("""\bype([A-Z])([A-Z]{2,4})\b"""),"Type $1$2")
            .replace(Regex("""(\d{1,3}[A-Z])Ix\s*([0-9D]\d)"""),"$1/$2")
            .replace(Regex("""(/)\s*D(\d)"""),"$10$2")
            .replace(Regex("""\s*-\s*"""), "-")
            // "50HH 118-" → "50HH/18-"  (alpha-left-of-slash; OCR reads "/" as space + spurious "1")
            .replace(Regex("""([A-Z0-9]*[A-Z][A-Z0-9]*)\s+1(\d{2,3})(?=-)"""),"$1/$2")
            // "50 HH/15-" → "50HH/15-"  (OCR inserts space between digit prefix and letter suffix before slash)
            .replace(Regex("""(\d+)\s+([A-Z]{1,4})(?=/)"""),"$1$2")
    }

    // ── Stop words — trim noise after the real code ────────────────────────
    private val STOP = Regex(
        """\s+(?:HP|kW|SQMM|SOMM|rpm|mtr|SR|Year|Ex|pm|OK|QA|Amp)\b""",
        RegexOption.IGNORE_CASE
    )

    private fun cleanCode(code: String): String {
        val m = STOP.find(code)
        val trimmed = if (m != null) code.substring(0, m.range.first) else code
        return trimmed.trim()
            //.trimEnd('-', ' ', ':')
            .replace(Regex("""([\d])(20\d{2})$"""), "$1")   // "1.02026" → "1.0"
            .replace(Regex(""":\d{4}$"""), "")   // ADD THIS — strips ":2025", ":2026" etc.
            .trimEnd('-', ' ', ':')
    }

    // ── Patterns ──────────────────────────────────────────────────────────

    // P3: keyword line — "Type: CORA 45/12-TRDX-S-2.0"
    private val P3 = Regex(
        """(?:ype|Type|Model|Series|Brand)\s*[-;:\s]\s*([A-Z]{2,6}\s*\d{1,4}/\d{1,4}[-A-Z0-9.:]{0,30})""",
        RegexOption.IGNORE_CASE
    )
    // P1: full standalone with brand prefix — "CORA 49/10-RLX-S-1.0:2026"
    private val P1 = Regex(
        """[A-Z]{2,6}\s*\d{1,4}/\d{1,4}(?:-[A-Z0-9]+)+(?:[.\-:]\d+)*(?::\d{4})?"""
    )
    // P2: short standalone with brand prefix — "COR 45/12-TRDX"
    private val P2 = Regex(
        """[A-Z]{2,6}\s*\d{1,4}/\d{1,4}-[A-Z]{2,8}"""
    )
    // P4: standalone code WITHOUT brand prefix — "45/12-TRDX-S-2.0"
    //     (OCR split brand onto a different line; brand is recovered from BRAND_RE)
    private val P4 = Regex(
        """(?<![/\w])(\d{1,4}/\d{1,4}(?:-[A-Z0-9]+)+(?:[.\-:]\d+)*)"""
    )
    // Brand finder: extract brand name near a Type/Model keyword
    private val BRAND_RE = Regex(
        """(?:Type|Model)\s*[:\-\s]\s*(CORA|COR|KSB)\b""",
        RegexOption.IGNORE_CASE
    )
    // P5: fallback for alphanumeric-left-of-slash codes — "CORA 20W/08/EDX", "KSB 32A/16"
    // Fires only when left side of "/" contains at least one letter mixed with digits.
    // Pure digit/digit codes are already covered by P1–P4, so this won't double-match them.
    private val P5 = Regex(
        """(?:ype|Type|Model|Series|Brand)\s*[-;:\s]\s*([A-Z]{2,6}\s*[A-Z0-9]*[A-Z][A-Z0-9]*/[A-Z0-9]{1,6}(?:/[A-Z0-9]{1,6})?(?:[-A-Z0-9.:]{0,30})?)""",
        RegexOption.IGNORE_CASE
    )
    // P6: fused-brand keyword — "ypeORA" style where brand is glued onto "ype"
    private val P6 = Regex(
        """(?:Type\s+)([A-Z]{2,6}\s+[A-Z0-9]+/[A-Z0-9]{1,6}(?:[-A-Z0-9.]{0,30})?)""",
        RegexOption.IGNORE_CASE
    )

    // P7: PATCH — keyword line where right-of-slash is alphanumeric (e.g. "47/1C -RLX -AL")
    //     Also tolerates spaces around dashes (OCR artifact: "1C -RLX -AL")
    private val P7 = Regex(
        """(?:"?type"?|"?model"?|"?series"?|"?brand"?|ype|Type|Model|Series|Brand)\s*["':\-;\s]+\s*([A-Z]{2,6}\s*\d{1,4}/[A-Z0-9]{1,5}(?:\s*-\s*[A-Z0-9]+)+(?:[.\-:]\d+)*)""",
        RegexOption.IGNORE_CASE
    )
    private val P8 = Regex(
        //"""(?<![/\w])([A-Z0-9]*[A-Z][A-Z0-9]*/[A-Z0-9]{1,6}(?:-[A-Z0-9]+){1,6}(?:[.\-:]\d+)*)"""
        """(?<![/\w])([A-Z0-9]*[A-Z][A-Z0-9]*/[A-Z0-9]{1,6}(?:/[A-Z0-9]{1,6})?(?:-[A-Z0-9]+){0,6}(?:[.\-:]\d+)*)"""
    )
    fun extract(rawText: String): String {
        val t0 = System.currentTimeMillis()

        log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        log("▶ START  length=${rawText.length}  lines=${rawText.lines().size}")
        log("  raw: ${rawText.take(100).replace("\n", "↵")}")

        if (rawText.isBlank()) {
            logWarn("  blank input — returning empty")
            return ""
        }

        // Step 1 – OCR fix
        val t1 = System.currentTimeMillis()
        val cleaned = applyOcrFixes(rawText)
        log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        log("① OCR FIX [${System.currentTimeMillis() - t1}ms]")
        log("  before : ${rawText.take(100).replace("\n", "↵")}")
        log("  after  : ${cleaned.take(100).replace("\n", "↵")}")

        // Step 2 – Match
        val t2 = System.currentTimeMillis()

        val p3Hits = P3.findAll(cleaned).map { Match(cleanCode(it.groupValues[1].trim()), "P3:Keyword",   90) }.toList()
        val p1Hits = P1.findAll(cleaned).map { Match(cleanCode(it.value.trim()),          "P1:FullModel", 85) }.toList()
        val p2Hits = P2.findAll(cleaned).map { Match(cleanCode(it.value.trim()),          "P2:Short",     80) }.toList()

        // P4: standalone digit/digit-CODE — prepend brand recovered from Type: line
        val brand = BRAND_RE.find(cleaned)?.groupValues?.get(1)?.uppercase()?.let { "$it " } ?: ""
        val p4Hits = P4.findAll(cleaned).map {
            val code = cleanCode(it.groupValues[1].trim())
            Match(if (brand.isNotEmpty()) "$brand$code" else code, "P4:Standalone", 75)
        }.toList()
        // ── NEW: P5 fallback for alphanumeric-before-slash codes ───────────────
        val p5Hits = P5.findAll(cleaned).map { Match(cleanCode(it.groupValues[1].trim()), "P5:AlphaSlash", 70) }.toList()
        val p6Hits = P6.findAll(cleaned).map {
            Match(cleanCode(it.groupValues[1].trim()), "P6:FusedBrand", 65)
        }.toList()
        val p7Hits = P7.findAll(cleaned).map {
            Match(cleanCode(it.groupValues[1].trim()), "P7:AlphaRightSlash", 60)
        }.toList()
        /*// ── NEW P8: standalone alpha-left-of-slash — "50HH/21-TRDX-S-1.5", "18WG/08-EDX-2P-3.0" ──
        val p8Hits = P8.findAll(cleaned).map {
            val code = cleanCode(it.groupValues[1].trim())
            Match(if (brand.isNotEmpty()) "$brand$code" else code, "P8:AlphaStandalone", 55)
        }.toList()*/

        // P8 only runs if P1–P7 found nothing — prevents false positives (e.g. "003V/M", "440V/50H")
        val p1Top = p3Hits + p1Hits + p2Hits + p4Hits + p5Hits + p6Hits + p7Hits
        val p8Hits = if (p1Top.isEmpty()) {
            P8.findAll(cleaned).map {
                val code = cleanCode(it.groupValues[1].trim())
                Match(if (brand.isNotEmpty()) "$brand$code" else code, "P8:AlphaStandalone", 55)
            }.toList()
        } else {
            emptyList()
        }

        val all = p1Top + p8Hits

        log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        log("② MATCH [${System.currentTimeMillis() - t2}ms]  hits=${all.size}")
        log("  P3 Keyword   (90): ${p3Hits.size} → ${p3Hits.map { "\"${it.code}\"" }}")
        log("  P1 FullModel (85): ${p1Hits.size} → ${p1Hits.map { "\"${it.code}\"" }}")
        log("  P2 Short     (80): ${p2Hits.size} → ${p2Hits.map { "\"${it.code}\"" }}")
        log("  P4 Standalone(75): ${p4Hits.size} → ${p4Hits.map { "\"${it.code}\"" }}  brand='$brand'")
        log("  P5 AlphaSlash(70): ${p5Hits.size} → ${p5Hits.map { "\"${it.code}\"" }}")
        log("  P6 FusedBrand(65): ${p6Hits.size} → ${p6Hits.map { "\"${it.code}\"" }}")
        log("  P7 AlphaRightSlash(60): ${p7Hits.size} → ${p7Hits.map { "\"${it.code}\"" }}")
        log("  P8 AlphaStandalone(55): ${p8Hits.size} → ${p8Hits.map { "\"${it.code}\"" }}")


        // Step 3 – Validate
        val t3 = System.currentTimeMillis()
        val valid   = all.filter { isValid(it.code) }
        val dropped = all.filterNot { isValid(it.code) }
        log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        log("③ VALIDATE [${System.currentTimeMillis() - t3}ms]  passed=${valid.size}  dropped=${dropped.size}")
        if (dropped.isNotEmpty()) log("  dropped: ${dropped.map { "\"${it.code}\"" }}")

        // Step 4 – Dedup (highest confidence wins; remove substrings of longer codes)
        val t4 = System.currentTimeMillis()
        val seen = linkedMapOf<String, Match>()
        for (m in valid.sortedByDescending { it.confidence }) {
            val key = m.code.uppercase().replace(Regex("\\s+"), " ")
            if (key !in seen) seen[key] = m
        }
        val final = seen.values.filter { c ->
            seen.values.none { o ->
                o.code != c.code && o.code.contains(c.code) && o.code.length > c.code.length
            }
        }.sortedByDescending { it.confidence }
        log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        log("④ DEDUP [${System.currentTimeMillis() - t4}ms]  ${valid.size} → ${final.size}")

        // Step 5 – Output
        val result = final.joinToString("\n") { it.code }
        val ms = System.currentTimeMillis() - t0
        log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        log("⑤ RESULT [${ms}ms]  codes=${final.size}")
        if (final.isEmpty()) logWarn("  ⚠ no codes found")
        else final.forEachIndexed { i, m ->
            log("  [${i + 1}] \"${m.code}\"  ${m.source}  ${m.confidence}%")
        }
        log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        return result
    }

    private fun isValid(code: String): Boolean {
        val s = code.trim()
        return s.length in 4..60
                && s.any(Char::isLetter)
                && s.any(Char::isDigit)
                && s.count { it == ' ' } < 5
    }

    private fun log(msg: String)     = Log.d(TAG, msg)
    private fun logWarn(msg: String) = Log.w(TAG, msg)
}