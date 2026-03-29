# MC Helper — 设计文档

**版本**: v0.1.0  
**最后更新**: 2026-03-29  
**技术栈**: Minecraft 1.20.1 + Forge 47.4.18 + Java 17

---

## 一、整体架构

```
mc_helper/
├── src/main/java/com/suwenkuang/mchelper/
│   ├── MCHelperMod.java              # 模组主类，生命周期入口
│   ├── ClientEventHandler.java       # 客户端运行时事件（快捷键）
│   ├── config/
│   │   └── MCHelperConfig.java       # 配置系统
│   ├── input/
│   │   └── KeyBindings.java          # 快捷键定义
│   ├── render/
│   │   ├── CoordinatesHudOverlay.java  # 坐标 HUD
│   │   ├── LookAtInfoOverlay.java      # 方块/实体信息
│   │   ├── LightOverlayRenderer.java   # 光照叠加层
│   │   └── EquipmentHudOverlay.java    # 装备耐久 HUD
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

    public static LightOverlayRenderer getLightOverlayRenderer();
}
```

- 注册客户端配置（CLIENT 类型）
- 向 `MinecraftForge.EVENT_BUS` 注册运行时事件监听器（`ClientEventHandler`、`LightOverlayRenderer`）
- 向 Mod 事件总线注册：HUD 覆盖层（`RegisterGuiOverlaysEvent`）、快捷键（`RegisterKeyMappingsEvent`）
- 暴露 `getLightOverlayRenderer()` 供快捷键处理器调用

### 2.2 MCHelperConfig（配置系统）

使用 Forge 的 `ForgeConfigSpec`，配置文件类型为 `CLIENT`，存储在 `.minecraft/config/mchelper-client.toml`。

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| coordinates_hud.show | bool | true | 坐标 HUD 开关 |
| coordinates_hud.showDirection | bool | true | 朝向显示 |
| coordinates_hud.showBiome | bool | true | 群系显示 |
| coordinates_hud.showDimension | bool | true | 维度显示 |
| coordinates_hud.posX | int | 10 | HUD X 坐标 |
| coordinates_hud.posY | int | 10 | HUD Y 坐标 |
| lookat_info.show | bool | true | 方块信息开关 |
| light_overlay.show | bool | false | 光照叠加层开关 |
| light_overlay.renderDistance | int | 16 | 光照渲染距离 |
| equipment_hud.show | bool | true | 装备耐久开关 |

运行时缓存为 `public static` 字段，通过 `onLoad` 事件监听器从配置文件同步。

---

## 三、HUD 渲染设计

### 3.1 渲染框架

所有 HUD 使用 `RegisterGuiOverlaysEvent.registerAboveAll()` 注册为 `IGuiOverlay` 接口实现，在游戏 HUD 最顶层渲染。

**统一视觉风格**：
- 背景：渐变色（深蓝紫 `0xCC1A1A2E` → 深蓝 `0xCC16213E`）
- 外边框：深蓝 `0xFF0F3460`
- 内高亮线：紫色 `0xFF533483`
- 缩放比例：0.4x（通过 `PoseStack.scale()` 实现，坐标补偿 = 实际坐标 / SCALE）

### 3.2 坐标 HUD（CoordinatesHudOverlay）

**渲染流程**：
1. `pushPose()` → `scale(0.4f)` → 补偿坐标
2. 计算要显示的行（按配置项过滤）
3. 根据最长行文本计算面板宽度（`min(MIN_WIDTH, 计算宽度)` = 180）
4. 绘制渐变背景 + 双层边框
5. 逐行绘制：图标前缀 + 彩色文字
6. `popPose()`

**数据来源**：
- XYZ: `mc.player.getX/Y/Z()`
- Block: `mc.player.blockPosition()`
- 朝向: `mc.player.getYRot()` → `DirectionUtil.getDirectionName()`
- 群系: `mc.level.getBiome(blockPos).value()` → 中文名映射表
- 维度: `mc.level.dimension().location()`

### 3.3 方块/实体信息（LookAtInfoOverlay）

**触发条件**: `mc.hitResult` 非 MISS，且 F3 调试界面未开启。

**方块信息数据来源**：
- 方块名/ID: `ForgeRegistries.BLOCKS.getKey(block)`
- 方块光照: `mc.level.getBrightness(LightLayer.BLOCK, pos.above())`（只看方块光照，不含天空光照）
- 硬度: `state.getDestroySpeed(mc.level, pos)`
- 工具: `state.is(BlockTags.MINEABLE_WITH_*)` 检查

**实体信息数据来源**：
- 血量: `living.getHealth()` / `living.getMaxHealth()`
- 护甲: `living.getArmorValue()`
- 分类: `instanceof Monster/Animal/Player`

### 3.4 光照叠加层（LightOverlayRenderer）

**渲染架构**：双事件监听器（Tick + RenderLevel）

```
ClientTickEvent (每10tick)
    ↓ 如果开启 && 玩家移动 >2格
    ↓ scanLightLevels() → 更新 cachedBlocks
    
RenderLevelStageEvent (AFTER_TRANSLUCENT_BLOCKS)
    ↓ 从 cachedBlocks 读取
    ↓ 逐方块绘制 debugQuads 颜色面
```

**扫描算法**：
```
for dx in [-renderDist, renderDist]:
  for dz in [-renderDist, renderDist]:
    if dx²+dz² > renderDist²: continue  // 圆形范围
    for dy in [-4, 4]:
      if pos 是固体方块 && pos.above() 是空气 && pos.above(2) 可通过:
        获取 blockLight
        分类: 0(危险/红) / 1~7(警告/黄) / >=8(安全/绿)
```

**性能优化**：
- `synchronized` 保护缓存列表（Tick 写 / Render 读）
- `forceRefresh` 标志：开启时立即扫描一次
- 关闭时调用 `clearCache()` 立即清除叠加层

### 3.5 装备耐久 HUD（EquipmentHudOverlay）

**装备扫描顺序**：主手 → 副手 → 头盔 → 胸甲 → 护腿 → 靴子  
**过滤条件**：`item.isDamageableItem()` 为 true  
**耐久计算**：`remaining = maxDamage - getDamageValue()`

**位置计算（右下角）**：
```java
int boxX = scaledWidth - BOX_WIDTH - MARGIN_RIGHT;
int boxY = scaledHeight - boxHeight - MARGIN_BOTTOM;  // MARGIN_BOTTOM=40, 避开快捷栏
```

---

## 四、快捷键系统

快捷键使用 GLFW 键码定义，通过 `RegisterKeyMappingsEvent` 注册：

| 快捷键对象 | GLFW 键码 | 默认键 |
|-----------|-----------|--------|
| TOGGLE_COORDINATES | GLFW_KEY_H | H |
| TOGGLE_LIGHT_OVERLAY | GLFW_KEY_O | O |
| TOGGLE_LOOKAT_INFO | GLFW_KEY_J | J |
| TOGGLE_EQUIPMENT | GLFW_KEY_K | K |

**⚠️ 注意**：游戏首次加载模组时会将默认键位写入 `options.txt`，之后修改代码中的默认值不会自动更新。如需更改键位，需同步修改 `.minecraft/options.txt` 中的 `key_key.mchelper.*` 键值，或在游戏设置中手动改绑。

`ClientEventHandler.onClientTick()` 使用 `KeyMapping.consumeClick()` 消费按键事件，避免重复触发。切换时通过 `player.displayClientMessage(..., true)` 在快捷栏上方显示提示文字。

---

## 五、构建与部署

### 开发环境

- macOS + JDK 17（Homebrew `openjdk@17 17.0.18`）
- JDK 路径：`/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`

### 构建命令

```bash
export JAVA_HOME=/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
./gradlew build --no-daemon -Dnet.minecraftforge.gradle.check.certs=false
```

输出：`build/libs/mchelper-0.1.0.jar`（~36K）

### 部署命令

```bash
cp build/libs/mchelper-0.1.0.jar ~/Games/minecraft/.minecraft/mods/
```

---

## 六、已知问题与技术说明

| 问题 | 说明 |
|------|------|
| 光照显示只用方块光照 | `getBrightness(LightLayer.BLOCK, pos)` 返回的是方块光照（不含天空光照），这是怪物刷怪的真正判断依据 |
| 天空光照不用于显示 | `LightLayer.SKY` 返回原始天空光照（不考虑昼夜），白天/夜晚都返回 15，因此不作为主要显示值 |
| 缩放坐标补偿 | 使用 `PoseStack.scale(0.4f)` 后，实际坐标需除以 0.4（即 ×2.5）才能对应屏幕像素位置 |

---

## 七、后续开发计划

| 功能 | 优先级 | 说明 |
|------|--------|------|
| 迷你地图 | P1 | 定时缓存方案，渲染周围地图 |
| 合成查询 | P1 | 查询物品合成配方 |
| 游戏时间/天气 | P2 | HUD 显示 |
| 刷怪检测 | P2 | 周围怪物数量/类型统计 |
| 性能监控 | P2 | FPS/内存 HUD |
| 建筑辅助 | P2 | 网格线/水平指示 |
