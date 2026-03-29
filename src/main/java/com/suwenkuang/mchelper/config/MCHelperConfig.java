package com.suwenkuang.mchelper.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import com.suwenkuang.mchelper.MCHelperMod;

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
    }
}
