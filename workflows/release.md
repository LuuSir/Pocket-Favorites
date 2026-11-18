# 发布与保留数据升级

在项目根目录执行。依赖本机 JDK、Android SDK、Python、Git、可用的 GitHub 凭据，以及当前正式发布密钥。

## 从 1.0.2 或更早版本迁移

1. 在旧版打开首页菜单 → 导出 JSON，将文件保存到系统「下载」或电脑。确认文件已保存并可打开。
2. 保存自定义剪贴板规则和设置。JSON 保存收藏树，媒体条目只记录路径。若收藏含图片、视频或文件，先在旧版逐个分享或保存可读的原件到应用外，并确认能打开，迁移后重新关联。
3. Uninstall 旧应用，从 [GitHub Release](https://github.com/LuuSir/Pocket-Favorites/releases/tag/v1.0.3) 安装 1.0.3，打开首页菜单 → 导入 JSON。核对收藏目录、条目、置顶和排序，重新配置规则与授权。

On 1.0.2 and earlier, use Home → menu → Export JSON and verify that the file was saved outside the app. Save custom rules, settings and copies of attached media before uninstalling. Install 1.0.3 and use Home → menu → Import JSON, then verify your favorites and reattach media. JSON contains media references only.

## 发布检查

1. 按 [命令速查](../reports/commands.md) 构建 Debug、运行本地测试并编译 AndroidTest；连接设备时执行真机回归。编译测试包与实际运行必须分别记录。
2. 有真实数据的手机，更新前先运行只读快照：

   ```powershell
   & 'D:\Android\SDK\platform-tools\adb.exe' shell am instrument -w -r -e class com.example.resouretree.InstalledDataPreservationInstrumentedTest -e preservationMode snapshot com.example.resouretree.test/androidx.test.runner.AndroidJUnitRunner
   ```

   保存结果中的 `snapshotNodes`、`snapshotTree`、`snapshotRules`、`snapshotSchema` 到本地忽略目录 `results/resourcetree/<YYYYMMDD>-<description>-<operator>/`；operator 默认为实际操作者缩写，用户使用 `lsf`。需要已安装同签名测试包；快照不读取资源正文到日志。
3. 配置本机环境变量 `RESOURCETREE_SIGNING_STORE`、`RESOURCETREE_SIGNING_STORE_PASSWORD`、`RESOURCETREE_SIGNING_ALIAS`、`RESOURCETREE_SIGNING_KEY_PASSWORD`，然后运行 `:app:clean :app:assembleRelease :app:lintRelease --no-build-cache -Pkotlin.incremental=false -Pksp.incremental=false`。必须从干净的编译输出生成正式 APK；旧增量缓存可能改变 Compose stability 代码，导致 APK 无法复现。1.0.3 起使用 `Pocket Favorites Release` 证书。核对签名指纹与 [publish_release.py](../scripts/publish_release.py) 及 fdroiddata 的 AllowedAPKSigningKeys 一致。维护机密钥位于 `%USERPROFILE%/.android/pocket-favorites-release/`。同目录的 DPAPI 密码文件仅当前 Windows 用户可解密。换电脑前，用 keytool 的 importkeystore 将同一私钥导出到由用户掌握密码的加密 PKCS12，再做离线备份和指纹校验。
4. 将已签名 APK 复制到 `build/releases/PocketFavorites-v<版本>.apk`，执行 `publish_release.py --check-only`。必须通过 ZIP、签名、版本和非调试校验。
5. 已使用当前正式签名的安装用 `adb install -r` 覆盖更新。1.0.2 及更早的安装按上面的换签迁移步骤处理。再次运行步骤 2 的检查，将模式改为 `compare`，并使用 `-e snapshotNodes <原值>` 等方式传入全部四个原始字段。只在 `OK (1 test)` 且无失败时认定数据一致；不完整参数会失败。
6. 核对 `git status --short`，仅提交相关源码、测试与文档；Word 稿件、数据库快照、截图和日志留在忽略目录。提交后推送 main，创建同一提交上的版本标签并推送标签。
7. 执行发布，以 v1.0 为例，将发布说明写入本地 results 目录后运行：

   ```powershell
   python scripts/publish_release.py --tag v1.0 --apk build/releases/PocketFavorites-v1.0.apk --build-tools D:/Android/SDK/build-tools/36.0.0 --notes results/resourcetree/20261116-release-1.0-codex/release-notes.md --direct-git
   ```

   工具确认远端 main 和标签指向当前提交，使用 Git credential helper 的凭据上传 APK 与校验文件，核对服务端摘要后发布。它拒绝修改已发布的同名 Release；先检查结果再决定新版本，不反复覆盖附件。

## 常见问题

| 现象 | 检查方法 |
| --- | --- |
| `PackageInfo is null` | 核对下载文件 SHA256、ZIP 与签名，重新下载完整 APK；历史问题曾由损坏下载导致 |
| 签名不一致不能覆盖 | 核对当前发布证书。首次从历史签名迁移时，先验证备份并按换签迁移步骤操作 |
| Gradle 等待依赖 | 已缓存环境使用 `--offline`；新电脑检查网络和代理后在线下载 |
| MIUI 测试无界面 | 解锁手机，检查后台启动限制；临时变更系统设置应记录原值并在结束后恢复 |
| 久置后快捷开关无法开启 | v0.9 使用独立前台启动窗口避开 MIUI 后台服务拦截；确认已更新、悬浮窗权限已授权，再查看启动失败提示；不要通过清空数据排查 |
| 悬浮单击无反应 | 检查悬浮窗及后台弹出界面权限；无匹配剪贴板规则时不会打开编辑表单 |
| compare 失败 | 暂停发布结论，先核对是否使用正确的升级前快照及是否有用户主动修改；不能以清空数据库修复 |

最后更新 [使用总览](../reports/README.md) 与 [验证报告](../reports/resourcetree.md)，区分实际通过、跳过和未执行项目。
