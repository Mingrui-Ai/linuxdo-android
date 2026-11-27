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
) {
    private val mutableState = MutableStateFlow(UserNotificationState())
    val state = mutableState.asStateFlow()
    private var revision = 0L

    suspend fun load() {
        if (state.value.saving) return
        val request = ++revision
        mutableState.value = state.value.copy(loading = true, fresh = false, error = null)
        try {
            val settings = read()
            if (request != revision) return
            mutableState.value = UserNotificationState(settings = settings, fresh = true)
            onObserved(settings)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            if (request == revision) mutableState.value = state.value.copy(error = failure.message ?: "读取通知设置失败")
        } finally {
            if (request == revision) mutableState.value = state.value.copy(loading = false)
        }
    }

    suspend fun save(level: UserNotificationLevel, expiringAt: String? = null): Boolean {
        val before = state.value
        val settings = before.settings ?: return false
        if (!before.fresh || before.loading || before.saving || !settings.allows(level)) return false
        val request = ++revision
        mutableState.value = before.copy(saving = true, error = null)
        try {
            val saved = write(settings, level, expiringAt)
            if (request != revision) return false
            mutableState.value = UserNotificationState(settings = saved, fresh = true)
            onObserved(saved)
            return true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            if (request == revision) mutableState.value = before.copy(fresh = false, error = failure.message ?: "通知设置保存失败")
            return false
        } finally {
            if (request == revision) mutableState.value = state.value.copy(saving = false)
        }
    }
}
