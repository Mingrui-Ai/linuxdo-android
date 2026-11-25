package org.linuxdo.android.ui

import android.app.Application
import android.net.Uri
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import org.linuxdo.android.App
import org.linuxdo.android.data.*
import java.net.URLEncoder

internal object ProfileEditorUpdates {
    private val _revision = MutableStateFlow(0L)
    val revision = _revision.asStateFlow()
    fun changed() { _revision.update { it + 1 } }
}

internal data class ProfileEditorUiState(
    val data: ProfileEditorData? = null,
    val values: Map<String,String> = emptyMap(),
    val loading: Boolean = false,
    val saving: Boolean = false,
    val uploading: String? = null,
    val error: String? = null,
    val notice: String? = null,
    val conflicts: Set<String> = emptySet(),
    val topicResults: List<ProfileChoice> = emptyList(),
    val topicError: String? = null,
    val searching: Boolean = false,
    val sessionExpired: Boolean = false,
    val pendingConfirmation: Map<String,String> = emptyMap(),
    val checkingConfirmation: Boolean = false,
) {
    val dirty: Boolean get() = data?.let { ProfileFields.changes(it.values,values).isNotEmpty() } == true
    val busy: Boolean get() = loading || saving || uploading != null
    val editable: Boolean get() = !busy && !sessionExpired
}

internal class ProfileEditorViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as App).container
    private val repository = container.topicRepository
    private val store = container.sessionStore
    private val _state = MutableStateFlow(ProfileEditorUiState())
    val state = _state.asStateFlow()
    private var userId = 0L
    private var generation = -1L
    private var username = ""
    private var work: Job? = null
    private var search: Job? = null
    private var confirmation: Job? = null
    private var confirmationRevision = 0L
    private var remoteConflict: ProfileEditorData? = null
    private var badgeRoot: JsonObject? = null
    private var epoch = 0L
    private var searchRevision = 0L
    private val path get() = "/u/${URLEncoder.encode(username,"UTF-8")}"

    private fun sessionGuard(): () -> Unit {
        val expectedId=userId; val expectedGeneration=generation; val expectedEpoch=epoch
        return {
            if (epoch != expectedEpoch || store.sessionChanging || store.currentUserId() != expectedId || store.sessionGeneration != expectedGeneration)
                throw CancellationException("账号已切换")
        }
    }

    private fun finish(token: Long, update: (ProfileEditorUiState) -> ProfileEditorUiState) {
        if (epoch != token) return
        _state.update {
            if (sameSession()) update(it) else it.copy(loading=false,saving=false,uploading=null,searching=false,checkingConfirmation=false,
                sessionExpired=true,error="登录会话已变化，请从网页刷新后继续编辑")
        }
    }

    fun open(id: Long, name: String) {
        if (id == userId && name == username && generation == store.sessionGeneration && (_state.value.dirty || _state.value.busy)) return
        stopConfirmation(); epoch++; searchRevision++; work?.cancel(); search?.cancel()
        userId=id; username=name; generation=store.sessionGeneration; badgeRoot=null
        _state.value=ProfileEditorUiState()
        reload()
    }

    private suspend fun load(guard: () -> Unit): ProfileEditorData {
        guard()
        val root=repository.profileEditorRead("$path.json",userId,generation)
        guard()
        require(root.obj("user").text("id").toLongOrNull() == userId) { "账号资料不匹配，请重新打开" }
        val badges=badgeRoot ?: repository.profileEditorRead("/user-badges/${URLEncoder.encode(username,"UTF-8")}.json",userId,generation).also { guard(); badgeRoot=it }
        guard()
        return ProfileFields.parse(root,badges)
    }

    fun reload() {
        if (_state.value.busy) return
        if (store.sessionChanging || store.currentUserId() != userId) {
            _state.update { it.copy(sessionExpired=true,error="账号已变化，请关闭后重新打开") }
            return
        }
        stopConfirmation()
        generation=store.sessionGeneration
        badgeRoot=null; remoteConflict=null
        val token=epoch
        val guard=sessionGuard()
        work=viewModelScope.launch {
            _state.update { it.copy(loading=true,error=null,conflicts=emptySet()) }
            try {
                val loaded=load(guard); guard()
                _state.value=ProfileEditorUiState(data=loaded,values=loaded.values)
                publish(loaded,guard)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { finish(token) { it.copy(error=e.message ?: "资料加载失败") } }
            finally { finish(token) { it.copy(loading=false) } }
        }
    }

    private fun sameSession() = store.currentUserId()==userId && store.sessionGeneration==generation && !store.sessionChanging
    fun edit(key: String, value: String) {
        val s=_state.value
        if (!s.editable || key !in s.data?.permissions.orEmpty()) return
        _state.update { it.copy(values=it.values+(key to value),notice=null,error=null,conflicts=emptySet()) }
    }

    fun acceptRemote() {
        val remote=remoteConflict ?: return
        stopConfirmation()
        _state.update { it.copy(data=remote,values=remote.values+ProfileFields.changes(it.data!!.values,it.values).filterKeys { key -> key !in it.conflicts },pendingConfirmation=it.pendingConfirmation-it.conflicts,conflicts=emptySet(),notice="已采用网页版本，其他修改仍保留") }
        remoteConflict=null
    }

    fun keepEdits() {
        val remote=remoteConflict ?: return
        stopConfirmation()
        _state.update { it.copy(data=remote,values=remote.values+ProfileFields.changes(it.data!!.values,it.values),pendingConfirmation=it.pendingConfirmation-it.conflicts,conflicts=emptySet(),notice="已保留本次修改，请再次保存") }
        remoteConflict=null
    }

    fun save() {
        val original=_state.value
        val base=original.data ?: return
        if (!original.editable || !original.dirty) return
        val dirty=ProfileFields.changes(base.values,original.values)
        if (dirty.all { (key,value) -> original.pendingConfirmation[key]==value }) {
            checkPendingSave()
            return
        }
        stopConfirmation()
        val token=epoch
        val guard=sessionGuard()
        work=viewModelScope.launch {
            _state.update { it.copy(saving=true,error=null,notice=null) }
            var attempted=false
            val edited=original.values
            val changes=ProfileFields.changes(base.values,edited)
            // Keep successful writes separate from failed or never-dispatched fields.
            val accepted=original.pendingConfirmation.filter { (key,value) -> changes[key]==value }.keys.toMutableSet()
            fun remainingChanges(saved: ProfileEditorData) = changes.filter { (key,value) ->
                !ProfileFields.equivalent(key,value,saved.values[key].orEmpty())
            }
            fun acceptVerified(saved: ProfileEditorData) {
                guard(); publish(saved,guard)
                val remaining=remainingChanges(saved)
                val confirmed=changes.keys-remaining.keys
                val pending=remaining.filterKeys { it in accepted }
                val unresolved=remaining.keys-pending.keys
                _state.value=ProfileEditorUiState(data=saved,values=saved.values+remaining,pendingConfirmation=pending,
                    notice=when {
                        remaining.isEmpty() -> "已保存到网页"
                        pending.isNotEmpty() -> "网页已接收修改，正在等待资料更新："+fieldNames(pending.keys)
                        else -> confirmed.takeIf { it.isNotEmpty() }?.let { "已确认保存："+fieldNames(it) }
                    },
                    error=if(unresolved.isEmpty()) null else "以下修改的保存结果暂未确认："+
                        fieldNames(unresolved)+"。修改已保留，请稍后重试。")
            }
            suspend fun verifySaved() {
                var saved=load(guard)
                // A successful write can briefly be followed by an older read. Recheck only
                // unresolved fields, without replaying writes or publishing the stale snapshot.
                for (waitMillis in listOf(600L,1400L)) {
                    if (!attempted || remainingChanges(saved).isEmpty()) break
                    delay(waitMillis); guard()
                    saved=load(guard)
                }
                acceptVerified(saved)
            }
            try {
                ProfileFields.validate(edited,changes.keys)
                val fresh=load(guard)
                val conflicts=ProfileFields.conflicts(base.values,fresh.values,edited).filterNot { key ->
                    original.pendingConfirmation[key]?.let { ProfileFields.equivalent(key,it,fresh.values[key].orEmpty()) } == true
                }.toSet()
                if (conflicts.isNotEmpty()) {
                    remoteConflict=fresh
                    _state.update { it.copy(conflicts=conflicts) }
                    return@launch
                }
                val pending=changes.filter { (key,value) -> key !in accepted && !ProfileFields.equivalent(key,value,fresh.values[key].orEmpty()) }
                require(pending.keys.all { it in fresh.permissions }) { "网页上的编辑权限已变化，请重新加载资料" }
                // Preserve remote changes to untouched fields before starting the independent writes.
                _state.update { it.copy(data=fresh,values=fresh.values+changes) }
                suspend fun put(suffix: String, body: JsonObject, keys: Set<String>) {
                    guard(); attempted=true
                    repository.profileEditorWrite(path+suffix,body,userId,generation)
                    guard(); accepted.addAll(keys)
                    _state.update { it.copy(pendingConfirmation=it.pendingConfirmation+changes.filterKeys { key -> key in keys }) }
                }
                val regular=pending.filterKeys { it !in setOf("avatar","featured_topic","card_badge") }
                if (regular.isNotEmpty()) put(".json",ProfileFields.payload(regular),regular.keys)
                pending["avatar"]?.let { put("/preferences/avatar/pick",ProfileFields.avatarPayload(it),setOf("avatar")) }
                pending["featured_topic"]?.let { id ->
                    put(if (id.isEmpty()) "/clear-featured-topic" else "/feature-topic",buildJsonObject { if (id.isNotEmpty()) put("topic_id",id.toLong()) },setOf("featured_topic"))
                }
                pending["card_badge"]?.let { id ->
                    put("/preferences/card-badge",buildJsonObject { put("user_badge_id",id.toLongOrNull()?.let(::JsonPrimitive) ?: JsonNull) },setOf("card_badge"))
                }
                verifySaved()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                finish(token) { it.copy(error=(if(attempted) "保存结果尚未确认，修改已保留。" else "")+(e.message ?: "保存失败，请重试")) }
                // A failed response may have reached the server. Re-read before deciding what needs retrying.
                if (attempted) try { verifySaved() }
                catch(e: CancellationException) { throw e } catch(_: Exception) { }
            } finally {
                finish(token) { it.copy(saving=false) }
                if (epoch==token && sameSession() && _state.value.pendingConfirmation.isNotEmpty()) checkPendingSave(automatic=true)
            }
        }
    }

    private fun fieldNames(keys: Set<String>) = keys.joinToString("、") { ProfileFields.labels[it].orEmpty() }

    private fun stopConfirmation() {
        confirmationRevision++; confirmation?.cancel(); confirmation=null
        _state.update { it.copy(checkingConfirmation=false) }
    }

    /** Poll only successful-but-not-yet-visible writes; never replay a save in the background. */
    fun checkPendingSave(automatic: Boolean = false) {
        if (!_state.value.editable || _state.value.pendingConfirmation.isEmpty()) return
        stopConfirmation()
        val revision=confirmationRevision
        val token=epoch
        val session=sessionGuard()
        val guard={ session(); if (revision!=confirmationRevision) throw CancellationException("核对已更新") }
        confirmation=viewModelScope.launch {
            _state.update { it.copy(checkingConfirmation=true) }
            try {
                for (waitMillis in if(automatic) listOf(3_000L,6_000L,10_000L) else listOf(0L)) {
                    delay(waitMillis); guard()
                    if (_state.value.pendingConfirmation.isEmpty()) break
                    val saved=load(guard); guard()
                    val current=_state.value
                    val confirmed=current.pendingConfirmation.filter { (key,value) ->
                        ProfileFields.equivalent(key,value,saved.values[key].orEmpty())
                    }.keys
                    if (confirmed.isEmpty()) continue
                    // Merge only confirmed fields; edits made while this read was in flight win.
                    val values=current.values+saved.values.filterKeys { it in confirmed && current.values[it]==current.pendingConfirmation[it] }
                    val baseline=current.data!!.copy(values=current.data.values+saved.values.filterKeys { it in confirmed })
                    val pending=current.pendingConfirmation-confirmed
                    val stillDirty=ProfileFields.changes(baseline.values,values).isNotEmpty()
                    publish(saved,guard)
                    _state.value=current.copy(data=baseline,values=values,pendingConfirmation=pending,
                        error=if(stillDirty) current.error else null,
                        notice=if(pending.isNotEmpty()) "仍在核对："+fieldNames(pending.keys)
                            else if(stillDirty) "先前修改已保存，当前编辑尚未保存" else "已保存到网页")
                    if(pending.isEmpty()) break
                }
                guard()
                if (_state.value.pendingConfirmation.isNotEmpty()) _state.update { it.copy(
                    notice="网页暂未返回最新资料："+fieldNames(it.pendingConfirmation.keys)+"。修改已保留，可核对结果或重新提交。") }
            } catch(e: CancellationException) { throw e }
            catch(_: Exception) {
                if (revision==confirmationRevision) finish(token) { it.copy(notice="暂时无法核对保存结果，修改已保留，联网后可再次核对。") }
            } finally {
                if (revision==confirmationRevision) finish(token) { it.copy(checkingConfirmation=false) }
            }
        }
    }

    fun retryPendingSave() {
        if (!_state.value.editable) return
        stopConfirmation()
        _state.update { it.copy(pendingConfirmation=emptyMap()) }
        save()
    }

    private fun publish(data: ProfileEditorData, guard: () -> Unit) {
        guard()
        AvatarCache.replace(data.username,data.user.animatedAvatar)
        repository.saveCachedUserCard(data.card)
        container.accountManager.updateProfile(data.user)
        store.setSeeSignaturesEnabled(data.userId,data.values["custom:see_signatures"].toBoolean())
        ProfileEditorUpdates.changed()
    }

    fun upload(uri: Uri, field: String) {
        val s=_state.value
        val data=s.data ?: return
        if (!s.editable || field !in data.permissions || (field=="avatar" && "upload_avatar" !in data.permissions)) return
        val uploadType=when(field) { "avatar"->"avatar"; "bio_raw"->"composer"; "profile_background_upload_url"->"profile_background"; "card_background_upload_url"->"card_background"; else->return }
        stopConfirmation()
        val guard=sessionGuard()
        val token=epoch
        work=viewModelScope.launch {
            _state.update { it.copy(uploading=field,error=null,notice=null) }
            try {
                val file=withContext(Dispatchers.IO) {
                    val resolver=getApplication<Application>().contentResolver
                    val bytes=resolver.openInputStream(uri)?.use { input ->
                        val output=java.io.ByteArrayOutputStream(); val buffer=ByteArray(8192)
                        while(true) { val n=input.read(buffer); if(n<0) break; require(output.size()+n<=20*1024*1024) { "图片不能超过 20 MB" }; output.write(buffer,0,n) }; output.toByteArray()
                    } ?: error("无法读取图片")
                    val info=BitmapFactory.Options().apply { inJustDecodeBounds=true }
                    BitmapFactory.decodeByteArray(bytes,0,bytes.size,info)
                    require(info.outWidth>0 && info.outHeight>0) { "请选择有效的图片文件" }
                    val mime=info.outMimeType ?: error("无法识别图片格式")
                    val ext=mapOf("image/png" to "png","image/jpeg" to "jpg","image/gif" to "gif","image/webp" to "webp")[mime] ?: error("请使用 PNG、JPEG、GIF 或 WebP 图片")
                    Triple("profile.$ext",mime,bytes)
                }
                guard()
                val result=repository.uploadFile(file.first,file.second,file.third,uploadType,guard)
                guard()
                val value=when(field) {
                    "avatar" -> "custom:${result.id ?: error("上传响应缺少图片编号")}"
                    "bio_raw" -> _state.value.values[field].orEmpty().trimEnd()+"\n\n![图片](${result.shortUrl})\n"
                    else -> result.url ?: error("上传响应缺少图片地址")
                }
                _state.update { current -> current.copy(values=current.values+(field to value),notice="图片已上传，保存后应用",data=if(field=="avatar") current.data?.copy(avatars=current.data.avatars+ProfileChoice(value,"新上传头像",result.url)) else current.data) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { finish(token) { it.copy(error=e.message ?: "图片上传失败") } }
            finally { finish(token) { it.copy(uploading=null) } }
        }
    }

    fun searchTopics(query: String) {
        val revision=++searchRevision
        search?.cancel()
        if(query.isBlank()) { _state.update{it.copy(topicResults=emptyList(),searching=false,topicError=null)}; return }
        val guard=sessionGuard()
        val token=epoch
        search=viewModelScope.launch {
            _state.update { it.copy(searching=true,topicResults=emptyList(),topicError=null) }
            try {
                delay(400); guard()
                val result=repository.profileEditorRead("/search.json?q="+URLEncoder.encode("${query.trim()} status:public", "UTF-8"),userId,generation)
                guard(); if (revision==searchRevision) _state.update { it.copy(topicResults=result.objects("topics").map { t -> ProfileChoice(t.text("id"),t.text("title")) }) }
            } catch (e: CancellationException) { throw e }
            catch(e: Exception) { if(revision==searchRevision) finish(token) { it.copy(topicError=e.message ?: "话题搜索失败") } }
            finally { if(revision==searchRevision) finish(token) { it.copy(searching=false) } }
        }
    }

    fun close() { stopConfirmation(); epoch++; searchRevision++; work?.cancel(); search?.cancel(); _state.value=ProfileEditorUiState(); badgeRoot=null; remoteConflict=null }
}
