# Glossary

| English term | 中文定义与本项目用法 |
| --- | --- |
| Content | 条目保存的内容；TEXT 使用 text，IMAGE / VIDEO / FILE 使用 path 与 mimeType |
| Action | 点击条目时处理 Content 的方式，target 为可选目标应用包名 |
| App locale | 单应用语言；Android 13+ 使用 LocaleManager，旧系统使用应用偏好与 Configuration |
| Follow system | 未设置 App locale，随系统语言变化；缺少对应翻译时回退到 English |
| AppPicker | 已安装应用的图标、名称与搜索选择组件 |
| Clipboard rule | 剪贴板预填规则，匹配前缀或通配符，支持候选包名 |
| Quick Settings tile | 通知栏快捷开关，控制实际 FloatingEntryService 状态 |
| Foreground service | 悬浮按钮运行所用的前台服务，带常驻通知 |
| Room migration | 数据库版本迁移；当前 schema v3，升级保留已有节点 |
| JSON schema | 导入导出协议；当前导出 v2，兼容导入 v1 |
| Release | 非调试签名 APK；沿用历史证书以支持覆盖更新 |
| Instrumentation | 运行在 Android 设备上的测试；数据核对使用只读 snapshot / compare |
