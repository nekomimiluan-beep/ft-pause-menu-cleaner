# 自由天地暂停菜单清理

NeoForge 1.21.1 客户端 MOD，用于清理进入游戏后按 ESC 打开的暂停菜单，并兼容 Epic Fight 的自动战斗模式与自动视角切换。

## 功能

- 隐藏暂停菜单里的“提供反馈”和“报告漏洞”。
- 将“举报玩家”按钮替换成 Epic Fight 视角切换按钮。
- 绿色按钮为“自动切换第三人称”，会开启 Epic Fight 的 `camera_auto_switch`。
- 红色按钮为“手动切换第三人称”，会关闭 Epic Fight 的 `camera_auto_switch`。
- 在 Epic Fight 自适应模式下，自动根据手持战斗/挖掘物品进出战斗模式。
- VR 模式启用时，独立禁用自动切换战斗模式，并把 Epic Fight 切回原版模式。
- 提供游戏内配置界面，可恢复或关闭上述暂停菜单清理项。

## 兼容目标

- Minecraft `1.21.1`
- NeoForge `21.1.x`
- Epic Fight，作为可选兼容项
- Vivecraft，作为可选 VR 检测项

Epic Fight 和 Vivecraft 都通过反射兼容，未安装时不会影响暂停菜单清理本体运行。

## 构建

需要 JDK 21。脚本不会写死客户端路径，可以通过参数传入 `.minecraft` 目录：

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

输出文件在 `build\libs`。
