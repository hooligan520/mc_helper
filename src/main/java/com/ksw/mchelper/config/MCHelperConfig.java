package com.ksw.mchelper.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import com.ksw.mchelper.MCHelperMod;

/**
 * MC Helper 配置类
 * 使用 Forge 的 ForgeConfigSpec 系统，配置文件为 TOML 格式
 */
@Mod.EventBusSubscriber(modid = MCHelperMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class MCHelperConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // ==================== 坐标 HUD ====================
    private static final ForgeConfigSpec.BooleanValue SHOW_COORDINATES = BUILDER
            .comment("是否显示坐标 HUD / Show coordinate HUD")
            .define("coordinates_hud.show", true);

    private static final ForgeConfigSpec.BooleanValue SHOW_DIRECTION = BUILDER
            .comment("是否显示朝向信息 / Show direction info")
            .define("coordinates_hud.showDirection", true);

    private static final ForgeConfigSpec.BooleanValue SHOW_BIOME = BUILDER
            .comment("是否显示生物群系 / Show biome name")
            .define("coordinates_hud.showBiome", true);

    private static final ForgeConfigSpec.BooleanValue SHOW_DIMENSION = BUILDER
            .comment("是否显示维度信息 / Show dimension")
            .define("coordinates_hud.showDimension", true);

    private static final ForgeConfigSpec.IntValue HUD_POS_X = BUILDER
            .comment("HUD X 坐标（从左边距像素）/ HUD X position")
            .defineInRange("coordinates_hud.posX", 10, 0, 3840);

    private static final ForgeConfigSpec.IntValue HUD_POS_Y = BUILDER
            .comment("HUD Y 坐标（从顶边距像素）/ HUD Y position")
            .defineInRange("coordinates_hud.posY", 10, 0, 2160);

    // ==================== 方块/实体信息 ====================
    private static final ForgeConfigSpec.BooleanValue SHOW_LOOKAT_INFO = BUILDER
            .comment("是否显示方块/实体信息提示 / Show look-at info")
            .define("lookat_info.show", true);

    // ==================== 光照叠加层 ====================
    private static final ForgeConfigSpec.BooleanValue SHOW_LIGHT_OVERLAY = BUILDER
            .comment("是否显示光照叠加层 / Show light level overlay")
            .define("light_overlay.show", false);

    private static final ForgeConfigSpec.IntValue LIGHT_RENDER_DISTANCE = BUILDER
            .comment("光照叠加层渲染距离（方块数）/ Light overlay render distance")
            .defineInRange("light_overlay.renderDistance", 16, 4, 64);

    // ==================== 装备耐久 HUD ====================
    private static final ForgeConfigSpec.BooleanValue SHOW_EQUIPMENT_HUD = BUILDER
            .comment("是否显示装备耐久 HUD / Show equipment durability HUD")
            .define("equipment_hud.show", true);

    // ==================== 时间/天气显示 ====================
    private static final ForgeConfigSpec.BooleanValue SHOW_TIME_WEATHER = BUILDER
            .comment("是否在坐标 HUD 中显示游戏时间和天气 / Show time and weather in HUD")
            .define("coordinates_hud.showTimeWeather", true);

    // ==================== 性能监控 ====================
    private static final ForgeConfigSpec.BooleanValue SHOW_PERFORMANCE = BUILDER
            .comment("是否在坐标 HUD 中显示 FPS 和内存 / Show FPS and memory in HUD")
            .define("coordinates_hud.showPerformance", true);

    // ==================== 刷怪检测 ====================
    private static final ForgeConfigSpec.BooleanValue SHOW_MOB_RADAR = BUILDER
            .comment("是否显示刷怪检测面板 / Show mob radar")
            .define("mob_radar.show", false);

    private static final ForgeConfigSpec.IntValue MOB_RADAR_DISTANCE = BUILDER
            .comment("刷怪检测半径（方块数）/ Mob radar detection radius")
            .defineInRange("mob_radar.distance", 32, 8, 128);

    // ==================== 迷你地图 ====================
    private static final ForgeConfigSpec.BooleanValue SHOW_MINIMAP = BUILDER
            .comment("是否显示迷你地图 / Show minimap")
            .define("minimap.show", false);

    private static final ForgeConfigSpec.IntValue MINIMAP_SIZE = BUILDER
            .comment("迷你地图大小（像素）/ Minimap size in pixels")
            .defineInRange("minimap.size", 100, 60, 200);

    // ==================== 合成查询 ====================
    private static final ForgeConfigSpec.BooleanValue SHOW_CRAFTING = BUILDER
            .comment("是否显示合成查询面板 / Show crafting recipe panel")
            .define("crafting.show", false);

    // ==================== 建筑辅助 ====================
    private static final ForgeConfigSpec.BooleanValue SHOW_BUILDING_ASSIST = BUILDER
            .comment("是否显示建筑辅助（网格线+水平线）/ Show building assist overlay")
            .define("building_assist.show", false);

    // ==================== 构建配置 ====================
    public static final ForgeConfigSpec SPEC = BUILDER.build();

    // ==================== 运行时缓存（避免频繁调用 .get()） ====================
    public static boolean showCoordinates;
    public static boolean showDirection;
    public static boolean showBiome;
    public static boolean showDimension;
    public static int hudPosX;
    public static int hudPosY;

    public static boolean showLookAtInfo;

    public static boolean showLightOverlay;
    public static int lightRenderDistance;

    public static boolean showEquipmentHud;

    public static boolean showTimeWeather;
    public static boolean showPerformance;
    public static boolean showMobRadar;
    public static int mobRadarDistance;
    public static boolean showMinimap;
    public static int minimapSize;
    public static boolean showCrafting;
    public static boolean showBuildingAssist;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        showCoordinates = SHOW_COORDINATES.get();
        showDirection = SHOW_DIRECTION.get();
        showBiome = SHOW_BIOME.get();
        showDimension = SHOW_DIMENSION.get();
        hudPosX = HUD_POS_X.get();
        hudPosY = HUD_POS_Y.get();

        showLookAtInfo = SHOW_LOOKAT_INFO.get();

        showLightOverlay = SHOW_LIGHT_OVERLAY.get();
        lightRenderDistance = LIGHT_RENDER_DISTANCE.get();

        showEquipmentHud = SHOW_EQUIPMENT_HUD.get();

        showTimeWeather = SHOW_TIME_WEATHER.get();
        showPerformance = SHOW_PERFORMANCE.get();
        showMobRadar = SHOW_MOB_RADAR.get();
        mobRadarDistance = MOB_RADAR_DISTANCE.get();
        showMinimap = SHOW_MINIMAP.get();
        minimapSize = MINIMAP_SIZE.get();
        showCrafting = SHOW_CRAFTING.get();
        showBuildingAssist = SHOW_BUILDING_ASSIST.get();
    }
}
