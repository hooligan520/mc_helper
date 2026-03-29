package com.ksw.mchelper;

import com.ksw.mchelper.config.MCHelperConfig;
import com.ksw.mchelper.input.KeyBindings;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 客户端运行时事件处理器
 * 监听快捷键输入、客户端 Tick 等事件
 */
public class ClientEventHandler {

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // H - 坐标 HUD
        if (KeyBindings.TOGGLE_COORDINATES.consumeClick()) {
            boolean newState = !MCHelperConfig.showCoordinates;
            MCHelperConfig.showCoordinates = newState;
            mc.player.displayClientMessage(
                Component.literal("\u00A7e[MC Helper] \u00A7f坐标 HUD " + (newState ? "\u00A7a已开启" : "\u00A7c已关闭")),
                true
            );
        }

        // O - 光照叠加层
        if (KeyBindings.TOGGLE_LIGHT_OVERLAY.consumeClick()) {
            boolean newState = !MCHelperConfig.showLightOverlay;
            MCHelperConfig.showLightOverlay = newState;
            if (newState) {
                MCHelperMod.getLightOverlayRenderer().requestRefresh();
            } else {
                MCHelperMod.getLightOverlayRenderer().clearCache();
            }
            mc.player.displayClientMessage(
                Component.literal("\u00A7e[MC Helper] \u00A7f光照叠加层 " + (newState ? "\u00A7a已开启" : "\u00A7c已关闭")),
                true
            );
        }

        // J - 方块信息
        if (KeyBindings.TOGGLE_LOOKAT_INFO.consumeClick()) {
            boolean newState = !MCHelperConfig.showLookAtInfo;
            MCHelperConfig.showLookAtInfo = newState;
            mc.player.displayClientMessage(
                Component.literal("\u00A7e[MC Helper] \u00A7f方块信息 " + (newState ? "\u00A7a已开启" : "\u00A7c已关闭")),
                true
            );
        }

        // K - 装备耐久
        if (KeyBindings.TOGGLE_EQUIPMENT.consumeClick()) {
            boolean newState = !MCHelperConfig.showEquipmentHud;
            MCHelperConfig.showEquipmentHud = newState;
            mc.player.displayClientMessage(
                Component.literal("\u00A7e[MC Helper] \u00A7f装备耐久 " + (newState ? "\u00A7a已开启" : "\u00A7c已关闭")),
                true
            );
        }

        // M - 刷怪检测
        if (KeyBindings.TOGGLE_MOB_RADAR.consumeClick()) {
            boolean newState = !MCHelperConfig.showMobRadar;
            MCHelperConfig.showMobRadar = newState;
            mc.player.displayClientMessage(
                Component.literal("\u00A7e[MC Helper] \u00A7f刷怪检测 " + (newState ? "\u00A7a已开启" : "\u00A7c已关闭")),
                true
            );
        }

        // N - 迷你地图
        if (KeyBindings.TOGGLE_MINIMAP.consumeClick()) {
            boolean newState = !MCHelperConfig.showMinimap;
            MCHelperConfig.showMinimap = newState;
            if (newState) {
                MCHelperMod.getMinimapOverlay().requestRefresh();
            } else {
                MCHelperMod.getMinimapOverlay().clearCache();
            }
            mc.player.displayClientMessage(
                Component.literal("\u00A7e[MC Helper] \u00A7f迷你地图 " + (newState ? "\u00A7a已开启" : "\u00A7c已关闭")),
                true
            );
        }
    }
}
