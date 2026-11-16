# Verification gaps

## Open

### U-001

- Status: Pending
- 影响：不同厂商在长时间后台限制后的 Quick Settings 启动行为，以及系统 App languages 外部切换，仍需实际设备操作验收。
- 已有证据：API 26 / 33 / 35 的本地测试覆盖开关、启动窗口与语言存储；手机覆盖升级的数据一致性另记 [报告](reports/resourcetree.md)。
- 验收：保持已授权悬浮权限，离开应用并久置；点 Quick Settings 确认悬浮按钮出现，再次点击关闭。在「菜单 → 界面语言」切换 English / 简体中文 / 跟随系统，确认主界面、编辑器与通知语言一致。
- 输入与设备：已更新 1.0 的手机，用户自行操作；无需连接电脑。返回设备系统版本、操作路径和异常现象即可。

## Verified

已完成的自动检查与真机数据比较记录在 [reports/resourcetree.md](reports/resourcetree.md)。
