package org.linuxdo.android.ui

import android.app.Activity
import android.content.ContextWrapper
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import org.linuxdo.android.data.*
import org.linuxdo.android.ui.design.*
import org.linuxdo.android.App
import org.linuxdo.android.ui.html.CookedBlock
import org.linuxdo.android.ui.html.profileBioPreview
import org.linuxdo.android.ui.screen.CookedContent
import org.linuxdo.android.ui.screen.ImageViewer
import org.linuxdo.android.ui.screen.ImageViewerItem
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

/** A standalone editor keeps navigation/layout refactors independent from profile data changes. */
@Composable
internal fun ProfileEditorDialog(username: String, userId: Long, onDismiss: () -> Unit) {
    val model: ProfileEditorViewModel = viewModel(key="native-profile-editor-$userId")
    val state by model.state.collectAsStateWithLifecycle()
    val context=LocalContext.current
    val emojiUrls = rememberProfileEmojis()
    var confirmClose by remember { mutableStateOf(false) }
    var confirmReload by remember { mutableStateOf(false) }
    var picker by remember { mutableStateOf<Pair<String,List<ProfileChoice>>?>(null) }
    var statusDialog by remember { mutableStateOf(false) }
    var birthdayDialog by remember { mutableStateOf(false) }
    var topicDialog by remember { mutableStateOf(false) }
    var uploadField by rememberSaveable { mutableStateOf("avatar") }
    val upload=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? -> uri?.let { model.upload(it,uploadField) } }
    LaunchedEffect(userId,username) { model.open(userId,username) }
    val activity=remember(context) { generateSequence(context) { (it as? ContextWrapper)?.baseContext }.filterIsInstance<Activity>().firstOrNull() }
    DisposableEffect(model) { onDispose { if (activity?.isChangingConfigurations != true) model.close() } }
    val close = { if(!state.busy) { if(state.dirty) confirmClose=true else { model.close(); onDismiss() } } }
    val regions = remember(context) {
        runCatching { context.assets.open("profile-regions.json").bufferedReader().use { Json.parseToJsonElement(it.readText()).jsonObject } }.getOrNull()
    }
    val regionChoices = remember(regions) {
        (regions?.get("regions") as? JsonArray).orEmpty().map { item ->
            val code=item.jsonPrimitive.content
            val country=Locale("",code.substringBefore('_').uppercase(Locale.ROOT)).getDisplayCountry(Locale.SIMPLIFIED_CHINESE)
            ProfileChoice(code, if('_' in code) "$country · $code" else country.ifBlank { code })
        }.sortedBy { it.label }
    }
    val colors=IosTheme.colors
    Dialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
        BackHandler(onBack=close)
        Column(Modifier.fillMaxSize().background(colors.groupedBackground).systemBarsPadding().imePadding()) {
            IosNavBar(title="编辑个人资料",onBack=close,trailing={
                TextButton(onClick=model::save,enabled=state.dirty && state.editable) { Text(if(state.saving) "保存中…" else "保存") }
            })
            if(state.loading || state.saving || state.uploading!=null) LinearProgressIndicator(Modifier.fillMaxWidth())
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                    Text("@$username",color=colors.secondaryLabel)
                    TextButton(onClick={if(state.dirty) confirmReload=true else model.reload()},enabled=!state.busy) { Text("从网页刷新") }
                }
                state.error?.let { Text(it,color=MaterialTheme.colorScheme.error) }
                state.notice?.let { Text(it,color=colors.accent) }
                state.data?.let { data ->
                    fun value(key:String)=state.values[key].orEmpty()
                    fun enabled(key:String)=state.editable && key in data.permissions
                    fun choices(key:String, options:List<ProfileChoice>) { picker=key to (listOf(ProfileChoice("","无"))+options) }
                    ProfileSection("基本资料") {
                        val avatar=data.avatars.find { it.value==value("avatar") }
                        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                            AsyncImage(model=profileImage(avatar?.image ?: data.avatarTemplate),contentDescription="当前头像",modifier=Modifier.size(72.dp).clip(RoundedCornerShape(36.dp)),contentScale=ContentScale.Crop)
                            Column {
                                TextButton(onClick={picker="avatar" to data.avatars},enabled=enabled("avatar")) { Text("选择头像") }
                                TextButton(onClick={uploadField="avatar";upload.launch("image/*")},enabled=enabled("avatar") && "upload_avatar" in data.permissions) { Text(if(state.uploading=="avatar") "上传中…" else "上传新头像") }
                            }
                        }
                        ProfileField("姓名",value("name"),enabled("name")) { model.edit("name",it) }
                        ProfileSelection("头衔",data.titles.find{it.value==value("title")}?.label ?: value("title").ifEmpty{"无"},enabled("title")) { choices("title",data.titles) }
                        ProfileSelection("资质",data.flairs.find{it.value==value("flair_group_id")}?.label ?: "无",enabled("flair_group_id")) { choices("flair_group_id",data.flairs) }
                        val status=runCatching { Json.parseToJsonElement(value("status")).jsonObject }.getOrNull()
                        ProfileSelection("自定义状态",status?.text("description")?.ifEmpty{"未设置"} ?: "未设置",enabled("status"),leading={
                            status?.text("emoji")?.takeIf { it.isNotBlank() }?.let { ProfileStatusEmoji(it,emojiUrls) }
                        }) { statusDialog=true }
                    }
                    ProfileSection("自我介绍") {
                        ProfileBioEditor(value("bio_raw"),enabled("bio_raw"),emojiUrls,onUpload={uploadField="bio_raw";upload.launch("image/*")}) { model.edit("bio_raw",it) }
                    }
                    ProfileSection("个人信息") {
                        ProfileSelection("时区",value("timezone"),enabled("timezone")) { picker="timezone" to ZoneId.getAvailableZoneIds().sorted().map{ProfileChoice(it,it)} }
                        TextButton(onClick={model.edit("timezone",ZoneId.systemDefault().id)},enabled=enabled("timezone")) { Text("使用当前时区") }
                        ProfileField("地点",value("location"),enabled("location")) { model.edit("location",it) }
                        ProfileField("网站",value("website"),enabled("website")) { model.edit("website",it) }
                        ProfileSelection("出生日期",value("date_of_birth").ifEmpty { "未设置" },enabled("date_of_birth")) { birthdayDialog=true }
                        Text("生日仅显示月日",style=IosTheme.type.footnote,color=colors.secondaryLabel)
                    }
                    ProfileSection("背景图片") {
                        listOf("profile_background_upload_url","card_background_upload_url").forEach { field ->
                            Text(ProfileFields.labels.getValue(field),color=colors.label)
                            if(value(field).isNotEmpty()) AsyncImage(model=profileImage(value(field)),contentDescription=ProfileFields.labels[field],modifier=Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(10.dp)),contentScale=ContentScale.Crop)
                            Row {
                                TextButton(onClick={uploadField=field;upload.launch("image/*")},enabled=enabled(field)) { Text(if(state.uploading==field) "上传中…" else "选择图片") }
                                if(value(field).isNotEmpty()) TextButton(onClick={model.edit(field,"")},enabled=enabled(field)) { Text("移除") }
                            }
                        }
                    }
                    ProfileSection("个人展示") {
                        ProfileSelection("精选话题",if(value("featured_topic").isEmpty()) "未设置" else if(value("featured_topic")==data.values["featured_topic"]) data.featuredTitle.ifEmpty { "话题 ${value("featured_topic")}" } else "话题 ${value("featured_topic")}",enabled("featured_topic")) { topicDialog=true }
                        if(value("featured_topic").isNotEmpty()) TextButton(onClick={model.edit("featured_topic","")},enabled=enabled("featured_topic")) { Text("取消精选话题") }
                        ProfileSelection("用户卡片徽章",data.badges.find{it.value==value("card_badge")}?.label ?: "无",enabled("card_badge")) { choices("card_badge",data.badges) }
                        Text("可选择已获得且带图片的徽章",style=IosTheme.type.footnote,color=colors.secondaryLabel)
                    }
                    ProfileSection("签名与通知") {
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                            Text("在帖子下方显示用户签名",modifier=Modifier.weight(1f),color=colors.label)
                            Switch(checked=value("custom:see_signatures").toBoolean(),onCheckedChange={model.edit("custom:see_signatures",it.toString())},enabled=enabled("custom:see_signatures"))
                        }
                        ProfileField("我的签名图片 URL",value("custom:signature_url"),enabled("custom:signature_url")) { model.edit("custom:signature_url",it) }
                        ProfileField("Telegram Chat ID",value("custom:telegram_chat_id"),enabled("custom:telegram_chat_id")) { model.edit("custom:telegram_chat_id",it) }
                        Text("填写 @linux_do_helper_bot 提供的 Chat ID，留空则禁用。",style=IosTheme.type.footnote,color=colors.secondaryLabel)
                        ProfileSelection("区域",regionChoices.find{it.value==value("custom:holidays-region")}?.label ?: value("custom:holidays-region").ifEmpty{"未设置"},enabled("custom:holidays-region")) { choices("custom:holidays-region",regionChoices) }
                        TextButton(onClick={
                            val region=regions?.obj("timezones")?.text(ZoneId.systemDefault().id).orEmpty()
                            if(regionChoices.any{it.value==region}) model.edit("custom:holidays-region",region) else choices("custom:holidays-region",regionChoices)
                        },enabled=enabled("custom:holidays-region")) { Text("使用当前区域") }
                    }
                    Text(if(state.dirty) "有未保存的修改" else "与网页个人资料同步",style=IosTheme.type.footnote,color=colors.secondaryLabel)
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
    picker?.let { (key,options) -> ProfileChoiceDialog(ProfileFields.labels[key].orEmpty(),options,state.values[key].orEmpty(),onDismiss={picker=null}) { model.edit(key,it);picker=null } }
    if(confirmClose) AlertDialog(onDismissRequest={confirmClose=false},title={Text("放弃未保存的修改？")},text={Text("网页资料仍保持原样。")},confirmButton={TextButton(onClick={confirmClose=false;model.close();onDismiss()}){Text("放弃修改")}},dismissButton={TextButton(onClick={confirmClose=false}){Text("继续编辑")}})
    if(confirmReload) AlertDialog(onDismissRequest={confirmReload=false},title={Text("重新载入网页资料？")},text={Text("本次未保存的修改会被替换。")},confirmButton={TextButton(onClick={confirmReload=false;model.reload()}){Text("重新载入")}},dismissButton={TextButton(onClick={confirmReload=false}){Text("取消")}})
    if(state.conflicts.isNotEmpty()) AlertDialog(onDismissRequest={},title={Text("网页资料已有更新")},text={Text("以下内容在两端都被修改：\n"+state.conflicts.joinToString("、"){ProfileFields.labels[it].orEmpty()})},confirmButton={TextButton(onClick=model::keepEdits){Text("保留本次修改")}},dismissButton={TextButton(onClick=model::acceptRemote){Text("采用网页版本")}})
    if(statusDialog) ProfileStatusDialog(state.values["status"].orEmpty(),emojiUrls,onDismiss={statusDialog=false}) { model.edit("status",it);statusDialog=false }
    if(birthdayDialog) ProfileBirthdayDialog(state.values["date_of_birth"].orEmpty(),onDismiss={birthdayDialog=false}) {model.edit("date_of_birth",it);birthdayDialog=false}
    if(topicDialog) ProfileTopicDialog(state,model,onDismiss={topicDialog=false}) {model.edit("featured_topic",it);topicDialog=false}
}

private fun profileImage(url:String?):String? = url?.takeIf(String::isNotBlank)?.replace("{size}","288")?.let { if(it.startsWith("//")) "https:$it" else if(it.startsWith("/")) org.linuxdo.android.Config.BASE_URL+it else it }

// The old job-search preset used a Unicode label rather than Discourse's shortcode.
private fun profileStatusEmojiCode(value:String):String = value.trim().trim(':').let {
    if(it=="magnifying_glass_tilted_left") "mag" else it
}

@Composable private fun ProfileSection(title:String,content:@Composable ColumnScope.()->Unit) {
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(title,style=IosTheme.type.footnote,color=IosTheme.colors.secondaryLabel)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(IosTheme.colors.card).padding(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp),content=content)
    }
}

@Composable private fun ProfileField(label:String,value:String,enabled:Boolean,onChange:(String)->Unit) {
    OutlinedTextField(value=value,onValueChange=onChange,label={Text(label)},enabled=enabled,modifier=Modifier.fillMaxWidth(),singleLine=true)
}

@Composable private fun ProfileSelection(label:String,value:String,enabled:Boolean,leading:(@Composable ()->Unit)?=null,onClick:()->Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min=48.dp).clickable(enabled=enabled,onClick=onClick),verticalAlignment=Alignment.CenterVertically) {
        leading?.invoke()
        Column(Modifier.weight(1f)) { Text(label,color=IosTheme.colors.label);Text(value,style=IosTheme.type.footnote,color=IosTheme.colors.secondaryLabel) }
        Text("›",color=IosTheme.colors.secondaryLabel)
    }
}

@Composable private fun ProfileChoiceDialog(title:String,options:List<ProfileChoice>,selected:String,onDismiss:()->Unit,onChoose:(String)->Unit) {
    var query by remember { mutableStateOf("") }
    ProfilePickerDialog(title,onDismiss) {
            OutlinedTextField(value=query,onValueChange={query=it},label={Text("搜索")},singleLine=true)
            LazyColumn(Modifier.weight(1f,fill=false).heightIn(max=380.dp)) {
                items(options.filter{query.isBlank()||it.label.contains(query,true)||it.value.contains(query,true)}) { choice ->
                    Row(Modifier.fillMaxWidth().clickable{onChoose(choice.value)}.padding(vertical=12.dp),verticalAlignment=Alignment.CenterVertically) {
                        if(!choice.image.isNullOrEmpty()) AsyncImage(model=profileImage(choice.image),contentDescription=null,modifier=Modifier.size(36.dp).padding(end=8.dp))
                        Text(choice.label,modifier=Modifier.weight(1f)); if(choice.value==selected) Text("✓",color=IosTheme.colors.accent)
                    }
                }
            }
    }
}

/** Own the full window so the IME inset constrains long picker results and keeps Cancel reachable. */
@Composable private fun ProfilePickerDialog(title:String,onDismiss:()->Unit,content:@Composable ColumnScope.()->Unit) {
    Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
        Box(Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(24.dp),contentAlignment=Alignment.Center) {
            Surface(modifier=Modifier.widthIn(max=560.dp).fillMaxWidth(),shape=RoundedCornerShape(28.dp),color=AlertDialogDefaults.containerColor) {
                Column(Modifier.padding(24.dp)) {
                    Text(title,style=MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(16.dp))
                    Column(Modifier.weight(1f,fill=false),content=content)
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick=onDismiss,modifier=Modifier.align(Alignment.End)) { Text("取消") }
                }
            }
        }
    }
}

@Composable private fun ProfileBioEditor(raw:String,enabled:Boolean,emojiUrls:Map<String,String>,onUpload:()->Unit,onChange:(String)->Unit) {
    var text by remember { mutableStateOf(TextFieldValue(raw)) }
    var emojis by remember { mutableStateOf(false) }
    var preview by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(raw) { if(raw!=text.text) text=TextFieldValue(raw,TextRange(raw.length)) }
    fun insert(before:String,after:String="") {
        val start=text.selection.min;val end=text.selection.max
        val updated=text.text.take(start)+before+text.text.substring(start,end)+after+text.text.drop(end)
        text=TextFieldValue(updated,TextRange(start+before.length,end+before.length));onChange(updated)
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        FilterChip(selected=!preview,onClick={preview=false},label={Text("编辑")})
        FilterChip(selected=preview,onClick={focus.clearFocus();keyboard?.hide();emojis=false;preview=true},label={Text("预览")})
    }
    if(preview) ProfileBioPreview(raw,emojiUrls) else {
    Row(Modifier.horizontalScroll(rememberScrollState())) {
        listOf("粗体" to ("**" to "**"),"斜体" to ("_" to "_"),"标题" to ("\n## " to ""),"链接" to ("[" to "](https://)"),"引用" to ("\n> " to ""),"列表" to ("\n- " to ""),"代码" to ("`" to "`")).forEach { (label,markers) -> TextButton(onClick={insert(markers.first,markers.second)},enabled=enabled){Text(label)} }
        TextButton(onClick={emojis=!emojis},enabled=enabled){Text("表情")}
        TextButton(onClick=onUpload,enabled=enabled){Text("图片")}
    }
    OutlinedTextField(value=text,onValueChange={text=it;onChange(it.text)},enabled=enabled,modifier=Modifier.fillMaxWidth().heightIn(min=160.dp),minLines=4,label={Text("支持 Markdown 和表情")})
    }
    if(emojis) ProfileEmojiDialog(onDismiss={emojis=false}) { insert(":$it:");emojis=false }
}

@Composable private fun ProfileBioPreview(raw:String,emojiUrls:Map<String,String>) {
    var blocks by remember(raw,emojiUrls) { mutableStateOf<List<CookedBlock>?>(null) }
    var error by remember(raw,emojiUrls) { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    var image by remember { mutableStateOf<Pair<List<ImageViewerItem>,Int>?>(null) }
    LaunchedEffect(raw,emojiUrls,retry) {
        error=null
        try { blocks=withContext(Dispatchers.Default) { profileBioPreview(raw,emojiUrls) } }
        catch(e:CancellationException) { throw e }
        catch(_:Exception) { error="预览暂时无法显示" }
    }
    Column(Modifier.fillMaxWidth().heightIn(min=160.dp).padding(vertical=8.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        when {
            raw.isBlank() -> Text("填写自我介绍后可在这里预览",color=IosTheme.colors.secondaryLabel)
            error!=null -> { Text(error!!); TextButton(onClick={retry++}){Text("重试")} }
            blocks==null -> LinearProgressIndicator(Modifier.fillMaxWidth())
            else -> CookedContent(blocks!!,onPicture={images,index -> image=images to index})
        }
    }
    image?.let { (images,index) -> ImageViewer(images=images,initialIndex=index,onDismiss={image=null}) }
}

@Composable private fun rememberProfileEmojis():Map<String,String> {
    val app=LocalContext.current.applicationContext as App
    val urls by produceState<Map<String,String>>(emptyMap(),app) { value=app.container.topicRepository.emojiUrls() }
    return urls
}

@Composable private fun ProfileStatusEmoji(name:String,urls:Map<String,String>) {
    val code=profileStatusEmojiCode(name)
    val url=urls[code] ?: "${org.linuxdo.android.Config.BASE_URL}/images/emoji/twemoji/$code.png?v=15"
    var failed by remember(url) { mutableStateOf(false) }
    if(failed) Text(":$code:",style=IosTheme.type.footnote,modifier=Modifier.widthIn(max=120.dp).padding(end=10.dp))
    else AsyncImage(model=rememberAnimatedImageRequest(url) ?: url,contentDescription="状态表情 :$code:",
        modifier=Modifier.padding(end=10.dp).size(28.dp),contentScale=ContentScale.Fit,onError={failed=true})
}

@Composable private fun ProfileEmojiDialog(onDismiss:()->Unit,onChoose:(String)->Unit) {
    val app=LocalContext.current.applicationContext as App
    val choices by produceState<List<ProfileChoice>?>(null,app) {
        value=app.container.topicRepository.emojiUrls().map { (name,url) -> ProfileChoice(name,name,url) }
    }
    if(choices==null) AlertDialog(onDismissRequest=onDismiss,title={Text("加载表情")},text={LinearProgressIndicator(Modifier.fillMaxWidth())},confirmButton={TextButton(onClick=onDismiss){Text("取消")}})
    else if(choices!!.isEmpty()) AlertDialog(onDismissRequest=onDismiss,title={Text("暂时无法加载表情")},text={Text("可以先输入 :表情名称:，或稍后再试。")},confirmButton={TextButton(onClick=onDismiss){Text("返回")}})
    else ProfileChoiceDialog("表情",choices!!,"",onDismiss,onChoose)
}

@Composable private fun ProfileBirthdayDialog(value:String,onDismiss:()->Unit,onSave:(String)->Unit) {
    var month by remember { mutableStateOf(value.substringBefore('-').toIntOrNull() ?: 1) }
    var day by remember { mutableStateOf(value.substringAfter('-').toIntOrNull() ?: 1) }
    var part by remember { mutableStateOf<String?>(null) }
    val days=YearMonth.of(1904,month).lengthOfMonth()
    AlertDialog(onDismissRequest=onDismiss,title={Text("生日（月日）")},text={Row { TextButton(onClick={part="month"}){Text("$month 月")};TextButton(onClick={part="day"}){Text("${day.coerceAtMost(days)} 日")} }},
        confirmButton={TextButton(onClick={onSave("%02d-%02d".format(month,day.coerceAtMost(days)))}){Text("确定")}},dismissButton={Row { TextButton(onClick={onSave("")}){Text("清除")};TextButton(onClick=onDismiss){Text("取消")} }})
    part?.let { p -> ProfileChoiceDialog(if(p=="month") "月份" else "日期",(1..if(p=="month")12 else days).map{ProfileChoice(it.toString(),it.toString())},if(p=="month")month.toString() else day.toString(),onDismiss={part=null}) {if(p=="month")month=it.toInt() else day=it.toInt();part=null} }
}

@Composable private fun ProfileStatusDialog(value:String,emojiUrls:Map<String,String>,onDismiss:()->Unit,onSave:(String)->Unit) {
    val initial=remember {runCatching{Json.parseToJsonElement(value).jsonObject}.getOrNull()}
    var description by remember { mutableStateOf(initial?.text("description").orEmpty()) }
    var emoji by remember { mutableStateOf(profileStatusEmojiCode(initial?.text("emoji").orEmpty()).ifEmpty{"slightly_smiling_face"}) }
    val format=DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    var expiry by remember {mutableStateOf(initial?.text("ends_at")?.takeIf{it.isNotBlank()}?.let{runCatching{Instant.parse(it).atZone(ZoneId.systemDefault()).format(format)}.getOrNull()}.orEmpty())}
    var error by remember {mutableStateOf<String?>(null)}
    var emojis by remember {mutableStateOf(false)}
    AlertDialog(onDismissRequest=onDismiss,title={Text("自定义状态")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically) {
            ProfileStatusEmoji(emoji,emojiUrls)
            Text(description.ifBlank { "状态预览" },color=IosTheme.colors.label)
        }
        ProfileField("状态文字",description,true){description=it}
        ProfileField("表情名称",emoji,true){emoji=profileStatusEmojiCode(it)}
        TextButton(onClick={emojis=true}){Text("选择表情")}
        Row(Modifier.horizontalScroll(rememberScrollState())) {listOf("微笑" to "slightly_smiling_face","工作" to "briefcase","度假" to "palm_tree","找工作" to "mag").forEach{(label,code)->TextButton(onClick={emoji=code}){ProfileStatusEmoji(code,emojiUrls);Text(label)}}}
        ProfileField("结束时间（留空长期保留）",expiry,true){expiry=it}
        Text("格式：yyyy-MM-dd HH:mm，使用本机时区",style=IosTheme.type.footnote)
        Row {TextButton(onClick={expiry=""}){Text("长期")};TextButton(onClick={expiry=LocalDateTime.now().plusHours(1).format(format)}){Text("1 小时")};TextButton(onClick={expiry=LocalDate.now().plusDays(1).atStartOfDay().format(format)}){Text("今天")}}
        error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
    }},confirmButton={TextButton(onClick={
        try {
            require(description.isNotBlank()&&emoji.isNotBlank()){"请填写状态文字和表情"}
            val ends=if(expiry.isBlank())null else LocalDateTime.parse(expiry,format).atZone(ZoneId.systemDefault()).toInstant().also{require(it.isAfter(Instant.now())){"结束时间须晚于现在"}}
            onSave(buildJsonObject{put("description",description);put("emoji",emoji);put("ends_at",ends?.toString()?.let(::JsonPrimitive) ?: JsonNull)}.toString())
        }catch(e:Exception){error=e.message?.takeIf{it.contains("请")||it.contains("时间须")}?:"请检查时间格式"}
    }){Text("确定")}},dismissButton={Row{TextButton(onClick={onSave("")}){Text("清除")};TextButton(onClick=onDismiss){Text("取消")}}})
    if(emojis) ProfileEmojiDialog(onDismiss={emojis=false}){emoji=it;emojis=false}
}

@Composable private fun ProfileTopicDialog(state:ProfileEditorUiState,model:ProfileEditorViewModel,onDismiss:()->Unit,onChoose:(String)->Unit) {
    var query by remember {mutableStateOf("")}
    DisposableEffect(model) { model.searchTopics(""); onDispose { model.searchTopics("") } }
    ProfilePickerDialog("精选话题",onDismiss) {
        OutlinedTextField(value=query,onValueChange={query=it;model.searchTopics(it)},label={Text("话题标题、链接或编号")},singleLine=true)
        val topicId=query.trim().toLongOrNull() ?: Regex("/t/(?:[^/]+/)?(\\d+)").find(query)?.groupValues?.get(1)?.toLongOrNull()
        if(topicId!=null && topicId>0) TextButton(onClick={onChoose(topicId.toString())}){Text("使用话题 $topicId")}
        if(state.searching) LinearProgressIndicator(Modifier.fillMaxWidth())
        LazyColumn(Modifier.weight(1f,fill=false).heightIn(max=350.dp)) {items(state.topicResults){topic ->Text(topic.label,Modifier.fillMaxWidth().clickable{onChoose(topic.value)}.padding(vertical=14.dp))}}
        state.topicError?.let { Text(it,color=MaterialTheme.colorScheme.error) }
        if(!state.searching && state.topicError==null && query.isNotBlank() && state.topicResults.isEmpty()) Text("没有匹配的话题")
    }
}
