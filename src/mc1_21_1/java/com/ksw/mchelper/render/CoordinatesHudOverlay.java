package com.ksw.mchelper.render;

import com.ksw.mchelper.config.MCHelperConfig;
import com.ksw.mchelper.util.DirectionUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 坐标与方向 HUD 覆盖层 — 自定义风格版
 * 特性：渐变背景、装饰边框、图标前缀、自适应宽度
 */
public class CoordinatesHudOverlay {

    // ==================== 颜色常量 ====================
    private static final int BG_TOP = 0xCC1A1A2E;          // 深蓝紫（上）
    private static final int BG_BOTTOM = 0xCC16213E;       // 深蓝（下）
    private static final int BORDER_OUTER = 0xFF0F3460;    // 外边框：深蓝
    private static final int BORDER_INNER = 0xFF533483;    // 内边框：紫色高亮
    private static final int TITLE_COLOR = 0xFF00D2FF;     // 标题颜色：青色
    private static final int VALUE_COLOR = 0xFFFFFFFF;     // 值颜色：白色
    private static final int LABEL_COLOR = 0xFFAABBCC;     // 标签颜色：浅蓝灰
    private static final int SEPARATOR_COLOR = 0x40FFFFFF;  // 分隔线：半透明白

    // ==================== 布局常量 ====================
    private static final float SCALE = 0.4f;
    private static final int LINE_HEIGHT = 13;
    private static final int PADDING_X = 8;
    private static final int PADDING_Y = 6;
    private static final int TITLE_HEIGHT = 14;
    private static final int MIN_WIDTH = 180;

    // ==================== 图标字符 ====================
    private static final String ICON_COORDS = "\u2316 ";    // ⌖ 坐标
    private static final String ICON_DIR = "\u2794 ";        // ➔ 方向
    private static final String ICON_BIOME = "\u2618 ";      // ☘ 群系
    private static final String ICON_DIM = "\u2726 ";        // ✦ 维度
    private static final String ICON_TIME = "\u263D ";       // ☽ 时间
    private static final String ICON_FPS = "\u25B6 ";        // ▶ 性能

    public static final LayeredDraw.Layer LAYER = (guiGraphics, deltaTracker) -> {
        int screenWidth = guiGraphics.guiWidth();
        int screenHeight = guiGraphics.guiHeight();
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
        if (!MCHelperConfig.showCoordinates) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.getDebugOverlay().showDebugScreen()) return;

        // 应用缩放
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(SCALE, SCALE, 1.0f);

        // 坐标补偿（除以缩放比例）
        int baseX = (int) (MCHelperConfig.hudPosX / SCALE);
        int baseY = (int) (MCHelperConfig.hudPosY / SCALE);

        // 收集要显示的行
        List<String[]> lines = new ArrayList<>();

        // 坐标（始终显示）
        String coordValue = String.format("%.1f / %.1f / %.1f",
                mc.player.getX(), mc.player.getY(), mc.player.getZ());
        lines.add(new String[]{ICON_COORDS + "XYZ", coordValue});

        // 方块坐标
        BlockPos blockPos = mc.player.blockPosition();
        String blockCoord = String.format("%d / %d / %d",
                blockPos.getX(), blockPos.getY(), blockPos.getZ());
        lines.add(new String[]{ICON_COORDS + "Block", blockCoord});

        // 朝向
        if (MCHelperConfig.showDirection) {
            float yaw = mc.player.getYRot();
            String dirName = DirectionUtil.getDirectionName(yaw);
            lines.add(new String[]{ICON_DIR + "朝向", String.format("%s (%.1f\u00B0)", dirName, yaw)});
        }

        // 生物群系
        if (MCHelperConfig.showBiome) {
            Holder<Biome> biomeHolder = mc.level.getBiome(blockPos);
            String biomeName = getBiomeDisplayName(biomeHolder);
            lines.add(new String[]{ICON_BIOME + "群系", biomeName});
        }

        // 维度
        if (MCHelperConfig.showDimension) {
            String dimName = getDimensionDisplayName(mc.level.dimension());
            lines.add(new String[]{ICON_DIM + "维度", dimName});
        }

        // 游戏时间 & 天气
        if (MCHelperConfig.showTimeWeather) {
            long dayTime = mc.level.getDayTime() % 24000L;
            int hour = (int) ((dayTime + 6000) / 1000) % 24;
            int minute = (int) (((dayTime + 6000) % 1000) * 60 / 1000);
            String timeStr = String.format("%02d:%02d", hour, minute);
            String period = (hour >= 6 && hour < 18) ? "§e白天" : "§9夜晚";
            // 天气判断
            String weather;
            if (mc.level.isThundering()) weather = "§8⚡雷暴";
            else if (mc.level.isRaining()) weather = "§b☂雨天";
            else weather = "§f☀晴天";
            lines.add(new String[]{ICON_TIME + "时间", timeStr + " " + period + " §7" + weather});
        }

        // FPS & 内存
        if (MCHelperConfig.showPerformance) {
            int fps = mc.getFps();
            String fpsColor = fps >= 60 ? "§a" : (fps >= 30 ? "§e" : "§c");
            long maxMem = Runtime.getRuntime().maxMemory() / 1024 / 1024;
            long usedMem = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024;
            String memColor = usedMem * 100 / maxMem > 80 ? "§c" : (usedMem * 100 / maxMem > 60 ? "§e" : "§a");
            lines.add(new String[]{ICON_FPS + "性能",
                    fpsColor + fps + " §7FPS  " + memColor + usedMem + "§7/" + maxMem + "MB"});
        }

        // 计算自适应宽度
        int maxWidth = MIN_WIDTH;
        for (String[] line : lines) {
            int lineWidth = mc.font.width(line[0] + ": " + line[1]) + PADDING_X * 2 + 4;
            if (lineWidth > maxWidth) maxWidth = lineWidth;
        }

        int totalHeight = TITLE_HEIGHT + lines.size() * LINE_HEIGHT + PADDING_Y * 2 + 2;

        // ==================== 绘制背景 ====================
        // 渐变背景（从上到下）
        drawGradientRect(guiGraphics, baseX, baseY, baseX + maxWidth, baseY + totalHeight,
                BG_TOP, BG_BOTTOM);

        // 外边框（2像素）
        drawBorder(guiGraphics, baseX, baseY, maxWidth, totalHeight, BORDER_OUTER);
        // 内边框高亮线（顶部和左侧各 1 像素偏移）
        guiGraphics.fill(baseX + 1, baseY + 1, baseX + maxWidth - 1, baseY + 2, BORDER_INNER);
        guiGraphics.fill(baseX + 1, baseY + 1, baseX + 2, baseY + totalHeight - 1, BORDER_INNER);

        // ==================== 绘制标题栏 ====================
        int textX = baseX + PADDING_X;
        int textY = baseY + PADDING_Y;

        guiGraphics.drawString(mc.font, "§b\u25C6 MC Helper", textX, textY, TITLE_COLOR, true);
        textY += TITLE_HEIGHT;

        // 标题下方分隔线
        guiGraphics.fill(baseX + 4, textY - 2, baseX + maxWidth - 4, textY - 1, SEPARATOR_COLOR);

        // ==================== 绘制数据行 ====================
        for (String[] line : lines) {
            // 标签
            String label = "§7" + line[0] + ": ";
            guiGraphics.drawString(mc.font, label, textX, textY, LABEL_COLOR, true);

            // 值（白色）
            int labelWidth = mc.font.width(line[0] + ": ");
            guiGraphics.drawString(mc.font, "§f" + line[1], textX + labelWidth, textY, VALUE_COLOR, true);

            textY += LINE_HEIGHT;
        }

        guiGraphics.pose().popPose();
    };

    /**
     * 绘制渐变矩形（垂直方向）
     */
    private static void drawGradientRect(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2,
                                          int colorTop, int colorBottom) {
        guiGraphics.fillGradient(x1, y1, x2, y2, colorTop, colorBottom);
    }

    /**
     * 绘制边框
     */
    private static void drawBorder(GuiGraphics guiGraphics, int x, int y, int width, int height, int color) {
        guiGraphics.fill(x, y, x + width, y + 1, color);                    // 上
        guiGraphics.fill(x, y + height - 1, x + width, y + height, color);  // 下
        guiGraphics.fill(x, y, x + 1, y + height, color);                   // 左
        guiGraphics.fill(x + width - 1, y, x + width, y + height, color);   // 右
    }

    /**
     * 获取生物群系的显示名称
     */
    private static String getBiomeDisplayName(Holder<Biome> biomeHolder) {
        Optional<ResourceKey<Biome>> key = biomeHolder.unwrapKey();
        if (key.isPresent()) {
            ResourceLocation loc = key.get().location();
            String path = loc.getPath();
            // 尝试用中文名
            String cnName = getBiomeChineseName(path);
            if (cnName != null) return cnName;
            return capitalizeWords(path.replace("_", " "));
        }
        return "未知";
    }

    /**
     * 常见生物群系中文名映射
     */
    private static String getBiomeChineseName(String biomeId) {
        switch (biomeId) {
            case "plains": return "平原";
            case "sunflower_plains": return "向日葵平原";
            case "snowy_plains": return "雪原";
            case "ice_spikes": return "冰刺平原";
            case "desert": return "沙漠";
            case "swamp": return "沼泽";
            case "mangrove_swamp": return "红树林沼泽";
            case "forest": return "森林";
            case "flower_forest": return "繁花森林";
            case "birch_forest": return "桦木森林";
            case "dark_forest": return "黑森林";
            case "old_growth_birch_forest": return "原始桦木森林";
            case "old_growth_pine_taiga": return "原始松木针叶林";
            case "old_growth_spruce_taiga": return "原始云杉针叶林";
            case "taiga": return "针叶林";
            case "snowy_taiga": return "雪原针叶林";
            case "savanna": return "热带草原";
            case "savanna_plateau": return "热带高原";
            case "windswept_hills": return "风袭丘陵";
            case "windswept_gravelly_hills": return "风袭沙砾丘陵";
            case "windswept_forest": return "风袭森林";
            case "windswept_savanna": return "风袭热带草原";
            case "jungle": return "丛林";
            case "sparse_jungle": return "稀疏丛林";
            case "bamboo_jungle": return "竹林";
            case "badlands": return "恶地";
            case "eroded_badlands": return "侵蚀恶地";
            case "wooded_badlands": return "疏林恶地";
            case "meadow": return "草甸";
            case "cherry_grove": return "樱花树林";
            case "grove": return "雪林";
            case "snowy_slopes": return "雪坡";
            case "frozen_peaks": return "冰封山峰";
            case "jagged_peaks": return "尖峭山峰";
            case "stony_peaks": return "裸岩山峰";
            case "river": return "河流";
            case "frozen_river": return "冻河";
            case "beach": return "沙滩";
            case "snowy_beach": return "雪沙滩";
            case "stony_shore": return "石岸";
            case "warm_ocean": return "暖水海洋";
            case "lukewarm_ocean": return "温水海洋";
            case "deep_lukewarm_ocean": return "温水深海";
            case "ocean": return "海洋";
            case "deep_ocean": return "深海";
            case "cold_ocean": return "冷水海洋";
            case "deep_cold_ocean": return "冷水深海";
            case "frozen_ocean": return "冻洋";
            case "deep_frozen_ocean": return "冰冻深海";
            case "mushroom_fields": return "蘑菇岛";
            case "dripstone_caves": return "溶洞";
            case "lush_caves": return "繁茂洞穴";
            case "deep_dark": return "深暗之域";
            case "nether_wastes": return "下界荒地";
            case "warped_forest": return "诡异森林";
            case "crimson_forest": return "绯红森林";
            case "soul_sand_valley": return "灵魂沙峡谷";
            case "basalt_deltas": return "玄武岩三角洲";
            case "the_end": return "末地";
            case "end_highlands": return "末地高地";
            case "end_midlands": return "末地荒岛";
            case "small_end_islands": return "末地小岛";
            case "end_barrens": return "末地边境";
            case "the_void": return "虚空";
            default: return null;
        }
    }

    /**
     * 获取维度的中文显示名称
     */
    private static String getDimensionDisplayName(ResourceKey<Level> dimension) {
        if (dimension == Level.OVERWORLD) return "主世界";
        if (dimension == Level.NETHER) return "下界";
        if (dimension == Level.END) return "末地";
        return dimension.location().getPath();
    }

    /**
     * 将字符串的每个单词首字母大写
     */
    private static String capitalizeWords(String str) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : str.toCharArray()) {
            if (c == ' ') {
                result.append(c);
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
