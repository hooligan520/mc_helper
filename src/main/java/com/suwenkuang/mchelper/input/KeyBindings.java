package com.suwenkuang.mchelper.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/**
 * MC Helper 快捷键定义
 * 所有快捷键属于 "MC Helper" 分类，可在游戏设置中自定义
 */
public class KeyBindings {

    public static final String CATEGORY = "key.categories.mchelper";

    /**
     * H 键 — 切换坐标 HUD 显示
     */
    public static final KeyMapping TOGGLE_COORDINATES = new KeyMapping(
            "key.mchelper.toggle_coordinates",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            CATEGORY
    );

    /**
     * O 键 — 切换光照叠加层
     */
    public static final KeyMapping TOGGLE_LIGHT_OVERLAY = new KeyMapping(
            "key.mchelper.toggle_light_overlay",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            CATEGORY
    );

    /**
     * J 键 — 切换方块/实体信息显示
     */
    public static final KeyMapping TOGGLE_LOOKAT_INFO = new KeyMapping(
            "key.mchelper.toggle_lookat_info",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            CATEGORY
    );

    /**
     * K 键 — 切换装备耐久 HUD
     */
    public static final KeyMapping TOGGLE_EQUIPMENT = new KeyMapping(
            "key.mchelper.toggle_equipment",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            CATEGORY
    );

    /**
     * M 键 — 切换迷你地图
     */
    public static final KeyMapping TOGGLE_MINIMAP = new KeyMapping(
            "key.mchelper.toggle_minimap",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            CATEGORY
    );

    /**
     * N 键 — 切换刷怪检测面板
     */
    public static final KeyMapping TOGGLE_MOB_RADAR = new KeyMapping(
            "key.mchelper.toggle_mob_radar",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_N,
            CATEGORY
    );
}
