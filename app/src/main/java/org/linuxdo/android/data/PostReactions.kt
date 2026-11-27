package org.linuxdo.android.data

/** 数量相同时保持服务端顺序，零个回应的表情不占位置。 */
val Post.sortedReactions: List<Reaction>
    get() = reactions.filter { it.count > 0 }.sortedByDescending { it.count }

/** 先撤掉原回应，再查找新回应的位置，避免删除旧项后索引错位。 */
internal fun Post.withToggledReaction(reaction: String): Post {
    val previous = currentUserReaction?.id
    val updated = reactions.toMutableList()
    if (previous != null) {
        val index = updated.indexOfFirst { it.id == previous }
        if (index >= 0) {
            val count = updated[index].count
            if (count <= 1) updated.removeAt(index)
            else updated[index] = updated[index].copy(count = count - 1)
        }
    }
    val selected = if (previous == reaction) null else Reaction(reaction, count = 1, canUndo = true)
    if (selected != null) {
        val index = updated.indexOfFirst { it.id == reaction }
        if (index >= 0) updated[index] = updated[index].copy(count = updated[index].count + 1)
        else updated.add(selected)
    }
    return copy(reactions = updated, currentUserReaction = selected)
}
