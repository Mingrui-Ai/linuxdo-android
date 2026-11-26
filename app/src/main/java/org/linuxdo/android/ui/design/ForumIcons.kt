package org.linuxdo.android.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import android.util.AtomicFile
import java.io.File
import coil.request.ImageRequest
import coil.compose.AsyncImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.linuxdo.android.App
import org.linuxdo.android.Config
import org.linuxdo.android.data.*
import org.linuxdo.android.ui.html.contentUrl
import org.linuxdo.android.ui.screen.parseCategoryColor

private data class ForumIconCatalog(
    val symbols: Map<String, String> = emptyMap(),
    val tags: Map<String, ForumIcon> = emptyMap(),
    val categories: Map<String, ForumIcon> = emptyMap(),
    val version: String = "bundled",
    val byId: Map<Int, Category> = emptyMap(),
    val byName: Map<String, Category> = emptyMap(),
)

private val LocalIcons = compositionLocalOf { ForumIconCatalog() }

@Composable
internal fun ProvideForumIcons(account: String?, active: Boolean, categories: List<Category>, tags: List<DiscourseTag>, content: @Composable () -> Unit) {
    val app = LocalContext.current.applicationContext as App
    val packaged by produceState(ForumIconCatalog(), app) {
        value = withContext(Dispatchers.IO) {
            fun asset(name: String) = app.assets.open("forum-icons/$name").bufferedReader().use { it.readText() }
            val public = Json.decodeFromString<List<Category>>(asset("categories.json"))
            ForumIconCatalog(
                symbols = Json.decodeFromString<Map<String, String>>(asset("symbols.json")),
                tags = parseForumIcons(asset("tags.txt")), byId = public.associateBy { it.id },
            )
        }
    }
    var snapshot by remember(account) { mutableStateOf(ForumIconSnapshot()) }
    LaunchedEffect(account, active) {
        // Discourse usernames contain only letters, numbers, hyphens, dots and underscores.
        val user = account.orEmpty().filter { it.isLetterOrDigit() || it in "-._" }
        val cache = AtomicFile(File(app.cacheDir, "forum-icons-$user.json"))
        snapshot = withContext(Dispatchers.IO) {
            runCatching { Json.decodeFromString<ForumIconSnapshot>(cache.readFully().decodeToString()) }
                .getOrDefault(ForumIconSnapshot())
        }
        if (!active) return@LaunchedEffect
        delay(10_000)
        while (true) {
            val age = System.currentTimeMillis() - snapshot.checkedAt
            if (age in 0 until 86_400_000L) delay(86_400_000L - age)
            try {
                val fresh = app.container.topicRepository.forumIconSnapshot(snapshot)
                snapshot = fresh
                withContext(Dispatchers.IO) {
                    val bytes = Json.encodeToString(ForumIconSnapshot.serializer(), fresh).encodeToByteArray()
                    val out = cache.startWrite()
                    try {
                        out.write(bytes)
                        cache.finishWrite(out)
                    } catch (error: Exception) {
                        cache.failWrite(out)
                        throw error
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                delay(30 * 60_000L)
            }
        }
    }
    val catalog = remember(packaged, snapshot, categories, tags) {
        val flat = flattenCategories(categories)
        val byId = packaged.byId.toMutableMap()
        for (cat in flat) {
            // A cached category from a pre-icon build has no style_type.
            byId[cat.id] = if (cat.styleType == null && cat.icon == null) {
                val seed = packaged.byId[cat.id]
                cat.copy(styleType = seed?.styleType, icon = seed?.icon)
            } else cat
        }
        packaged.copy(
            symbols = snapshot.symbols.ifEmpty { packaged.symbols },
            version = snapshot.sprite ?: "bundled",
            tags = tagIconsByName(snapshot.tags?.let(::parseForumIcons) ?: packaged.tags, tags),
            categories = snapshot.categories?.let(::parseForumIcons).orEmpty(),
            byId = byId, byName = byId.values.associateBy { it.name },
        )
    }
    CompositionLocalProvider(LocalIcons provides catalog, content = content)
}

@Composable
internal fun ForumTagText(
    tag: String, style: TextStyle, color: Color, modifier: Modifier = Modifier,
    label: String = tag, suffix: String = "", fallbackHash: Boolean = true,
    maxLines: Int = 1, overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        ForumTagIcon(tag, with(LocalDensity.current) { style.fontSize.toDp() }, fallbackHash)
        Text(label + suffix, style = style, color = color, maxLines = maxLines, overflow = overflow)
    }
}

@Composable
private fun SiteSymbol(icon: ForumIcon, tint: Color, size: Dp, fallback: @Composable () -> Unit) {
    val key = icon.name.removePrefix("fas-").removePrefix("fa-")
    val color = parseCategoryColor(icon.color) ?: tint
    val catalog = LocalIcons.current
    val svg = catalog.symbols[key]
    if (svg != null) {
        val context = LocalContext.current
        val request = remember(context, key, svg, catalog.version) {
            ImageRequest.Builder(context).data(svg.encodeToByteArray())
                .memoryCacheKey("forum-symbol:" + catalog.version + ":" + key)
                .decoderFactory(coil.decode.SvgDecoder.Factory()).crossfade(false).build()
        }
        IconImage(request, size, color, fallback)
    } else if (forumIconName.matches(key)) {
        IconImage(Config.BASE_URL + "/svg-sprite/" + Config.HOST + "/icon/$key.svg", size, color, fallback)
    } else fallback()
}

@Composable
private fun IconImage(url: Any, size: Dp, tint: Color?, fallback: @Composable () -> Unit) {
    var loaded by remember(url) { mutableStateOf(false) }
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        if (!loaded) fallback()
        AsyncImage(model = url, contentDescription = null, modifier = Modifier.fillMaxSize(),
            colorFilter = tint?.let { ColorFilter.tint(it) }, onSuccess = { loaded = true }, onError = { loaded = false })
    }
}

@Composable
internal fun ForumTagIcon(tag: String, size: Dp, fallbackHash: Boolean = true) {
    val icon = LocalIcons.current.tags[tag.trim().lowercase()]
    val tint = IosTheme.colors.secondaryLabel
    val fallback: @Composable () -> Unit = {
        if (fallbackHash) Text("#", Modifier.clearAndSetSemantics {}, color = tint, fontSize = with(LocalDensity.current) { size.toSp() })
    }
    if (icon != null) SiteSymbol(icon, tint, size, fallback) else fallback()
}

@Composable
internal fun ForumCategoryIcon(category: Category?, size: Dp, color: Color,
    name: String? = null, id: Int? = null, directory: Boolean = false) {
    val catalog = LocalIcons.current
    val cat = if (category?.styleType != null || category?.icon != null) category else
        catalog.byId[category?.id ?: id] ?: category ?: catalog.byName[name]
    val fallback: @Composable () -> Unit = {
        Box(Modifier.size(size * .6f).background(color, RoundedCornerShape(2.dp)))
    }
    if (cat == null) { fallback(); return }
    val themeIcon = catalog.categories[cat.slug.lowercase()] ?: catalog.categories[cat.id.toString()]
    val logo = (if (IosTheme.colors.isDark) cat.logoDark?.url ?: cat.logo?.url else cat.logo?.url)?.let(::contentUrl)
    val style = cat.styleType
    when {
        themeIcon != null -> SiteSymbol(themeIcon, color, size, fallback)
        style == "emoji" && !cat.emoji.isNullOrBlank() -> {
            val code = cat.emoji.trim(':')
            if (code.any { it.code > 127 }) Text(code, Modifier.clearAndSetSemantics {}, fontSize = with(LocalDensity.current) { size.toSp() })
            else {
                val url = catalogEmojiUrl(code)
                if (url != null) IconImage(url, size, null, fallback) else fallback()
            }
        }
        (style == null || style == "icon") && !cat.icon.isNullOrBlank() -> SiteSymbol(ForumIcon(cat.icon, null), color, size, fallback)
        directory && logo != null -> IconImage(logo, size, null, fallback)
        else -> fallback()
    }
}

@Composable
private fun catalogEmojiUrl(code: String): String? {
    val repo = (LocalContext.current.applicationContext as App).container.topicRepository
    val url by produceState<String?>(null, repo, code) {
        value = repo.emojiUrls()[code]?.let(::contentUrl) ?: Config.BASE_URL + "/images/emoji/twemoji/$code.png"
    }
    return url
}
