# Cobblemon Team Preview

A Fabric mod for Minecraft 1.21.1 that lets you **preview a trainer's team before you accept a battle**.

Right-click a trainer NPC and a panel opens showing the Pokémon they are about to send out — level, held item, ability, nature, IVs and EVs — with **Continue Battle** / **Cancel** buttons. No more walking into a trainer blind.

---

## Features

- **Right-click a trainer NPC** to open the "Trainer Team" preview panel.
- Shows, per Pokémon: **level, held item, ability, nature, IVs, EVs** (each row can be toggled off).
- **Continue Battle** starts the battle as usual; **Cancel** simply closes the panel.
- **Block battle on sight** — optional switch that stops trainers from forcing a battle the moment they spot you.
- Fully configurable from the **Team Preview Settings** screen (keybind, default **K**, or the button in Mod Menu).
- Pure client + server-safe logic; the preview is built from the trainer data the server already syncs.

## Requirements

| Component | Version |
|---|---|
| Minecraft | 1.21.1 |
| Fabric Loader | 0.15.0 or newer |
| Fabric API | any 1.21.1 build |
| [Cobblemon](https://modrinth.com/mod/cobblemon) | 1.8.1 – 1.8.x |
| [Radical Cobblemon Trainers (RCT)](https://modrinth.com/mod/rctmod) | 0.19.2-beta – 0.19.x |
| RCT API (rctapi) | 0.16.1-beta – 0.16.x |
| [Mod Menu](https://modrinth.com/mod/modmenu) | *optional* — adds a config button |

The dependency ranges above are the versions this build was compiled and tested against. Version ranges are enforced by Fabric at startup: anything outside them is refused before the game loads.

## Installation

1. Install Fabric Loader for Minecraft 1.21.1.
2. Drop the following into your `mods/` folder:
   - this mod's jar,
   - Fabric API,
   - Cobblemon,
   - Radical Cobblemon Trainers + RCT API.
3. Optionally install Mod Menu to reach the settings from the mod list.

## Configuration

Open **Team Preview Settings** with the keybind (default **K**) or from the mod's config button.

| Option | Default | Description |
|---|---|---|
| Enable Team Preview | on | Master switch for the whole preview flow |
| Block battle on sight | off | Stop trainers auto-starting a battle when they spot you |
| Show IVs | on | Show individual values in the panel |
| Show EVs | on | Show effort values in the panel |
| Show held items | on | Show each Pokémon's held item |
| Show abilities | on | Show each Pokémon's ability |
| Show natures | on | Show each Pokémon's nature |

## Building from source

```bash
./gradlew build
```

Cobblemon, RCT and RCT API are not pulled from Maven here; they are consumed from local jars referenced in `build.gradle`'s `_deps` entries, because the target modpack ships its own copies.

## Compatibility notes

- Also works alongside NPC/dialogue mods: **hold Shift + right-click** to hand the interaction over to the other mod, plain right-click still opens the preview.
- This release is the **Fabric** build only, for Minecraft 1.21.1 on the Cobblemon 1.8.x line.

## License

MIT — see [LICENSE](LICENSE).

## Credits

- **Author:** nightpoem
- Built for [Cobblemon](https://modrinth.com/mod/cobblemon) and [Radical Cobblemon Trainers](https://modrinth.com/mod/rctmod).

*This project is a fan-made add-on. It is not affiliated with, endorsed by, or associated with Cobblemon, Radical Cobblemon Trainers, Nintendo, Game Freak or The Pokémon Company.*

---
---

# Cobblemon 队伍预览

一个面向 Minecraft 1.21.1 的 Fabric 模组：**在答应开战之前，先看一眼训练家的队伍**。

右键训练家 NPC，会弹出一个面板，列出他即将派出的宝可梦 —— 等级、携带道具、特性、性格、个体值、努力值 —— 底部有 **继续战斗** / **取消** 两个按钮。再也不用盲着眼一头撞进训练家。

## 主要功能

- **右键训练家 NPC** 打开「训练家队伍」预览面板。
- 每只宝可梦展示：**等级、携带道具、特性、性格、个体值、努力值**（每一项都可以单独关掉）。
- **继续战斗** 正常进入对战；**取消** 只是关掉面板。
- **禁止注视自动开战** —— 可选开关，防止训练家看到你就直接强制开战。
- 通过 **队伍预览设置** 界面完整配置（按键默认 **K**，或从 Mod Menu 的按钮进入）。
- 逻辑对服务端友好，只使用服务端本来就同步给客户端的训练家数据。

## 运行需求

| 组件 | 版本 |
|---|---|
| Minecraft | 1.21.1 |
| Fabric Loader | 0.15.0 及以上 |
| Fabric API | 任意 1.21.1 版本 |
| [Cobblemon](https://modrinth.com/mod/cobblemon) | 1.8.1 – 1.8.x |
| [激进训练家 RCT](https://modrinth.com/mod/rctmod) | 0.19.2-beta – 0.19.x |
| RCT API (rctapi) | 0.16.1-beta – 0.16.x |
| [Mod Menu](https://modrinth.com/mod/modmenu) | *可选* —— 提供配置入口 |

上表中的范围是本版本编译、验证过的版本。Fabric 会在启动阶段强制校验该范围：范围外的版本会在游戏加载前直接被拒绝。

## 安装

1. 为 Minecraft 1.21.1 安装 Fabric Loader。
2. 把以下内容放进 `mods/` 目录：
   - 本模组的 jar；
   - Fabric API；
   - Cobblemon；
   - 激进训练家（rctmod）+ RCT API。
3. 可选：安装 Mod Menu，即可从模组列表进入设置。

## 配置说明

用按键（默认 **K**）或模组配置按钮打开 **队伍预览设置**。

| 选项 | 默认 | 说明 |
|---|---|---|
| 启用队伍预览 | 开 | 整个预览流程的总开关 |
| 禁止注视自动开战 | 关 | 阻止训练家看到你时自动开战 |
| 显示个体值 | 开 | 面板中显示个体值 |
| 显示努力值 | 开 | 面板中显示努力值 |
| 显示携带道具 | 开 | 显示每只宝可梦的携带道具 |
| 显示特性 | 开 | 显示每只宝可梦的特性 |
| 显示性格 | 开 | 显示每只宝可梦的性格 |
