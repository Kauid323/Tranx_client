# Tranx Community - Android 社区应用

基于 Material Design 3 的 Android 社区应用，使用 Jetpack Compose 构建。

## 功能特性

- ✅ 用户注册和登录
- ✅ 自定义服务器地址（支持 HTTP 和 HTTPS）
- ✅ 帖子浏览、发布、点赞、收藏
- ✅ 评论功能
- ✅ 每日签到系统
- ✅ 用户等级和硬币系统
- ✅ Material Design 3 设计风格
- ✅ 板块筛选
- ✅ 多种排序方式（最新、热门、最近回复）

## 技术栈

- **语言**: Kotlin
- **UI框架**: Jetpack Compose
- **架构**: MVVM
- **网络请求**: Retrofit + OkHttp
- **图片加载**: Coil
- **本地存储**: SharedPreferences
- **异步处理**: Kotlin Coroutines + Flow

## 项目结构

```
app/src/main/java/com/tranx/community/
├── data/
│   ├── api/          # API接口定义
│   ├── local/        # 本地存储
│   └── model/        # 数据模型
├── ui/
│   ├── activity/     # Activity页面
│   ├── screen/       # ViewModel层
│   └── theme/        # 主题配置
├── MainActivity.kt   # 主入口
└── TranxApp.kt      # Application类
```

## 服务器配置

应用支持自定义服务器地址：

1. 在登录界面点击右上角设置图标
2. 选择协议（HTTP/HTTPS）
3. 输入服务器地址（如：localhost:4999）
4. 点击确定保存

**常用配置：**
- 本地：`http://localhost:4999`
- 模拟器：`http://10.0.2.2:4999`
- 自定义：`http://your-server-address:port`

## 编译和运行

1. 确保安装了 Android Studio（推荐最新版本）
2. 克隆项目到本地
3. 使用 Android Studio 打开项目
4. 等待 Gradle 同步完成
5. 连接设备或启动模拟器
6. 点击运行按钮

**最低要求：**
- Android SDK 24 (Android 7.0)
- 目标 SDK 34 (Android 14)

## API 说明

本应用基于 TaruApp API 开发，详细API文档请参考项目中的 `API_使用说明.md`。

**主要 API 端点：**
- `/api/auth/login` - 用户登录
- `/api/auth/register` - 用户注册
- `/api/posts/list` - 获取帖子列表
- `/api/posts/create` - 创建帖子
- `/api/comments/list` - 获取评论列表
- `/api/checkin` - 每日签到

## 注意事项

1. **HTTP 明文传输**：确保在 AndroidManifest.xml 中设置了 `android:usesCleartextTraffic="true"`
2. **网络权限**：已在 Manifest 中添加 INTERNET 权限
3. **服务器地址**：首次使用需要配置服务器地址
4. **Token管理**：Token有效期30天，过期需重新登录

## 开发者信息

- **开发工具**: Android Studio
- **Kotlin版本**: 1.9.20
- **Compose版本**: 1.5.4
- **Gradle版本**: 8.2.0

## License

本项目仅供学习和参考使用。

