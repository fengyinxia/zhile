# 知了

基于 [shatyuka/Zhiliao](https://github.com/shatyuka/Zhiliao) 的非官方 Xposed 模块适配版本，采用 GPL-3.0 许可证。保留原包名 `com.shatyuka.zhiliao`、设置页面和独立功能开关。

## 适用范围

当前仅针对 **arm64 知乎 11.10.0（41012）** 的指定宿主构建；相同版本的其他渠道、32 位版本或热更新代码不保证兼容。需要支持传统 Xposed API 的运行环境，并将模块作用域设置为知乎。

应用显示名为 **知了**，当前版本显示 `26.10.08`。`r8` 仅用于区分构建产物，不出现在应用版本栏中。源码不修改、替换或重签知乎安装包。

## 主要功能

- 原有广告过滤、界面净化和导航按钮设置。
- 标题、作者、卡片摘要的正则过滤，包含媒体卡和已知嵌套 SDUI 结构；修改规则后重新筛选已加载列表。
- 搜索页推荐内容的缓存及展示前过滤。
- 回答底部原生相关搜索区块过滤，遵守“去相关搜索”独立开关。
- 保留独立的“移除下一个回答按钮”；左右划功能及灵敏度设置已移除。

英文正则默认区分大小写，可自行添加 `(?i)`。内容过滤指卡片摘要，不是完整回答或文章正文。客户端路径可能变化，功能覆盖不等于所有场景已经验证。

## 构建

本仓库只提供当前的 **Java Hook 构建版**，使用 `build_official.py`，不依赖 Gradle 或 NDK，也不包含 native 重签名兼容层。

依赖：

- Python 3.11+。
- JDK 17+，`java`、`javac`、`jar`、`keytool` 可从 PATH 使用。
- Android SDK Platform 36 与 Build-tools 36.1.0 或 36.0.0。

设置 `ANDROID_SDK_ROOT` 后运行：

```sh
python build_official.py
```

脚本在 `.work/` 创建构建缓存，首次构建会下载 Xposed API 82 编译依赖。输出为仓库根目录的 `zhiliao-11.10.0-arm64-r8.apk`。

### 签名

签名材料保存在本地 `.work/zhiliao-signing/`，不属于仓库。Windows 首次构建会创建本地签名并以当前用户 DPAPI 保护密码；非 Windows 需通过 `ZH_RESEARCH_KEYSTORE_PASSWORD` 环境变量提供密码。不要把密码直接写入源码或提交记录。

升级已安装模块时必须保留原签名文件与凭据。**全新生成的签名无法覆盖安装使用另一证书的 APK**；仓库不会提供发布者的私钥或签名密码，也不要为了升级而擅自卸载已有模块。

## 源码与测试

- `app/src/main/java/`：模块入口、兼容映射和 Hook。
- `app/src/main/res/`、`assets/`：设置资源及 Xposed 入口。
- `app/src/test/java/com/shatyuka/zhiliao/FeedKeywordsTest.java`：纯 JVM 的关键词提取回归测试，可通过 JUnit 4.13.2 运行；需要编译 `FeedKeywords.java` 和该测试类。

本仓库不包含宿主样本、逆向分析产物、内部交接资料、本机安装记录或已签名 APK。

## 致谢与许可证

原项目：[shatyuka/Zhiliao](https://github.com/shatyuka/Zhiliao)。源代码与资源保留原作者归属，修改版遵守 [GPL-3.0](LICENSE.md)。
