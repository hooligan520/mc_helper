package com.ksw.mchelper.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/**
 * MC Helper 快捷键定义
 * 1.21.11: KeyMapping 第4个参数从 String 改为 KeyMapping.Category 对象
 */
public class KeyBindings {

    // 1.21.11: 使用 KeyMapping.Category.MISC 替代字符串 "key.categories.mchelper"
    // 注意：自定义分类需要 KeyMapping.Category.register(Identifier)，这里先用 MISC
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.MISC;

    public static final KeyMapping TOGGLE_COORDINATES = new KeyMapping(
            "key.mchelper.toggle_coordinates",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            CATEGORY
    );

    public static final KeyMapping TOGGLE_LIGHT_OVERLAY = new KeyMapping(
            "key.mchelper.toggle_light_overlay",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            CATEGORY
    );

    public static final KeyMapping TOGGLE_LOOKAT_INFO = new KeyMapping(
            "key.mchelper.toggle_lookat_info",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            CATEGORY
    );

    public static final KeyMapping TOGGLE_EQUIPMENT = new KeyMapping(
            "key.mchelper.toggle_equipment",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            CATEGORY
    );

    public static final KeyMapping TOGGLE_MINIMAP = new KeyMapping(
            "key.mchelper.toggle_minimap",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            CATEGORY
    );

    public static final KeyMapping TOGGLE_MOB_RADAR = new KeyMapping(
            "key.mchelper.toggle_mob_radar",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_N,
            CATEGORY
    );

    public static final KeyMapping TOGGLE_CRAFTING = new KeyMapping(
            "key.mchelper.toggle_crafting",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CATEGORY
    );

    public static final KeyMapping TOGGLE_BUILDING_ASSIST = new KeyMapping(
            "key.mchelper.toggle_building_assist",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            CATEGORY
    );
}
