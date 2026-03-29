# OpenList Android Client

一个专为 OpenList（AList 分支）设计的 Android 客户端，采用 Kotlin + Jetpack Compose 构建。

## 功能特性

- 🔐 支持用户名密码登录 OpenList 服务器
- 📁 文件浏览器（列表/网格视图切换）
- 🔍 文件搜索
- 🖼️ 图片、视频、音频、PDF 在线预览
- ⬇️ 后台下载（支持 Android 6+ 后台保活）
- 🌙 深色/浅色模式
- 📲 响应式设计，适配手机和平板
- 🔔 Android 13+ 通知权限适配
- ⚡ 适配 Android 14/15

## 截图

（待添加）

## 项目结构

```
app/
├── src/main/
│   ├── java/org/openlist/app/
│   │   ├── OpenListApp.kt         # Application 类
│   │   ├── data/
│   │   │   ├── api/               # Retrofit API 接口
│   │   │   ├── model/             # 数据模型
│   │   │   └── repository/        # 数据仓库
│   │   ├── di/                    # Hilt 依赖注入
│   │   ├── ui/
│   │   │   ├── components/        # 可复用 UI 组件
│   │   │   ├── screens/            # 页面
│   │   │   ├── theme/             # 主题
│   │   │   └── viewmodels/        # ViewModel
│   │   └── util/                  # 工具类
│   └── res/                       # 资源文件
└── build.gradle.kts
```

## 技术栈

- **语言**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **架构**: MVVM + Clean Architecture
- **DI**: Hilt
- **网络**: Retrofit + OkHttp
- **图片**: Coil
- **视频**: ExoPlayer (Media3)
- **本地存储**: DataStore Preferences
- **后台任务**: WorkManager + Foreground Service
- **编译**: Gradle 8.7 + AGP 8.3.2

## 构建

### 本地构建（需要 Android SDK）

```bash
# 方式1: 使用 Gradle Wrapper
./gradlew assembleDebug

# 方式2: 使用已安装的 Gradle
gradle assembleDebug
```

APK 输出位置: `app/build/outputs/apk/debug/app-debug.apk`

### CI/CD 构建（无需本地环境）

推送到 GitHub 后，GitHub Actions 会自动构建，APK 在 Artifacts 中下载。

## 下载 APK

从 GitHub Actions 构建产物下载最新调试版：
1. 访问本仓库的 Actions 页面
2. 选择最新的 Build workflow
3. 下载 `debug-apk` artifacts

## 配置

### 连接 OpenList 服务器

在应用登录界面输入你的 OpenList 服务器地址，例如：
- `https://alist.example.com`
- `http://192.168.1.100:5244`

支持自签名证书（已在 network_security_config.xml 中配置）。

## License

AGPL-3.0
