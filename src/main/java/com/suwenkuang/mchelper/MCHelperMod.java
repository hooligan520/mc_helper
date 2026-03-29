package com.suwenkuang.mchelper;

import com.mojang.logging.LogUtils;
import com.suwenkuang.mchelper.config.MCHelperConfig;
import com.suwenkuang.mchelper.input.KeyBindings;
import com.suwenkuang.mchelper.render.CoordinatesHudOverlay;
import com.suwenkuang.mchelper.render.EquipmentHudOverlay;
import com.suwenkuang.mchelper.render.LookAtInfoOverlay;
import com.suwenkuang.mchelper.render.LightOverlayRenderer;
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

    // LightOverlayRenderer 需要同时监听 tick 和 render 事件
    private static final LightOverlayRenderer lightOverlayRenderer = new LightOverlayRenderer();

    public static LightOverlayRenderer getLightOverlayRenderer() {
        return lightOverlayRenderer;
    }

    public MCHelperMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        // 注册客户端配置
        context.registerConfig(ModConfig.Type.CLIENT, MCHelperConfig.SPEC);

        // 注册 Forge 事件总线（用于游戏运行时事件）
        MinecraftForge.EVENT_BUS.register(lightOverlayRenderer);
        MinecraftForge.EVENT_BUS.register(new ClientEventHandler());

        LOGGER.info("MC Helper 正在初始化...");
    }

    /**
     * 客户端事件注册（通过 Mod 事件总线）
     */
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("MC Helper 客户端初始化完成！");
        }

        @SubscribeEvent
        public static void registerOverlays(RegisterGuiOverlaysEvent event) {
            // 注册坐标 HUD 覆盖层
            event.registerAboveAll("coordinates_hud", CoordinatesHudOverlay.HUD_COORDINATES);
            // 注册方块/实体信息覆盖层
            event.registerAboveAll("lookat_info", LookAtInfoOverlay.HUD_LOOKAT);
            // 注册装备耐久 HUD 覆盖层
            event.registerAboveAll("equipment_hud", EquipmentHudOverlay.HUD_EQUIPMENT);
            LOGGER.info("MC Helper HUD 覆盖层已注册");
        }

        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(KeyBindings.TOGGLE_COORDINATES);
            event.register(KeyBindings.TOGGLE_LIGHT_OVERLAY);
            event.register(KeyBindings.TOGGLE_LOOKAT_INFO);
            event.register(KeyBindings.TOGGLE_EQUIPMENT);
            LOGGER.info("MC Helper 快捷键已注册");
        }
    }
}
