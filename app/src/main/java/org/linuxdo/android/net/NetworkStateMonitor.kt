package org.linuxdo.android.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NetworkSecurityState(
    val isVpn: Boolean = false,
    val isProxy: Boolean = false,
) {
    val summary: String get() = when {
        isVpn && isProxy -> "系统 VPN 和 HTTP 代理已开启"
        isVpn -> "系统 VPN 已开启"
        isProxy -> "系统 HTTP 代理已开启"
        else -> "未检测到系统 VPN 或 HTTP 代理"
    }
}

/**
 * 网络安全状态监听器（感知 VPN 与系统代理的开启/关闭）。
 *
 * 只报告环境变化并通知连接池刷新；不更改用户选择的联网方式。
 */
class NetworkStateMonitor(
    context: Context,
    private val onStateChanged: (NetworkSecurityState) -> Unit = {}
) {
    companion object {
        private const val TAG = "NetworkStateMonitor"
    }

    private val connectivity = context.getSystemService(ConnectivityManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var lastDetectedState = NetworkSecurityState()
    private val _state = MutableStateFlow(detect())
    val state = _state.asStateFlow()

    private val refreshTask = Runnable {
        val newState = detect()
        if (newState != _state.value) {
            Log.i(TAG, "网络安全状态变更: 旧 ${_state.value} → 新 $newState")
            _state.value = newState
            onStateChanged(newState)
        }
    }

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { scheduleRefresh() }
        override fun onLost(network: Network) { scheduleRefresh() }
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { scheduleRefresh() }
        override fun onLinkPropertiesChanged(network: Network, properties: android.net.LinkProperties) { scheduleRefresh() }
    }

    @Volatile private var lastDefaultNetwork: Network? = null
    var onDefaultNetworkChanged: (() -> Unit)? = null

    private val defaultNetworkChangedTask = Runnable {
        onDefaultNetworkChanged?.invoke()
    }

    private val defaultNetworkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            val previous = lastDefaultNetwork
            lastDefaultNetwork = network
            scheduleRefresh()
            if (previous != null && previous != network) {
                Log.i(TAG, "默认网络切换: $previous -> $network")
                handler.removeCallbacks(defaultNetworkChangedTask)
                handler.post(defaultNetworkChangedTask)
            }
        }

        override fun onLost(network: Network) {
            scheduleRefresh()
        }
    }

    init {
        runCatching {
            val request = NetworkRequest.Builder()
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                .build()
            connectivity?.registerNetworkCallback(request, callback)
            connectivity?.registerDefaultNetworkCallback(defaultNetworkCallback)
        }.onFailure {
            Log.w(TAG, "注册网络回调失败: ${it.message}")
        }
    }

    private fun scheduleRefresh() {
        handler.removeCallbacks(refreshTask)
        handler.post(refreshTask)
    }

    @Synchronized
    fun detect(): NetworkSecurityState {
        val cm = connectivity
        // 其他 UID 使用的 VPN 不代表本 App 的出口；状态暂不可读时避免切换传输策略。
        val isVpn = if (cm == null) null else runCatching {
            val active = cm.activeNetwork
            active?.let { cm.getNetworkCapabilities(it) }
                ?.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        }.onFailure {
            Log.w(TAG, "读取应用默认网络失败，保留上次 VPN 状态: ${it.javaClass.simpleName}")
        }.getOrNull()

        val isProxy = if (cm == null) null else runCatching {
            cm.defaultProxy != null
        }.onFailure {
            Log.w(TAG, "读取系统代理失败，保留上次代理状态: ${it.javaClass.simpleName}")
        }.getOrNull()

        return NetworkSecurityState(
            isVpn = isVpn ?: lastDetectedState.isVpn,
            isProxy = isProxy ?: lastDetectedState.isProxy,
        ).also { lastDetectedState = it }
    }
}
