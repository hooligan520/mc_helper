package com.ksw.mchelper.render;

import com.ksw.mchelper.config.MCHelperConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 方块/实体信息覆盖层 — 自定义风格版
 * 准星指向方块或实体时，在屏幕上方显示详细信息
 * 特性：渐变背景、分类图标、工具提示、硬度信息、实体分类颜色
 */
public class LookAtInfoOverlay {

    // ==================== 颜色常量 ====================
    private static final int BG_TOP = 0xCC1A1A2E;
    private static final int BG_BOTTOM = 0xCC16213E;
    private static final int BORDER_OUTER = 0xFF0F3460;
    private static final int BORDER_INNER = 0xFF533483;
    private static final int TEXT_WHITE = 0xFFFFFFFF;
    private static final int TEXT_GRAY = 0xFFAABBCC;
    private static final int TEXT_DARK = 0xFF778899;
    private static final int SEPARATOR_COLOR = 0x40FFFFFF;

    // 实体分类颜色
    private static final int COLOR_HOSTILE = 0xFFFF5555;    // 敌对 - 红色
    private static final int COLOR_PASSIVE = 0xFF55FF55;    // 友好 - 绿色
    private static final int COLOR_PLAYER = 0xFF55FFFF;     // 玩家 - 青色
    private static final int COLOR_NEUTRAL = 0xFFFFFF55;    // 中立 - 黄色

    // 血条颜色
    private static final int BAR_BG = 0xFF222233;
    private static final int BAR_HIGH = 0xFF00CC66;
    private static final int BAR_MID = 0xFFCCCC00;
    private static final int BAR_LOW = 0xFFCC3333;

    // ==================== 布局常量 ====================
    private static final float SCALE = 0.4f;
    private static final int LINE_HEIGHT = 13;
    private static final int PADDING = 8;
    private static final int BOX_WIDTH = 240;
    private static final int BAR_HEIGHT = 5;

    public static final LayeredDraw.Layer LAYER = (guiGraphics, deltaTracker) -> {
        int screenWidth = guiGraphics.guiWidth();
        int screenHeight = guiGraphics.guiHeight();
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
        if (!MCHelperConfig.showLookAtInfo) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.getDebugOverlay().showDebugScreen()) return;

        HitResult hitResult = mc.hitResult;
        if (hitResult == null || hitResult.getType() == HitResult.Type.MISS) return;

        // 应用缩放
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(SCALE, SCALE, 1.0f);

        int boxX = (int) ((screenWidth - BOX_WIDTH * SCALE) / 2 / SCALE);
        int boxY = (int) (4 / SCALE);

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            renderBlockInfo(guiGraphics, mc, (BlockHitResult) hitResult, boxX, boxY);
        } else if (hitResult.getType() == HitResult.Type.ENTITY) {
            renderEntityInfo(guiGraphics, mc, (EntityHitResult) hitResult, boxX, boxY);
        }

        guiGraphics.pose().popPose();
    };

    // ==================== 方块信息渲染 ====================
    private static void renderBlockInfo(GuiGraphics guiGraphics, Minecraft mc,
                                         BlockHitResult hitResult, int x, int y) {
        BlockPos pos = hitResult.getBlockPos();
        BlockState state = mc.level.getBlockState(pos);
        Block block = state.getBlock();

        // 收集数据
        String blockName = block.getName().getString();
        String blockId = ForgeRegistries.BLOCKS.getKey(block).toString();
        String coordText = String.format("%d, %d, %d", pos.getX(), pos.getY(), pos.getZ());

        // 只显示方块光照（怪物刷怪判断只看方块光照）
        int blockLight = mc.level.getBrightness(LightLayer.BLOCK, pos.above());

        float hardness = state.getDestroySpeed(mc.level, pos);
        String toolHint = getToolHint(state);

        // 计算高度：标题 + ID + 分隔线 + 坐标 + 光照 + 硬度 + 工具
        int lineCount = 6;
        int boxHeight = lineCount * LINE_HEIGHT + PADDING * 2 + 4;

        // 绘制背景
        guiGraphics.fillGradient(x, y, x + BOX_WIDTH, y + boxHeight, BG_TOP, BG_BOTTOM);
        drawBorder(guiGraphics, x, y, BOX_WIDTH, boxHeight);
        // 内高亮
        guiGraphics.fill(x + 1, y + 1, x + BOX_WIDTH - 1, y + 2, BORDER_INNER);

        int textX = x + PADDING;
        int textY = y + PADDING;

        // 方块名称（大标题，带图标）
        guiGraphics.drawString(mc.font, "\u00A7f\u25A0 " + blockName, textX, textY, TEXT_WHITE, true);
        textY += LINE_HEIGHT;

        // 方块 ID（小字灰色）
        guiGraphics.drawString(mc.font, "\u00A78" + blockId, textX + 8, textY, TEXT_DARK, true);
        textY += LINE_HEIGHT;

        // 分隔线
        guiGraphics.fill(x + 4, textY, x + BOX_WIDTH - 4, textY + 1, SEPARATOR_COLOR);
        textY += 4;

        // 坐标
        guiGraphics.drawString(mc.font, "\u00A77\u2316 坐标: \u00A7f" + coordText,
                textX, textY, TEXT_GRAY, true);
        textY += LINE_HEIGHT;

        // 光照（显示方块光照，带颜色标记）
        String lightColor = blockLight >= 8 ? "\u00A7a" : (blockLight >= 1 ? "\u00A7e" : "\u00A7c");
        guiGraphics.drawString(mc.font,
                String.format("\u00A77\u2600 光照: %s%d \u00A77(方块)", lightColor, blockLight),
                textX, textY, TEXT_GRAY, true);
        textY += LINE_HEIGHT;

        // 硬度 + 工具
        String hardnessStr = hardness < 0 ? "不可破坏" : String.format("%.1f", hardness);
        String hardnessColor = hardness < 0 ? "\u00A7c" : (hardness > 10 ? "\u00A7e" : "\u00A7a");
        guiGraphics.drawString(mc.font,
                String.format("\u00A77\u2692 硬度: %s%s \u00A77| \u00A77工具: \u00A7f%s", hardnessColor, hardnessStr, toolHint),
                textX, textY, TEXT_GRAY, true);
    }

    // ==================== 实体信息渲染 ====================
    private static void renderEntityInfo(GuiGraphics guiGraphics, Minecraft mc,
                                          EntityHitResult hitResult, int x, int y) {
        Entity entity = hitResult.getEntity();
        String entityName = entity.getName().getString();
        String entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
        boolean isLiving = entity instanceof LivingEntity;

        // 确定实体类型和颜色
        String typeLabel;
        int typeColor;
        if (entity instanceof Monster) {
            typeLabel = "\u2620 敌对生物";
            typeColor = COLOR_HOSTILE;
        } else if (entity instanceof Animal) {
            typeLabel = "\u2665 友好生物";
            typeColor = COLOR_PASSIVE;
        } else if (entity instanceof Player) {
            typeLabel = "\u263A 玩家";
            typeColor = COLOR_PLAYER;
        } else {
            typeLabel = "\u25CB 中立";
            typeColor = COLOR_NEUTRAL;
        }

        // 计算高度
        int lineCount = 3; // 名称 + ID + 类型
        if (isLiving) lineCount += 2; // 血量文本 + 血条
        int boxHeight = lineCount * LINE_HEIGHT + PADDING * 2 + 4;
        if (isLiving) boxHeight += BAR_HEIGHT + 4;

        // 绘制背景
        guiGraphics.fillGradient(x, y, x + BOX_WIDTH, y + boxHeight, BG_TOP, BG_BOTTOM);
        drawBorder(guiGraphics, x, y, BOX_WIDTH, boxHeight);
        guiGraphics.fill(x + 1, y + 1, x + BOX_WIDTH - 1, y + 2, BORDER_INNER);

        int textX = x + PADDING;
        int textY = y + PADDING;

        // 实体名称
        guiGraphics.drawString(mc.font, "\u00A7f\u25CF " + entityName, textX, textY, TEXT_WHITE, true);
        textY += LINE_HEIGHT;

        // 实体 ID
        guiGraphics.drawString(mc.font, "\u00A78" + entityId, textX + 8, textY, TEXT_DARK, true);
        textY += LINE_HEIGHT;

        // 分隔线
        guiGraphics.fill(x + 4, textY, x + BOX_WIDTH - 4, textY + 1, SEPARATOR_COLOR);
        textY += 4;

        // 类型标签
        guiGraphics.drawString(mc.font, typeLabel, textX, textY, typeColor, true);
        textY += LINE_HEIGHT;

        // 血量
        if (isLiving) {
            LivingEntity living = (LivingEntity) entity;
            float health = living.getHealth();
            float maxHealth = living.getMaxHealth();
            float armor = living.getArmorValue();
            float healthPercent = Math.min(health / maxHealth, 1.0f);

            // 血量文本 + 护甲值
            String armorStr = armor > 0 ? String.format(" \u00A77| \u00A7b\u2748 %.0f", armor) : "";
            String healthColor = healthPercent > 0.5f ? "\u00A7a" : (healthPercent > 0.25f ? "\u00A7e" : "\u00A7c");
            guiGraphics.drawString(mc.font,
                    String.format("\u00A77\u2764 %s%.1f \u00A77/ \u00A7f%.1f%s", healthColor, health, maxHealth, armorStr),
                    textX, textY, TEXT_GRAY, true);
            textY += LINE_HEIGHT;

            // 血条
            int barWidth = BOX_WIDTH - PADDING * 2;
            guiGraphics.fill(textX, textY, textX + barWidth, textY + BAR_HEIGHT, BAR_BG);

            int healthWidth = Math.max(1, (int) (barWidth * healthPercent));
            int barColor = healthPercent > 0.5f ? BAR_HIGH : (healthPercent > 0.25f ? BAR_MID : BAR_LOW);
            guiGraphics.fill(textX, textY, textX + healthWidth, textY + BAR_HEIGHT, barColor);

            // 血条边框
            guiGraphics.fill(textX, textY, textX + barWidth, textY + 1, 0x40FFFFFF);
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 获取推荐工具提示
     */
    private static String getToolHint(BlockState state) {
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) return "镐";
        if (state.is(BlockTags.MINEABLE_WITH_AXE)) return "斧";
        if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) return "锹";
        if (state.is(BlockTags.MINEABLE_WITH_HOE)) return "锄";
        float hardness = state.getBlock().defaultDestroyTime();
        if (hardness < 0) return "不可破坏";
        if (hardness == 0) return "手撸";
        return "任意";
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
