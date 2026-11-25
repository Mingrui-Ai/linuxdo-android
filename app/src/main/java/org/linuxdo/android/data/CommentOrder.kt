package org.linuxdo.android.data

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

enum class CommentOrder(val title: String) {
    TimeAscending("时间正序"),
    TimeDescending("时间倒序"),
    Likes("点赞数"),
}

/** 只计点赞，不把其他表情回应或 Boost 混入点赞数。 */
val Post.sortLikeCount: Int
    get() = reactions.firstOrNull { it.id == "heart" }?.count?.coerceAtLeast(0)
        ?: actionsSummary.firstOrNull { it.id == 2 }?.count?.coerceAtLeast(0) ?: 0

fun likedPostRows(posts: List<Post>): List<TreePost> = flatPostRows(posts).sortedWith(
    compareBy<TreePost> { it.post.postNumber != 1 }
        .thenByDescending { it.post.sortLikeCount }
        .thenBy { it.post.postNumber }
        .thenBy { it.post.id },
)

/**
 * 点赞数不在 stream ID 列表中。完整补齐快照后才切换排序，避免把分页窗口冒充全帖排名。
 * 沿用每批 20 楼与串行请求；异常/取消直接交给调用方，成功但未返回的 ID 视为不可访问。
 */
internal suspend fun collectCommentPosts(
    stream: List<Long>,
    loaded: List<Post>,
    consumed: Set<Long>,
    fetch: suspend (List<Long>) -> List<Post>,
    onProgress: (Int, Int) -> Unit = { _, _ -> },
): List<Post> {
    val ids = stream.distinct()
    val allowed = ids.toHashSet()
    val posts = loaded.filter { it.id in allowed }.associateByTo(linkedMapOf()) { it.id }
    val missing = ids.filterNot { it in consumed || it in posts }
    var done = ids.size - missing.size
    onProgress(done, ids.size)
    for (batch in missing.chunked(20)) {
        val fetched = fetch(batch)
        currentCoroutineContext().ensureActive()
        fetched.filter { it.id in batch }.forEach { posts[it.id] = it }
        done += batch.size
        onProgress(done, ids.size)
    }
    return posts.values.sortedBy { it.postNumber }
}
