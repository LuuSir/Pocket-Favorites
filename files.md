# Architecture and entry points

Android 源码根目录为 [com/example/resouretree](app/src/main/java/com/example/resouretree/)。界面使用 Compose，ViewModel 管理状态，repository 连接 Room 和 JSON 导入导出。

| 责任 | 入口 |
| --- | --- |
| 应用与依赖 | [ResourceTreeApplication.kt](app/src/main/java/com/example/resouretree/ResourceTreeApplication.kt)，[app/build.gradle.kts](app/build.gradle.kts) |
| 主界面与路由 | [MainActivity.kt](app/src/main/java/com/example/resouretree/MainActivity.kt)，[ResourceTreeApp.kt](app/src/main/java/com/example/resouretree/ui/navigation/ResourceTreeApp.kt) |
| 语言选择 | [AppLanguage.kt](app/src/main/java/com/example/resouretree/localization/AppLanguage.kt)，[LanguageScreen.kt](app/src/main/java/com/example/resouretree/ui/screens/LanguageScreen.kt) |
| 界面与非 Compose 文案 | [English strings](app/src/main/res/values/strings.xml)，[中文 strings](app/src/main/res/values-zh/strings.xml)，[AppText.kt](app/src/main/java/com/example/resouretree/localization/AppText.kt)；后者仅持有 Application context，按当前语言读取校验、服务与 ViewModel 文案 |
| 系统单应用语言列表 | [locales_config.xml](app/src/main/res/xml/locales_config.xml)，[AndroidManifest.xml](app/src/main/AndroidManifest.xml) |
| 编辑、应用选择和搜索 | [screens/](app/src/main/java/com/example/resouretree/ui/screens/)，[AppPicker.kt](app/src/main/java/com/example/resouretree/ui/components/AppPicker.kt)，[viewmodel/](app/src/main/java/com/example/resouretree/ui/viewmodel/) |
| 图布局与连续过渡 | [ResourceGraphView.kt](app/src/main/java/com/example/resouretree/ui/components/ResourceGraphView.kt)，[graph/](app/src/main/java/com/example/resouretree/ui/graph/) |
| 悬浮窗口、尺寸、通知与快捷开关 | [overlay/](app/src/main/java/com/example/resouretree/overlay/)；SystemUI → FloatingEntryStartActivity → FloatingEntryService，创建窗口成功后才报告开启 |
| 独立剪贴板收集 | [ClipboardCaptureActivity.kt](app/src/main/java/com/example/resouretree/ClipboardCaptureActivity.kt)，[clipboard/](app/src/main/java/com/example/resouretree/data/clipboard/)；不覆盖主界面草稿 |
| 数据、导入导出和动作 | [ResourceNode.kt](app/src/main/java/com/example/resouretree/domain/model/ResourceNode.kt)，[data/](app/src/main/java/com/example/resouretree/data/)，[action/](app/src/main/java/com/example/resouretree/domain/action/) |
| 本地回归 | [app/src/test/](app/src/test/)；AppLanguageTest 覆盖 API 26 / 33 / 35，LanguageScreenTest 验证英文界面、切换和草稿恢复 |
| 真机只读数据检查 | [InstalledDataPreservationInstrumentedTest.kt](app/src/androidTest/java/com/example/resouretree/InstalledDataPreservationInstrumentedTest.kt)，传 snapshot / compare 及全部四项基线 |
| 发布脚本 | [scripts/publish_release.py](scripts/publish_release.py)，参数与输出见 [release workflow](workflows/release.md)；[tools/](tools/) 保留历史工具入口 |
| 日志与数据摘要 | `results/resourcetree/<YYYYMMDD>-<description>-<operator>/`，Git 忽略；当前汇总为 [reports/resourcetree.md](reports/resourcetree.md) |

语言设置只改变资源解析，不写入 Room、Content、Action 或 Clipboard rule。Android 13+ 使用原生 LocaleManager 并支持系统的 App languages 页面；Android 8–12 使用 SharedPreferences 和 LocalizedActivity，Activity 重建后由原有 SavedStateHandle 恢复导航与草稿。运行中的悬浮通知通过语言变更广播 / Configuration 更新文案。

构建与安装参数见 [commands](reports/commands.md)，完整发布与升级顺序见 [workflow](workflows/release.md)。
