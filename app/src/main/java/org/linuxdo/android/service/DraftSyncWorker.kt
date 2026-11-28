package org.linuxdo.android.service

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.linuxdo.android.App
import org.linuxdo.android.data.DraftMergeAction
import org.linuxdo.android.data.draftMergeAction
import org.linuxdo.android.net.HttpStatusException

/** 只备份已加密保存的内容；任务中仅携带账号和草稿键，退出或进程回收后仍可补同步。 */
class DraftSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.Main.immediate) {
        val userId = inputData.getLong("userId", 0L)
        val key = inputData.getString("draftKey")?.takeIf { it.isNotBlank() }
            ?: return@withContext Result.success()
        val container = (applicationContext as App).container
        val session = container.sessionStore
        val generation = session.sessionGeneration
        fun sameAccount() = userId > 0 && userId == session.currentUserId() &&
            generation == session.sessionGeneration && !session.sessionChanging
        // 前台沿用编辑器边界同步；下次 onStop 会重新登记所有未完成的新话题。
        if (!sameAccount() || session.isAppInForeground()) return@withContext Result.success()
        val drafts = container.accountDraftStore
        val repository = container.topicRepository
        try {
            container.draftSyncLocks.getOrPut(key) { Mutex() }.withLock {
                if (!sameAccount() || session.isAppInForeground()) return@withLock Result.success()
                val snapshot = drafts.get(userId, key)?.takeIf { it.dirty }
                    ?: return@withLock Result.success()
                fun checkSnapshot() {
                    check(sameAccount() && !session.isAppInForeground() && drafts.get(userId, key) == snapshot) {
                        "草稿或编辑会话已有变化，留待下次同步"
                    }
                }
                withTimeout(20_000L) {
                    val remote = repository.fetchDraft(key, ::checkSnapshot)
                    checkSnapshot()
                    when (draftMergeAction(snapshot, remote)) {
                        DraftMergeAction.UseRemote -> Result.success()
                        DraftMergeAction.Conflict -> {
                            // 保留两端版本，不在退出时弹出选择，也不覆盖网页的新修改。
                            drafts.put(userId, snapshot.copy(conflict = true))
                            Result.success()
                        }
                        DraftMergeAction.Unchanged -> {
                            drafts.put(userId, snapshot.copy(base = remote.content, sequence = remote.sequence,
                                remoteData = remote.data, dirty = false, conflict = false))
                            Result.success()
                        }
                        DraftMergeAction.SaveLocal -> {
                            val sequence = if (snapshot.content.empty) {
                                repository.deleteDraft(key, remote.sequence, ::checkSnapshot)
                                repository.fetchDraft(key, ::checkSnapshot).sequence
                            } else repository.saveSyncedDraft(key, remote.sequence, snapshot.content, remote.data, ::checkSnapshot)
                            if (!sameAccount()) return@withTimeout Result.success()
                            val newest = drafts.get(userId, key) ?: return@withTimeout Result.success()
                            // 请求期间的新输入保留，仅确认本次已上传快照的基线。
                            val dirty = newest.content != snapshot.content
                            drafts.put(userId, newest.copy(base = snapshot.content.takeUnless { it.empty }, sequence = sequence,
                                remoteData = remote.data, dirty = dirty, conflict = false))
                            if (dirty) Result.retry() else Result.success()
                        }
                    }
                }
            }
        } catch (_: TimeoutCancellationException) {
            Result.retry()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            if (!sameAccount() || session.isAppInForeground()) Result.success()
            else if (error is HttpStatusException && error.status == 409) Result.success()
            else Result.retry()
        }
    }

    companion object {
        fun enqueue(context: Context, userId: Long, key: String) {
            if (userId <= 0 || key.isBlank()) return
            val work = OneTimeWorkRequestBuilder<DraftSyncWorker>()
                .setInputData(workDataOf("userId" to userId, "draftKey" to key))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            // 不取消已经发出的保存；每次关闭/退后台都保证有一次读取最新本机内容的机会。
            WorkManager.getInstance(context).enqueueUniqueWork(
                "creator-draft-$userId-$key", ExistingWorkPolicy.APPEND_OR_REPLACE, work,
            )
        }
    }
}
