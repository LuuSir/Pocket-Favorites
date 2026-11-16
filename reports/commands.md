# 常用命令

以下命令在项目根目录 `D:\Android\Projects\ResoureTree` 的 PowerShell 执行。路径是当前已验证环境，其他电脑需替换为本机路径。

```powershell
$env:JAVA_HOME = 'D:\Android\Android Studio\jbr'
$env:GRADLE_USER_HOME = 'D:\Android\GradleCache'
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest :app:lintDebug --offline --console=plain
```

| 命令或参数 | 使用场景与预期结果 |
| --- | --- |
| `:app:assembleDebug` | 构建开发 APK：`app/build/outputs/apk/debug/app-debug.apk` |
| `:app:testDebugUnitTest` | 本地测试；HTML 报告位于 `app/build/reports/tests/testDebugUnitTest/index.html` |
| `:app:assembleDebugAndroidTest` | 仅编译真机测试 APK，不代表真机测试通过 |
| `:app:lintDebug` / `:app:lintRelease` | 静态检查；报告位于 `app/build/reports/` |
| `:app:assembleRelease` | 发布构建；需先配置签名环境，否则仅生成未签名包 |
| `--offline` | 使用本地缓存；当前环境已缓存依赖。新电脑首次构建应联网并去掉该参数 |
| `--console=plain` | 输出普通文本，便于保存日志和检查失败原因 |

```powershell
& 'D:\Android\SDK\platform-tools\adb.exe' devices
& 'D:\Android\SDK\platform-tools\adb.exe' install -r 'build/releases/PocketFavorites-v1.0.apk'
```

`devices` 应列出 `device`；`unauthorized` 需在手机确认 USB 调试。`install -r` 使用同签名包覆盖安装，保留数据，成功输出 `Success`。

快捷磁贴与悬浮手势的定向本地回归（已执行通过）：

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests '*FloatingEntry*' --offline --console=plain
```

`--tests` 仅运行匹配的本地测试类；磁贴和独立前台启动窗口覆盖 Android 8、13、15，包括进程内无现有 Activity 的启动请求、成功/失败/超时以及重复启动。真实久置后的系统行为需在手机手动验收。首次运行缺少对应 Robolectric 运行库时仍需要下载；`--offline` 仅限制 Gradle 自身的依赖解析。

开发测试需先覆盖安装 Debug APK 和 `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`，再运行：

```powershell
& 'D:\Android\SDK\platform-tools\adb.exe' shell am instrument -w -r -e notClass com.example.resouretree.DouyinDeepLinkDebugTest com.example.resouretree.test/androidx.test.runner.AndroidJUnitRunner
```

`-w` 等待完成；instrumentation 的 `-r` 输出原始结果；`-e notClass` 排除仅供本地诊断的抖音测试。检查最终结果和逐项状态：`0` 通过、`-4` 跳过、`-2` 失败；不能只看 ADB 退出码。测试需解锁手机，MIUI 可能另需允许测试宿主后台启动。结束后恢复正式 Release 及测试前设置。

发布前检查（不会上传）：

```powershell
python scripts/publish_release.py --tag v1.0 --apk build/releases/PocketFavorites-v1.0.apk --build-tools D:/Android/SDK/build-tools/36.0.0 --check-only
```

| 发布参数 | 含义 |
| --- | --- |
| `--tag` | 版本标签，必须与 APK versionName 一致 |
| `--apk` | 已签名 APK 输入路径 |
| `--build-tools` | SDK 工具目录，需含 aapt 和 apksigner |
| `--check-only` | 只验证，输出包名、版本、大小与 SHA256 |
| `--notes` | 发布说明 Markdown 文件；实际上传时必填 |
| `--direct-git` | 仅此次调用忽略 Git 代理；当前本机旧代理不可用时使用 |

实际发布及升级摘要参数见 [完整教程](../workflows/release.md)。不要将手机密码、签名密码或 GitHub 令牌写入文档或脚本。
