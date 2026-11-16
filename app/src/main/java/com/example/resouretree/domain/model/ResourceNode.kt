package com.example.resouretree.domain.model

import com.example.resouretree.R
import com.example.resouretree.localization.AppText

enum class NodeType { FOLDER, ITEM }
enum class ActionType { NONE, COPY, LAUNCH_APP, COPY_AND_LAUNCH, SHARE, OPEN_WEBVIEW }
enum class ContentType { TEXT, IMAGE, VIDEO, FILE }

data class ResourceContent(
    val type: ContentType = ContentType.TEXT,
    val text: String = "",
    val path: String = "",
    val mimeType: String = ""
)

fun validMime(type: ContentType, mime: String): Boolean =
    Regex("[A-Za-z0-9!#$&^_.+\\-]+/[A-Za-z0-9!#$&^_.+\\-]+").matches(mime) &&
        (type != ContentType.IMAGE || mime.startsWith("image/")) &&
        (type != ContentType.VIDEO || mime.startsWith("video/"))

data class ResourceAction(
    val type: ActionType = ActionType.NONE,
    val target: String = ""
)

data class ResourceNode(
    val id: String,
    val parentId: String?,
    val type: NodeType,
    val name: String,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val content: ResourceContent = ResourceContent(),
    val tags: List<String> = emptyList(),
    val action: ResourceAction = ResourceAction(),
    val isPinned: Boolean = false
)

object Tree {
    val order = compareByDescending<ResourceNode> { it.isPinned }.thenBy { it.sortOrder }.thenBy { it.createdAt }.thenBy { it.id }

    fun descendants(nodes: List<ResourceNode>, id: String): Set<String> {
        val children = nodes.groupBy { it.parentId }
        val found = linkedSetOf<String>()
        val pending = ArrayDeque<String>()
        pending.add(id)
        while (pending.isNotEmpty()) {
            val current = pending.removeFirst()
            if (found.add(current)) children[current].orEmpty().forEach { pending.add(it.id) }
        }
        return found
    }

    fun breadcrumb(nodes: List<ResourceNode>, id: String?): List<ResourceNode> {
        val byId = nodes.associateBy { it.id }
        val path = mutableListOf<ResourceNode>()
        val visited = mutableSetOf<String>()
        var current = id
        while (current != null) {
            require(visited.add(current)) { AppText.get(R.string.tree_cycle) }
            val node = requireNotNull(byId[current]) { AppText.get(R.string.folder_missing) }
            path.add(node)
            current = node.parentId
        }
        return path.reversed()
    }

    fun validate(nodes: List<ResourceNode>) {
        val byId = nodes.associateBy { it.id }
        require(byId.size == nodes.size) { AppText.get(R.string.duplicate_node_id) }
        nodes.forEach { node ->
            require(node.name.isNotBlank()) { AppText.get(R.string.name_empty) }
            if (node.parentId != null) require(byId[node.parentId]?.type == NodeType.FOLDER) { AppText.get(R.string.invalid_parent) }
            val visited = mutableSetOf<String>()
            var current: ResourceNode? = node
            while (current != null) {
                require(visited.add(current.id)) { AppText.get(R.string.tree_cycle) }
                require(visited.size <= 100) { AppText.get(R.string.tree_too_deep) }
                current = current.parentId?.let(byId::get)
            }
        }
    }
}
