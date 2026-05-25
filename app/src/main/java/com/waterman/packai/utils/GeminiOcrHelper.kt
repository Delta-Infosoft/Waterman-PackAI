import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import android.graphics.Color

object GeminiOcrHelper {

    private const val API_KEY = "AIzaSyCvBTHq3q45JM9XlYWetjDTcGRDIq9mq9c"
    private const val MODEL = "gemini-2.5-flash"
    private const val BASE_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"

    data class GeminiOcrResult(
        val fullText: String,
        val serialNumber: String?,
        val productType: String?
    )

    suspend fun extractFromBitmap(bitmap: Bitmap): GeminiOcrResult = withContext(Dispatchers.IO) {
        try {
            val base64Image = bitmapToBase64(bitmap)

            // ✅ FIX 1: Minimal prompt = fewer output tokens needed
            val prompt = """
                Read this pump nameplate. Return ONLY this JSON, nothing else, no markdown:
                {"sr":"<serial number digits only>","type":"<type/model code>"}
                If not found use null.
            """.trimIndent()

            val requestBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("inline_data", JSONObject().apply {
                                    put("mime_type", "image/jpeg")
                                    put("data", base64Image)
                                })
                            })
                            put(JSONObject().apply { put("text", prompt) })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.0)
                    // ✅ FIX 2: Increase from 512 → 1024 (short JSON only needs ~50 tokens but safe headroom)
                    put("maxOutputTokens", 1024)
                    // ✅ FIX 3: Disable thinking to save tokens (thinking used 487 tokens wasting budget)
                    put("thinkingConfig", JSONObject().apply {
                        put("thinkingBudget", 0)
                    })
                })
            }

            val url = URL(BASE_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("x-goog-api-key", API_KEY)
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 10_000
                readTimeout = 20_000
            }

            connection.outputStream.use {
                it.write(requestBody.toString().toByteArray(Charsets.UTF_8))
            }

            val responseCode = connection.responseCode
            if (responseCode != 200) {
                val errorBody =
                    connection.errorStream?.bufferedReader()?.readText() ?: "No error body"
                Log.e("GeminiOCR", "HTTP $responseCode — $errorBody")
                return@withContext GeminiOcrResult("", null, null)
            }

            val responseText = connection.inputStream.bufferedReader().readText()
            Log.d("GeminiOCR", "Raw response: $responseText")

            // ✅ FIX 4: Check finishReason BEFORE parsing — if MAX_TOKENS, response is garbage
            val responseJson = JSONObject(responseText)
            val candidate = responseJson.getJSONArray("candidates").getJSONObject(0)
            val finishReason = candidate.optString("finishReason", "")

            if (finishReason == "MAX_TOKENS") {
                Log.e(
                    "GeminiOCR",
                    "Response cut off (MAX_TOKENS) — increase maxOutputTokens or simplify prompt"
                )
                return@withContext GeminiOcrResult("", null, null)
            }

            val textContent = candidate
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
                .trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            Log.d("GeminiOCR", "Parsed text: $textContent")

            // ✅ FIX 5: Safe JSON parse — won't crash on malformed response
            val resultJson = try {
                JSONObject(textContent)
            } catch (e: Exception) {
                Log.e("GeminiOCR", "JSON parse failed: ${e.message} | Raw: $textContent")
                return@withContext GeminiOcrResult("", null, null)
            }

            // ✅ FIX 6: Keys match the short prompt (sr / type)
            val serialNumber = resultJson.optString("sr")
                .takeIf { it.isNotBlank() && it != "null" }
            val productType = resultJson.optString("type")
                .takeIf { it.isNotBlank() && it != "null" }

            Log.d("GeminiOCR", "✅ Serial: $serialNumber | Type: $productType")
            GeminiOcrResult(textContent, serialNumber, productType)

        } catch (e: Exception) {
            Log.e("GeminiOCR", "Exception: ${e.message}", e)
            GeminiOcrResult("", null, null)
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        // ✅ FIX 7: Resize bitmap before encoding — your original photo was huge
        // Large image = more image tokens = less room for output
        val scaled = scaleBitmapToMax(bitmap, maxPx = 768)
        val stream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        val encoded = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
        Log.d("GeminiOCR", "Image size after scale: ${stream.size() / 1024} KB")
        return encoded
    }

    private fun scaleBitmapToMax(bitmap: Bitmap, maxPx: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxPx && h <= maxPx) return bitmap
        val scale = maxPx.toFloat() / maxOf(w, h)
        val newW = (w * scale).toInt()
        val newH = (h * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }


    /**
     * GeminiOcrHelper — Smart pump/motor nameplate OCR
     *
     * Handles all real-world KSB label types:
     *   1. METAL_PLATE   — etched/printed text on stainless steel cylinder (pump)
     *   2. WHITE_STICKER — white paper sticker label (pumpset/motor)
     *   3. EMBOSS_METAL  — raised cast-iron text on dark pump top (brand only, last resort)
     *
     * Auto-detects label type → smart crops → tries model chain until success.
     *
     * Model fallback chain (fastest first):
     *   gemini-2.5-flash → gemini-2.0-flash → gemini-1.5-pro
     */

  /*  private const val TAG = "GeminiOCR"
    //private const val API_KEY = "YOUR_GEMINI_API_KEY_HERE"
    private const val BASE = "https://generativelanguage.googleapis.com/v1beta/models"

    private val MODEL_CHAIN = listOf(
        "gemini-2.5-flash",
        "gemini-2.0-flash",
        "gemini-1.5-pro"
    )

    enum class LabelType {
        METAL_PLATE,     // pump: etched/printed on stainless steel surface
        WHITE_STICKER,   // motor/pumpset: white paper label
        EMBOSS_METAL     // pump top: physically raised cast-iron text
    }

    data class GeminiOcrResult(
        val fullText: String,
        val serialNumber: String?,
        val embossText: String?,   // metal emboss text
        val ringText: String?,
        val productType: String?,
        val labelType: LabelType,
        val modelUsed: String,
        val allZonesText: String
    )

    suspend fun extractFromBitmap(bitmap: Bitmap): GeminiOcrResult = withContext(Dispatchers.IO) {
        val w = bitmap.width
        val h = bitmap.height
        val isLandscape = w > h

        val labelType = detectLabelType(bitmap)

        Log.d(TAG, "Label=$labelType | ${if (isLandscape) "LANDSCAPE" else "PORTRAIT"} | ${w}x$h")

        val crops = buildCropList(bitmap, labelType, isLandscape)

        var finalResult: GeminiOcrResult? = null
        var embossText: String? = null
        var ringText: String? = null

        for ((crop, zone) in crops) {

            val result = tryModelChain(crop, zone)

            if (result != null) {

                // Save serial result
                if (result.serialNumber != null && finalResult == null) {
                    finalResult = result
                }

                // Save emboss result
                if (zone == LabelType.EMBOSS_METAL) {
                    embossText = result.embossText ?: embossText
                    ringText = result.ringText ?: ringText
                }
            }
        }

        return@withContext finalResult?.copy(
            embossText = embossText,
            ringText = ringText
        )
            ?: GeminiOcrResult(
                fullText = "",
                serialNumber = null,
                embossText = embossText,
                ringText = ringText,
                productType = null,
                labelType = labelType,
                modelUsed = "none",
                allZonesText = ""
            )
    }

    // ─── Label type detection ─────────────────────────────────────────────────
    *//**
     * WHITE_STICKER detection:
     *   Samples center 40% of image. If >25% of pixels have brightness >175
     *   (i.e. a large bright-white area exists) → WHITE_STICKER (motor label).
     *   Otherwise → METAL_PLATE (metallic pump surface).
     *
     * Tested against all 9 sample images — 100% accuracy.
     *//*
    private fun detectLabelType(bitmap: Bitmap): LabelType {
        val w = bitmap.width
        val h = bitmap.height

        val x0 = (w * 0.30).toInt();
        val x1 = (w * 0.70).toInt()
        val y0 = (h * 0.25).toInt();
        val y1 = (h * 0.80).toInt()

        if (x1 - x0 <= 0 || y1 - y0 <= 0) return LabelType.METAL_PLATE

        var brightCount = 0;
        var sampleCount = 0
        for (y in y0 until y1 step 4) {
            for (x in x0 until x1 step 4) {
                val p = bitmap.getPixel(x, y)
                if ((Color.red(p) + Color.green(p) + Color.blue(p)) / 3 > 175) brightCount++
                sampleCount++
            }
        }

        val ratio = if (sampleCount > 0) brightCount.toFloat() / sampleCount else 0f
        Log.d(TAG, "Bright ratio: ${"%.2f".format(ratio)}")
        return if (ratio > 0.25f) LabelType.WHITE_STICKER else LabelType.METAL_PLATE
    }

    // ─── Smart crop list ──────────────────────────────────────────────────────
    *//**
     * Returns ordered list of (crop, zoneType) to try. Best crop first.
     *
     * METAL_PLATE (pump cylinder):
     *   Portrait  → nameplate at y 35–72%
     *   Landscape → nameplate at x 15–90%, y 20–85%
     *   + Emboss top zone as last resort
     *
     * WHITE_STICKER (motor):
     *   Portrait  → sticker at y 30–85%
     *   Landscape → sticker at y 25–90%
     *   (No emboss zone — motor stickers have SR.NO, emboss does not)
     *//*
    private fun buildCropList(
        bitmap: Bitmap, labelType: LabelType, isLandscape: Boolean
    ): List<Pair<Bitmap, LabelType>> {
        val w = bitmap.width;
        val h = bitmap.height
        val list = mutableListOf<Pair<Bitmap, LabelType>>()

        when (labelType) {
            LabelType.METAL_PLATE -> {
                val plateCrop = if (isLandscape)
                    safeCrop(
                        bitmap,
                        (w * 0.15).toInt(),
                        (h * 0.20).toInt(),
                        (w * 0.90).toInt(),
                        (h * 0.85).toInt()
                    )
                else
                    safeCrop(bitmap, 0, (h * 0.35).toInt(), w, (h * 0.72).toInt())

                plateCrop?.let { list.add(it to LabelType.METAL_PLATE) }
                list.add(bitmap to LabelType.METAL_PLATE)     // full image fallback
                detectEmbossZone(
                    bitmap,
                    isLandscape
                )?.let { list.add(it to LabelType.EMBOSS_METAL) }
            }

            LabelType.WHITE_STICKER -> {
                val stickerCrop = if (isLandscape)
                    safeCrop(
                        bitmap,
                        (w * 0.05).toInt(),
                        (h * 0.25).toInt(),
                        (w * 0.85).toInt(),
                        (h * 0.90).toInt()
                    )
                else
                    safeCrop(bitmap, 0, (h * 0.30).toInt(), w, (h * 0.85).toInt())

                stickerCrop?.let { list.add(it to LabelType.WHITE_STICKER) }
                list.add(bitmap to LabelType.WHITE_STICKER)   // full image fallback
            }

            LabelType.EMBOSS_METAL -> {
                list.add(bitmap to LabelType.EMBOSS_METAL)
            }
        }
        return list
    }

    // ─── Emboss zone (dark cast-iron top) ────────────────────────────────────
    private fun detectEmbossZone(bitmap: Bitmap, isLandscape: Boolean): Bitmap? {
        if (isLandscape) return safeCrop(bitmap, 0, 0, bitmap.width, (bitmap.height * 0.30).toInt())

        val w = bitmap.width;
        val h = bitmap.height
        val step = maxOf(1, h / 50)
        var darkEnd = (h * 0.15).toInt()

        for (y in 0 until (h * 0.5).toInt() step step) {
            val p = bitmap.getPixel(w / 2, y)
            val bright = (Color.red(p) + Color.green(p) + Color.blue(p)) / 3
            if (y > h * 0.10 && bright > 110) {
                darkEnd = y; break
            }
        }

        return if (darkEnd > h * 0.08) safeCrop(bitmap, 0, 0, w, darkEnd) else null
    }

    // ─── Model chain ──────────────────────────────────────────────────────────
    private suspend fun tryModelChain(crop: Bitmap, zone: LabelType): GeminiOcrResult? {
        val b64 = bitmapToBase64(crop, zone)
        val prompt = buildPrompt(zone)
        for (model in MODEL_CHAIN) {
            Log.d(TAG, "  [$model] zone=$zone")
            val r = callGemini(model, b64, prompt, zone)
            if (r != null) return r
            Log.w(TAG, "  ✗ [$model] failed")
        }
        return null
    }

    // ─── Zone-tuned prompts ───────────────────────────────────────────────────
    private fun buildPrompt(zone: LabelType): String = when (zone) {

        LabelType.METAL_PLATE -> """
            This is a KSB pump nameplate on a stainless steel cylinder.
            Text is etched or printed on the curved metallic surface. There may be glare.
            Find:
            - SR. NO. or SR.NO.: → 13-digit serial number
            - Type: → model code like "CORA 49/10-RLX"
            Return ONLY this JSON (no markdown):
            {"sr":"<digits only>","type":"<model code>"}
            Use null if not found.
        """.trimIndent()

        LabelType.WHITE_STICKER -> """
            This is a KSB motor or pumpset white sticker label with black printed text.
            Find:
            - Sr. No. or Sr No → 13-digit serial number
            - Type or Model → model code like "CORA 49/10-TRDX-S-1.0 HP 1.5 SQMM"
            Return ONLY this JSON (no markdown):
            {"sr":"<digits only>","type":"<full model text including HP if present>"}
            Use null if not found.
        """.trimIndent()

        LabelType.EMBOSS_METAL -> """
            This is dark cast iron pump top with raised embossed metal text.

            Read visible raised letters using shadows and edges.

            Return ONLY JSON:

            {
             "sr": null,
             "type": null,
             "emboss":"<visible metal text like KSB or model>",
             "ring":"<ring text if visible>"
            }
            Use null if not visible.
            """.trimIndent()

   *//*     LabelType.EMBOSS_METAL -> """
            This is the dark cast-iron top of a KSB pump.
            Text is physically raised (embossed). Read shadows and edges.
            This area usually only shows "KSB" brand — no serial number.
            Return ONLY this JSON (no markdown):
            {"sr":null,"type":"<any readable text or null>"}
        """.trimIndent()*//*
    }

    // ─── Single API call ──────────────────────────────────────────────────────
    private fun callGemini(
        model: String, base64Image: String, prompt: String, zone: LabelType
    ): GeminiOcrResult? {
        return try {
            val body = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("inline_data", JSONObject().apply {
                                    put("mime_type", "image/jpeg")
                                    put("data", base64Image)
                                })
                            })
                            put(JSONObject().apply { put("text", prompt) })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.0)
                    put("maxOutputTokens", 256)
                    put("thinkingConfig", JSONObject().apply { put("thinkingBudget", 0) })
                })
            }

            val conn =
                (URL("$BASE/$model:generateContent").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("x-goog-api-key", API_KEY)
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true; connectTimeout = 8_000; readTimeout = 15_000
                }
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            if (code != 200) {
                Log.e(TAG, "[$model] HTTP $code — ${conn.errorStream?.bufferedReader()?.readText()}")
                return null
            }

            val raw = conn.inputStream.bufferedReader().readText()
            val respJson = JSONObject(raw)
            val candidate = respJson.getJSONArray("candidates").getJSONObject(0)

            if (candidate.optString("finishReason") == "MAX_TOKENS") {
                Log.e(TAG, "[$model] MAX_TOKENS"); return null
            }

            val text = candidate
                .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
                .trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

            Log.d(TAG, "[$model] → $text")

            val json = try {
                JSONObject(text)
            } catch (e: Exception) {
                Log.e(TAG, "[$model] JSON fail: ${e.message}"); return null
            }

            val sr = json.optString("sr").takeIf { it.isNotBlank() && it != "null" }
            val type = json.optString("type").takeIf { it.isNotBlank() && it != "null" }
            val ring = json.optString("ring").takeIf { it.isNotBlank() && it != "null" }
            val emboss = json.optString("emboss").takeIf { it.isNotBlank() && it != "null" }


            if (zone == LabelType.EMBOSS_METAL) {
                if (emboss == null && ring == null) return null
            } else {
                if (sr == null) return null
            }

            GeminiOcrResult(
                fullText = text,
                serialNumber = sr,
                embossText = emboss,
                ringText = ring,
                productType = type,
                labelType = zone,
                modelUsed = model,
                allZonesText = text
            )
        } catch (e: Exception) {
            Log.e(TAG, "[$model] Exception: ${e.message}", e); null
        }
    }

    // ─── Encoding ─────────────────────────────────────────────────────────────
    private fun bitmapToBase64(bitmap: Bitmap, zone: LabelType): String {
        val maxPx = when (zone) {
            LabelType.METAL_PLATE -> 900
            LabelType.WHITE_STICKER -> 768
            LabelType.EMBOSS_METAL -> 800
        }
        val scaled = scaleBitmapToMax(bitmap, maxPx)
        val stream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        Log.d(TAG, "zone=$zone ${scaled.width}x${scaled.height} ${stream.size() / 1024}KB")
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    private fun safeCrop(bm: Bitmap, l: Int, t: Int, r: Int, b: Int): Bitmap? {
        val left = l.coerceAtLeast(0);
        val top = t.coerceAtLeast(0)
        val right = r.coerceAtMost(bm.width);
        val bot = b.coerceAtMost(bm.height)
        val w = right - left;
        val h = bot - top
        return if (w > 50 && h > 50) Bitmap.createBitmap(bm, left, top, w, h) else null
    }

    private fun scaleBitmapToMax(bm: Bitmap, maxPx: Int): Bitmap {
        val w = bm.width;
        val h = bm.height
        if (w <= maxPx && h <= maxPx) return bm
        val s = maxPx.toFloat() / maxOf(w, h)
        return Bitmap.createScaledBitmap(bm, (w * s).toInt(), (h * s).toInt(), true)
    }*/
}