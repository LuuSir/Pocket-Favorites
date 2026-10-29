# 重要文件与脚本

路径均相对项目根目录；Android 源码包根目录为 `app/src/main/java/com/example/resouretree/`。

| 文件或目录 | 用途、输入输出与依赖 |
| --- | --- |
| `MainActivity.kt` | 主界面入口；在窗口有焦点时按剪贴板规则自动预填 |
| `overlay/FloatingEntryService.kt` | 管理悬浮窗口与通知；单击启动 ClipboardCaptureActivity；运行状态供设置页与快捷开关观察 |
| `overlay/FloatingEntryTouchListener.kt` | 区分短按、拖动和取消；按住可继续拖动，长按松手不生成点击 |
| `overlay/FloatingEntryTileService.kt` | 系统 Quick Settings 磁贴；单击启动/停止悬浮服务，面板可见期间观察实际状态；缺权限时打开系统授权页 |
| `overlay/FloatingEntrySettings.kt` | SharedPreferences 保存尺寸；沿用原位置设置，不修改 Room |
| `ClipboardCaptureActivity.kt` | 独立收集窗口；获得焦点后读取一次剪贴板，匹配规则、解析目标并复用编辑器；保存/取消关闭自身任务 |
| `data/clipboard/ClipboardDraftInbox.kt` | 自动预填去重；主动收集可重读已处理文本，不向主界面发送导航请求 |
| `ui/screens/EditorScreen.kt`、`ui/viewmodel/EditorViewModel.kt` | 编辑 Content、Action、目标应用；用户点击保存后写入 repository |
| `ui/components/ResourceGraphView.kt`、`ui/graph/` | 图布局、缩放及连续过渡；不改变资源排序 |
| `data/local/database/ResourceDatabase.kt`、`app/schemas/` | Room 数据库与迁移；已有数据必须经 migration 保留 |
| `app/src/test/` | 本地单元及 Robolectric/Compose 测试，运行方式见命令速查 |
| `app/src/debug/` | 仅 Debug 存在的独立悬浮测试宿主，避免外部应用测试之间共用 Activity；不进入 Release |
| `app/src/androidTest/` | 真机测试；InstalledDataPreservationInstrumentedTest 的 snapshot/compare 输出数据摘要，默认跳过 |
| `app/build.gradle.kts` | 版本、依赖、测试和 Release 签名配置；读取环境变量，不保存签名密码 |
| `tools/publish_release.py` | 校验包名、版本、非调试标记、签名、ZIP；验证远端引用并上传 APK 与 SHA256 到 GitHub Release |
| `tools/bilibili-export/` | 独立 Userscript；浏览器读取收藏夹，输出可导入 JSON，见其 README |
| `build/`、`app/build/` | 自动生成的 APK、测试报告和本地核验文件；Git 忽略，不作为源码提交 |

## 目录与修改入口

| 目录 | 职责与常见修改点 |
| --- | --- |
| `app/src/main/` | 正式应用入口、Manifest、资源；新增系统组件需同步注册 Manifest |
| `domain/` | Content / Action、树结构与剪贴板匹配规则 |
| `data/` | Room、JSON 和规则存储；数据问题从 repository 与迁移测试检查 |
| `ui/` | Compose 页面、组件、ViewModel；界面和编辑状态在此维护 |
| `overlay/` | 悬浮服务、快捷磁贴、手势与尺寸；开关问题先检查系统权限及服务实际状态 |
| `app/src/test/`、`app/src/androidTest/` | 本地测试和设备测试；快捷开关本地回归见 FloatingEntryTileTest |
| `tools/`、`reports/` | 辅助工具及用户维护文档 |

上表 `domain/`、`data/`、`ui/`、`overlay/` 均位于开头所列 Android 源码包目录。快捷磁贴和设置页控制同一个 FloatingEntryService，服务创建窗口后才报告开启，避免另存一份开关状态与真实窗口脱节。

发布脚本参数、执行环境和输出见 [命令速查](commands.md)；多步骤顺序见 [发布教程](workflows/release.md)。本地 Word 稿件被忽略，不属于公开源码或发布附件。
