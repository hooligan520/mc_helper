# MC Helper

**一个轻量级 Minecraft 客户端辅助模组，让你告别频繁按 F3**

![Minecraft 1.20.1](https://img.shields.io/badge/Minecraft-1.20.1-green)
![Minecraft 1.21.4](https://img.shields.io/badge/Minecraft-1.21.4-brightgreen)
![Minecraft 1.21.11](https://img.shields.io/badge/Minecraft-1.21.11-brightgreen)
![Forge](https://img.shields.io/badge/Forge-多版本-orange)
![Java](https://img.shields.io/badge/Java-17%20%7C%2021-blue)
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
☽ 时间: 19:30 夜晚 ⚡雷暴
▶ 性能: 85 FPS  512/2048MB
```

### 🔍 方块/实体信息（J 键切换）

准星指向时自动弹出详细面板：
- **方块**：名称、ID、坐标、光照等级（颜色标记）、硬度、推荐工具
- **实体**：名称、ID、分类颜色、血量血条、护甲值

### 💡 光照叠加层（O 键切换）

| 颜色 | 光照 | 含义 |
|------|------|------|
| 🔴 红色 | 0 | 必定刷怪 |
| 🟡 黄色 | 1~7 | 夜晚可能刷怪 |
| 🟢 绿色 | ≥8 | 安全 |

### ⚔️ 装备耐久 HUD（K 键切换）

实时监控六件装备耐久，低耐久闪烁预警，右下角显示。

### 🗺️ 迷你地图（M 键切换）

- MapColor 着色，右上角俯视地图
- 玩家实时位置 + 朝向箭头，**零延迟**
- **异步渲染**，开启后 FPS 几乎无影响

### 👾 刷怪检测（N 键切换）

实时统计周围 32 格内的敌对生物，按类型分组，颜色标记危险等级。

### 🔨 合成查询（R 键切换）

**手持任意物品**时按 R，自动显示该物品的合成配方：
- 有序合成（2×2 / 3×3 格子可视化）
- 无序合成
- 烧炼配方（熔炉 / 高炉 / 烟熏炉，显示经验值）
- 多个配方时显示第一种并标注总数量
- ⚠️ 仅支持单人模式（多人服务器暂不支持）

### 🏗️ 建筑辅助（B 键切换）

- **地面网格线**：玩家脚下 8 格范围，辅助方块对齐
- **水平高度线**：玩家当前 Y 高度的青色参考线，建筑找平神器
- **准心高亮**：黄色半透明覆盖当前指向的方块
- **右下角 HUD**：实时 Y 高度 + 准心方块坐标

---

## 🎮 快捷键

| 按键 | 功能 | 默认 |
|------|------|------|
| **H** | 坐标 HUD | 开 |
| **J** | 方块/实体信息 | 开 |
| **O** | 光照叠加层 | 关 |
| **K** | 装备耐久 | 开 |
| **M** | 迷你地图 | 关 |
| **N** | 刷怪检测 | 关 |
| **R** | 合成查询 | 关 |
| **B** | 建筑辅助 | 关 |

> 所有快捷键可在 **设置 → 控制 → 按键设置** 中自定义。

---

## 🌍 支持版本

| MC 版本 | Forge | Java |
|---------|-------|------|
| **1.20.1** | 47.4.18 | 17 |
| **1.21.4** | 54.1.16 | 21 |
| **1.21.11** | 61.1.5 | 21 |

---

## 📦 安装方法

1. 根据你的 Minecraft 版本安装对应的 Forge（通过 HMCL、MultiMC 等启动器）
2. 从 [Releases](https://github.com/hooligan520/mc_helper/releases) 下载对应版本的 jar：
   - `mchelper-x.x.x-mc1.20.1.jar` → 1.20.1
   - `mchelper-x.x.x-mc1.21.4.jar` → 1.21.4
   - `mchelper-x.x.x-mc1.21.11.jar` → 1.21.11
3. 放入 `.minecraft/mods/` 目录
4. 启动游戏即可

---

## 🔨 自行构建

克隆仓库（`main` 分支，包含所有版本代码）：

```bash
git clone https://github.com/hooligan520/mc_helper.git
cd mc_helper
```

使用统一构建脚本，按目标版本传参：

```bash
# 构建 1.20.1 版本（需要 JDK 17）
./build.sh mc1_20_1

# 构建 1.21.4 版本（需要 JDK 21）
./build.sh mc1_21_4

# 构建 1.21.11 版本（需要 JDK 21）
./build.sh mc1_21_11
```

构建产物在 `build/libs/` 目录下，文件名含版本后缀（如 `mchelper-0.1.0-mc1.21.11.jar`）。

### 项目结构

```
src/
├── main/          # 共享代码（工具类 + 翻译文件）
├── mc1_20_1/      # 1.20.1 专用代码
├── mc1_21_4/      # 1.21.4 专用代码
└── mc1_21_11/     # 1.21.11 专用代码

build.gradle.fg6   # ForgeGradle 6 配置（1.20.1）
build.gradle.fg7   # ForgeGradle 7 配置（1.21.4 / 1.21.11）
build.sh           # 统一构建入口
```

---

## ⚙️ 配置文件

`.minecraft/config/mchelper-client.toml` 支持配置：
- 各功能默认开关
- HUD 位置（posX / posY）
- 光照渲染距离（默认 16，最大 64）
- 刷怪检测半径（默认 32，最大 128）
- 迷你地图尺寸（默认 100px，范围 60~200）

---

## 🗺️ 开发路线

- [x] 坐标 HUD（含群系/维度/朝向/时间/天气）
- [x] 方块/实体信息面板
- [x] 光照等级叠加层
- [x] 装备耐久 HUD
- [x] FPS / 内存监控
- [x] 迷你地图（异步渲染）
- [x] 刷怪检测
- [x] 合成配方查询
- [x] 建筑辅助（网格线 + 水平线 + 方块高亮）
- [x] 多版本支持（1.20.1 / 1.21.4 / 1.21.11）
- [ ] 支持 1.21.1（Forge 52.1.14）
- [ ] 支持 26.1（Forge 62.0.8）

---

## 📄 许可证

[MIT License](LICENSE.txt)

---

## 🙏 致谢

- [Minecraft Forge](https://minecraftforge.net/)
- [MiniHUD](https://www.curseforge.com/minecraft/mc-mods/minihud)、[WAILA](https://www.curseforge.com/minecraft/mc-mods/waila)、[Xaero's Minimap](https://www.curseforge.com/minecraft/mc-mods/xaeros-minimap) — 功能设计参考
