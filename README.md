# 外置收藏夹 · Pocket Favorites 1.0.2

**简体中文** | [English](README.en.md)

把不同应用中的文字、链接、图片、视频和文件收进同一个本地收藏夹。按文件夹整理，点击条目即可复制、分享、打开应用或在应用内浏览网页。

## 开始使用

- 从 [Release 1.0.2](https://github.com/LuuSir/Pocket-Favorites/releases/tag/v1.0.2) 下载 `PocketFavorites-v1.0.2.apk`。已安装 ResourceTree 的用户直接覆盖更新，包名和签名沿用旧版。1.0.2 通过干净构建修复 F-Droid 可复现性。
- 首页「＋ 新建」添加条目或文件夹；「更多」中置顶和批量整理，长按列表行拖拽排序。
- 「菜单 → 界面语言」选择「跟随系统 / 简体中文 / English」。默认跟随系统；其他系统语言使用 English。语言切换保留已有内容与规则原文。
- 点击左上角应用名称切换可缩放图视角；点击文件夹进入，点击条目执行动作；支持平移和展开全部后代。
- 「菜单 → 悬浮按钮」开启悬浮收集：单击直接读取并按规则预填，下拉通知栏添加「外置收藏夹悬浮窗」快捷开关，点亮开启、再点关闭；按钮大小可调。
- 剪贴板规则支持前缀、通配符和候选包名；只预填，确认保存后才写入首页。
- 导入 JSON v1 / v2，统一导出 v2。同名根目录再次导入会追加独立副本。
- [B 站教程视频](https://www.bilibili.com/video/BV1dQYs6JEbT/?vd_source=6076e72eb03a33f77c50c30c9947e961)

## 构建与维护

Android Studio 打开项目根目录，使用自带 JBR 与 Android SDK 37；已缓存依赖时执行：

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest :app:lintDebug --offline --console=plain
```

Debug APK 输出到 `app/build/outputs/apk/debug/app-debug.apk`。签名 Release、真机数据核对和发布步骤见 [发布流程](workflows/release.md)。

从 [reports/README.md](reports/README.md) 查看完整功能速查和文档索引：

- [架构与修改入口](files.md)
- [English 技术术语表](glossary.md)
- [构建、测试与安装命令](reports/commands.md)
- [发布与保留数据升级](workflows/release.md)
- [当前验证报告](reports/resourcetree.md)
- [待验证事项](unsure.md)
- [B 站收藏夹导出工具](tools/bilibili-export/README.md)

后续重点是媒体收藏体验和真机兼容性。Android 包名继续使用 `com.example.resouretree`，Room schema 保持 v3，JSON schema 保持 v2。

## License

本项目采用 [MIT License](LICENSE)。F-Droid 应用介绍位于 [fastlane/metadata/android](fastlane/metadata/android/)。

已提交 [F-Droid MR !57711](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/57711)，正在验证可复现构建并等待维护者审核；目前尚未上架。验证结果见 [报告](reports/resourcetree.md)，维护步骤见 [流程](workflows/fdroid.md)。
