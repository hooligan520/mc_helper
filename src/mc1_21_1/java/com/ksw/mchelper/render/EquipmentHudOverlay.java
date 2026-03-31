package com.ksw.mchelper.render;

import com.ksw.mchelper.config.MCHelperConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 装备耐久 HUD 覆盖层
 * 在屏幕右下角显示手持物品和穿戴盔甲的耐久度
 * 特性：渐变背景、物品图标、耐久条、低耐久警告
 */
public class EquipmentHudOverlay {

    // ==================== 颜色常量 ====================
    private static final int BG_TOP = 0xAA1A1A2E;
    private static final int BG_BOTTOM = 0xAA16213E;
    private static final int BORDER_OUTER = 0xFF0F3460;
    private static final int BORDER_INNER = 0xFF533483;
    private static final int TEXT_WHITE = 0xFFFFFFFF;
    private static final int TEXT_GRAY = 0xFFAABBCC;
    private static final int SEPARATOR_COLOR = 0x40FFFFFF;

    // 耐久条颜色
    private static final int DUR_HIGH = 0xFF00CC66;
    private static final int DUR_MID = 0xFFCCCC00;
    private static final int DUR_LOW = 0xFFCC3333;
    private static final int DUR_BG = 0xFF222233;

    // 低耐久警告阈值
    private static final float LOW_DURABILITY_THRESHOLD = 0.15f;

    // ==================== 布局常量 ====================
    private static final float SCALE = 0.4f;
    private static final int ITEM_SIZE = 16;
    private static final int ROW_HEIGHT = 20;
    private static final int PADDING = 6;
    private static final int BOX_WIDTH = 160;
    private static final int BAR_HEIGHT = 3;
    private static final int MARGIN_RIGHT = 4;
    private static final int MARGIN_BOTTOM = 40; // 避开快捷栏

    // ==================== 数据结构 ====================
    private static class EquipmentInfo {
        final ItemStack stack;
        final String label;

        EquipmentInfo(ItemStack stack, String label) {
            this.stack = stack;
            this.label = label;
        }
    }

    public static final LayeredDraw.Layer LAYER = (guiGraphics, deltaTracker) -> {
        int screenWidth = guiGraphics.guiWidth();
        int screenHeight = guiGraphics.guiHeight();
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
        if (!MCHelperConfig.showEquipmentHud) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.getDebugOverlay().showDebugScreen()) return;

        Player player = mc.player;

        // 收集有耐久度的装备
        List<EquipmentInfo> equipments = new ArrayList<>();

        // 主手
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.isDamageableItem()) {
            equipments.add(new EquipmentInfo(mainHand, "主手"));
        }

        // 副手
        ItemStack offHand = player.getOffhandItem();
        if (offHand.isDamageableItem()) {
            equipments.add(new EquipmentInfo(offHand, "副手"));
        }

        // 盔甲（从头到脚）
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.isDamageableItem()) {
            equipments.add(new EquipmentInfo(helmet, "头盔"));
        }

        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chestplate.isDamageableItem()) {
            equipments.add(new EquipmentInfo(chestplate, "胸甲"));
        }

        ItemStack leggings = player.getItemBySlot(EquipmentSlot.LEGS);
        if (leggings.isDamageableItem()) {
            equipments.add(new EquipmentInfo(leggings, "护腿"));
        }

        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (boots.isDamageableItem()) {
            equipments.add(new EquipmentInfo(boots, "靴子"));
        }

        // 没有可显示的装备时不渲染
        if (equipments.isEmpty()) return;

        // 应用缩放
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(SCALE, SCALE, 1.0f);

        // 计算位置（右下角，需要补偿缩放）
        int scaledWidth = (int) (screenWidth / SCALE);
        int scaledHeight = (int) (screenHeight / SCALE);
        int titleHeight = 16;
        int boxHeight = titleHeight + equipments.size() * ROW_HEIGHT + PADDING * 2;
        int boxX = scaledWidth - BOX_WIDTH - MARGIN_RIGHT;
        int boxY = scaledHeight - boxHeight - MARGIN_BOTTOM;

        // 绘制背景
        guiGraphics.fillGradient(boxX, boxY, boxX + BOX_WIDTH, boxY + boxHeight, BG_TOP, BG_BOTTOM);
        drawBorder(guiGraphics, boxX, boxY, BOX_WIDTH, boxHeight);
        guiGraphics.fill(boxX + 1, boxY + 1, boxX + BOX_WIDTH - 1, boxY + 2, BORDER_INNER);

        int textX = boxX + PADDING;
        int textY = boxY + PADDING;

        // 标题
        guiGraphics.drawString(mc.font, "\u00A7b\u2726 装备耐久", textX, textY, TEXT_WHITE, true);
        textY += titleHeight;

        // 分隔线
        guiGraphics.fill(boxX + 4, textY - 2, boxX + BOX_WIDTH - 4, textY - 1, SEPARATOR_COLOR);

        // 逐项渲染
        for (EquipmentInfo eq : equipments) {
            renderEquipmentRow(guiGraphics, mc, eq, textX, textY, BOX_WIDTH - PADDING * 2);
            textY += ROW_HEIGHT;
        }

        guiGraphics.pose().popPose();
    };

    /**
     * 渲染单行装备信息
     */
    private static void renderEquipmentRow(GuiGraphics guiGraphics, Minecraft mc,
                                            EquipmentInfo eq, int x, int y, int availableWidth) {
        ItemStack stack = eq.stack;
        int maxDamage = stack.getMaxDamage();
        int currentDamage = stack.getDamageValue();
        int remaining = maxDamage - currentDamage;
        float percent = (float) remaining / maxDamage;

        // 物品图标
        guiGraphics.renderItem(stack, x, y);

        // 物品名称 + 耐久数值
        int textOffsetX = x + ITEM_SIZE + 4;
        boolean isLow = percent < LOW_DURABILITY_THRESHOLD;
        String durColor = isLow ? "\u00A7c" : (percent < 0.5f ? "\u00A7e" : "\u00A7a");

        // 低耐久闪烁效果
        String prefix = "";
        if (isLow) {
            long time = System.currentTimeMillis();
            if ((time / 500) % 2 == 0) {
                prefix = "\u00A7c\u26A0 ";
            }
        }

        String durText = String.format("%s%s%d/%d", prefix, durColor, remaining, maxDamage);
        guiGraphics.drawString(mc.font, durText, textOffsetX, y + 1, TEXT_WHITE, true);

        // 耐久条
        int barX = textOffsetX;
        int barY = y + 12;
        int barWidth = availableWidth - ITEM_SIZE - 8;

        guiGraphics.fill(barX, barY, barX + barWidth, barY + BAR_HEIGHT, DUR_BG);

        int filledWidth = Math.max(1, (int) (barWidth * percent));
        int barColor = percent > 0.5f ? DUR_HIGH : (percent > LOW_DURABILITY_THRESHOLD ? DUR_MID : DUR_LOW);
        guiGraphics.fill(barX, barY, barX + filledWidth, barY + BAR_HEIGHT, barColor);
    }

    /**
     * 绘制边框
     */
    private static void drawBorder(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + 1, BORDER_OUTER);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, BORDER_OUTER);
        guiGraphics.fill(x, y, x + 1, y + height, BORDER_OUTER);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, BORDER_OUTER);
    }
}
