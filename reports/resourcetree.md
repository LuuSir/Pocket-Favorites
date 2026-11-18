# Pocket Favorites verification

## F-Droid CI repair — 2026-11-18

[Pipeline 3077393364](https://gitlab.com/fdroid/fdroiddata/-/pipelines/3077393364) 由维护者 linsui 在更新 MR 分支后触发，实际运行并失败。失败有两项：`fdroid rewritemeta` 要求长 Binaries URL 折行；`fdroid build` 的 gradlew-fdroid 只识别官方 Gradle distributionUrl，腾讯镜像 URL 导致 `No suitable gradle version found`，尚未进入 Gradle 编译。其余 schema / lint / source checks 均成功，APK check 因 build 失败而跳过。

### Repair and local checks

- 从维护者更新后的 `382d8adbdb3e2aaf71e518c2c1e211a9f33069a5` 创建新的本地修复分支，没有覆盖旧本地分支或改写远端历史。仅修改 metadata，commit `819b3007c59264681f46bd54d12b092d0431d24f` 已正常 push 到同一 [MR !57711](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/57711)。
- 用 CI 相同 fdroidserver source commit `fc2c86473e66a44fc641f403c338a8b2cccfad88` 与 `ruamel.yaml==0.18.10` 校验；原本 PyPI 2.4.5 / ruamel.yaml 0.17.21 的结果与该 CI formatter 有差异。项目 venv 已对齐源码和依赖，pip check、readmeta、rewritemeta、lint、官方 JSON schema 全部通过。重复 rewritemeta 后的 LF bytes 不变；长 URL 后的空格为官方 formatter 输出。
- prebuild 在 F-Droid 源码副本将 wrapper URL 从腾讯镜像改成 services.gradle.org。实际 WSL bash 运行两条 prebuild 成功，官方版本解析函数从无法识别变为 `9.5.0`；Gradle 版本和原 SHA256 `553c78f50dafcd54d65b9a444649057857469edf836431389695608536d6b746` 保持不变。
- 源码固定为原 `c63290ee5bdf58fd3a1f1810649548b9a513311b`，Binaries URL、签名证书和 1.0.1 / 11 不变。第一轮修复未修改应用源码、已发布 APK 或本机数据；后续 clean-build 版本单独验证。

### New CI

[Pipeline 3082312584](https://gitlab.com/LuuSir/fdroiddata/-/pipelines/3082312584) 已自动启动，未再次受到账号验证限制。CI 的 rewritemeta、lint、schema、source code checks 已成功；Linux build 已识别并运行 Gradle 9.5.0。Linux build 成功生成 APK，但 Binaries 比较失败；差异只在 classes2.dex 与关联 baseline.prof。按方法比较发现 9 个 Compose stability 相关生成方法不同。

随后在全新 Windows 源码目录关闭 Kotlin / KSP incremental 与 build cache，原 1.0.1 unsigned APK 与 Linux CI 产物逐字节一致：8,727,912 bytes，SHA256 `619e39fc5c7c4c57cff25e93526e32aa9b1d482f9029e460cb4526d25f202c68`。确认原发布包使用了旧增量编译输出，Windows 原缓存下 signed / unsigned 的一致性不足以覆盖 clean Linux build。1.0.2 / 12 已完成干净构建：Debug / Release / AndroidTest 编译、168 项本地测试全部通过；Debug / Release lint 各 0 errors、21 warnings。AndroidTest 未在设备运行，本轮没有安装或改动真机数据。正式 APK 8,736,104 bytes，SHA256 `136bc05ca0017f8439b2cf9b64da61e0793fd4130ce1813687dde406726196e2`，包名、非调试属性、ZIP 和历史签名证书通过验证。旧 Release 保留；新版本将用于下一轮 CI 比较。

### Final APK gate and signing decision

[1.0.2 Release](https://github.com/LuuSir/Pocket-Favorites/releases/tag/v1.0.2) 已公开，原签名和旧 Release 保留；公开 API 完整下载的大小、SHA256、ZIP CRC 与本地一致。源码 tag 固定为 `aadd471839d1da33b00dc582cab1703807f5c28b`，fdroiddata metadata commit 为 `0eebd673f54ad303d7f7419963414af517522d0c`。

[Pipeline 3082347377](https://gitlab.com/LuuSir/fdroiddata/-/pipelines/3082347377) 为 8 success / 1 failed。Linux build、公开 Binaries 签名复制比较已成功，日志明确为 `compared built binary to supplied reference binary successfully`；最终 check apk 拒绝 upstream signed APK 的 Dependency metadata signing block 和 `C=US, O=Android, CN=Android Debug` 证书。未宣称整条 pipeline 通过。

已在上游 Gradle DSL 中加入 `dependenciesInfo { includeInApk = false; includeInBundle = false }`，assembleRelease 返回 0，并实际解析重建签名块确认不再含 `0x504b4453`；已发布的 1.0.2 附件不变。此配置修复准备供后续 F-Droid 构建使用。

签名分发策略待用户确认：建议 GitHub 保持原签名，F-Droid 移除 Binaries / AllowedAPKSigningKeys 后使用自身签名。两渠道不能直接相互覆盖；现有用户可留在 GitHub 渠道。另一个选择是统一迁移正式证书，也会影响旧安装覆盖更新。本轮未生成新密钥、换签名或卸载手机应用。

切换渠道的数据验收必须包括媒体：DocumentStore 将选定文件复制到 app-private `files/media`，JSON 仅保存相对路径，单独导出 JSON 不能备份媒体。任何卸载前需要先实现或完成可验证的 JSON 与媒体备份及恢复；当前用户只需选择分发策略，不应据此卸载应用。见 [remaining work](../unsure.md#u-002)。

原始日志与本地校验位于 [results](../results/resourcetree/20261118-fdroid-ci-codex/)。Python 工具依赖从 TUNA 安装，CI 工具源码和 pipeline 日志使用官方 GitLab；保留 TLS 校验，没有修改全局代理或证书。Gradle 官方 URL 仅用于 F-Droid 构建副本的版本识别与下载，本机项目仍保留既有镜像配置。后续方法见 [workflow](../workflows/fdroid.md)。

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

- 已创建公开 [fork](https://gitlab.com/LuuSir/fdroiddata)，在不受保护的 `com.example.resouretree` 分支提交 metadata，commit `ddceadf019f47f338c0a9a7cdf92c35d2032f0ad`。
- 已提交官方上游 [MR !57711 — New app: Pocket Favorites](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/57711)，状态 opened，目标 master。API 核对差异只新增 `metadata/com.example.resouretree.yml`，构建版本 1.0.1 / 11，固定源码 `c63290ee5bdf58fd3a1f1810649548b9a513311b`。
- [Release v1.0.1](https://github.com/LuuSir/Pocket-Favorites/releases/tag/v1.0.1) 已公开，APK 和 SHA256 已上传并核对服务端 digest。通过公开 GitHub release asset API 完整下载 8,736,104 bytes，SHA256 和 ZIP CRC 再次验证一致。
- fork pipeline `3077070876` 和 [MR pipeline `3077089377`](https://gitlab.com/LuuSir/fdroiddata/-/pipelines/3077089377) 都在创建作业前失败。GraphQL 明确返回 `The pipeline failed due to the user not being verified.`；并非已观测到的 metadata / Linux build 失败。按 F-Droid 官方模板在 MR 中请求维护者触发上游 CI，未提供支付信息或进行手机验证。
- F-Droid 对新应用开发时长与收录条件的判断留给维护者；首次源码提交为 2026-09-07，尚未满三个月，MR 如实说明作者真机使用，未虚构更多用户或提前宣称收录成功。后续限制见 [unsure.md](../unsure.md#u-002)。

### Environment and raw evidence

原始记录位于 Git 忽略的 [results](../results/resourcetree/20261117-fdroid-submission-codex/)。下载源先查 sources-current.md；Python 校验工具在项目独立 venv 中从 TUNA 安装成功。GitLab 官方仓库、API 和 F-Droid scanner catalog 用于指定的提交任务，不更改全局代理或信任配置。Windows urllib 对 GitLab 发生过 TLS EOF，切换 requests 的复用 HTTPS 会话后完成 MR 创建，保留证书验证。GitHub 浏览器下载路径受现有路由影响：Windows TLS 失败，WSL curl 60 秒取得部分数据后超时；随后公开 GitHub asset API 完整下载成功。GitHub 路由沿用已授权的 Scholar 环境（网关终止该路径的 TLS），没有新增信任证书，也未关闭校验；本次成功仅限定该客户端、API 路径和附件。GitLab 授权通过隐藏输入交给系统凭据库，Token 未写入项目或日志。未安装或启用 Docker，未修改系统 Python。

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
