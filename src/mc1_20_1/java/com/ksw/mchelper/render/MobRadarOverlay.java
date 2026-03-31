package com.ksw.mchelper.render;

import com.ksw.mchelper.config.MCHelperConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 刷怪检测面板（M 键切换）
 *
 * 扫描玩家周围指定范围内的敌对生物，按类型分类统计。
 * 显示每种怪物的数量，总数超过阈值时高亮提醒。
 */
public class MobRadarOverlay {

    // ==================== 颜色常量 ====================
    private static final int BG_TOP    = 0xCC1A1A2E;
    private static final int BG_BOTTOM = 0xCC16213E;
    private static final int BORDER_OUTER = 0xFF0F3460;
    private static final int BORDER_INNER = 0xFF533483;
    private static final int SEPARATOR_COLOR = 0x40FFFFFF;

    private static final int COLOR_TITLE   = 0xFFFF5555;   // 红色标题
    private static final int COLOR_DANGER  = 0xFFFF5555;   // 危险（怪物多）
    private static final int COLOR_WARN    = 0xFFFFAA00;   // 警告
    private static final int COLOR_SAFE    = 0xFF55FF55;   // 安全（0 怪）
    private static final int COLOR_LABEL   = 0xFFAABBCC;
    private static final int COLOR_COUNT   = 0xFFFFFFFF;

    // ==================== 布局常量 ====================
    private static final float SCALE = 0.4f;
    private static final int LINE_HEIGHT = 13;
    private static final int PADDING = 8;
    private static final int BOX_WIDTH = 200;
    private static final int MARGIN_LEFT = 4;
    private static final int MARGIN_TOP  = 4;

    // 危险阈值
    private static final int DANGER_THRESHOLD = 10;
    private static final int WARN_THRESHOLD   = 5;

    public static final IGuiOverlay HUD_MOB_RADAR = (ForgeGui gui, GuiGraphics guiGraphics,
                                                      float partialTick, int screenWidth, int screenHeight) -> {
        if (!MCHelperConfig.showMobRadar) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.options.renderDebug) return;

        // 扫描周围怪物
        double rangeSq = (double) MCHelperConfig.mobRadarDistance * MCHelperConfig.mobRadarDistance;
        List<Entity> entities = mc.level.getEntities(mc.player,
                mc.player.getBoundingBox().inflate(MCHelperConfig.mobRadarDistance),
                e -> e instanceof Monster && !(e instanceof Player));

        // 按类型统计
        Map<String, Integer> mobCounts = new LinkedHashMap<>();
        for (Entity e : entities) {
            if (e.distanceToSqr(mc.player) > rangeSq) continue;
            String name = e.getName().getString();
            mobCounts.merge(name, 1, Integer::sum);
        }
        int total = mobCounts.values().stream().mapToInt(Integer::intValue).sum();

        // 应用缩放
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(SCALE, SCALE, 1.0f);

        int x = (int) (MARGIN_LEFT / SCALE);
        // 放在坐标 HUD 下方（动态估算，约 180px 下方）
        int y = (int) ((MCHelperConfig.hudPosY + 180) / SCALE);

        // 计算高度：标题 + 分隔 + 合计行 + 各种怪
        int lineCount = 2 + Math.max(mobCounts.size(), 1); // 至少显示"无怪物"
        int boxHeight = LINE_HEIGHT + PADDING * 2 + 4 + lineCount * LINE_HEIGHT;

        // 背景 + 边框
        guiGraphics.fillGradient(x, y, x + BOX_WIDTH, y + boxHeight, BG_TOP, BG_BOTTOM);
        drawBorder(guiGraphics, x, y, BOX_WIDTH, boxHeight);
        guiGraphics.fill(x + 1, y + 1, x + BOX_WIDTH - 1, y + 2, BORDER_INNER);

        int textX = x + PADDING;
        int textY = y + PADDING;

        // 标题（根据威胁程度变色）
        int titleColor = total >= DANGER_THRESHOLD ? COLOR_DANGER
                       : total >= WARN_THRESHOLD   ? COLOR_WARN
                       : COLOR_SAFE;
        String titleIcon = total >= DANGER_THRESHOLD ? "⚠" : total > 0 ? "⚔" : "✓";
        guiGraphics.drawString(mc.font,
                "§f" + titleIcon + " 刷怪检测  §7(半径" + MCHelperConfig.mobRadarDistance + "格)",
                textX, textY, titleColor, true);
        textY += LINE_HEIGHT;

        guiGraphics.fill(x + 4, textY, x + BOX_WIDTH - 4, textY + 1, SEPARATOR_COLOR);
        textY += 4;

        // 合计
        String totalColor = total >= DANGER_THRESHOLD ? "§c" : total >= WARN_THRESHOLD ? "§e" : "§a";
        guiGraphics.drawString(mc.font,
                "§7合计: " + totalColor + total + " §7只",
                textX, textY, COLOR_LABEL, true);
        textY += LINE_HEIGHT;

        // 各类型怪物
        if (mobCounts.isEmpty()) {
            guiGraphics.drawString(mc.font, "§a  无敌对生物", textX, textY, COLOR_SAFE, true);
        } else {
            // 按数量降序排列显示
            mobCounts.entrySet().stream()
                    .sorted((a, b) -> b.getValue() - a.getValue())
                    .forEach(entry -> {
                        // 这里用 lambda 无法修改 textY，用成员变量绕过
                    });
            int ty = textY;
            for (Map.Entry<String, Integer> entry :
                    mobCounts.entrySet().stream()
                            .sorted((a, b) -> b.getValue() - a.getValue())
                            .collect(java.util.stream.Collectors.toList())) {
                String countColor = entry.getValue() >= 5 ? "§c" : entry.getValue() >= 3 ? "§e" : "§f";
                guiGraphics.drawString(mc.font,
                        "§7  " + entry.getKey() + ": " + countColor + entry.getValue(),
                        textX, ty, COLOR_LABEL, true);
                ty += LINE_HEIGHT;
            }
        }

        guiGraphics.pose().popPose();
    };

    private static void drawBorder(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + 1, BORDER_OUTER);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, BORDER_OUTER);
        guiGraphics.fill(x, y, x + 1, y + height, BORDER_OUTER);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, BORDER_OUTER);
    }
}
