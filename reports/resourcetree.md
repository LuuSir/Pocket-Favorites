# Pocket Favorites verification

## Release 1.0 — 2026-11-16

应用中文名改为「外置收藏夹」，英文名为 Pocket Favorites。新增「菜单 → 界面语言」：Follow system / 简体中文 / English，默认跟随系统。Android 13+ 使用 LocaleManager 与 localeConfig，Android 8–12 使用偏好保存与 Activity Configuration。README 中英文互链。

Content / Action、Room schema v3、JSON schema v2、包名及签名保持原样。此次只调整文案、语言选择和相应的通知刷新；图布局、拖拽排序、置顶、多选与 AppPicker 保持原有实现。

### Verification

| 检查 | 实际结果 |
| --- | --- |
| assembleDebug / assembleRelease / assembleDebugAndroidTest | 全部通过，最终日志 build-verified.log |
| Unit / Robolectric / Compose | 168 passed，0 failures / errors / skipped；语言存储与资源覆盖 API 26 / 33 / 35；英文页面、切换及草稿恢复覆盖 API 26 |
| lintDebug / lintRelease | 各 0 errors、21 warnings；没有翻译缺失、格式参数或 PluralsCandidate 问题 |
| 语言资源一致性 | English / 中文各 257 个对应的 string / plurals key；英文单复数分别处理 |
| APK preflight | ZIP、包名、versionName 1.0、versionCode 10、非调试属性及历史签名证书全部通过 |
| 真机覆盖安装 | Android 13 已连接手机，从 0.9 同签名覆盖安装 1.0；adb 输出 Success，无卸载或清空数据 |
| 真机 Instrumentation | 9 passed：语言资源 2、独立 in-memory repository 6、包名 1；未启动界面或修改真实收藏数据 |
| 升级前后数据比较 | 只读 snapshot / compare，1 passed；923 nodes、完整树摘要、剪贴板规则摘要、Room schema v3 均一致 |
| 文档 | 中英文 README 互链；本轮文档中的本地链接检查通过 |

APK 为 `PocketFavorites-v1.0.apk`，8,736,193 bytes，SHA256：`78334ddfe40dbb8e7b789b306e6df3cfa616104f8f74d395f6fbf1243ffe3d3a`。证书 SHA256 与旧版一致：`3afc7881c8be9742b3075ce0380b55d7338d86264bfba223f935cfe0aa586b4f`。

初轮回归的旧应用名断言及 Robolectric API 26 signature permission 授权夹具已更新，之后完整套件通过。生产代码保留非导出的语言广播接收器及 signature permission；测试夹具显式模拟 Android 本来会授予的合并权限。

### Publication

- 已公开发布 [Release v1.0](https://github.com/LuuSir/Pocket-Favorites/releases/tag/v1.0)，标题为「外置收藏夹 / Pocket Favorites v1.0」。
- 完整源码和中英文 README 已推送到 `main`；annotated tag `v1.0` 指向提交 `8749c76b054997acadbf4bc3a2a0076926e3348c`。
- Release 附件仅含 `PocketFavorites-v1.0.apk` 与对应 `.sha256`。服务端 size / digest 校验通过；随后通过公开 browser_download_url 下载完整 APK，大小、SHA256、ZIP CRC 与历史签名证书均通过检查，和手机安装包一致。
- 私人 Word、真机数据摘要、日志及本地诊断测试不进入本次源码提交；发布引用可达的 Git object 路径已检查，未包含私人 Word。
- 用户随后将仓库更名为 `LuuSir/Pocket-Favorites`；本地 origin、中英文 README 和发布脚本同步采用新地址，原始下载证据中的旧 URL 保留为历史记录。

原始日志与数据摘要保存在 Git 忽略的 [results directory](../results/resourcetree/20261116-release-1.0-codex/)。目录末尾 `codex` 为此次操作者；后续实验可替换为实际操作者缩写，用户为 `lsf`。

### Environment and sources

Windows / PowerShell 7.6.10，Android Studio JBR 25.0.2，Android SDK 37，Gradle cache 位于 `D:/Android/GradleCache`。本轮使用 `--offline`，未更改依赖或下载源。2026-11-16 读取本机 sources-current.md（2026-11-01 记录）；GitHub 发布与远端同步使用指定仓库和 Windows 原生 Git / Python，按调用绕过失效的本地 Git 代理，不改全局代理、DNS 或证书。fetch、push、ls-remote、GitHub API 上传与公开附件完整下载均完成。本次下载结果仅针对指定 v1.0 附件，不推广为其他 GitHub Release 下载的长期源例外。首次 Git commit 自动审批因额度错误未执行；原审批流程重试恢复后完成提交和推送。

### Remaining acceptance

实际手机交互按用户约定由用户完成。需要检查的行为见 [unsure.md](../unsure.md#u-001)。覆盖安装与只读 Instrumentation 不代替完整界面或长时间后台验收。

## Historical evidence

0.9 及此前的已有验证记录保留在 [BUILD-RESULTS.md](../BUILD-RESULTS.md)，历史文件路径和原始日志不迁移。本报告为后续实验与发布验证的统一汇总；操作步骤位于 [workflows/](../workflows/release.md)，架构位于 [files.md](../files.md)。
