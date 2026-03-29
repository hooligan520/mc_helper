package com.ksw.mchelper;

import com.mojang.logging.LogUtils;
import com.ksw.mchelper.config.MCHelperConfig;
import com.ksw.mchelper.input.KeyBindings;
import com.ksw.mchelper.render.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(MCHelperMod.MODID)
public class MCHelperMod {
    public static final String MODID = "mchelper";
    private static final Logger LOGGER = LogUtils.getLogger();

    // 需要同时监听 tick 和 render 事件的渲染器
    private static final LightOverlayRenderer lightOverlayRenderer = new LightOverlayRenderer();
    private static final MinimapOverlay minimapOverlay = new MinimapOverlay();
    private static final BuildingAssistOverlay buildingAssistOverlay = new BuildingAssistOverlay();

    public static LightOverlayRenderer getLightOverlayRenderer() { return lightOverlayRenderer; }
    public static MinimapOverlay getMinimapOverlay() { return minimapOverlay; }

    public MCHelperMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        context.registerConfig(ModConfig.Type.CLIENT, MCHelperConfig.SPEC);

        MinecraftForge.EVENT_BUS.register(lightOverlayRenderer);
        MinecraftForge.EVENT_BUS.register(minimapOverlay);
        MinecraftForge.EVENT_BUS.register(buildingAssistOverlay);
        MinecraftForge.EVENT_BUS.register(new ClientEventHandler());

        LOGGER.info("MC Helper 正在初始化...");
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("MC Helper 客户端初始化完成！");
        }

        @SubscribeEvent
        public static void registerOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAboveAll("coordinates_hud", CoordinatesHudOverlay.HUD_COORDINATES);
            event.registerAboveAll("lookat_info", LookAtInfoOverlay.HUD_LOOKAT);
            event.registerAboveAll("equipment_hud", EquipmentHudOverlay.HUD_EQUIPMENT);
            event.registerAboveAll("mob_radar", MobRadarOverlay.HUD_MOB_RADAR);
            event.registerAboveAll("minimap", MCHelperMod.minimapOverlay.HUD_MINIMAP);
            event.registerAboveAll("crafting", CraftingOverlay.HUD_CRAFTING);
            event.registerAboveAll("building_assist", BuildingAssistOverlay.HUD_BUILDING);
            LOGGER.info("MC Helper HUD 覆盖层已注册");
        }

        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(KeyBindings.TOGGLE_COORDINATES);
            event.register(KeyBindings.TOGGLE_LIGHT_OVERLAY);
            event.register(KeyBindings.TOGGLE_LOOKAT_INFO);
            event.register(KeyBindings.TOGGLE_EQUIPMENT);
            event.register(KeyBindings.TOGGLE_MOB_RADAR);
            event.register(KeyBindings.TOGGLE_MINIMAP);
            event.register(KeyBindings.TOGGLE_CRAFTING);
            event.register(KeyBindings.TOGGLE_BUILDING_ASSIST);
            LOGGER.info("MC Helper 快捷键已注册");
        }
    }
}
