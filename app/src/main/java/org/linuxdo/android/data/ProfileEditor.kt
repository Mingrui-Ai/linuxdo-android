package org.linuxdo.android.data

import kotlinx.serialization.json.*
import java.time.LocalDate
import java.time.ZoneId

/** Only editable profile data is kept here; credentials and account security data are never retained. */
internal data class ProfileChoice(val value: String, val label: String, val image: String? = null)
internal data class ProfileEditorData(
    val userId: Long,
    val username: String,
    val values: Map<String, String>,
    val permissions: Set<String>,
    val titles: List<ProfileChoice>,
    val flairs: List<ProfileChoice>,
    val badges: List<ProfileChoice>,
    val avatars: List<ProfileChoice>,
    val avatarTemplate: String?,
    val featuredTitle: String,
    val card: UserCardData,
    val user: DiscourseUser,
)

internal object ProfileFields {
    val labels = linkedMapOf(
        "name" to "姓名", "title" to "头衔", "flair_group_id" to "资质",
        "bio_raw" to "自我介绍", "timezone" to "时区", "location" to "地点", "website" to "网站",
        "profile_background_upload_url" to "个人主页背景", "card_background_upload_url" to "用户卡片背景",
        "date_of_birth" to "出生日期", "custom:see_signatures" to "显示用户签名",
        "custom:signature_url" to "我的签名", "custom:telegram_chat_id" to "Telegram 通知",
        "custom:holidays-region" to "区域", "status" to "自定义状态",
        "avatar" to "头像", "featured_topic" to "精选话题", "card_badge" to "用户卡片徽章",
    )
    val permissionKeys = mapOf(
        "name" to "can_edit_name", "bio_raw" to "can_change_bio", "location" to "can_change_location",
        "website" to "can_change_website", "avatar" to "can_edit_avatar",
        "profile_background_upload_url" to "can_upload_profile_header",
        "card_background_upload_url" to "can_upload_user_card_background",
    )
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    fun parse(root: JsonObject, badgeRoot: JsonObject): ProfileEditorData {
        val u = root["user"] as? JsonObject ?: error("资料响应缺少用户")
        require(u.flag("can_edit")) { "当前账号无法编辑这些资料" }
        val custom = u.obj("custom_fields")
        val options = u.obj("user_option")
        val grants = badgeRoot.objects("user_badges")
        val badges = badgeRoot.objects("badges").associateBy { it.text("id") }
        val groups = u.objects("groups")
        val values = labels.keys.associateWith { key ->
            when {
                key == "timezone" -> options.text(key).ifEmpty { u.text(key) }
                key == "date_of_birth" -> u.text("birthdate").ifEmpty { u.text(key) }.let { date ->
                    if (date.isEmpty()) "" else runCatching { LocalDate.parse(date).let { "%02d-%02d".format(it.monthValue,it.dayOfMonth) } }.getOrDefault("")
                }
                key == "custom:see_signatures" -> (custom["see_signatures"]?.jsonPrimitive?.booleanOrNull ?: u.flag("see_signatures")).toString()
                key.startsWith("custom:") -> custom.text(key.removePrefix("custom:"))
                key == "avatar" -> if (u.text("selected_user_associated_account_id").isNotEmpty()) "associated_account:${u.text("selected_user_associated_account_id")}:${u.text("uploaded_avatar_id")}"
                    else if (u.text("uploaded_avatar_id").isEmpty()) "system" else "current:${u.text("uploaded_avatar_id")}"
                key == "featured_topic" -> u.obj("featured_topic").text("id")
                key == "card_badge" -> grants.firstOrNull { it.text("badge_id") == u.text("card_image_badge_id") }?.text("id").orEmpty()
                key == "status" -> u.obj("status").let { status -> if (status.text("description").isEmpty()) "" else JsonObject(mapOf(
                    "description" to JsonPrimitive(status.text("description")), "emoji" to JsonPrimitive(status.text("emoji")),
                    "ends_at" to (status["ends_at"] ?: JsonNull),
                )).toString() }
                else -> u.text(key)
            }
        }
        val titleChoices = (groups.mapNotNull { it.text("title").takeIf(String::isNotBlank) } +
            badges.values.filter { it.flag("allow_title") }.map { it.text("name") }).distinct().sorted().map { ProfileChoice(it,it) }
        val avatarChoices = buildList {
            add(ProfileChoice("system", "系统默认头像", u.text("system_avatar_template")))
            u.text("custom_avatar_upload_id").takeIf(String::isNotBlank)?.let { add(ProfileChoice("custom:$it", "已上传头像", u.text("custom_avatar_template"))) }
            u.objects("associated_account_avatars").forEach { a ->
                add(ProfileChoice("associated_account:${a.text("id")}:${a.text("upload_id")}", a.text("name"), a.text("avatar_template")))
            }
            if (none { it.value == values["avatar"] }) add(ProfileChoice(values.getValue("avatar"),"当前头像",u.text("avatar_template")))
        }
        return ProfileEditorData(
            u.text("id").toLong(), u.text("username"), values,
            labels.keys.filter { permissionKeys[it]?.let(u::flag) ?: true }.toSet() + if (u.flag("can_upload_avatar")) setOf("upload_avatar") else emptySet(),
            titleChoices, groups.filter { it.text("flair_url").isNotBlank() }.map { ProfileChoice(it.text("id"),it.text("full_name").ifEmpty{it.text("name")},it.text("flair_url")) },
            grants.distinctBy { it.text("badge_id") }.mapNotNull { grant -> badges[grant.text("badge_id")]?.takeIf { it.text("image_url").isNotBlank() }?.let { b ->
                ProfileChoice(grant.text("id"),b.text("name"),b.text("image_url").ifEmpty { null })
            } }, avatarChoices, u.text("avatar_template"), u.obj("featured_topic").text("title"),
            json.decodeFromJsonElement<UserCardData>(u), json.decodeFromJsonElement<DiscourseUser>(u),
        )
    }

    fun changes(base: Map<String,String>, edited: Map<String,String>) = edited.filter { (key,value) -> key in labels && value != base[key].orEmpty() }

    fun equivalent(key: String, expected: String, actual: String): Boolean {
        if (expected == actual) return true
        if (key == "avatar") {
            fun upload(value: String) = value.split(':').lastOrNull()?.toLongOrNull()
            return expected != "system" && actual != "system" && upload(expected) != null && upload(expected) == upload(actual)
        }
        if (key == "status") return runCatching {
            val a=json.parseToJsonElement(expected).jsonObject; val b=json.parseToJsonElement(actual).jsonObject
            a.text("description")==b.text("description") && a.text("emoji")==b.text("emoji") &&
                a.text("ends_at").takeIf(String::isNotEmpty)?.let(java.time.Instant::parse) == b.text("ends_at").takeIf(String::isNotEmpty)?.let(java.time.Instant::parse)
        }.getOrDefault(false)
        if (key == "website" && expected.isNotBlank()) return actual.trimEnd('/') == (if("://" in expected) expected else "https://$expected").trimEnd('/')
        if (key.endsWith("_background_upload_url") && expected.isNotEmpty() && actual.isNotEmpty()) return runCatching { java.net.URI(expected).path == java.net.URI(actual).path }.getOrDefault(false)
        return false
    }

    fun conflicts(base: Map<String,String>, remote: Map<String,String>, edited: Map<String,String>): Set<String> =
        changes(base,edited).filter { (key,value) -> !equivalent(key,base[key].orEmpty(),remote[key].orEmpty()) && !equivalent(key,value,remote[key].orEmpty()) }.keys

    fun validate(edited: Map<String,String>, dirty: Set<String>) {
        fun value(key: String) = edited[key].orEmpty()
        if ("timezone" in dirty) require(value("timezone") in ZoneId.getAvailableZoneIds()) { "请选择有效时区" }
        if ("date_of_birth" in dirty && value("date_of_birth").isNotBlank()) require(runCatching { LocalDate.parse("1904-${value("date_of_birth")}") }.isSuccess) { "请选择有效的生日月日" }
        if ("website" in dirty && value("website").isNotBlank()) {
            val uri = runCatching { java.net.URI(value("website").let { if ("://" in it) it else "https://$it" }) }.getOrNull()
            require(uri?.scheme in setOf("https","http") && !uri?.host.isNullOrBlank()) { "请输入有效的网站地址" }
        }
        if ("custom:signature_url" in dirty && value("custom:signature_url").isNotBlank()) {
            val uri = runCatching { java.net.URI(value("custom:signature_url")) }.getOrNull()
            require(uri?.scheme in setOf("https","http") && !uri?.host.isNullOrBlank()) { "签名需要完整的图片 URL" }
        }
        if ("custom:telegram_chat_id" in dirty && value("custom:telegram_chat_id").isNotBlank()) require(value("custom:telegram_chat_id").matches(Regex("-?\\d+"))) { "Telegram Chat ID 应为数字，可包含负号" }
        if ("featured_topic" in dirty && value("featured_topic").isNotEmpty()) require((value("featured_topic").toLongOrNull() ?: 0) > 0) { "请选择有效话题" }
        if ("status" in dirty && value("status").isNotEmpty()) {
            val status = json.parseToJsonElement(value("status")).jsonObject
            require(status.text("description").isNotBlank() && status.text("emoji").isNotBlank()) { "状态文字和表情不能为空" }
        }
    }

    fun payload(changes: Map<String,String>): JsonObject {
        val regular = linkedMapOf<String,JsonElement>()
        val custom = linkedMapOf<String,JsonElement>()
        changes.forEach { (key,value) ->
            when {
                key.startsWith("custom:") -> custom[key.removePrefix("custom:")] = if (key == "custom:see_signatures") JsonPrimitive(value.toBoolean()) else JsonPrimitive(value)
                key == "status" -> regular[key] = if (value.isEmpty()) JsonNull else json.parseToJsonElement(value)
                key == "date_of_birth" -> regular[key] = JsonPrimitive(if (value.isEmpty()) "" else "1904-$value")
                key in labels && key !in setOf("avatar","featured_topic","card_badge") -> regular[key] = JsonPrimitive(value)
            }
        }
        if (custom.isNotEmpty()) regular["custom_fields"] = JsonObject(custom)
        return JsonObject(regular)
    }

    fun avatarPayload(value: String): JsonObject {
        val parts=value.split(':')
        require(parts[0] in setOf("system","custom","current","associated_account"))
        return buildJsonObject {
            put("type",parts[0])
            if (parts[0] == "associated_account") { put("associated_account_id",parts[1].toLong()); put("upload_id",parts[2].toLong()) }
            else if (parts[0] != "system") put("upload_id",parts[1].toLong())
        }
    }
}

internal fun JsonObject.text(key: String) = (get(key) as? JsonPrimitive)?.contentOrNull.orEmpty()
internal fun JsonObject.flag(key: String) = (get(key) as? JsonPrimitive)?.booleanOrNull == true
internal fun JsonObject.obj(key: String) = get(key) as? JsonObject ?: JsonObject(emptyMap())
internal fun JsonObject.objects(key: String) = (get(key) as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
