# SMS Relay for Bark

SMS Relay 是一个轻量 Android 应用：接收短信，将待投递内容写入本地加密队列，并且只在 Android 提供已验证 Wi-Fi 网络时转发到 Bark。

这是独立的社区项目，与 Bark 不存在隶属、授权或运营关系。

[English](README.md)

## 功能

- 合并多段短信，并在重启或解锁后对系统短信箱进行补偿同步。
- 使用 Android Keystore 支持的 AES-GCM 加密本地队列中的发件人和正文。
- 每条 Bark 正文使用 AES-256-CBC 和全新的随机 IV 加密。
- 只绑定 Android 选定的已验证 Wi-Fi 网络投递，不使用蜂窝网络传输。
- 对临时故障进行有上限的退避重试，已完成的本地记录保留 30 天。
- 重启后恢复持久任务，并可发送每日 Bark 健康心跳。
- 不含广告、统计分析、账号系统或应用自建云端。

应用申请短信接收/读取、网络、网络状态、开机完成和 Android 13+ 通知权限；不申请通讯录、定位、相机、麦克风或存储权限。

## 环境要求

- Android 8.0 或更高版本
- 可通过 Wi-Fi 访问的 Bark 服务和 Bark Device Key
- Windows PowerShell 5.1+ 或 PowerShell 7
- JDK 17、Android SDK platform 35 和 Windows Android build-tools

## 构建

在仓库根目录运行：

```powershell
.\build.ps1
```

脚本会运行主机测试、在不依赖 Gradle 的情况下编译 Android 应用、验证 APK 签名与清单，并输出到 `build/outputs/sms-relay-<version>.apk`。

独立克隆默认在被忽略的 `.keys/` 目录生成本地调试密钥；GitHub Actions 使用一次性调试密钥。正式分发前应创建并妥善保管独立发布密钥，绝不能提交密钥或密码。

## 配置 Bark

1. 在接收设备安装 Bark，并只复制 Device Key。
2. 在 SMS Relay 中打开“Bark 与加密”，填写 HTTPS Bark 服务器和 Device Key 后保存。
3. 将应用生成的 AES Key 与备用 IV 填入 Bark 的推送加密设置，选择 AES256、CBC 和 PKCS7。
4. 在依赖无人值守转发前，先用非生产内容完成应用内 Bark 测试。

本项目有意使用独立 package ID `com.local.smsrelay` 和新的 Keystore 别名，不会升级、覆盖或读取早期私有变体的数据。Fork 若要发布到应用商店，应先改为自己控制的 application ID。

## 安全边界

短信和验证码高度敏感。只能在你有权管理的设备与消息上使用本应用。本应用降低静态存储和传输过程中的暴露，但无法保护已 Root、已解锁、感染恶意软件或以其他方式失陷的手机。通知、截图、元数据、时间、服务商日志和接收设备仍可能泄露信息。

Wi-Fi 中断、Android 省电策略、Bark 凭据失效或服务故障都可能造成延迟。部署前请阅读 [PRIVACY.md](PRIVACY.md)、[SECURITY.md](SECURITY.md) 和[发布检查清单](OPEN_SOURCE_CHECKLIST.md)。

## 许可证

MIT，详见 [LICENSE](LICENSE)。
