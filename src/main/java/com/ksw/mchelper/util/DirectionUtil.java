package com.ksw.mchelper.util;

/**
 * 方向工具类
 * 将玩家的 yaw 角度转换为方向名称（中文）
 */
public class DirectionUtil {

    private static final String[] DIRECTIONS = {
            "南", "西南", "西", "西北", "北", "东北", "东", "东南"
    };

    /**
     * 根据 yaw 角度获取方向的中文名称
     * Minecraft 中 yaw 的方向：
     *   0° = 南, 90° = 西, 180°/-180° = 北, -90° = 东
     *
     * @param yaw 玩家的 yaw 角度
     * @return 方向的中文名称
     */
    public static String getDirectionName(float yaw) {
        // 归一化到 0-360
        float normalizedYaw = ((yaw % 360) + 360) % 360;

        // 每个方向占 45°，偏移 22.5° 使边界居中
        int index = (int) ((normalizedYaw + 22.5f) / 45.0f) % 8;
        return DIRECTIONS[index];
    }

    /**
     * 获取详细方向描述（带角度）
     */
    public static String getDetailedDirection(float yaw) {
        return String.format("%s (%.1f°)", getDirectionName(yaw), yaw);
    }
}
