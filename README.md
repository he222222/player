# 局域网网盘

安卓 App，点开直接在 App 内访问 **http://192.168.1.1:5244/**（OpenList），无首页、无原生文件管理。

## 功能

- Android 7.0 及以上（minSdk 24，targetSdk 34）；手机须连接能访问该地址的局域网。
- 启用 JavaScript 和网页存储，供 OpenList 页面及登录使用。
- 网页视频全屏播放：全屏时隐藏系统栏并保持屏幕常亮；旋转手机不会重新加载网页。
- 返回键：先退出视频全屏，然后返回网页上一级，最后退出应用。
- 连不上时显示提示和「重新加载」按钮。
- 仅对 `192.168.1.1` 开放明文 HTTP；未跳过 HTTPS 证书校验，无原生 JavaScript 接口。

## 结构

| 路径 | 说明 |
|---|---|
| `app/src/main/java/com/local/player/MainActivity.java` | 全部逻辑：WebView、全屏、错误页、返回键 |
| `app/src/main/res/layout/activity_main.xml` | WebView + 进度条 + 错误面板（`<merge>`） |
| `app/src/main/res/xml/network_security_config.xml` | 明文 HTTP 仅限 `192.168.1.1` |
| `app/src/androidTest/java/com/local/player/SmokeTest.java` | 冒烟测试（Instrumentation 子类） |

## 构建

需要 JDK 17 或 21、Android SDK 34 和 Gradle Wrapper；首次构建需要下载构建依赖。
`local.properties` 不入库，本机自行创建：

```properties
sdk.dir=D:/commandlinetools-win-11076708_latest
```

```powershell
.\gradlew.bat assembleDebug lintDebug assembleDebugAndroidTest
```

产物 `app/build/outputs/apk/debug/app-debug.apk` 使用本机调试密钥签名，可直接安装；
如需上架，需要更新目标 SDK 并配置正式发布签名。

## 设备检查

接入专用测试设备或模拟器后：

```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w com.local.player.test/com.local.player.SmokeTest
```

自动检查启动地址、JavaScript、网页存储、本地文件访问限制和 HTTP 允许范围；
在 Android 8.0 及以上还检查进入和退出全屏、重复请求、返回键和系统栏恢复。
另需在手机上人工检查实际视频播放、旋转手机、登录、网页跳转以及断网后重试。

## 局限

- 服务地址 `192.168.1.1:5244` 硬编码在 `MainActivity.HOME_URL`，换服务器需同时改
  `network_security_config.xml` 里的域名白名单。
- 网页容器定位：没有原生上传、下载、多标签或书签。
- 只适配自家 OpenList 页面，不对其他站点做兼容。

## 许可证

[MIT](LICENSE) © 2026 he222222