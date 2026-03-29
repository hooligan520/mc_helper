# MC Helper — 设计文档

**版本**: v0.2.0  
**最后更新**: 2026-03-29  
**技术栈**: Minecraft 1.20.1 + Forge 47.4.18 + Java 17

---

## 一、整体架构

```
mc_helper/
├── src/main/java/com/ksw/mchelper/
│   ├── MCHelperMod.java              # 模组主类，生命周期入口
│   ├── ClientEventHandler.java       # 客户端运行时事件（快捷键）
│   ├── config/
│   │   └── MCHelperConfig.java       # 配置系统
│   ├── input/
│   │   └── KeyBindings.java          # 快捷键定义
│   ├── render/
│   │   ├── CoordinatesHudOverlay.java  # 坐标 HUD（含时间/天气/FPS/内存）
│   │   ├── LookAtInfoOverlay.java      # 方块/实体信息
│   │   ├── LightOverlayRenderer.java   # 光照叠加层
│   │   ├── EquipmentHudOverlay.java    # 装备耐久 HUD
│   │   ├── MobRadarOverlay.java        # 刷怪检测面板
│   │   └── MinimapOverlay.java         # 迷你地图
│   └── util/
│       └── DirectionUtil.java          # 方向工具
└── src/main/resources/
    ├── META-INF/mods.toml              # 模组元数据
    ├── assets/mchelper/lang/
    │   ├── zh_cn.json                  # 中文翻译
    │   └── en_us.json                  # 英文翻译
    └── pack.mcmeta                     # 资源包元数据
```

---

## 二、核心类设计

### 2.1 MCHelperMod（主类）

```java
@Mod(MCHelperMod.MODID)
public class MCHelperMod {
    public static final String MODID = "mchelper";
    private static final LightOverlayRenderer lightOverlayRenderer = new LightOverlayRenderer();
    private static final MinimapOverlay minimapOverlay = new MinimapOverlay();

    public static LightOverlayRenderer getLightOverlayRenderer();
    public static MinimapOverlay getMinimapOverlay();
}
```

- 注册客户端配置（CLIENT 类型）
- 向 `MinecraftForge.EVENT_BUS` 注册：`ClientEventHandler`、`LightOverlayRenderer`、`MinimapOverlay`
- 向 Mod 事件总线注册：6 个 HUD 覆盖层、6 个快捷键

### 2.2 MCHelperConfig（配置系统）

配置文件类型为 `CLIENT`，存储在 `.minecraft/config/mchelper-client.toml`。

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| coordinates_hud.show | bool | true | 坐标 HUD 开关 |
| coordinates_hud.showDirection | bool | true | 朝向显示 |
| coordinates_hud.showBiome | bool | true | 群系显示 |
| coordinates_hud.showDimension | bool | true | 维度显示 |
| coordinates_hud.showTimeWeather | bool | true | 时间/天气显示 |
| coordinates_hud.showPerformance | bool | true | FPS/内存显示 |
| coordinates_hud.posX | int | 10 | HUD X 坐标 |
| coordinates_hud.posY | int | 10 | HUD Y 坐标 |
| lookat_info.show | bool | true | 方块信息开关 |
| light_overlay.show | bool | false | 光照叠加层开关 |
| light_overlay.renderDistance | int | 16 | 光照渲染距离 |
| equipment_hud.show | bool | true | 装备耐久开关 |
| mob_radar.show | bool | false | 刷怪检测开关 |
| mob_radar.distance | int | 32 | 检测半径 |
| minimap.show | bool | false | 迷你地图开关 |
| minimap.size | int | 100 | 地图尺寸（像素） |

---

## 三、HUD 渲染设计

### 3.1 渲染框架

所有 HUD 使用 `RegisterGuiOverlaysEvent.registerAboveAll()` 注册为 `IGuiOverlay`。

**统一视觉风格**：
- 背景：渐变色（深蓝紫 `0xCC1A1A2E` → 深蓝 `0xCC16213E`）
- 外边框：深蓝 `0xFF0F3460`，内高亮：紫色 `0xFF533483`
- HUD 缩放：0.4x（`PoseStack.scale()`，坐标补偿 = 实际坐标 / 0.4）

### 3.2 坐标 HUD（CoordinatesHudOverlay）

扩展显示行（可通过配置开关）：
- 时间：`level.getDayTime() % 24000` 换算为 HH:MM，`isRaining()`/`isThundering()` 判断天气
- FPS：`mc.getFps()`，颜色分级
- 内存：`Runtime.getRuntime().totalMemory() - freeMemory()` / `maxMemory()`

### 3.3 方块/实体信息（LookAtInfoOverlay）

光照显示使用 `getBrightness(LightLayer.BLOCK, pos.above())`，只显示**方块光照**（不含天空光照），这才是怪物刷怪的实际判断依据。

### 3.4 光照叠加层（LightOverlayRenderer）

双事件监听：`ClientTickEvent`（每 10 tick 扫描）+ `RenderLevelStageEvent`（绘制 debugQuads）。  
`forceRefresh` 标志：开启时立即扫描；`clearCache()` 关闭时清空。

### 3.5 装备耐久 HUD（EquipmentHudOverlay）

扫描 6 个槽位（主手/副手/头盔/胸甲/护腿/靴子），过滤 `isDamageableItem()`，右下角渲染，耐久 < 15% 闪烁。

### 3.6 刷怪检测（MobRadarOverlay）

```java
mc.level.getEntities(player, player.getBoundingBox().inflate(radius),
    e -> e instanceof Monster)
```
按名称统计，降序排列，危险阈值：≥10 红色，≥5 黄色，否则绿色。

### 3.7 迷你地图（MinimapOverlay）⭐ 性能关键

**架构**：异步扫描 + GPU 纹理渲染

```
ClientTickEvent（每 40 tick 或移动 16 格）
    └─ AtomicBoolean 防重入
    └─ CompletableFuture.runAsync() ─→ 后台线程
           └─ scanMap(): 逐列读取 Heightmap + MapColor
           └─ 结果写入 volatile int[] pendingColors

渲染帧（IGuiOverlay）
    ├─ 检测 pendingColors 不为 null？
    │       └─ 主线程上传 NativeImage → DynamicTexture → GPU
    ├─ guiGraphics.blit(mapTextureId, ...)  ← 一次 draw call
    ├─ drawPlayerArrow(mc.player.getYRot())  ← 实时，无滞后
    └─ 坐标文字（mc.player.blockPosition()，实时）
```

**NativeImage 格式**：ABGR（Minecraft 的 `setPixelRGBA` 接受 ABGR），需将 ARGB 转换。

**性能对比**：

| 方案 | 主线程开销 | GPU draw call |
|------|-----------|---------------|
| 旧（每帧 fill） | 扫描阻塞 + 10000 draw | 10000 次/帧 |
| 新（异步+纹理） | 近零（仅纹理上传，每 2 秒一次） | 1 次/帧 |

---

## 四、快捷键系统

| 快捷键对象 | 默认键 | 功能 |
|-----------|--------|------|
| TOGGLE_COORDINATES | H | 坐标 HUD |
| TOGGLE_LIGHT_OVERLAY | O | 光照叠加层 |
| TOGGLE_LOOKAT_INFO | J | 方块/实体信息 |
| TOGGLE_EQUIPMENT | K | 装备耐久 |
| TOGGLE_MINIMAP | M | 迷你地图 |
| TOGGLE_MOB_RADAR | N | 刷怪检测 |

> ⚠️ 修改代码默认键位后，需同步修改 `.minecraft/options.txt` 中的 `key_key.mchelper.*` 缓存值。

---

## 五、构建与部署

```bash
# 构建
export JAVA_HOME=/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
./gradlew build --no-daemon -Dnet.minecraftforge.gradle.check.certs=false

# 部署
cp build/libs/mchelper-0.1.0.jar ~/Games/minecraft/.minecraft/mods/
```

---

## 六、已知问题与技术说明

| 问题 | 说明 |
|------|------|
| 光照只显示方块光照 | 方块光照才是怪物刷怪依据，天空光照昼夜均返回 15，不适合显示 |
| 缩放坐标补偿 | `PoseStack.scale(0.4f)` 后，坐标需 / 0.4 对应屏幕像素 |
| 快捷键缓存 | 首次加载写入 options.txt，改代码默认值不自动更新 |
| 迷你地图多线程 | Level 读操作对只读多线程安全；写操作（纹理上传）严格在主线程 |

---

## 七、后续开发计划

| 功能 | 优先级 | 说明 |
|------|--------|------|
| 合成查询 | P1 | 查询物品合成配方 |
| 建筑辅助 | P2 | 网格线/水平指示 |
