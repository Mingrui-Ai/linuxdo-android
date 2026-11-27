package org.linuxdo.android.ui

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.linuxdo.android.data.UserNotificationLevel
import org.linuxdo.android.data.UserNotificationSettings

internal data class UserNotificationState(
    val settings: UserNotificationSettings? = null,
    val loading: Boolean = false,
    val saving: Boolean = false,
    val fresh: Boolean = false,
    val error: String? = null,
)

/** Owned by one profile and login session; old reads cannot overwrite a later read or save. */
internal class UserNotificationController(
    private val read: suspend () -> UserNotificationSettings,
    private val write: suspend (UserNotificationSettings, UserNotificationLevel, String?) -> UserNotificationSettings,
    private val onObserved: (UserNotificationSettings) -> Unit,
    private val nowMillis: () -> Long = { System.nanoTime() / 1_000_000 },
) {
    private val mutableState = MutableStateFlow(UserNotificationState())
    val state = mutableState.asStateFlow()
    private var revision = 0L
    private var syncedAt: Long? = null

    suspend fun load(force: Boolean = false) {
        val before = state.value
        if (before.saving || before.loading) return
        if (!force && before.fresh && syncedAt?.let { nowMillis() - it in 0 until 30_000L } == true) return
        val request = ++revision
        // Reopening the menu is immediate; a background refresh keeps the last known choices usable.
        mutableState.value = before.copy(loading = true, error = null)
        try {
            val settings = read()
            if (request != revision) return
            syncedAt = nowMillis()
            mutableState.value = UserNotificationState(settings = settings, fresh = true)
            onObserved(settings)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            if (request == revision) mutableState.value = state.value.copy(fresh = false, error = failure.message ?: "读取通知设置失败")
        } finally {
            if (request == revision) mutableState.value = state.value.copy(loading = false)
        }
    }

    suspend fun save(level: UserNotificationLevel, expiringAt: String? = null): Boolean {
        val before = state.value
        val settings = before.settings ?: return false
        if (!before.fresh || before.saving || !settings.allows(level)) return false
        if (settings.level == level && level != UserNotificationLevel.Ignore && !before.loading &&
            syncedAt?.let { nowMillis() - it in 0 until 30_000L } == true) return true
        val request = ++revision
        mutableState.value = before.copy(loading = false, saving = true, error = null)
        try {
            val saved = write(settings, level, expiringAt)
            if (request != revision) return false
            syncedAt = nowMillis()
            mutableState.value = UserNotificationState(settings = saved, fresh = true)
            onObserved(saved)
            return true
        } catch (cancelled: CancellationException) {
            if (request == revision) mutableState.value = before.copy(loading = false, fresh = false)
            throw cancelled
        } catch (failure: Exception) {
            if (request == revision) mutableState.value = before.copy(loading = false, fresh = false, error = failure.message ?: "通知设置保存失败")
            return false
        } finally {
            if (request == revision) mutableState.value = state.value.copy(saving = false)
        }
    }
}
