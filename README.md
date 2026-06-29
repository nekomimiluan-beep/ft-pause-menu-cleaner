# 自由天地暂停菜单清理

NeoForge 1.21.1 客户端 MOD，用于清理进入游戏后按 ESC 打开的暂停菜单，并兼容 Epic Fight 的自动战斗模式、自动视角切换和 Vivecraft VR 模式。

当前源码版本：`1.2.11`

## 主要功能

### 暂停菜单清理

- 隐藏 ESC 暂停菜单里的“提供反馈”按钮。
- 隐藏 ESC 暂停菜单里的“报告漏洞”按钮。
- 将“举报玩家”按钮替换为 Epic Fight 视角切换按钮。
- 提供游戏内配置界面，可以单独恢复或关闭这些菜单改动。

### 举报按钮替换为视角切换按钮

原版“举报玩家”按钮的位置会变成一个切换按钮：

- 绿色文字：“自动切换第三人称”
- 红色文字：“手动切换第三人称”

按钮会实时联动 Epic Fight 的客户端设置：

- 绿色模式会开启 Epic Fight 的 `camera_auto_switch`。
- 红色模式会关闭 Epic Fight 的 `camera_auto_switch`。
- 切换后会同时更新 Epic Fight 当前运行时配置和 `config/epicfight-client.toml`。
- 第一次加载本 MOD 时，默认会把 Epic Fight 自动切换视角设置为开启。

### Epic Fight 自适应战斗模式兼容

当 Epic Fight 的玩家行为策略是自适应模式，也就是 `ADAPTIVE` 时，本 MOD 会让“自适应准星/自适应战斗模式”和“自动切换第三人称”共存：

- 手持 Epic Fight 识别为战斗分类的物品时，自动进入 Epic Fight 战斗模式。
- 手持 Epic Fight 识别为挖掘分类的主手物品时，自动回到原版模式。
- 绿色模式下，进入战斗模式时会切到第三人称背后视角，回到原版模式时会切回第一人称。
- 红色模式下，仍然会自动进出战斗模式，但不会自动切换第三人称/第一人称视角。
- 玩家手动切换战斗模式后，只要手持物品没有再次发生变化，本 MOD 不会每 tick 抢回战斗模式。

### Vivecraft VR 模式保护

检测到 Vivecraft VR 正在运行时，会启用一个独立保护逻辑：

- 只在 `org.vivecraft.client_vr.VRState.VR_RUNNING` 为 `true` 时生效。
- VR 模式开启时禁用本 MOD 的自动进出战斗模式逻辑。
- 如果当前已经处于 Epic Fight 战斗模式，会切回原版模式。
- 切回原版模式时不会顺手改动 Epic Fight 的自动视角配置。
- 这个功能独立于菜单清理和红绿按钮，不会影响普通非 VR 模式。

Epic Fight 和 Vivecraft 都是可选兼容项。未安装 Epic Fight 或 Vivecraft 时，本 MOD 会通过反射失败保护跳过对应功能，暂停菜单清理本体仍可运行。

## 配置文件

本 MOD 会生成配置文件：

```text
config/ft_pause_menu_cleaner.properties
```

配置项：

```properties
hideSendFeedback=true
hideReportBugs=true
replaceReportPlayerWithPerspectiveToggle=true
appliedEpicFightPerspectiveDefault=true
```

说明：

- `hideSendFeedback`：是否隐藏“提供反馈”。
- `hideReportBugs`：是否隐藏“报告漏洞”。
- `replaceReportPlayerWithPerspectiveToggle`：是否把“举报玩家”替换成红绿视角按钮。
- `appliedEpicFightPerspectiveDefault`：是否已经应用过 Epic Fight 自动视角默认开启设置，主要用于避免每次启动都覆盖玩家设置。

也可以从游戏内 MOD 配置界面修改前三个菜单清理选项。

## 兼容目标

- Minecraft `1.21.1`
- NeoForge `21.1.x`
- Epic Fight，可选
- Vivecraft，可选
- JDK `21`，用于编译

## 源码结构

```text
src/main/java/cn/ziyoutiandi/pausecleaner/
  PauseMenuCleanerMod.java
  PauseMenuCleanerConfig.java
  PauseMenuCleanerConfigScreen.java
  EpicFightPerspectiveBridge.java
  EpicFightPerspectiveCompatibilityPatch.java
  VivecraftVrModeGuard.java

src/main/resources/
  META-INF/neoforge.mods.toml
  assets/ft_pause_menu_cleaner/lang/
```

核心类说明：

- `PauseMenuCleanerMod`：注册客户端事件，处理暂停菜单按钮隐藏和替换。
- `PauseMenuCleanerConfig`：读取和保存本 MOD 的 properties 配置。
- `PauseMenuCleanerConfigScreen`：游戏内配置界面。
- `EpicFightPerspectiveBridge`：读取、修改 Epic Fight 自动视角配置。
- `EpicFightPerspectiveCompatibilityPatch`：处理 Epic Fight 自适应战斗模式与自动视角共存逻辑。
- `VivecraftVrModeGuard`：检测 Vivecraft VR 是否正在运行。

## 构建方式

需要 JDK 21。构建脚本不会写死客户端路径，可以通过参数传入 `.minecraft` 目录：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\build.ps1 -MinecraftDir "H:\mcziyou\.minecraft"
```

也可以设置 `MINECRAFT_HOME` 环境变量后直接运行：

```powershell
$env:MINECRAFT_HOME = "H:\mcziyou\.minecraft"
powershell -ExecutionPolicy Bypass -File .\scripts\build.ps1
```

脚本会扫描：

- `.minecraft\libraries`
- `.minecraft\versions`
- `.minecraft\mods`
- `.minecraft\mods\1.21.1`

输出文件在：

```text
build/libs/ft-pause-menu-cleaner-neoforge-1.21.1-版本号.jar
```

示例输出：

```text
build/libs/ft-pause-menu-cleaner-neoforge-1.21.1-1.2.11.jar
```

编译时如果出现 Mixin Annotation Processor 或 owo config 注解处理器的 source 版本警告，一般不影响本 MOD jar 输出。

## 注意事项

- 本仓库只提交源码、资源文件、README 和构建脚本。
- `build/`、`backup/`、本地 `classpath*.txt` 不提交，因为它们包含构建产物或本机绝对路径。
- 本 MOD 是客户端 MOD，不应安装到纯服务端作为服务端功能使用。
