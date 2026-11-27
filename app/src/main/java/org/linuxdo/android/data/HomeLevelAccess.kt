package org.linuxdo.android.data

internal enum class HomeLevelAccess(val label: String, val selectable: Boolean) {
    Visible("可见", true),
    Unverified("待确认", true),
    UpgradeRequired("未解锁", false),
    LoginRequired("需登录", false),
    Unsupported("未开放", false),
}

/** 等级只用于解释菜单状态；网站已返回的可见分类优先，未知等级不当成 Lv0。 */
internal fun homeLevelAccess(
    level: Int,
    signedIn: Boolean,
    trustLevel: Int?,
    categorySupported: Boolean,
    serverVisible: Boolean,
): HomeLevelAccess {
    if (level !in 0..3) return HomeLevelAccess.Unsupported
    if (categorySupported && serverVisible) return HomeLevelAccess.Visible
    if (!signedIn && level > 0) return HomeLevelAccess.LoginRequired
    val knownLevel = trustLevel?.takeIf { it in 0..4 }
    if (signedIn && knownLevel != null && level > knownLevel) return HomeLevelAccess.UpgradeRequired
    if (!categorySupported) return HomeLevelAccess.Unsupported
    return if (signedIn && knownLevel == null) HomeLevelAccess.Unverified else HomeLevelAccess.Visible
}
