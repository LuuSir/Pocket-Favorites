# F-Droid submission

## Prerequisites

公开的上游源码与自由软件许可证，GitLab 账号，Git Credential Manager，Python 与 fdroidserver。应用包名为 `com.example.resouretree`。应用介绍在上游 [Fastlane](../fastlane/metadata/android/)，只把 build metadata 提交到 fdroiddata。

本机 PowerShell 根目录为 `D:/Android/Projects/ResoureTree`。GitLab 需要授权时，创建短期 `api` scope token，再在本机运行：

```powershell
& .\scripts\authorize_gitlab.ps1 -Username LuuSir
```

脚本使用隐藏输入和系统凭据库，预期输出 `GitLab authorization ready for LuuSir.`。不将 Token 写进项目、命令行参数或聊天记录。授权设置和参数见 [script help](../scripts/authorize_gitlab.ps1)。提交结束后可从 GitLab Personal access tokens 页面撤销短期 Token。

## Prepare and check

1. 从 [fdroiddata](https://gitlab.com/fdroid/fdroiddata) fork 到自己的 GitLab 账号；本机在 `build/fdroiddata` 做独立 checkout，创建 `com.example.resouretree` 分支，保持 master 不变。
2. 编写 `metadata/com.example.resouretree.yml`。使用官方 [Build Metadata Reference](https://f-droid.org/en/docs/Build_Metadata_Reference/)，固定完整 source commit SHA，`subdir: app`、Gradle release。需要沿用 upstream 签名时，`Binaries` 指向公开的版本化 APK，`AllowedAPKSigningKeys` 限定正式证书。先检查 certificate DN；Android Debug 证书被 check apk 拒收。若由 F-Droid 独立签名，应移除这两个字段，并明确两渠道不能相互覆盖。
3. prebuild 只对 F-Droid 构建副本移除 Aliyun / Huawei Maven mirrors 和 Foojay JDK resolver，并把腾讯 Gradle distributionUrl 改为 `https://services.gradle.org/distributions/`；保留 Gradle 9.5.0、distributionSha256Sum 和官方 Google Maven / Maven Central / Gradle Plugin Portal。gradlew-fdroid 只识别官方 distributionUrl，镜像 URL 会导致 `No suitable gradle version found`。不要用 scanignore 绕过扫描。
4. 从 CI job trace 核对 fdroidserver source commit 与 ruamel.yaml 版本，在项目独立 venv 中安装匹配工具。2026-11-18 复现使用 `fc2c86473e66a44fc641f403c338a8b2cccfad88` 与 `ruamel.yaml==0.18.10`，依赖从 TUNA 安装；PyPI fdroidserver 2.4.5 不是该 CI 源码版本。然后在 fdroiddata checkout 中依次执行：

   ```text
   fdroid readmeta
   fdroid rewritemeta com.example.resouretree
   fdroid lint com.example.resouretree
   ```

   用 `schemas/metadata.json` 验证 YAML，并确认 metadata 保存为 LF。在 Windows 上 rewritemeta 后将 CRLF 转为 LF，再重复执行一次并比较规范化后的 bytes 应无变化；保留 formatter 对长 Binaries URL 的折行格式。Windows 运行校验工具时的 POSIX permissions / apksigner 查找警告需与应用 metadata 问题区分；实际签名由 Android build-tools 单独验证。
5. 使用 upstream signed APK 和不配置签名的重建 APK 做 apksigcopier 比较。期望复制公开签名后 SHA256 完全一致。Release build type 使用 `vcsInfo.include = false`，避免构建 checkout 的 Git revision 被写入 APK；不需要向 F-Droid 提供私钥。
6. 提交 metadata 单个文件，push 到公开 fork 的独立分支。检查 GitLab pipeline；若账号被要求付费或提供信用卡以运行 CI，按 F-Droid 的模板在 MR 说明，请维护者触发，不设置支付信息。
7. 按官方 Default MR template 提交 `New app: Pocket Favorites`，目标 `fdroid/fdroiddata:master`。仅勾选已实际完成的检查。说明项目开发时间与用户使用情况，等待维护者审核；MR 创建不等于已收录到 F-Droid。

## Evidence and troubleshooting

原始日志放在 `results/resourcetree/<YYYYMMDD>-fdroid-submission-<operator>/`，操作者示例 `codex`，用户使用 `lsf`。metadata 的唯一正式版本位于 fdroiddata fork；不在 reports 复制源码。结果写入 [canonical report](../reports/resourcetree.md)。

| 问题 | 检查入口 |
| --- | --- |
| 无 GitLab 授权 | 本机运行 authorize_gitlab.ps1，核对用户名与 Token scope / expiry |
| metadata 检查失败 | 官方 schema、rewritemeta 输出和 lint 日志；核对 CI 的 fdroidserver commit、ruamel.yaml 版本与 LF |
| No suitable gradle version found | 核对 prebuild 后 distributionUrl 为官方地址，Gradle 版本和 distributionSha256Sum 不变 |
| 镜像或 Foojay 被 scanner 标记 | 核对 prebuild 是否在正确 subdir 执行，不跳过扫描 |
| APK 只有 Git 信息不同 | 核对 release 的 vcsInfo 设置，并发布新的版本；不改写已发布标签和 APK |
| Linux 构建或 reproducibility 失败 | 阅读 CI job trace，比较 APK entry；Windows 比较通过不能代替 Linux CI |
| APK extra signing block | 在 android.dependenciesInfo 关闭 includeInApk / includeInBundle，解析实际签名块确认没有 0x504b4453 |
| Android Debug certificate | 需要确认渠道签名策略；不能通过更名字段或跳过 check apk 规避，换签名前验证完整数据迁移 |
| pipeline 无法运行 | 记录 GitLab 返回的限制，保留 MR 并请求 F-Droid 维护者触发 |
