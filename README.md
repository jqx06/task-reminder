# 事项提醒

一个简洁、离线优先的 Android 待办事项应用。

## 功能

- 今天、本周、长期和周期任务
- 可调整截止时间、顺序和紧迫度
- 根据截止时间动态提高紧迫度
- 完成记录、恢复和永久删除
- 数据只保存在设备本地
- 每 24 小时自动检查新版 APK，也可在首页主动检查
- 独立课表页面：按周查看课程，支持前后周切换
- 从 `.xls` / `.xlsx` 导入课表，自动生成课程待办
- 大学物理课程自动提前一周生成 MOOC 学习任务

## 导入课表

在首页点击“导入课表”，选择 Excel 文件，输入本学期第一周周一的日期并确认。应用只生成尚未结束的课程，重复导入会自动跳过已有事项；课表文件仅在手机本地解析。

## 安装

打开 [Releases](https://github.com/jqx06/task-reminder/releases/latest)，下载 `task-reminder-1.9.0.apk`。首次安装可能需要允许浏览器安装未知来源应用。

覆盖安装新版可保留任务数据；不要先卸载旧版。

## 构建

需要 JDK、Android SDK 以及 Gradle 9.5：

```bash
gradle assembleDebug
```

生成文件位于 `app/build/outputs/apk/debug/app-debug.apk`。

## 隐私

任务数据通过 WebView 的本地存储保存在设备中。应用联网仅用于读取：

`https://raw.githubusercontent.com/jqx06/task-reminder/main/update.json`

更新检查不会上传任务内容。

## 更新发布

1. 提高 `app/build.gradle` 中的 `versionCode` 和 `versionName`。
2. 使用与旧版本相同的签名密钥构建 APK。
3. 将 APK 放到仓库根目录。
4. 修改 `update.json` 的版本号和下载地址。
5. 提交并推送。

> 必须始终使用同一签名密钥，否则 Android 无法覆盖安装。

## License

MIT
