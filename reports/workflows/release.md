# 发布与保留数据升级

在项目根目录执行。依赖本机 JDK、Android SDK、Python、Git、可用的 GitHub 凭据，以及与旧版相同的签名密钥。

1. 按 [命令速查](../commands.md) 构建 Debug、运行本地测试并编译 AndroidTest；连接设备时执行真机回归。编译测试包与实际运行必须分别记录。
2. 有真实数据的手机，更新前先运行只读快照：

   ```powershell
   & 'D:\Android\SDK\platform-tools\adb.exe' shell am instrument -w -r -e class com.example.resouretree.InstalledDataPreservationInstrumentedTest -e preservationMode snapshot com.example.resouretree.test/androidx.test.runner.AndroidJUnitRunner
   ```

   保存结果中的 `snapshotNodes`、`snapshotTree`、`snapshotRules`、`snapshotSchema` 到本地忽略目录。需要已安装同签名测试包；快照不读取资源正文到日志。
3. 配置本机环境变量 `RESOURCETREE_SIGNING_STORE`、`RESOURCETREE_SIGNING_STORE_PASSWORD`、`RESOURCETREE_SIGNING_ALIAS`、`RESOURCETREE_SIGNING_KEY_PASSWORD`，然后运行 `:app:assembleRelease :app:lintRelease`。历史安装沿用原证书，不能随意换密钥；不将密钥或密码提交到仓库。
4. 将已签名 APK 复制到 `build/releases/ResourceTree-v<版本>.apk`，执行 `publish_release.py --check-only`。必须通过 ZIP、签名、版本和非调试校验。
5. 使用 `adb install -r` 覆盖安装正式包。再次运行步骤 2 的检查，将模式改为 `compare`，并使用 `-e snapshotNodes <原值>` 等方式传入全部四个原始字段。只在 `OK (1 test)` 且无失败时认定数据一致；不完整参数会失败。
6. 核对 `git status --short`，仅提交相关源码、测试与文档；Word 稿件、数据库快照、截图和日志留在忽略目录。提交后推送 main，创建同一提交上的版本标签并推送标签。
7. 执行发布，下面以 v0.7 的历史命名为例，需替换为实际版本与已准备好的说明文件：

   ```powershell
   python tools/publish_release.py --tag v0.7 --apk build/releases/ResourceTree-v0.7.apk --build-tools D:/Android/SDK/build-tools/36.0.0 --notes RELEASE-v0.7.md --direct-git
   ```

   工具确认远端 main 和标签指向当前提交，使用 Git credential helper 的凭据上传 APK 与校验文件，核对服务端摘要后发布。它拒绝修改已发布的同名 Release；先检查结果再决定新版本，不反复覆盖附件。

## 常见问题

| 现象 | 检查方法 |
| --- | --- |
| `PackageInfo is null` | 核对下载文件 SHA256、ZIP 与签名，重新下载完整 APK；历史问题曾由损坏下载导致 |
| 签名不一致不能覆盖 | 使用原密钥重新构建；不要通过卸载或清空数据绕过 |
| Gradle 等待依赖 | 已缓存环境使用 `--offline`；新电脑检查网络和代理后在线下载 |
| MIUI 测试无界面 | 解锁手机，检查后台启动限制；临时变更系统设置应记录原值并在结束后恢复 |
| 久置后快捷开关无法开启 | v0.9 使用独立前台启动窗口避开 MIUI 后台服务拦截；确认已更新、悬浮窗权限已授权，再查看启动失败提示；不要通过清空数据排查 |
| 悬浮单击无反应 | 检查悬浮窗及后台弹出界面权限；无匹配剪贴板规则时不会打开编辑表单 |
| compare 失败 | 暂停发布结论，先核对是否使用正确的升级前快照及是否有用户主动修改；不能以清空数据库修复 |

最后更新 [使用总览](../README.md) 与 [验证结果](../../BUILD-RESULTS.md)，区分实际通过、跳过和未执行项目。
