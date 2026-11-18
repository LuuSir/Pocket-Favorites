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
- 影响：F-Droid 尚未收录；Linux 构建、跨环境 reproducibility 和新应用时长例外仍需维护者验证。
- 证据：[MR !57711](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/57711) 已开放，只有一个 metadata 文件。本地 metadata 校验、source scanner、168 项测试和 Windows signed / unsigned 重建比较通过。
- 剩余限制：维护者 pipeline 3077393364 实际运行后发现 formatter 和 Gradle URL 识别问题，已在原 MR 修正。新 [pipeline](https://gitlab.com/LuuSir/fdroiddata/-/pipelines/3082312584) 正常启动，metadata / source checks 已通过，Linux build 和 APK 比较仍在运行。首次源码提交为 2026-09-07，尚未满三个月，MR 已如实说明作者真机使用情况。
- 验证步骤：跟踪现有 pipeline，预期 metadata / scanner / Linux build / Binaries 比较全部通过，再由 F-Droid 维护者完成收录审核。无需用户提供信用卡或手机验证来启动 fork CI。
- 结论：fork、metadata 和 MR 提交已完成；CI 和收录仍为外部待办，不宣称已上架。

## Verified


已完成的自动检查与真机数据比较记录在 [reports/resourcetree.md](reports/resourcetree.md)。
