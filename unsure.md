# Verification gaps

## Open

### U-001

- Status: Pending
- 影响：不同厂商在长时间后台限制后的 Quick Settings 启动行为，以及系统 App languages 外部切换，仍需实际设备操作验收。
- 已有证据：API 26 / 33 / 35 的本地测试覆盖开关、启动窗口与语言存储；手机覆盖升级的数据一致性另记 [报告](reports/resourcetree.md)。
- 验收：保持已授权悬浮权限，离开应用并久置；点 Quick Settings 确认悬浮按钮出现，再次点击关闭。在「菜单 → 界面语言」切换 English / 简体中文 / 跟随系统，确认主界面、编辑器与通知语言一致。
- 输入与设备：已更新 1.0 的手机，用户自行操作；无需连接电脑。返回设备系统版本、操作路径和异常现象即可。

### U-002

- Status: Pending
- 目标：1.0.3 起用同一正式发布证书覆盖 GitHub 和 F-Droid。
- 已验证：正式签名 APK、关闭 Dependency metadata、168 项本地测试、Release lint 与一台 Android 13 真机的备份、重装和数据验证。
- 已验证：[Pipeline 3083958689](https://gitlab.com/LuuSir/fdroiddata/-/pipelines/3083958689) 9 项全部通过，含 Linux 可复现构建、正式证书和 APK 检查。
- 剩余：由 F-Droid 维护者审核 [MR !57711](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/57711) 并收录。首次提交为 2026-09-07，开发时长由维护者判断。

## Verified


已完成的自动检查与真机数据比较记录在 [reports/resourcetree.md](reports/resourcetree.md)。
