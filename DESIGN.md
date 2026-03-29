# MC Helper — 设计文档

**版本**: v0.3.0  
**最后更新**: 2026-03-29  
**技术栈**: Minecraft 1.20.1 + Forge 47.4.18 + Java 17

---

## 一、整体架构

```
mc_helper/
├── src/main/java/com/ksw/mchelper/
│   ├── MCHelperMod.java                  # 模组主类，生命周期入口
│   ├── ClientEventHandler.java           # 快捷键处理（8 个功能开关）
│   ├── config/
│   │   └── MCHelperConfig.java           # 配置系统（16 个配置项）
│   ├── input/
│   │   └── KeyBindings.java              # 8 个快捷键定义
│   ├── render/
│   │   ├── CoordinatesHudOverlay.java    # 坐标/时间/天气/FPS/内存 HUD
│   │   ├── LookAtInfoOverlay.java        # 方块/实体信息
│   │   ├── LightOverlayRenderer.java     # 光照叠加层
│   │   ├── EquipmentHudOverlay.java      # 装备耐久 HUD
│   │   ├── MobRadarOverlay.java          # 刷怪检测面板
│   │   ├── MinimapOverlay.java           # 迷你地图（异步渲染）
│   │   ├── CraftingOverlay.java          # 合成查询面板
│   │   └── BuildingAssistOverlay.java    # 建筑辅助（3D + HUD）
│   └── util/
│       └── DirectionUtil.java            # 方向工具
└── src/main/resources/
    ├── META-INF/mods.toml
    ├── assets/mchelper/lang/zh_cn.json   # 中文翻译
    ├── assets/mchelper/lang/en_us.json   # 英文翻译
    └── pack.mcmeta
```

---

## 二、核心类设计

### 2.1 MCHelperMod（主类）

注册的组件：
- **Forge EVENT_BUS**：`LightOverlayRenderer`（tick+render）、`MinimapOverlay`（tick+render）、`BuildingAssistOverlay`（render）、`ClientEventHandler`（tick）
- **HUD 覆盖层**（8 个）：通过 `RegisterGuiOverlaysEvent.registerAboveAll()` 注册
- **快捷键**（8 个）：通过 `RegisterKeyMappingsEvent` 注册

### 2.2 MCHelperConfig（配置系统）

| 配置项 | 类型 | 默认 |
|--------|------|------|
| coordinates_hud.show | bool | true |
| coordinates_hud.showDirection | bool | true |
| coordinates_hud.showBiome | bool | true |
| coordinates_hud.showDimension | bool | true |
| coordinates_hud.showTimeWeather | bool | true |
| coordinates_hud.showPerformance | bool | true |
| coordinates_hud.posX/posY | int | 10 |
| lookat_info.show | bool | true |
| light_overlay.show | bool | false |
| light_overlay.renderDistance | int | 16 |
| equipment_hud.show | bool | true |
| mob_radar.show | bool | false |
| mob_radar.distance | int | 32 |
| minimap.show | bool | false |
| minimap.size | int | 100 |
| crafting.show | bool | false |
| building_assist.show | bool | false |

---

## 三、各模块设计

### 3.1 坐标 HUD（CoordinatesHudOverlay）

动态行数面板，按配置过滤，自适应宽度。时间/天气/FPS/内存数据直接从 `mc.level` 和 `Runtime` 读取，每帧刷新。

### 3.2 方块/实体信息（LookAtInfoOverlay）

触发条件：`mc.hitResult` 非 MISS，F3 未开启。  
光照只显示**方块光照**（`LightLayer.BLOCK`），这是怪物刷怪的实际依据。

### 3.3 光照叠加层（LightOverlayRenderer）

双事件：`ClientTickEvent`（每 10 tick 扫描）+ `RenderLevelStageEvent`（绘制 `debugQuads`）。  
`forceRefresh` 开启时立即扫描，`clearCache()` 关闭时清空。

### 3.4 装备耐久 HUD（EquipmentHudOverlay）

扫描 6 个槽位，过滤 `isDamageableItem()`，右下角渲染，耐久 < 15% 闪烁。

### 3.5 刷怪检测（MobRadarOverlay）

```java
mc.level.getEntities(player, player.getBoundingBox().inflate(radius),
    e -> e instanceof Monster)
```
按名称统计，降序排列，危险阈值：≥10 红色，≥5 黄色。

### 3.6 迷你地图（MinimapOverlay）⭐

异步扫描 + GPU 纹理，详见 v0.2.0 设计说明。

**NativeImage 格式注意**：`setPixelRGBA` 接受 ABGR，需将 ARGB 的 R/B 通道互换。

### 3.7 合成查询（CraftingOverlay）

**触发条件**：`showCrafting = true` 且手持物品不为空。

**配方查找**：
```java
// 1.20.1 中 RecipeManager.getRecipes() 直接返回 Collection<Recipe<?>>
for (Recipe<?> recipe : manager.getRecipes()) {
    ItemStack result = recipe.getResultItem(RegistryAccess.EMPTY);
    if (result.is(item.getItem())) { ... }
}
```

**支持的配方类型**：
- `ShapedRecipe`：有序合成，展开为 `gridSize × gridSize` 网格渲染
- `ShapelessRecipe`：无序合成，铺满 3×3 网格显示
- `AbstractCookingRecipe`：烧炼（`BlastingRecipe`/`SmokingRecipe`/其他），线性显示输入→输出+经验

**渲染**：格子背景 + `renderItem()` 绘制物品图标，与原版合成书风格一致。

### 3.8 建筑辅助（BuildingAssistOverlay）

**3D 渲染**（`RenderLevelStageEvent.AFTER_TRANSLUCENT_BLOCKS`）：

| 元素 | 范围 | 颜色 | 实现 |
|------|------|------|------|
| 地面网格 | 玩家脚下 ±8 格 | 白蓝色，alpha=50 | `debugQuads` 细片 |
| 水平高度线 | 玩家 Y ±12 格 | 青色，alpha=80 | 四边方向各一排线 |
| 准心方块高亮 | 当前指向方块 | 黄色，alpha=60 | 方块顶面单个 quad |

**HUD 信息**（`IGuiOverlay`）：
- 右下角显示当前 Y 高度（装备 HUD 上方）
- 准心指向方块时额外显示目标坐标

---

## 四、快捷键系统

| 对象 | 默认键 | 功能 |
|------|--------|------|
| TOGGLE_COORDINATES | H | 坐标 HUD |
| TOGGLE_LIGHT_OVERLAY | O | 光照叠加层 |
| TOGGLE_LOOKAT_INFO | J | 方块/实体信息 |
| TOGGLE_EQUIPMENT | K | 装备耐久 |
| TOGGLE_MINIMAP | M | 迷你地图 |
| TOGGLE_MOB_RADAR | N | 刷怪检测 |
| TOGGLE_CRAFTING | R | 合成查询 |
| TOGGLE_BUILDING_ASSIST | B | 建筑辅助 |

> ⚠️ 修改默认键位后需同步更新 `.minecraft/options.txt` 中的 `key_key.mchelper.*` 缓存。

---

## 五、构建与部署

```bash
export JAVA_HOME=/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
./gradlew build --no-daemon -Dnet.minecraftforge.gradle.check.certs=false
cp build/libs/mchelper-0.1.0.jar ~/Games/minecraft/.minecraft/mods/
```

---

## 六、已知问题与技术说明

| 问题 | 说明 |
|------|------|
| 光照只显示方块光照 | 方块光照才是怪物刷怪依据，天空光照昼夜均返回 15 不适合显示 |
| 缩放坐标补偿 | `scale(0.4f)` 后，HUD 坐标需 ÷ 0.4 才对应屏幕像素 |
| 快捷键缓存 | 首次加载写入 options.txt，改代码默认值不自动生效 |
| 迷你地图多线程 | Level 只读操作线程安全；纹理上传严格在主线程 |
| NativeImage 格式 | `setPixelRGBA` 接受 ABGR，ARGB 需转换（R/B 互换） |
| 合成 API | 1.20.1 `getRecipes()` 返回 `Collection<Recipe<?>>` |
