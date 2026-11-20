package com.kinnect.i230507_i230017


data class Post(
    var postId: String = "",
    var authorUid: String = "",
    var authorName: String = "",
    var text: String = "",
    var imageCount: Int = 0,        // the images live in postImages/{postId}/{index}
    var commentCount: Int = 0,
    var createdAt: Long = 0L
)

data class Comment(
    var commentId: String = "",
    var authorUid: String = "",
    var authorName: String = "",
    var text: String = "",
    var imageB64: String = "",      // image comment (small Base64), empty if none
    var parentId: String = "",      // empty for a top-level comment, otherwise the comment replied to
    var replyToName: String = "",   // shown as "@Name" in a reply
    var createdAt: Long = 0L
)

data class Story(
    var storyId: String = "",
    var authorUid: String = "",
    var authorName: String = "",
    var mediaType: String = "image", // "image" or "video"
    var label: String = "",          // text label from the Story editor
    var audience: String = "all",    // "all" (Your story) or "close" (Close friends)
    var createdAt: Long = 0L,
    var expiresAt: Long = 0L         // createdAt + 24h
)

enum class Reaction(val key: String, val label: String) {
    LIKE("like", "Like"),
    LOVE("love", "Love"),
    HAHA("haha", "Haha"),
    WOW("wow", "Wow"),
    SAD("sad", "Sad"),
    ANGRY("angry", "Angry");

    companion object {
        fun fromKey(key: String?): Reaction? = values().firstOrNull { it.key == key }
    }
}

object StoryTtl {
    const val DAY_MS = 24L * 60 * 60 * 1000
}