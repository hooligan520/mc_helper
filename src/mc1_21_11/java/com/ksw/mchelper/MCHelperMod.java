package com.ksw.mchelper;

import com.mojang.logging.LogUtils;
import com.ksw.mchelper.config.MCHelperConfig;
import com.ksw.mchelper.input.KeyBindings;
import com.ksw.mchelper.render.*;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.AddFramePassEvent;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
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
        context.registerConfig(ModConfig.Type.CLIENT, MCHelperConfig.SPEC);

        // ====================================================================
        // EventBus 7（Forge 61+）注册规则：
        //
        // 1. 仍是 IModBusEvent 的（如 FMLClientSetupEvent）→ 可用
        //    @Mod.EventBusSubscriber(bus=MOD) + @SubscribeEvent
        //
        // 2. 不再是 IModBusEvent 的（有独立 BUS 字段）→ 必须用
        //    EventClass.BUS.addListener(method::ref)
        //    包括：AddGuiOverlayLayersEvent、RegisterKeyMappingsEvent、
        //          TickEvent.ClientTickEvent.Post、AddFramePassEvent
        // ====================================================================

        // Tick 事件
        ClientEventHandler handler = new ClientEventHandler();
        TickEvent.ClientTickEvent.Post.BUS.addListener(handler::onClientTick);
        TickEvent.ClientTickEvent.Post.BUS.addListener(lightOverlayRenderer::onClientTick);
        TickEvent.ClientTickEvent.Post.BUS.addListener(minimapOverlay::onClientTick);

        // 3D 渲染 pass 事件
        AddFramePassEvent.BUS.addListener(lightOverlayRenderer::onAddFramePass);
        AddFramePassEvent.BUS.addListener(buildingAssistOverlay::onAddFramePass);

        // HUD 层注册（不再是 IModBusEvent，用 BUS 注册）
        AddGuiOverlayLayersEvent.BUS.addListener(MCHelperMod::registerOverlays);

        // 按键注册（不再是 IModBusEvent，用 BUS 注册）
        RegisterKeyMappingsEvent.BUS.addListener(MCHelperMod::registerKeyMappings);

        LOGGER.info("MC Helper 正在初始化...");
    }

    private static void registerOverlays(AddGuiOverlayLayersEvent event) {
        var draw = event.getLayeredDraw();
        draw.add(rl("coordinates_hud"), CoordinatesHudOverlay.LAYER);
        draw.add(rl("lookat_info"),     LookAtInfoOverlay.LAYER);
        draw.add(rl("equipment_hud"),   EquipmentHudOverlay.LAYER);
        draw.add(rl("mob_radar"),       MobRadarOverlay.LAYER);
        draw.add(rl("minimap"),         minimapOverlay.MINIMAP_LAYER);
        draw.add(rl("crafting"),        CraftingOverlay.LAYER);
        draw.add(rl("building_assist"), BuildingAssistOverlay.HUD_LAYER);
        LOGGER.info("MC Helper HUD 层已注册");
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
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

    // FMLClientSetupEvent 仍是 IModBusEvent，保留 @Mod.EventBusSubscriber 方式
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("MC Helper 客户端初始化完成！");
        }
    }

    private static Identifier rl(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
