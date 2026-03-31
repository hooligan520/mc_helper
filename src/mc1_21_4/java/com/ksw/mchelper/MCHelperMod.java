package com.ksw.mchelper;

import com.mojang.logging.LogUtils;
import com.ksw.mchelper.config.MCHelperConfig;
import com.ksw.mchelper.input.KeyBindings;
import com.ksw.mchelper.render.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
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
        public static void registerOverlays(AddGuiOverlayLayersEvent event) {
            // 1.21.4 使用 ForgeLayeredDraw 注册 HUD 层
            var draw = event.getLayeredDraw();
            draw.add(rl("coordinates_hud"), CoordinatesHudOverlay.LAYER);
            draw.add(rl("lookat_info"), LookAtInfoOverlay.LAYER);
            draw.add(rl("equipment_hud"), EquipmentHudOverlay.LAYER);
            draw.add(rl("mob_radar"), MobRadarOverlay.LAYER);
            draw.add(rl("minimap"), MCHelperMod.minimapOverlay.MINIMAP_LAYER);
            draw.add(rl("crafting"), CraftingOverlay.LAYER);
            draw.add(rl("building_assist"), BuildingAssistOverlay.HUD_LAYER);
            LOGGER.info("MC Helper HUD 层已注册");
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

        private static ResourceLocation rl(String path) {
            return ResourceLocation.fromNamespaceAndPath(MODID, path);
        }
    }
}
