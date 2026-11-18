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
- 影响：F-Droid 尚未收录，签名策略需要用户选择，随后维护者仍需审核开发时长等条件。
- 已验证：[pipeline 3082347377](https://gitlab.com/LuuSir/fdroiddata/-/pipelines/3082347377) 的 8 个 job 成功，包括 Linux 构建、Binaries 可复现比较、metadata、源码检查。168 项本地测试及 Debug / Release lint 通过；[MR !57711](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/57711) 仍开放。
- 当前失败：check apk 拒绝 Dependency metadata signing block 及历史 Android Debug 证书。dependency metadata 已在源码关闭并用新构建的实际签名块验证；原已发布 1.0.2 未替换。
- 决策：建议 GitHub 保持原签名，F-Droid 使用自身签名；得到用户确认后移除 metadata 的 Binaries / AllowedAPKSigningKeys、固定修复后的源码并重跑 CI。若选择统一新证书，则先完成包含媒体的数据迁移与恢复验证。未生成或替换签名密钥。
- 数据条件：JSON 不包含 app-private files/media；不能把单独 JSON 导出视为卸载前完整备份。保持现有安装及 GitHub 覆盖更新可保留数据。
- 验收步骤：用户选择签名渠道策略后，更新现有 MR、重跑 CI，预期全部 job 成功，再由 F-Droid 维护者完成收录审核。首次源码提交为 2026-09-07，时长条件仍由维护者判断。

## Verified


已完成的自动检查与真机数据比较记录在 [reports/resourcetree.md](reports/resourcetree.md)。
