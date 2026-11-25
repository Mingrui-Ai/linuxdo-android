# LinuxDo 增强版

面向 [LINUX DO](https://linux.do/) 社区的 Android 原生客户端，基于 [xuemk/linuxdo-android](https://github.com/xuemk/linuxdo-android) 继续开发，由 [Mingrui-Ai](https://github.com/Mingrui-Ai) 独立维护，主要优化刷帖、消息聊天和个人主页的使用体验。

> **开发中 · 非官方衍生版本**
>
> 当前开发分支为 [`independent/main`](https://github.com/Mingrui-Ai/linuxdo-android/tree/independent/main)，正在持续迭代和真机调试，尚未发布增强版 APK Release。本 README 介绍该分支的进展，查看或构建增强版源码时请切换到此分支。

原作者已于 **2026 年 11 月 23 日** [明确同意修改、公开维护源码及免费分发衍生 APK](https://github.com/xuemk/linuxdo-android/issues/15#issuecomment-7101351472)。本项目遵循原项目的非商业、无广告授权条款，保留原作者署名、原仓库链接及 [LICENSE](./LICENSE)。

## 当前开发进展

以下改动已纳入增强版开发分支，交互细节仍在完善：

| 方向 | 主要改动 |
| --- | --- |
| 导航与页面切换 | 底部提供“首页、发帖、消息”三个入口，发帖按钮直接打开编辑器；首页左上角头像展开个人侧栏，右上角搜索，保留“最新”“热门”两个分类；底栏横滑切换首页与消息。 |
| 消息与实时聊天 | 通知、频道和直接消息集中在“消息”页；整合实时同步、未读提醒和回复风格的聊天编辑器。通知与聊天统一提供已读、搜索和顶部下拉刷新操作，聊天批量已读保留操作期间新到的消息。 |
| 个人侧栏与详细主页 | 侧栏顶部展示头像、昵称、等级与简介，点击头像进入详细主页，右上双箭头切换账号。收藏、历史、草稿、关注、分类、标签及账号工具集中在侧栏，底部固定设置与深浅色切换；返回时恢复侧栏滚动位置。详细主页默认打开“总结”，保留背景图、简介 emoji、上滑收栏和分类吸顶。 |
| 统计、等级与徽章 | 总结中的八项统计采用带原 emoji 的两排四列布局；社区分与徽章入口放在用户名旁，自己的 Lv 标记可打开等级积分；徽章改为独立网格页，保留筛选、数量与单枚详情。 |
| 帖子阅读与跳楼 | 支持选楼方式切换与右侧边缘滑条，拖动预览、松手定位，目标回复尽量居中显示，并处理删楼造成的楼层缺口。 |
| 资料与交互修复 | 站内用户链接可进入原生个人主页，补齐资料卡和详细主页的简介表情；个人内容列表的分类与时间在右上角对齐、回应使用表情图片，回复显示目标楼层并修正跳转偏移；修复导航页面点击穿透及详细主页背景图缺失等问题。 |
| 联网方式与故障排查 | 在“设置 → 网络与代理”选择内置网络或跟随系统，自动保存选择并显示当前方式；加载失败提示提供“代理设置”直达入口。 |

同时保留上游提供的帖子浏览与搜索、发帖回复、图文编辑与草稿、多账号、收藏与历史、分类管理、浅色/深色主题，以及 DoH / ECH 网络功能。感谢原作者提供的基础实现。

## 正在完善

- 个人主页、消息页的收栏与下拉刷新衔接，以及横滑、返回等手势体验。
- 个人侧栏的真机交互、大字体显示和返回手势体验。
- 聊天批量已读和消息同步的真机验证，以及徽章页在不同屏幕与字体大小下的显示。
- 原生个人资料编辑与背景图上传；目前“编辑个人资料”仍打开网页。

通知搜索当前仅覆盖已加载的记录，可继续加载更早通知后搜索。后台消息到达时效仍受系统休眠、后台限制与网络状况影响。

“跟随系统”使用手机当前网络及系统代理/VPN（如 FlClash）。“内置网络”不使用系统 HTTP 代理，但底层流量仍可能被 Android 的 VPN 接管；如需完全绕过 FlClash，请在 FlClash 中将本 App 排除。切换后可返回原页面重试。

## 获取与构建

目前以本地构建体验为主，后续发布信息以 [本仓库 Releases](https://github.com/Mingrui-Ai/linuxdo-android/releases) 为准。

当前测试包名为 `org.linuxdo.android.debug`，应用名称为“LinuxDo 增强版”，可与上游原版共存。使用同一签名覆盖安装可保留已有测试数据。独立 Release 的包名、更新源与图标配置仍在准备中；当前应用内更新地址仍指向上游，体验增强版请使用本分支构建的 APK。

### 准备环境

- 推荐使用 JDK 21，并配置 `JAVA_HOME`。
- 安装 Android SDK 36 和 Platform-Tools，在本地 `local.properties` 配置 `sdk.dir`；该文件不提交。
- 当前构建面向 Android 8.0 及以上的 `arm64-v8a` 设备。

先获取增强版开发分支：

```powershell
git clone --branch independent/main --single-branch https://github.com/Mingrui-Ai/linuxdo-android.git
cd linuxdo-android
```

### Windows 编译与 USB 安装

```powershell
# 只编译，APK 位于 app/build/outputs/apk/debug/app-debug.apk
.\gradlew.bat :app:assembleDebug

# 编译、覆盖安装并启动；将参数替换为 adb devices 显示的设备序列号
.\run-debug.ps1 -DeviceSerial "设备序列号"

# 日常性能体验：关闭 debuggable，并在设备上执行 ART 预编译
.\run-debug.ps1 -DeviceSerial "设备序列号" -Performance
```

只有一台已授权设备时可省略 `-DeviceSerial`。如需代理，通过脚本参数 `-ProxyUrl` 传入本机 HTTP 代理地址，不写入共享配置。安装使用覆盖方式，保留应用数据；手机上的 USB 调试授权与安装确认需手动允许。

macOS / Linux 可使用 `sh ./gradlew :app:assembleDebug` 编译，生成路径相同。

## 参与开发

欢迎向[本仓库](https://github.com/Mingrui-Ai/linuxdo-android/pulls)提交改进 PR，目标分支为 `independent/main`。提交问题修复时，请说明构建版本或提交号、设备型号、Android 版本和复现步骤；截图与日志请先移除账号凭据及其他隐私信息。

增强版在 `independent/main` 独立迭代，向上游贡献的改动会另行整理为 PR。

## 授权与致谢

本项目沿用原作者 **xuemk** 的 [LinuxDo Android Non-Commercial License](./LICENSE)，完整条款以许可证和[原作者授权回复](https://github.com/xuemk/linuxdo-android/issues/15#issuecomment-7101351472)为准：

- 允许个人学习、交流、非商业修改与公开维护，以及免费编译、分发衍生 APK；再分发仍须遵守相同的非商业、无广告限制。
- 保留原作者署名、原仓库链接及许可证，显著标注非官方衍生版本并说明主要修改。
- 禁止商业用途、收费下载、付费解锁、商业套壳、导流变现，以及植入广告、推广弹窗或未告知用户的追踪代码。
- 分发 APK 前须使用独立的 `applicationId`，修改或关闭指向上游的更新地址，并区分应用名称与图标。
- 仓库附带的文件与组件（包括 `liblinuxdo_doh.so`、`libpush_core.so`、`libtunnel_core.so`）仅随本项目或符合条款的免费衍生版本使用、分发，不单独拆分用于其他项目或商业用途。
- GeckoView、OkHttp、AndroidX、Coil、Jsoup 等第三方依赖继续遵守各自的许可证。

感谢 [xuemk](https://github.com/xuemk) 开发并授权 [LinuxDo Android](https://github.com/xuemk/linuxdo-android)，也感谢 [LINUX DO](https://linux.do/) 社区的建议与测试支持。[原项目社区介绍](https://linux.do/t/topic/2998703)。
