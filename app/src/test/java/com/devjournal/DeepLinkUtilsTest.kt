package com.devjournal

import com.devjournal.presentation.util.DeepLinkUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeepLinkUtilsTest {

    @Test
    fun buildPostWebUrl_formatsCorrectly() {
        val url = DeepLinkUtils.buildPostWebUrl("post123")
        assertEquals("https://devjournal-web.vercel.app/posts/post123", url)
    }

    @Test
    fun buildPostDeepLink_formatsCorrectly() {
        val deepLink = DeepLinkUtils.buildPostDeepLink("post123")
        assertEquals("devjournal://posts/post123", deepLink)
    }

    @Test
    fun extractPostId_httpsAppLink_returnsPostId() {
        val postId = DeepLinkUtils.extractPostId(
            scheme = "https",
            host = "devjournal-web.vercel.app",
            path = "/posts/Ip0M2dtBZH8rzBJh9H1z"
        )
        assertEquals("Ip0M2dtBZH8rzBJh9H1z", postId)
    }

    @Test
    fun extractPostId_legacyHost_returnsPostId() {
        val postId = DeepLinkUtils.extractPostId(
            scheme = "https",
            host = "devjournal.app",
            path = "/posts/legacyPost123"
        )
        assertEquals("legacyPost123", postId)
    }

    @Test
    fun extractPostId_customScheme_returnsPostId() {
        val postId = DeepLinkUtils.extractPostId(
            scheme = "devjournal",
            host = "posts",
            path = "/customPost123"
        )
        assertEquals("customPost123", postId)
    }

    @Test
    fun extractPostId_queryParameter_returnsPostId() {
        val postId = DeepLinkUtils.extractPostId(
            scheme = "https",
            host = "devjournal-web.vercel.app",
            path = "/posts",
            queryPostId = "queryPost123"
        )
        assertEquals("queryPost123", postId)
    }

    @Test
    fun extractPostId_unrelatedHost_returnsNull() {
        val postId = DeepLinkUtils.extractPostId(
            scheme = "https",
            host = "example.com",
            path = "/posts/post123"
        )
        assertNull(postId)
    }

    @Test
    fun extractPostId_nonPostPath_returnsNull() {
        val postId = DeepLinkUtils.extractPostId(
            scheme = "https",
            host = "devjournal-web.vercel.app",
            path = "/users/user123"
        )
        assertNull(postId)
    }

    @Test
    fun extractPostId_emptyPath_returnsNull() {
        val postId = DeepLinkUtils.extractPostId(
            scheme = "https",
            host = "devjournal-web.vercel.app",
            path = "/posts/"
        )
        assertNull(postId)
    }

    @Test
    fun extractPostId_httpsAppLink_withQueryParam_stripsQueryParam() {
        val postId = DeepLinkUtils.extractPostId(
            scheme = "https",
            host = "devjournal-web.vercel.app",
            path = "/posts/Ip0M2dtBZH8rzBJh9H1z?v=1728144000"
        )
        assertEquals("Ip0M2dtBZH8rzBJh9H1z", postId)
    }

    @Test
    fun extractPostId_customScheme_withQueryParam_stripsQueryParam() {
        val postId = DeepLinkUtils.extractPostId(
            scheme = "devjournal",
            host = "posts",
            path = "/customPost123?v=1728144000"
        )
        assertEquals("customPost123", postId)
    }

    @Test
    fun extractPostId_queryParameter_withFragment_stripsFragment() {
        val postId = DeepLinkUtils.extractPostId(
            scheme = "https",
            host = "devjournal-web.vercel.app",
            path = "/posts",
            queryPostId = "queryPost123?v=123#section"
        )
        assertEquals("queryPost123", postId)
    }
}
