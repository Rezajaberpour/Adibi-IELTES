package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.api.YouTubeFeedEntry
import com.example.data.api.YouTubeFeedParser
import com.example.data.repository.YouTubeSyncRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class YouTubeSyncTest {

    @Test
    fun `xml feed parser handles entities and extracts complete fields`() {
        val sampleXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <feed xmlns:yt="http://www.youtube.com/xml/schemas/2015" xmlns:media="http://search.yahoo.com/mrss/" xmlns="http://www.w3.org/2005/Atom">
              <title>Zahra Adibi</title>
              <entry>
                <id>yt:video:G4jmQ4dXyhA</id>
                <yt:videoId>G4jmQ4dXyhA</yt:videoId>
                <title>Check Your English Vocabulary for IELTS/Lesson Forty_ Six/ Zahra Adibi</title>
                <link rel="alternate" href="https://www.youtube.com/watch?v=G4jmQ4dXyhA"/>
                <published>2026-11-17T06:15:29+00:00</published>
                <media:group>
                  <media:title>Check Your English Vocabulary for IELTS/Lesson Forty_ Six/ Zahra Adibi</media:title>
                  <media:thumbnail url="https://i4.ytimg.com/vi/G4jmQ4dXyhA/hqdefault.jpg" width="480" height="360"/>
                  <media:description>Explore the topic of Education through practical English conversations &amp; vocabulary.</media:description>
                </media:group>
              </entry>
              <entry>
                <id>yt:video:Vba0QCnbPOs</id>
                <yt:videoId>Vba0QCnbPOs</yt:videoId>
                <title>Reciprocal Pronouns: Each Other &amp; One Another | English Grammar/Lesson 10/ Zahra Adibi</title>
                <link rel="alternate" href="https://www.youtube.com/watch?v=Vba0QCnbPOs"/>
                <published>2026-10-14T10:41:23+00:00</published>
                <media:group>
                  <media:thumbnail url="https://i1.ytimg.com/vi/Vba0QCnbPOs/hqdefault.jpg" width="480" height="360"/>
                  <media:description>Learn how to use reciprocal pronouns in English.</media:description>
                </media:group>
              </entry>
            </feed>
        """.trimIndent()

        val entries = YouTubeFeedParser.parse(sampleXml)
        assertEquals(2, entries.size)
        assertEquals("G4jmQ4dXyhA", entries[0].videoId)
        assertEquals("Check Your English Vocabulary for IELTS/Lesson Forty_ Six/ Zahra Adibi", entries[0].title)
        assertEquals("https://i4.ytimg.com/vi/G4jmQ4dXyhA/hqdefault.jpg", entries[0].thumbnailUrl)
        assertTrue(entries[0].description.contains("Education"))

        // Verify XML entity &amp; did NOT truncate the title!
        assertEquals(
            "Reciprocal Pronouns: Each Other & One Another | English Grammar/Lesson 10/ Zahra Adibi",
            entries[1].title
        )
    }

    @Test
    fun `html fallback parser extracts lockupViewModel and videoRenderer entries`() {
        val sampleHtml = """
            <html><head><script>var ytInitialData = {
              "contents": {
                "twoColumnBrowseResultsRenderer": {
                  "tabs": [{
                    "tabRenderer": {
                      "selected": true,
                      "content": {
                        "richGridRenderer": {
                          "contents": [
                            {
                              "richItemRenderer": {
                                "content": {
                                  "lockupViewModel": {
                                    "contentId": "ZwV0PimKSSo",
                                    "contentImage": {
                                      "thumbnailViewModel": {
                                        "image": {
                                          "sources": [{"url": "https://i.ytimg.com/vi/ZwV0PimKSSo/hq720.jpg"}]
                                        }
                                      }
                                    },
                                    "metadata": {
                                      "lockupMetadataViewModel": {
                                        "title": {"content": "Check Your English Vocabulary for IELTS/Lesson Forty_ Five/ Zahra Adibi"},
                                        "metadata": {
                                          "contentMetadataViewModel": {
                                            "metadataRows": [{"metadataParts": [{"text": {"content": "2d ago"}}]}]
                                          }
                                        }
                                      }
                                    }
                                  }
                                }
                              }
                            }
                          ]
                        }
                      }
                    }
                  }]
                }
              }
            };</script></head></html>
        """.trimIndent()

        val entries = YouTubeFeedParser.parseChannelVideosHtml(sampleHtml)
        assertEquals(1, entries.size)
        assertEquals("ZwV0PimKSSo", entries[0].videoId)
        assertEquals("Check Your English Vocabulary for IELTS/Lesson Forty_ Five/ Zahra Adibi", entries[0].title)
        assertEquals("2d ago", entries[0].published)
    }

    @Test
    fun `repository creates dynamic lessons and scenarios for new videos`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        YouTubeSyncRepository.init(context)

        val entries = listOf(
            YouTubeFeedEntry(
                videoId = "G4jmQ4dXyhA",
                title = "Check Your English Vocabulary for IELTS/Lesson Forty_ Six/ Zahra Adibi",
                link = "https://www.youtube.com/watch?v=G4jmQ4dXyhA",
                published = "2026-11-17T06:15:29+00:00",
                description = "Explore the topic of Education through practical English conversations.",
                thumbnailUrl = "https://i4.ytimg.com/vi/G4jmQ4dXyhA/hqdefault.jpg"
            ),
            YouTubeFeedEntry(
                videoId = "ZwV0PimKSSo",
                title = "Check Your English Vocabulary for IELTS/Lesson Forty_ Five/ Zahra Adibi",
                link = "https://www.youtube.com/watch?v=ZwV0PimKSSo",
                published = "2026-11-15T08:36:42+00:00",
                description = "Crime and the Law | Learn English with Adibi",
                thumbnailUrl = "https://i4.ytimg.com/vi/ZwV0PimKSSo/hqdefault.jpg"
            ),
            YouTubeFeedEntry(
                videoId = "FUTURE47ID",
                title = "Check Your English Vocabulary for IELTS/Lesson Forty_Seven/ Zahra Adibi",
                link = "https://www.youtube.com/watch?v=FUTURE47ID",
                published = "2026-11-20T09:00:00+00:00",
                description = "Science and Technology Vocabulary",
                thumbnailUrl = "https://i4.ytimg.com/vi/FUTURE47ID/hqdefault.jpg"
            )
        )

        YouTubeSyncRepository.processFeedEntries(entries, isLive = true)

        val lessons = YouTubeSyncRepository.lessons.value
        val scenarios = YouTubeSyncRepository.scenarios.value

        assertTrue(lessons.any { it.id == "lesson_yt_G4jmQ4dXyhA" && it.titlePersian.contains("۴۶") })
        assertTrue(lessons.any { it.id == "lesson_yt_ZwV0PimKSSo" && it.titlePersian.contains("۴۵") })
        assertTrue(lessons.any { it.id == "lesson_yt_FUTURE47ID" && it.titlePersian.contains("۴۷") })

        assertTrue(scenarios.any { it.id == "scenario_yt_G4jmQ4dXyhA" })
        assertTrue(scenarios.any { it.id == "scenario_yt_ZwV0PimKSSo" })
        assertTrue(scenarios.any { it.id == "scenario_yt_FUTURE47ID" })
        assertFalse(YouTubeSyncRepository.isSyncing.value)
    }
}
