package com.example.data.api

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

    fun parse(xmlContent: String): List<YouTubeFeedEntry> {
        val entries = mutableListOf<YouTubeFeedEntry>()
        if (xmlContent.isBlank()) return entries

        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xmlContent))

            var eventType = parser.eventType
            var inEntry = false
            var currentTag = ""
            var currentPrefix = ""

            var videoId = ""
            var title = ""
            var link = ""
            var published = ""
            var description = ""
            var thumbnailUrl = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name
                        currentPrefix = parser.prefix ?: ""

                        if (currentTag.equals("entry", ignoreCase = true)) {
                            inEntry = true
                            videoId = ""
                            title = ""
                            link = ""
                            published = ""
                            description = ""
                            thumbnailUrl = ""
                        } else if (inEntry) {
                            if (currentTag.equals("link", ignoreCase = true)) {
                                val rel = parser.getAttributeValue(null, "rel")
                                val href = parser.getAttributeValue(null, "href")
                                if (rel == "alternate" && !href.isNullOrBlank()) {
                                    link = href
                                }
                            } else if (currentTag.equals("thumbnail", ignoreCase = true)) {
                                val url = parser.getAttributeValue(null, "url")
                                if (!url.isNullOrBlank() && thumbnailUrl.isBlank()) {
                                    thumbnailUrl = url
                                }
                            }
                        }
                    }

                    XmlPullParser.TEXT -> {
                        if (inEntry) {
                            val text = parser.text?.trim() ?: ""
                            if (text.isNotEmpty()) {
                                when (currentTag.lowercase()) {
                                    "videoid" -> if (videoId.isBlank()) videoId = text
                                    "title" -> if (title.isBlank()) title = text
                                    "published" -> if (published.isBlank()) published = text
                                    "description" -> {
                                        description = if (description.isEmpty()) text else "$description $text"
                                    }
                                }
                            }
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        val endTag = parser.name
                        if (endTag.equals("entry", ignoreCase = true)) {
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
                        currentTag = ""
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return entries
    }
}
