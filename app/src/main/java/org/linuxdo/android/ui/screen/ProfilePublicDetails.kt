package org.linuxdo.android.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jsoup.Jsoup
import org.linuxdo.android.data.*
import org.linuxdo.android.data.ProfileDisplayField.*
import org.linuxdo.android.ui.rememberProfileDisplayPreferences
import org.linuxdo.android.ui.design.IosTheme

/** 详细主页的公开信息；缺失、隐藏和被撤回的字段不占位。 */
@Composable
internal fun ProfilePublicDetails(
    card: UserCardData?,
    onOpenWebsite: (String) -> Unit,
    onOpenTopic: (Long, String, Int?) -> Unit,
) {
    val display = rememberProfileDisplayPreferences()
    val website = profileWebsiteUrl(card?.website).takeIf { display[Website] }
    val topic = card?.featuredTopic?.takeIf { display[FeaturedTopic] && it.id > 0 && it.title.isNotBlank() }
    val colors = IosTheme.colors
    if (website == null && topic == null) return
    Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        if (website != null) Row(Modifier.fillMaxWidth().heightIn(min = 40.dp)
            .clickable(role = Role.Button, onClickLabel = "打开个人网站") { onOpenWebsite(website) },
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("↗", style = IosTheme.type.footnote, color = colors.secondaryLabel, modifier = Modifier.width(16.dp))
            Text(website.substringAfter("://").trimEnd('/'), style = IosTheme.type.footnote, color = colors.accent,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (topic != null) Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClickLabel = "查看精选话题") { onOpenTopic(topic.id, topic.title, null) }
            .padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Star, null, tint = colors.secondaryLabel, modifier = Modifier.size(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("精选话题", style = IosTheme.type.caption, color = colors.secondaryLabel)
                Text(Jsoup.parse(topic.title).text(), style = IosTheme.type.footnote, color = colors.accent,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.KeyboardArrowRight, null, tint = colors.secondaryLabel, modifier = Modifier.size(16.dp))
        }
    }
}
