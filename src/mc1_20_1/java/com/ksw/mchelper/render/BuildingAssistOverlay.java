package com.ksw.mchelper.render;

import com.ksw.mchelper.config.MCHelperConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;

/**
 * 建筑辅助覆盖层（B 键切换）
 *
 * 功能：
 * 1. 地面网格线：在玩家脚下渲染方块网格，帮助建筑对齐
 * 2. 水平高度线：沿玩家当前 Y 高度渲染一圈水平线，便于建筑找平
 * 3. 屏幕 HUD：显示当前高度（Y）、准心所指方向的方块坐标
 */
public class BuildingAssistOverlay {

    // 网格颜色（白色，半透明）
    private static final int[] GRID_COLOR  = {200, 200, 255, 50};
    // 水平线颜色（青色，半透明）
    private static final int[] LEVEL_COLOR = {0, 220, 220, 80};
    // 准心方块高亮色（黄色，半透明）
    private static final int[] TARGET_COLOR = {255, 220, 0, 60};

    // 网格/水平线渲染范围（格）
    private static final int GRID_RANGE  = 8;
    private static final int LEVEL_RANGE = 12;

    // ==================== 3D 渲染（网格线 + 水平线） ====================
    @SubscribeEvent
    public void onRenderLevel(RenderLevelStageEvent event) {
        if (!MCHelperConfig.showBuildingAssist) return;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        PoseStack poseStack = event.getPoseStack();
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        BlockPos playerPos = mc.player.blockPosition();

        poseStack.pushPose();
        poseStack.translate(-cam.x, -cam.y, -cam.z);

        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buf.getBuffer(RenderType.debugQuads());
        Matrix4f mat = poseStack.last().pose();

        // 1. 地面网格线（玩家脚下的方块网格）
        int playerY = playerPos.getY(); // 脚下方块的 Y
        for (int dx = -GRID_RANGE; dx <= GRID_RANGE; dx++) {
            for (int dz = -GRID_RANGE; dz <= GRID_RANGE; dz++) {
                int bx = playerPos.getX() + dx;
                int bz = playerPos.getZ() + dz;

                // 只在方块边缘绘制线条（沿 X 方向和 Z 方向各画一条线）
                float y0 = playerY + 0.005f;

                // X 方向线（沿 z = bz）
                if (Math.abs(dz) <= GRID_RANGE) {
                    drawLine(mat, vc,
                            bx, y0, bz,
                            bx + 1, y0, bz,
                            GRID_COLOR);
                }
                // Z 方向线（沿 x = bx）
                if (Math.abs(dx) <= GRID_RANGE) {
                    drawLine(mat, vc,
                            bx, y0, bz,
                            bx, y0, bz + 1,
                            GRID_COLOR);
                }
            }
        }

        // 2. 水平高度线（在玩家当前 Y 高度绘制一圈横线）
        float levelY = playerY + 0.01f;
        for (int d = -LEVEL_RANGE; d <= LEVEL_RANGE; d++) {
            int bx = playerPos.getX() + d;
            int bz = playerPos.getZ() + d;

            // 北边和南边的水平线
            drawLine(mat, vc, bx, levelY, playerPos.getZ() - LEVEL_RANGE,
                              bx + 1, levelY, playerPos.getZ() - LEVEL_RANGE, LEVEL_COLOR);
            drawLine(mat, vc, bx, levelY, playerPos.getZ() + LEVEL_RANGE,
                              bx + 1, levelY, playerPos.getZ() + LEVEL_RANGE, LEVEL_COLOR);
            // 西边和东边的水平线
            drawLine(mat, vc, playerPos.getX() - LEVEL_RANGE, levelY, bz,
                              playerPos.getX() - LEVEL_RANGE, levelY, bz + 1, LEVEL_COLOR);
            drawLine(mat, vc, playerPos.getX() + LEVEL_RANGE, levelY, bz,
                              playerPos.getX() + LEVEL_RANGE, levelY, bz + 1, LEVEL_COLOR);
        }

        // 3. 准心指向的方块高亮（可选：辅助定位目标）
        if (mc.hitResult != null &&
                mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            net.minecraft.world.phys.BlockHitResult bhr =
                    (net.minecraft.world.phys.BlockHitResult) mc.hitResult;
            BlockPos tp = bhr.getBlockPos();
            float tx = tp.getX(), ty = tp.getY(), tz = tp.getZ();
            drawQuad(mat, vc, tx, ty + 1.003f, tz, tx + 1, ty + 1.003f, tz + 1, TARGET_COLOR);
        }

        buf.endBatch(RenderType.debugQuads());
        poseStack.popPose();
    }

    // ==================== HUD 信息叠加（高度 + 坐标提示） ====================
    public static final IGuiOverlay HUD_BUILDING = (ForgeGui gui, GuiGraphics guiGraphics,
                                                     float partialTick, int screenWidth, int screenHeight) -> {
        if (!MCHelperConfig.showBuildingAssist) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.options.renderDebug) return;

        BlockPos pos = mc.player.blockPosition();

        // 右下角（装备 HUD 上方）显示当前 Y 高度
        String yText = String.format("§bY: §f%d", pos.getY());
        int tw = mc.font.width("Y: " + pos.getY());
        int bx = screenWidth - tw - 12;
        int by = screenHeight - 90; // 在装备 HUD 上方

        guiGraphics.fill(bx - 4, by - 2, bx + tw + 4, by + 11, 0x88000000);
        guiGraphics.drawString(mc.font, yText, bx, by, 0xFFAABBCC, true);

        // 准心指向方块时显示目标坐标
        if (mc.hitResult != null &&
                mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            net.minecraft.world.phys.BlockHitResult bhr =
                    (net.minecraft.world.phys.BlockHitResult) mc.hitResult;
            BlockPos tp = bhr.getBlockPos();
            String tCoord = String.format("§7目标: §f%d, %d, %d", tp.getX(), tp.getY(), tp.getZ());
            int tw2 = mc.font.width("目标: " + tp.getX() + ", " + tp.getY() + ", " + tp.getZ());
            int bx2 = screenWidth - tw2 - 12;
            int by2 = by + 13;
            guiGraphics.fill(bx2 - 4, by2 - 2, bx2 + tw2 + 4, by2 + 11, 0x88000000);
            guiGraphics.drawString(mc.font, tCoord, bx2, by2, 0xFFAABBCC, true);
        }
    };

    // ==================== 工具方法 ====================

    /** 绘制一条细线（用两个极窄的 quad 模拟） */
    private static void drawLine(Matrix4f mat, VertexConsumer vc,
                                  float x0, float y0, float z0,
                                  float x1, float y1, float z1,
                                  int[] c) {
        float w = 0.02f; // 线宽
        // 判断线方向，画垂直于方向的薄片
        if (Math.abs(x1 - x0) > Math.abs(z1 - z0)) {
            // X 方向线
            drawQuad(mat, vc, x0, y0, z0, x1, y1 + w, z1, c);
        } else {
            // Z 方向线
            drawQuad(mat, vc, x0, y0, z0, x1, y1 + w, z1, c);
        }
    }

    /** 绘制一个平面四边形（水平面） */
    private static void drawQuad(Matrix4f mat, VertexConsumer vc,
                                  float x0, float y, float z0,
                                  float x1, float y1, float z1,
                                  int[] c) {
        vc.vertex(mat, x0, y, z0).color(c[0], c[1], c[2], c[3]).endVertex();
        vc.vertex(mat, x0, y, z1).color(c[0], c[1], c[2], c[3]).endVertex();
        vc.vertex(mat, x1, y, z1).color(c[0], c[1], c[2], c[3]).endVertex();
        vc.vertex(mat, x1, y, z0).color(c[0], c[1], c[2], c[3]).endVertex();
    }
}
