# Pocket Favorites verification

## F-Droid submission / Release 1.0.1 — 2026-11-17

用户授权采用 MIT，已公开 [LICENSE](../LICENSE) 和 en-US / zh-CN Fastlane title、summary、description、changelog。GitLab 账号为 LuuSir，fork 为 [LuuSir/fdroiddata](https://gitlab.com/LuuSir/fdroiddata)，metadata 路径 `metadata/com.example.resouretree.yml`。

### Build and metadata evidence

- 官方 fdroiddata checkout 基于 `194a3c26c77c21a3d9d721ea7145ed91069a2a26`，独立分支 `com.example.resouretree`。metadata 仅添加该应用，使用完整 source commit SHA、版本化 Binaries 和已验证的 AllowedAPKSigningKeys。
- fdroidserver 2.4.5：readmeta / rewritemeta / lint 返回 0；官方 metadata JSON schema 与 LF 检查通过。Windows 环境另有 POSIX config permissions、apksigner 路径查找警告；实际 APK 已用 Android build-tools 单独验证。
- 在独立公开源码副本中实际执行 Linux sed prebuild，移除镜像仓库与 Foojay JDK 下载插件；source scanner fatal errors 为 0，无 scanignore。
- 原 v1.0 无签名重建只有 `META-INF/version-control-info.textproto` 内容不同，记录的是构建前 Git HEAD。因此发布 v1.0.1（versionCode 11），仅关闭 release build type 的 VCS revision 注入、更新版本及 About 文案，不改变 Room / JSON / 应用数据行为。
- v1.0.1 的 Debug / Release / AndroidTest 编译和 168 项本地测试通过；lintDebug / lintRelease 各 0 errors、21 warnings。未再次操作手机界面或安装 1.0.1。
- 再次从不含签名环境变量的子进程构建 unsigned APK，apksigcopier 仅复制公开签名后，与 upstream signed APK 逐字节一致。Windows / JBR 25.0.2 / Gradle 9.5.0 上通过；Linux F-Droid CI 待验证。
- v1.0.1 APK：8,736,104 bytes，SHA256 `17c59e301070bef2a1375ff794b4b45fdaa3808a149491da7105e021bca1a0ef`，历史签名证书保持一致。

### Submission status

GitLab fork 已完成导入，尚未提交最终 metadata 分支及 MR。F-Droid 对新应用开发时长与收录条件的判断留给维护者；本项目首次提交为 2026-09-07，MR 将如实说明，不能提前宣称收录成功。

### Environment and raw evidence

原始记录位于 Git 忽略的 [results](../results/resourcetree/20261117-fdroid-submission-codex/)。下载源先查 sources-current.md；Python 校验工具在项目独立 venv 中从 TUNA 安装成功。GitLab 官方仓库、API 和 F-Droid scanner catalog 用于指定的提交任务，不更改全局代理或信任配置。WSL 仅用于执行隔离的 prebuild；未安装或启用 Docker，未修改系统 Python。GitLab 授权通过隐藏输入交给系统凭据库，Token 未写入项目或日志。

后续操作见 [workflow](../workflows/fdroid.md)。

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
