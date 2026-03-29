# MC Helper

**一个轻量级 Minecraft 客户端辅助模组，让你告别频繁按 F3**

![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-green)
![Forge](https://img.shields.io/badge/Forge-47.4.18-orange)
![Java](https://img.shields.io/badge/Java-17-blue)
![License](https://img.shields.io/badge/License-MIT-lightgrey)

---

## ✨ 功能一览

### 📍 坐标 HUD（H 键切换）

精确坐标、方块坐标、朝向、生物群系、维度、游戏时间、天气、FPS 和内存，全部一屏展示。

```
✦ MC Helper
⌖ XYZ: -136.4 / 67.0 / 33.5
⌖ Block: -137 / 67 / 33
➔ 朝向: 东北 (230.9°)
☘ 群系: 平原
✦ 维度: 主世界
☽ 时间: 19:30 §9夜晚 §8⚡雷暴
▶ 性能: §a85 FPS  §a512/2048MB
```

### 🔍 方块/实体信息（J 键切换）

准星指向方块或生物时，自动弹出详细信息面板。

- **方块**：名称、ID、坐标、光照等级（颜色标记安全性）、硬度、推荐工具
- **实体**：名称、ID、分类（敌对/友好/玩家/中立）、血量血条、护甲值

### 💡 光照叠加层（O 键切换）

一键显示周围方块的刷怪危险等级：

| 颜色 | 光照等级 | 含义 |
|------|----------|------|
| 🔴 红色 | 0 | 危险！此处必定刷怪 |
| 🟡 黄色 | 1 ~ 7 | 夜晚可能刷怪 |
| 🟢 绿色 | ≥ 8 | 安全 |

### ⚔️ 装备耐久 HUD（K 键切换）

实时监控武器、工具、护甲的耐久状态，低耐久闪烁预警，再也不会在关键时刻装备断裂。

### 🗺️ 迷你地图（M 键切换）

右上角俯视地图，实时显示周围地形。

- MapColor 着色，与原版地图颜色完全一致
- 玩家实时位置（中心）+ 朝向箭头，**零延迟**
- 高度明暗对比，地形一目了然
- **异步渲染**，开启后 FPS 几乎无影响

### 👾 刷怪检测（N 键切换）

实时统计周围 32 格内的敌对生物数量，按类型分组显示。

```
⚠ 刷怪检测  (半径32格)
合计: §c12 只
  僵尸: §c6
  骷髅: §c4
  蜘蛛: §e2
```

---

## 🎮 快捷键

| 按键 | 功能 |
|------|------|
| **H** | 坐标 HUD 开/关 |
| **J** | 方块/实体信息 开/关 |
| **O** | 光照叠加层 开/关 |
| **K** | 装备耐久 HUD 开/关 |
| **M** | 迷你地图 开/关 |
| **N** | 刷怪检测 开/关 |

> 所有快捷键可在 **设置 → 控制 → 按键设置** 中自定义。

---

## 📦 安装方法

### 前置要求

- Minecraft Java Edition **1.20.1**
- **Forge 1.20.1-47.x.x**（通过 HMCL、MultiMC 等启动器安装）

### 安装步骤

1. 从 [Releases](https://github.com/hooligan520/mc_helper/releases) 下载最新的 `mchelper-x.x.x.jar`
2. 将 jar 文件放入 `.minecraft/mods/` 目录
3. 启动 **1.20.1 Forge** 版本
4. 进入游戏，按 **H / J / O / K / M / N** 体验各功能

---

## 🔨 自行构建

```bash
# 克隆仓库
git clone https://github.com/hooligan520/mc_helper.git
cd mc_helper

# 需要 JDK 17
export JAVA_HOME=/path/to/jdk17

# 构建
./gradlew build --no-daemon

# 输出 JAR 在 build/libs/mchelper-x.x.x.jar
```

---

## ⚙️ 配置文件

游戏启动后，配置文件自动生成于：

```
.minecraft/config/mchelper-client.toml
```

可配置项包括：
- 各功能的默认开关状态
- HUD 显示位置（posX / posY）
- 光照叠加层渲染距离（默认 16 格，最大 64 格）
- 刷怪检测半径（默认 32 格，最大 128 格）
- 迷你地图尺寸（默认 100px，范围 60~200）

---

## 🗺️ 开发路线

- [x] 坐标 HUD（含群系、维度、朝向）
- [x] 方块/实体信息面板
- [x] 光照等级叠加层（性能优化版）
- [x] 装备耐久 HUD
- [x] 游戏时间 / 天气显示
- [x] FPS / 内存性能监控
- [x] 迷你地图（异步渲染，低性能损耗）
- [x] 刷怪检测
- [ ] 合成配方查询
- [ ] 建筑辅助

---

## 📄 许可证

本项目基于 [MIT License](LICENSE.txt) 开源。

---

## 🙏 致谢

- [Minecraft Forge](https://minecraftforge.net/) — 模组加载器
- [MiniHUD](https://www.curseforge.com/minecraft/mc-mods/minihud)、[WAILA](https://www.curseforge.com/minecraft/mc-mods/waila)、[Xaero's Minimap](https://www.curseforge.com/minecraft/mc-mods/xaeros-minimap) — 功能设计参考
