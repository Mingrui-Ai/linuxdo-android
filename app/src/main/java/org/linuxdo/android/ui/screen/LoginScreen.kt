package org.linuxdo.android.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import org.linuxdo.android.ui.LoginUiState
import org.linuxdo.android.ui.design.IosActivityIndicator
import org.linuxdo.android.ui.design.IosTheme

// ── 登录页品牌色板（与「曜石黑底 · 跃升 L+」图标统一，支持浅/深色自适应） ──

private val BrandAmber = Color(0xFFFAB50F)
private val BrandAmberLight = Color(0xFFFFC424)
private val BrandAmberDeep = Color(0xFFE5A000)
private val BrandObsidian = Color(0xFF151517)

private data class LoginPalette(
    val isDark: Boolean,
    val bgTop: Color,
    val bgMid: Color,
    val bgBottom: Color,
    val glowColor: Color,
    val title: Color,
    val subtitle: Color,
    val muted: Color,
    val link: Color,
    val badgeBg: Color,
    val badgeBorder: Color,
    val badgeText: Color,
    val cardBg: Color,
    val cardBorder: Color,
    val cardShadow: Color,
    val segBg: Color,
    val segActiveBg: Color,
    val segActiveText: Color,
    val segInactiveText: Color,
    val fieldBg: Color,
    val fieldFocusedBg: Color,
    val fieldBorder: Color,
    val fieldFocusedBorder: Color,
    val fieldText: Color,
    val fieldPlaceholder: Color,
    val fieldIcon: Color,
    val primaryBgStart: Color,
    val primaryBgEnd: Color,
    val primaryContent: Color,
    val primaryShadow: Color,
    val disabledBg: Color,
    val disabledContent: Color,
    val secondaryBg: Color,
    val secondaryBorder: Color,
    val secondaryContent: Color,
    val codeBtnBg: Color,
    val codeBtnContent: Color,
    val checkActiveBg: Color,
    val checkActiveMark: Color,
    val checkInactiveBorder: Color,
    val closeBg: Color,
    val closeBorder: Color,
    val divider: Color,
    val actionAccent: Color,
)

private val LightLoginPalette = LoginPalette(
    isDark = false,
    bgTop = Color(0xFFFBFBFD),
    bgMid = Color(0xFFF4F5F8),
    bgBottom = Color(0xFFEEF0F5),
    glowColor = Color(0x22FAB50F),
    title = BrandObsidian,
    subtitle = Color(0xFF6E7480),
    muted = Color(0xFF8B92A1),
    link = Color(0xFF4A5160),
    badgeBg = Color(0x22FAB50F),
    badgeBorder = Color(0x52FAB50F),
    badgeText = Color(0xFF9A6700),
    cardBg = Color(0xFFFFFFFF),
    cardBorder = Color(0x12151517),
    cardShadow = Color(0x14000000),
    segBg = Color(0xFFF1F2F6),
    segActiveBg = Color(0xFFFFFFFF),
    segActiveText = BrandObsidian,
    segInactiveText = Color(0xFF6E7480),
    fieldBg = Color(0xFFF7F8FA),
    fieldFocusedBg = Color(0xFFFFFFFF),
    fieldBorder = Color(0xFFE4E7EE),
    fieldFocusedBorder = BrandAmber,
    fieldText = BrandObsidian,
    fieldPlaceholder = Color(0xFF9AA2B1),
    fieldIcon = Color(0xFF9AA2B1),
    primaryBgStart = Color(0xFF232429),
    primaryBgEnd = Color(0xFF121316),
    primaryContent = Color.White,
    primaryShadow = Color(0x38151517),
    disabledBg = Color(0xFFE2E5EC),
    disabledContent = Color(0xFF9AA2B1),
    secondaryBg = Color(0xFFFFFFFF),
    secondaryBorder = Color(0xFFDFE3EC),
    secondaryContent = BrandObsidian,
    codeBtnBg = BrandObsidian,
    codeBtnContent = BrandAmber,
    checkActiveBg = BrandObsidian,
    checkActiveMark = BrandAmber,
    checkInactiveBorder = Color(0xFFCBD2DF),
    closeBg = Color(0xE8FFFFFF),
    closeBorder = Color(0x14151517),
    divider = Color(0xFFE7E9F0),
    actionAccent = Color(0xFFB37800),
)

private val DarkLoginPalette = LoginPalette(
    isDark = true,
    bgTop = Color(0xFF0E0F12),
    bgMid = Color(0xFF14161B),
    bgBottom = Color(0xFF0D0E11),
    glowColor = Color(0x1EFAB50F),
    title = Color(0xFFF5F6F8),
    subtitle = Color(0xFF8E95A5),
    muted = Color(0xFF6B7280),
    link = Color(0xFFC5CBD6),
    badgeBg = Color(0x24FAB50F),
    badgeBorder = Color(0x52FAB50F),
    badgeText = BrandAmber,
    cardBg = Color(0xFF1B1D23),
    cardBorder = Color(0x14FFFFFF),
    cardShadow = Color(0x55000000),
    segBg = Color(0xFF121318),
    segActiveBg = Color(0xFF272A33),
    segActiveText = Color(0xFFFFFFFF),
    segInactiveText = Color(0xFF8E95A5),
    fieldBg = Color(0xFF13151A),
    fieldFocusedBg = Color(0xFF171A21),
    fieldBorder = Color(0xFF2A2E38),
    fieldFocusedBorder = BrandAmber,
    fieldText = Color(0xFFF5F6F8),
    fieldPlaceholder = Color(0xFF636A79),
    fieldIcon = Color(0xFF6C7382),
    primaryBgStart = BrandAmberLight,
    primaryBgEnd = BrandAmberDeep,
    primaryContent = BrandObsidian,
    primaryShadow = Color(0x3DFAB50F),
    disabledBg = Color(0xFF272B35),
    disabledContent = Color(0xFF636A79),
    secondaryBg = Color(0xFF23262F),
    secondaryBorder = Color(0xFF323744),
    secondaryContent = Color(0xFFF5F6F8),
    codeBtnBg = BrandAmber,
    codeBtnContent = BrandObsidian,
    checkActiveBg = BrandAmber,
    checkActiveMark = BrandObsidian,
    checkInactiveBorder = Color(0xFF3E4454),
    closeBg = Color(0x16FFFFFF),
    closeBorder = Color(0x1FFFFFFF),
    divider = Color(0xFF2A2E38),
    actionAccent = BrandAmber,
)

/** 原生登录页面，支持账号密码 / 邮箱验证码双模式、2FA 两步验证与网页授权登录，并自适应浅色与深色主题。 */
@Composable
fun LoginScreen(
    state: LoginUiState,
    onPasswordLogin: (login: String, password: String, secondFactorToken: String?, secondFactorMethod: Int, rememberPassword: Boolean) -> Unit,
    onSendEmailCode: (email: String) -> Unit,
    onEmailCodeLogin: (email: String, token: String, secondFactorToken: String?, secondFactorMethod: Int) -> Unit,
    onDismissError: () -> Unit,
    onResetSecondFactor: () -> Unit = {},
    onOpenWebLogin: (login: String?, password: String?, secondFactorToken: String?, secondFactorMethod: Int, rememberPassword: Boolean) -> Unit = { _, _, _, _, _ -> },
    onStartWebAuth: () -> Unit = {},
    onDismissRiskControl: () -> Unit = {},
    onCancelWebAuth: () -> Unit = {},
    onOpenSavedAccounts: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
) {
    if (onCancel != null) {
        BackHandler(onBack = onCancel)
    }

    val palette = if (IosTheme.colors.isDark) DarkLoginPalette else LightLoginPalette

    // Tab 选择：0=账号密码，1=邮箱验证码
    var tab by rememberSaveable { mutableStateOf(0) }
    var showForgotDialog by rememberSaveable { mutableStateOf(false) }

    // 账号密码 Tab 的状态
    var rememberPassword by rememberSaveable { mutableStateOf(state.rememberPassword) }
    var loginText by rememberSaveable { mutableStateOf(state.savedLogin) }
    var passwordText by rememberSaveable { mutableStateOf(state.savedPassword) }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    // 验证码 Tab 的状态
    var emailText by rememberSaveable { mutableStateOf("") }
    var codeText by rememberSaveable { mutableStateOf("") }

    // 两步验证 (2FA) 状态：1=TOTP 6位动态口令，2=备用恢复码
    var secondFactorText by rememberSaveable { mutableStateOf("") }
    var secondFactorMethod by rememberSaveable { mutableStateOf(1) }
    val secondFactorFocus = remember { FocusRequester() }

    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val backEdgeWidth = with(density) { 24.dp.toPx() }
    val backGestureDistance = with(density) { 80.dp.toPx() }

    LaunchedEffect(state.secondFactorRequired) {
        if (state.secondFactorRequired) {
            runCatching { secondFactorFocus.requestFocus() }
        } else {
            secondFactorText = ""
            secondFactorMethod = 1
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(palette.bgTop, palette.bgMid, palette.bgBottom)))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { focusManager.clearFocus() }
            .pointerInput(onCancel) {
                val cancel = onCancel ?: return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (down.position.x > backEdgeWidth) return@awaitEachGesture

                    var travelled = 0f
                    val start = awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
                        travelled += over
                        change.consume()
                    } ?: return@awaitEachGesture

                    horizontalDrag(start.id) { change ->
                        travelled += change.positionChange().x
                        change.consume()
                    }
                    if (travelled >= backGestureDistance) cancel()
                }
            },
    ) {
        // 顶部右上角克制的琥珀氛围光，呼应新图标的暗金折角语言，同时自然延展到状态栏后方
        Canvas(Modifier.fillMaxSize()) {
            val glowCenter = Offset(size.width * 0.84f, size.height * 0.10f)
            val glowRadius = size.width * 0.62f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(palette.glowColor, Color.Transparent),
                    center = glowCenter,
                    radius = glowRadius,
                ),
                radius = glowRadius,
                center = glowCenter,
            )
        }

        val topInsetModifier = Modifier.windowInsetsPadding(
            WindowInsets.systemBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Top),
        )

        if (onCancel != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .then(topInsetModifier)
                    .zIndex(1f)
                    .padding(start = 20.dp, top = 12.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(palette.closeBg)
                    .border(1.dp, palette.closeBorder, CircleShape)
                    .clickable(onClick = onCancel),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "取消",
                    tint = palette.title,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        onOpenSavedAccounts?.let { openSavedAccounts ->
            Text(
                text = "已存账号",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.actionAccent,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .then(topInsetModifier)
                    .zIndex(1f)
                    .padding(end = 18.dp, top = 16.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = openSavedAccounts)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }

        Column(
            Modifier
                .fillMaxSize()
                .then(topInsetModifier)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(54.dp))

            // 品牌图标徽章
            AppIconBadge(isDark = palette.isDark)

            Spacer(Modifier.height(16.dp))

            // 品牌标题 + 增强版标识
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "LINUX DO",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.4).sp,
                    color = palette.title,
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(palette.badgeBg)
                        .border(1.dp, palette.badgeBorder, RoundedCornerShape(999.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "增强版",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.badgeText,
                    )
                }
            }
            Text(
                text = "新的理想型社区 · 欢迎回来",
                fontSize = 13.sp,
                color = palette.subtitle,
                modifier = Modifier.padding(top = 5.dp),
            )

            Spacer(Modifier.height(24.dp))

            // ── 登录主要卡片容器 ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = if (palette.isDark) 20.dp else 14.dp,
                        shape = RoundedCornerShape(24.dp),
                        spotColor = palette.cardShadow,
                        ambientColor = palette.cardShadow,
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(palette.cardBg)
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(24.dp))
                    .padding(horizontal = 18.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // 胶囊分段切换栏
                LoginTabBar(
                    selected = tab,
                    palette = palette,
                    onSelect = {
                        if (tab != it) {
                            tab = it
                            secondFactorText = ""
                            onResetSecondFactor()
                        }
                    },
                )

                Spacer(Modifier.height(18.dp))

                if (tab == 0) {
                    // ── 账号密码登录 ──
                    LoginField(
                        value = loginText,
                        onValueChange = {
                            loginText = it
                            if (state.secondFactorRequired) onResetSecondFactor()
                        },
                        placeholder = "请输入用户名 / 手机号 / 邮箱",
                        leadingIcon = { UserIcon(color = palette.fieldIcon) },
                        palette = palette,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    )

                    Spacer(Modifier.height(12.dp))

                    LoginField(
                        value = passwordText,
                        onValueChange = {
                            passwordText = it
                            if (state.secondFactorRequired) onResetSecondFactor()
                        },
                        placeholder = "请输入密码",
                        leadingIcon = { LockIcon(color = palette.fieldIcon, holeColor = palette.fieldBg) },
                        palette = palette,
                        trailingContent = {
                            Box(
                                Modifier
                                    .padding(end = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { passwordVisible = !passwordVisible }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = if (passwordVisible) "隐藏" else "显示",
                                    fontSize = 12.sp,
                                    color = if (passwordVisible) palette.actionAccent else palette.subtitle,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = if (state.secondFactorRequired) ImeAction.Next else ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            keyboard?.hide(); focusManager.clearFocus()
                            if (loginText.isNotBlank() && passwordText.isNotBlank()) {
                                onPasswordLogin(
                                    loginText.trim(),
                                    passwordText,
                                    secondFactorText.takeIf { state.secondFactorRequired },
                                    secondFactorMethod,
                                    rememberPassword,
                                )
                            }
                        }),
                    )

                    if (state.secondFactorRequired) {
                        Spacer(Modifier.height(12.dp))
                        SecondFactorSection(
                            value = secondFactorText,
                            onValueChange = { secondFactorText = it },
                            method = secondFactorMethod,
                            onMethodChange = {
                                secondFactorMethod = it
                                secondFactorText = ""
                            },
                            focusRequester = secondFactorFocus,
                            palette = palette,
                            onDone = {
                                keyboard?.hide(); focusManager.clearFocus()
                                if (loginText.isNotBlank() && passwordText.isNotBlank() && secondFactorText.isNotBlank()) {
                                    onPasswordLogin(
                                        loginText.trim(),
                                        passwordText,
                                        secondFactorText.trim(),
                                        secondFactorMethod,
                                        rememberPassword,
                                    )
                                }
                            },
                        )
                    }

                    // 记住密码与忘记密码
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { rememberPassword = !rememberPassword }
                                .padding(vertical = 4.dp, horizontal = 2.dp),
                        ) {
                            RememberCheckBox(checked = rememberPassword, palette = palette)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "记住密码",
                                fontSize = 13.sp,
                                color = palette.title,
                            )
                        }

                        Text(
                            text = "忘记密码？",
                            fontSize = 13.sp,
                            color = palette.link,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showForgotDialog = true }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    val canSubmitPwd = loginText.isNotBlank() &&
                        passwordText.isNotBlank() &&
                        (!state.secondFactorRequired || secondFactorText.isNotBlank()) &&
                        !state.loading

                    LoginButton(
                        text = if (state.secondFactorRequired) "验证并登录" else "登录",
                        loading = state.loading,
                        enabled = canSubmitPwd,
                        palette = palette,
                        onClick = {
                            keyboard?.hide(); focusManager.clearFocus()
                            onPasswordLogin(
                                loginText.trim(),
                                passwordText,
                                secondFactorText.trim().takeIf { state.secondFactorRequired },
                                secondFactorMethod,
                                rememberPassword,
                            )
                        },
                    )
                } else {
                    // ── 邮箱验证码登录 ──
                    LoginField(
                        value = emailText,
                        onValueChange = { emailText = it },
                        placeholder = "请输入邮箱",
                        leadingIcon = { EmailIcon(color = palette.fieldIcon) },
                        palette = palette,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                        ),
                    )

                    Spacer(Modifier.height(12.dp))

                    // 验证码行：输入框 + 发送邮件按钮
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LoginField(
                            value = codeText,
                            onValueChange = {
                                codeText = it
                                if (state.secondFactorRequired) onResetSecondFactor()
                            },
                            placeholder = "粘贴登录链接或 token",
                            leadingIcon = { ShieldIcon(color = palette.fieldIcon) },
                            palette = palette,
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Ascii,
                                imeAction = if (state.secondFactorRequired) ImeAction.Next else ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(onDone = {
                                keyboard?.hide(); focusManager.clearFocus()
                            }),
                        )

                        Spacer(Modifier.width(8.dp))

                        SendCodeButton(
                            loading = state.sendingCode,
                            enabled = emailText.isNotBlank() && !state.sendingCode,
                            palette = palette,
                            onClick = {
                                keyboard?.hide(); focusManager.clearFocus()
                                onSendEmailCode(emailText.trim())
                            },
                        )
                    }

                    if (state.secondFactorRequired) {
                        Spacer(Modifier.height(12.dp))
                        SecondFactorSection(
                            value = secondFactorText,
                            onValueChange = { secondFactorText = it },
                            method = secondFactorMethod,
                            onMethodChange = {
                                secondFactorMethod = it
                                secondFactorText = ""
                            },
                            focusRequester = secondFactorFocus,
                            palette = palette,
                            onDone = {
                                keyboard?.hide(); focusManager.clearFocus()
                                if (emailText.isNotBlank() && codeText.isNotBlank() && secondFactorText.isNotBlank()) {
                                    onEmailCodeLogin(
                                        emailText.trim(),
                                        codeText.trim(),
                                        secondFactorText.trim(),
                                        secondFactorMethod,
                                    )
                                }
                            },
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "验证码将通过邮件发送，可直接粘贴完整登录链接或 /session/email-login/ 后的 token",
                        fontSize = 12.sp,
                        color = palette.subtitle,
                        textAlign = TextAlign.Center,
                        lineHeight = 17.sp,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(18.dp))

                    val canSubmitEmail = emailText.isNotBlank() &&
                        codeText.isNotBlank() &&
                        (!state.secondFactorRequired || secondFactorText.isNotBlank()) &&
                        !state.loading

                    LoginButton(
                        text = if (state.secondFactorRequired) "验证并登录" else "登录",
                        loading = state.loading,
                        enabled = canSubmitEmail,
                        palette = palette,
                        onClick = {
                            keyboard?.hide(); focusManager.clearFocus()
                            onEmailCodeLogin(
                                emailText.trim(),
                                codeText.trim(),
                                secondFactorText.trim().takeIf { state.secondFactorRequired },
                                secondFactorMethod,
                            )
                        },
                    )
                }

                // ── 登录方式分隔线 ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(Modifier.weight(1f).height(1.dp).background(palette.divider))
                    Text(
                        text = "或使用其他方式",
                        fontSize = 12.sp,
                        color = palette.muted,
                    )
                    Box(Modifier.weight(1f).height(1.dp).background(palette.divider))
                }

                // ── 网页授权登录按钮 ──
                WebAuthButton(
                    loading = state.webAuthLoading,
                    enabled = !state.loading && !state.webAuthLoading,
                    palette = palette,
                    onClick = {
                        keyboard?.hide()
                        focusManager.clearFocus()
                        onStartWebAuth()
                    },
                )
            }

            Spacer(Modifier.height(22.dp))

            Text(
                text = "登录凭据仅加密保存在本机",
                fontSize = 12.sp,
                color = palette.muted,
            )

            Spacer(Modifier.height(32.dp))
        }
    }

    // 忘记密码提示对话框
    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = { showForgotDialog = false },
            title = { Text("忘记密码") },
            text = { Text("请在 web 端执行忘记密码操作。\n\n访问 linux.do 网页版，使用忘记密码功能重置后再回来登录。") },
            confirmButton = {
                TextButton(onClick = { showForgotDialog = false }) { Text("知道了") }
            },
        )
    }

    // 错误提示对话框
    if (state.error != null) {
        AlertDialog(
            onDismissRequest = onDismissError,
            title = { Text(if (state.isRiskOrCaptchaBlocked) "安全验证提示" else "登录失败") },
            text = { Text(state.error) },
            confirmButton = {
                if (state.isRiskOrCaptchaBlocked) {
                    TextButton(onClick = {
                        onDismissError()
                        val u = if (tab == 0) loginText.trim() else emailText.trim()
                        val p = if (tab == 0) passwordText else null
                        val sf = secondFactorText.trim().takeIf { state.secondFactorRequired && it.isNotBlank() }
                        onOpenWebLogin(u, p, sf, secondFactorMethod, rememberPassword)
                    }) {
                        Text("进入安全验证登录", fontWeight = FontWeight.Bold, color = palette.actionAccent)
                    }
                } else {
                    TextButton(onClick = onDismissError) { Text("确定") }
                }
            },
            dismissButton = if (state.isRiskOrCaptchaBlocked) {
                { TextButton(onClick = onDismissError) { Text("取消") } }
            } else null,
        )
    }

    // 邮件已发送提示
    if (state.emailSent) {
        AlertDialog(
            onDismissRequest = onDismissError,
            title = { Text("邮件已发送") },
            text = { Text("登录邮件已发送至您的邮箱。\n\n可打开邮件直接复制整条登录链接，粘贴到验证码输入框后点击登录（也可以只粘贴 /session/email-login/ 后面的 token）。") },
            confirmButton = {
                TextButton(onClick = onDismissError) { Text("知道了") }
            },
        )
    }

    // L站安全风控提示对话框（403）
    if (state.showRiskControlDialog) {
        AlertDialog(
            onDismissRequest = onDismissRiskControl,
            title = { Text("安全验证未通过") },
            text = {
                Text(
                    "已尝试在应用内完成人机验证，但站点仍在拦截当前请求。\n\n" +
                        "可稍后重试，或改用网页授权认证完成登录。",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDismissRiskControl()
                    onStartWebAuth()
                }) {
                    Text("网页授权认证", color = palette.actionAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRiskControl) {
                    Text("取消")
                }
            },
        )
    }

    // 网页授权进行中提示
    if (state.webAuthLoading) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("网页授权认证") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                ) {
                    IosActivityIndicator(diameter = 28.dp, color = palette.actionAccent)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        state.webAuthMessage ?: "正在进行网页授权认证...",
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onCancelWebAuth) {
                    Text("取消")
                }
            },
        )
    }
}

// ── 两步验证 (2FA) 输入区域 ──────────────────────────────────────────

@Composable
private fun SecondFactorSection(
    value: String,
    onValueChange: (String) -> Unit,
    method: Int,
    onMethodChange: (Int) -> Unit,
    focusRequester: FocusRequester,
    palette: LoginPalette,
    onDone: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.segBg)
            .border(1.dp, palette.fieldBorder, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (method == 1) "两步验证 · 动态口令 (TOTP)" else "两步验证 · 备用恢复码",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.title,
            )
            Text(
                text = if (method == 1) "改用备用码" else "改用动态口令",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.actionAccent,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onMethodChange(if (method == 1) 2 else 1) }
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        LoginField(
            value = value,
            onValueChange = onValueChange,
            placeholder = if (method == 1) "请输入 6 位身份验证器验证码" else "请输入备用恢复码",
            leadingIcon = { ShieldIcon(color = palette.fieldIcon) },
            palette = palette,
            modifier = Modifier.focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions(
                keyboardType = if (method == 1) KeyboardType.NumberPassword else KeyboardType.Ascii,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
        )
    }
}

// ── 胶囊分段 Tab 切换栏 ──────────────────────────────────────────────

@Composable
private fun LoginTabBar(
    selected: Int,
    palette: LoginPalette,
    onSelect: (Int) -> Unit,
) {
    val tabs = listOf("账号密码", "邮箱验证码")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.segBg)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tabs.forEachIndexed { index, label ->
            val isActive = selected == index
            val textColor by animateColorAsState(
                targetValue = if (isActive) palette.segActiveText else palette.segInactiveText,
                animationSpec = tween(180),
                label = "tab-text-$index",
            )
            val bgColor by animateColorAsState(
                targetValue = if (isActive) palette.segActiveBg else Color.Transparent,
                animationSpec = tween(180),
                label = "tab-bg-$index",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .then(
                        if (isActive) {
                            Modifier.shadow(
                                elevation = if (palette.isDark) 4.dp else 2.dp,
                                shape = RoundedCornerShape(10.dp),
                                ambientColor = palette.cardShadow,
                                spotColor = palette.cardShadow,
                            )
                        } else {
                            Modifier
                        },
                    )
                    .clip(RoundedCornerShape(10.dp))
                    .background(bgColor)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    color = textColor,
                )
            }
        }
    }
}

// ── 输入框 ─────────────────────────────────────────────────────────

@Composable
private fun LoginField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: @Composable () -> Unit,
    palette: LoginPalette,
    modifier: Modifier = Modifier,
    trailingContent: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        placeholder = {
            Text(placeholder, fontSize = 14.sp, color = palette.fieldPlaceholder)
        },
        leadingIcon = { Box(Modifier.padding(start = 4.dp)) { leadingIcon() } },
        trailingIcon = trailingContent?.let { { it() } },
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = palette.fieldText,
            unfocusedTextColor = palette.fieldText,
            unfocusedContainerColor = palette.fieldBg,
            focusedContainerColor = palette.fieldFocusedBg,
            unfocusedBorderColor = palette.fieldBorder,
            focusedBorderColor = palette.fieldFocusedBorder,
            cursorColor = BrandAmber,
        ),
    )
}

// ── 主登录按钮 ───────────────────────────────────────────────────────

@Composable
private fun LoginButton(
    text: String,
    loading: Boolean,
    enabled: Boolean,
    palette: LoginPalette,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(50.dp)
            .shadow(
                elevation = if (enabled) 10.dp else 0.dp,
                shape = RoundedCornerShape(15.dp),
                ambientColor = palette.primaryShadow,
                spotColor = palette.primaryShadow,
            )
            .clip(RoundedCornerShape(15.dp))
            .background(
                if (enabled) {
                    Brush.horizontalGradient(listOf(palette.primaryBgStart, palette.primaryBgEnd))
                } else {
                    Brush.horizontalGradient(listOf(palette.disabledBg, palette.disabledBg))
                },
            )
            .then(if (enabled && !loading) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            IosActivityIndicator(diameter = 22.dp, color = palette.primaryContent)
        } else {
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (enabled) palette.primaryContent else palette.disabledContent,
            )
        }
    }
}

// ── 发送验证码按钮 ─────────────────────────────────────────────────

@Composable
private fun SendCodeButton(
    loading: Boolean,
    enabled: Boolean,
    palette: LoginPalette,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .height(52.dp)
            .width(92.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) palette.codeBtnBg else palette.disabledBg)
            .then(if (enabled && !loading) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            IosActivityIndicator(diameter = 18.dp, color = palette.codeBtnContent)
        } else {
            Text(
                text = "发送邮件",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (enabled) palette.codeBtnContent else palette.disabledContent,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ── App 图标徽章（与增强版桌面图标「曜石黑底 · 跃升 L+」保持一致） ──

@Composable
private fun AppIconBadge(isDark: Boolean) {
    Box(
        Modifier
            .size(82.dp)
            .shadow(
                elevation = if (isDark) 18.dp else 14.dp,
                shape = RoundedCornerShape(23.dp),
                ambientColor = Color(0x2E000000),
                spotColor = Color(0x24000000),
            )
            .clip(RoundedCornerShape(23.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF202126), Color(0xFF111215)),
                ),
            )
            .border(
                width = 1.dp,
                color = if (isDark) Color(0x22FFFFFF) else Color(0x14000000),
                shape = RoundedCornerShape(23.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(72.dp)) {
            val scaleX = size.width / 108f
            val scaleY = size.height / 108f
            scale(scaleX, scaleY, Offset.Zero) {
                // 折角处细微暗金光晕
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x18FAB50F), Color(0x00FAB50F)),
                        center = Offset(72f, 70f),
                        radius = 24f,
                    ),
                    radius = 24f,
                    center = Offset(72f, 70f),
                )
                // 纯白几何 L 主体（保留经典 45° 斜切比例）
                val letterL = Path().apply {
                    moveTo(31.5f, 29.5f)
                    quadraticTo(31.5f, 27f, 34f, 27f)
                    lineTo(42.5f, 27f)
                    quadraticTo(45f, 27f, 45f, 29.5f)
                    lineTo(45f, 63f)
                    lineTo(67.5f, 63f)
                    lineTo(47f, 81f)
                    lineTo(34f, 81f)
                    quadraticTo(31.5f, 81f, 31.5f, 78.5f)
                    close()
                }
                drawPath(letterL, Color.White)
                // 琥珀黄几何折角
                val cornerFold = Path().apply {
                    moveTo(52f, 81f)
                    lineTo(74.5f, 60.2f)
                    quadraticTo(76.5f, 58.5f, 76.5f, 61.6f)
                    lineTo(76.5f, 72f)
                    quadraticTo(76.5f, 81f, 67.2f, 81f)
                    close()
                }
                drawPath(cornerFold, BrandAmber)
                // 右上角增强四芒星加号
                val enhancedSpark = Path().apply {
                    moveTo(64.5f, 26.5f)
                    cubicTo(66.2f, 34.4f, 68.1f, 36.3f, 76f, 38f)
                    cubicTo(68.1f, 39.7f, 66.2f, 41.6f, 64.5f, 49.5f)
                    cubicTo(62.8f, 41.6f, 60.9f, 39.7f, 53f, 38f)
                    cubicTo(60.9f, 36.3f, 62.8f, 34.4f, 64.5f, 26.5f)
                    close()
                }
                drawPath(enhancedSpark, BrandAmber)
            }
        }
    }
}

// ── 小图标（Canvas 手绘，不依赖 Material Icons） ──────────────────

/** 用户图标 */
@Composable
private fun UserIcon(color: Color) {
    Canvas(Modifier.size(18.dp)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        drawCircle(color, radius = size.width * 0.22f, center = Offset(cx, cy * 0.7f))
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(size.width * 0.1f, cy * 0.9f),
            size = Size(size.width * 0.8f, size.height * 0.6f),
            style = Fill,
        )
    }
}

/** 锁图标 */
@Composable
private fun LockIcon(color: Color, holeColor: Color) {
    Canvas(Modifier.size(18.dp)) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.15f, h * 0.45f),
            size = Size(w * 0.7f, h * 0.5f),
            cornerRadius = CornerRadius(w * 0.12f),
        )
        drawCircle(holeColor, w * 0.1f, Offset(w / 2f, h * 0.67f))
        drawArc(
            color = color,
            startAngle = 200f,
            sweepAngle = -220f,
            useCenter = false,
            topLeft = Offset(w * 0.25f, h * 0.08f),
            size = Size(w * 0.5f, h * 0.5f),
            style = Stroke(width = w * 0.13f),
        )
    }
}

/** 邮件图标 */
@Composable
private fun EmailIcon(color: Color) {
    Canvas(Modifier.size(18.dp)) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.05f, h * 0.2f),
            size = Size(w * 0.9f, h * 0.6f),
            cornerRadius = CornerRadius(w * 0.1f),
            style = Stroke(width = w * 0.1f),
        )
        val path = Path().apply {
            moveTo(w * 0.05f, h * 0.25f)
            lineTo(w * 0.5f, h * 0.55f)
            lineTo(w * 0.95f, h * 0.25f)
        }
        drawPath(path, color, style = Stroke(width = w * 0.1f))
    }
}

/** 盾牌图标（验证码） */
@Composable
private fun ShieldIcon(color: Color) {
    Canvas(Modifier.size(18.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.05f)
            lineTo(w * 0.95f, h * 0.25f)
            lineTo(w * 0.95f, h * 0.6f)
            cubicTo(w * 0.95f, h * 0.85f, w * 0.5f, h * 0.97f, w * 0.5f, h * 0.97f)
            cubicTo(w * 0.5f, h * 0.97f, w * 0.05f, h * 0.85f, w * 0.05f, h * 0.6f)
            lineTo(w * 0.05f, h * 0.25f)
            close()
        }
        drawPath(path, color)
    }
}

// ── 网页授权登录相关组件 ──────────────────────────────────────────

/** 地球/网页图标 */
@Composable
private fun GlobeIcon(
    modifier: Modifier = Modifier.size(18.dp),
    color: Color,
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val strokeW = 1.5.dp.toPx()
        val radius = (w.coerceAtMost(h) / 2f) - strokeW / 2f
        val cx = w / 2f
        val cy = h / 2f

        drawCircle(
            color = color,
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(width = strokeW),
        )
        drawLine(
            color = color,
            start = Offset(cx - radius, cy),
            end = Offset(cx + radius, cy),
            strokeWidth = strokeW,
        )
        drawLine(
            color = color,
            start = Offset(cx, cy - radius),
            end = Offset(cx, cy + radius),
            strokeWidth = strokeW,
        )
        drawOval(
            color = color,
            topLeft = Offset(cx - radius * 0.52f, cy - radius),
            size = Size(radius * 1.04f, radius * 2f),
            style = Stroke(width = strokeW),
        )
    }
}

/** 网页授权登录次级按钮 */
@Composable
private fun WebAuthButton(
    loading: Boolean,
    enabled: Boolean,
    palette: LoginPalette,
    onClick: () -> Unit,
) {
    val contentColor = if (enabled) palette.secondaryContent else palette.disabledContent
    Box(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(15.dp))
            .border(
                width = 1.dp,
                color = if (enabled) palette.secondaryBorder else palette.divider,
                shape = RoundedCornerShape(15.dp),
            )
            .background(palette.secondaryBg)
            .then(if (enabled && !loading) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IosActivityIndicator(diameter = 18.dp, color = contentColor)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "正在授权...",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = contentColor,
                )
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                GlobeIcon(color = contentColor)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "网页授权登录",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                )
            }
        }
    }
}

@Composable
private fun RememberCheckBox(checked: Boolean, palette: LoginPalette) {
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(if (checked) palette.checkActiveBg else Color.Transparent)
            .border(
                width = 1.4.dp,
                color = if (checked) palette.checkActiveBg else palette.checkInactiveBorder,
                shape = RoundedCornerShape(5.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Canvas(modifier = Modifier.size(11.dp)) {
                val path = Path().apply {
                    moveTo(size.width * 0.15f, size.height * 0.52f)
                    lineTo(size.width * 0.42f, size.height * 0.82f)
                    lineTo(size.width * 0.88f, size.height * 0.22f)
                }
                drawPath(
                    path = path,
                    color = palette.checkActiveMark,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }
        }
    }
}
