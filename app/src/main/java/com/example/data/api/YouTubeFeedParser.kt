package com.example.data.api

import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

data class YouTubeFeedEntry(
    val videoId: String,
    val title: String,
    val link: String,
    val published: String,
    val description: String,
    val thumbnailUrl: String
)

object YouTubeFeedParser {

    /**
     * Parses YouTube's official Atom RSS XML feed:
     * https://www.youtube.com/feeds/videos.xml?channel_id=...
     * Uses proper tag-stack tracking and StringBuilder accumulation so XML entities (&amp;, etc.)
     * never truncate video titles or descriptions.
     */
    fun parse(xmlContent: String): List<YouTubeFeedEntry> {
        val entries = mutableListOf<YouTubeFeedEntry>()
        if (xmlContent.isBlank()) return entries

        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xmlContent))

            val tagStack = ArrayDeque<String>()
            val textBuffer = StringBuilder()

            var inEntry = false
            var videoId = ""
            var title = ""
            var link = ""
            var published = ""
            var description = ""
            var thumbnailUrl = ""

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val tagName = (parser.name ?: "").lowercase()
                        tagStack.addLast(tagName)
                        textBuffer.setLength(0)

                        if (tagName == "entry") {
                            inEntry = true
                            videoId = ""
                            title = ""
                            link = ""
                            published = ""
                            description = ""
                            thumbnailUrl = ""
                        } else if (inEntry) {
                            if (tagName == "link") {
                                val rel = parser.getAttributeValue(null, "rel")
                                val href = parser.getAttributeValue(null, "href")
                                if ((rel == null || rel == "alternate") && !href.isNullOrBlank()) {
                                    link = href
                                }
                            } else if (tagName == "thumbnail") {
                                val url = parser.getAttributeValue(null, "url")
                                if (!url.isNullOrBlank() && thumbnailUrl.isBlank()) {
                                    thumbnailUrl = url
                                }
                            }
                        }
                    }

                    XmlPullParser.TEXT, XmlPullParser.CDSECT -> {
                        if (inEntry && parser.text != null) {
                            textBuffer.append(parser.text)
                        }
                    }

                    XmlPullParser.ENTITY_REF -> {
                        if (inEntry) {
                            val entityText = parser.text
                            if (!entityText.isNullOrEmpty()) {
                                textBuffer.append(entityText)
                            } else {
                                when (parser.name?.lowercase()) {
                                    "amp" -> textBuffer.append("&")
                                    "lt" -> textBuffer.append("<")
                                    "gt" -> textBuffer.append(">")
                                    "quot" -> textBuffer.append("\"")
                                    "apos" -> textBuffer.append("'")
                                }
                            }
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        val endTag = (parser.name ?: "").lowercase()
                        if (inEntry) {
                            val parentTag = if (tagStack.size >= 2) tagStack[tagStack.size - 2] else ""
                            val text = textBuffer.toString().trim()

                            when (endTag) {
                                "videoid" -> if (videoId.isBlank() && text.isNotEmpty()) videoId = text
                                "title" -> {
                                    if (parentTag == "entry" && text.isNotEmpty()) {
                                        title = text
                                    } else if (title.isBlank() && text.isNotEmpty()) {
                                        title = text
                                    }
                                }
                                "published" -> if (parentTag == "entry" && published.isBlank() && text.isNotEmpty()) {
                                    published = text
                                }
                                "description" -> if (text.isNotEmpty()) {
                                    description = text
                                }
                                "entry" -> {
                                    if (videoId.isNotBlank()) {
                                        if (link.isBlank()) {
                                            link = "https://www.youtube.com/watch?v=$videoId"
                                        }
                                        if (thumbnailUrl.isBlank()) {
                                            thumbnailUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                                        }
                                        entries.add(
                                            YouTubeFeedEntry(
                                                videoId = videoId,
                                                title = title,
                                                link = link,
                                                published = published,
                                                description = description,
                                                thumbnailUrl = thumbnailUrl
                                            )
                                        )
                                    }
                                    inEntry = false
                                }
                            }
                        }
                        if (tagStack.isNotEmpty()) {
                            tagStack.removeLast()
                        }
                        textBuffer.setLength(0)
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return entries
    }

    /**
     * Fallback / Extended parser for YouTube channel /videos HTML page (ytInitialData JSON).
     * Extracts up to 30 videos even if the RSS endpoint fails or only returns 15 items.
     * Supports both `lockupViewModel` (modern YouTube layout) and `videoRenderer`.
     */
    fun parseChannelVideosHtml(html: String): List<YouTubeFeedEntry> {
        val entries = mutableListOf<YouTubeFeedEntry>()
        if (html.isBlank()) return entries

        try {
            val jsonStr = extractYtInitialDataJson(html) ?: return entries
            val root = JSONObject(jsonStr)
            val tabs = root.optJSONObject("contents")
                ?.optJSONObject("twoColumnBrowseResultsRenderer")
                ?.optJSONArray("tabs") ?: return entries

            for (i in 0 until tabs.length()) {
                val tabRenderer = tabs.optJSONObject(i)?.optJSONObject("tabRenderer") ?: continue
                val richContents = tabRenderer.optJSONObject("content")
                    ?.optJSONObject("richGridRenderer")
                    ?.optJSONArray("contents") ?: continue

                parseRichGridContents(richContents, entries)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return entries.distinctBy { it.videoId }
    }

    private fun parseRichGridContents(contents: JSONArray, out: MutableList<YouTubeFeedEntry>) {
        for (i in 0 until contents.length()) {
            val content = contents.optJSONObject(i)
                ?.optJSONObject("richItemRenderer")
                ?.optJSONObject("content") ?: continue

            // Modern YouTube layout: lockupViewModel
            content.optJSONObject("lockupViewModel")?.let { vm ->
                val videoId = vm.optString("contentId", "").trim()
                if (videoId.isNotBlank()) {
                    val metadata = vm.optJSONObject("metadata")
                        ?.optJSONObject("lockupMetadataViewModel")
                    val title = metadata?.optJSONObject("title")
                        ?.optString("content", "")
                        ?.trim()
                        .orEmpty()

                    var publishedText = ""
                    val rows = metadata?.optJSONObject("metadata")
                        ?.optJSONObject("contentMetadataViewModel")
                        ?.optJSONArray("metadataRows")
                    if (rows != null) {
                        for (r in 0 until rows.length()) {
                            val parts = rows.optJSONObject(r)?.optJSONArray("metadataParts") ?: continue
                            for (p in 0 until parts.length()) {
                                val txt = parts.optJSONObject(p)?.optJSONObject("text")
                                    ?.optString("content", "")?.trim().orEmpty()
                                if (txt.contains("ago", ignoreCase = true) ||
                                    txt.contains("پیش") ||
                                    txt.contains("ساعت") ||
                                    txt.contains("روز") ||
                                    txt.contains("هفته") ||
                                    txt.contains("ماه")
                                ) {
                                    publishedText = txt
                                }
                            }
                        }
                    }

                    var thumbUrl = ""
                    val sources = vm.optJSONObject("contentImage")
                        ?.optJSONObject("thumbnailViewModel")
                        ?.optJSONObject("image")
                        ?.optJSONArray("sources")
                    if (sources != null && sources.length() > 0) {
                        thumbUrl = sources.optJSONObject(sources.length() - 1)?.optString("url", "").orEmpty()
                    }
                    if (thumbUrl.isBlank()) {
                        thumbUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                    }

                    if (title.isNotBlank()) {
                        out.add(
                            YouTubeFeedEntry(
                                videoId = videoId,
                                title = title,
                                link = "https://www.youtube.com/watch?v=$videoId",
                                published = publishedText,
                                description = "",
                                thumbnailUrl = thumbUrl
                            )
                        )
                    }
                }
            }

            // Classic YouTube layout: videoRenderer
            content.optJSONObject("videoRenderer")?.let { vr ->
                val videoId = vr.optString("videoId", "").trim()
                if (videoId.isNotBlank()) {
                    val title = extractRunsText(vr.optJSONObject("title"))
                    val desc = extractRunsText(vr.optJSONObject("descriptionSnippet"))
                    val published = vr.optJSONObject("publishedTimeText")?.optString("simpleText", "").orEmpty()
                    var thumbUrl = ""
                    val thumbs = vr.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                    if (thumbs != null && thumbs.length() > 0) {
                        thumbUrl = thumbs.optJSONObject(thumbs.length() - 1)?.optString("url", "").orEmpty()
                    }
                    if (thumbUrl.isBlank()) {
                        thumbUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                    }

                    if (title.isNotBlank()) {
                        out.add(
                            YouTubeFeedEntry(
                                videoId = videoId,
                                title = title,
                                link = "https://www.youtube.com/watch?v=$videoId",
                                published = published,
                                description = desc,
                                thumbnailUrl = thumbUrl
                            )
                        )
                    }
                }
            }
        }
    }

    private fun extractRunsText(obj: JSONObject?): String {
        if (obj == null) return ""
        val simple = obj.optString("simpleText", "")
        if (simple.isNotBlank()) return simple.trim()
        val runs = obj.optJSONArray("runs") ?: return ""
        val sb = StringBuilder()
        for (i in 0 until runs.length()) {
            sb.append(runs.optJSONObject(i)?.optString("text", "").orEmpty())
        }
        return sb.toString().trim()
    }

    private fun extractYtInitialDataJson(html: String): String? {
        val markers = listOf(
            "var ytInitialData = ",
            "window[\"ytInitialData\"] = ",
            "ytInitialData = "
        )
        var startIdx = -1
        for (marker in markers) {
            val idx = html.indexOf(marker)
            if (idx >= 0) {
                startIdx = idx + marker.length
                break
            }
        }
        if (startIdx < 0) return null

        val braceStart = html.indexOf('{', startIdx)
        if (braceStart < 0) return null

        var depth = 0
        var inString = false
        var escaped = false
        for (i in braceStart until html.length) {
            val c = html[i]
            if (escaped) {
                escaped = false
                continue
            }
            if (c == '\\' && inString) {
                escaped = true
                continue
            }
            if (c == '"') {
                inString = !inString
                continue
            }
            if (!inString) {
                if (c == '{') depth++
                else if (c == '}') {
                    depth--
                    if (depth == 0) {
                        return html.substring(braceStart, i + 1)
                    }
                }
            }
        }
        return null
    }
}
