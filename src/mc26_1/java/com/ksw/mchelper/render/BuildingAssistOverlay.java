package com.ksw.mchelper.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.ksw.mchelper.config.MCHelperConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.FramePassManager;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraftforge.client.event.AddFramePassEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayer;
import org.joml.Matrix4f;

/**
 * 建筑辅助覆盖层（B 键切换）
 *
 * 功能：
 * 1. 地面网格线：在玩家脚下渲染方块网格，帮助建筑对齐
 * 2. 水平高度线：沿玩家当前 Y 高度渲染一圈水平线，便于建筑找平
 * 3. 准心方块高亮：黄色半透明覆盖
 * 4. 屏幕 HUD：显示当前 Y 高度和准心坐标
 */
public class BuildingAssistOverlay {

    // 网格颜色（白蓝色，半透明）
    private static final int[] GRID_COLOR   = {200, 200, 255, 50};
    // 水平线颜色（青色，半透明）
    private static final int[] LEVEL_COLOR  = {0, 220, 220, 80};
    // 准心方块高亮色（黄色，半透明）
    private static final int[] TARGET_COLOR = {255, 220, 0, 60};

    // 渲染范围（格）
    private static final int GRID_RANGE  = 8;
    private static final int LEVEL_RANGE = 12;

    // ==================== 3D 渲染（AddFramePassEvent，1.21.11 方式）====================
    // AddFramePassEvent 只触发一次，必须无条件注册 pass，开关检查放 executes() 里
    public void onAddFramePass(AddFramePassEvent event) {
        event.addPass(
            Identifier.fromNamespaceAndPath("mchelper", "building_assist_3d"),
            new FramePassManager.PassDefinition() {
                @Override
                public void extracts(LevelTargetBundle bundle, com.mojang.blaze3d.framegraph.FramePass pass) {
                    bundle.main = pass.readsAndWrites(bundle.main);
                }

                @Override
                public void executes(LevelRenderState state) {
                    // 开关检查放在 executes() 里，每帧判断
                    if (!MCHelperConfig.showBuildingAssist) return;

                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player == null || mc.level == null) return;

                    BlockPos playerPos = mc.player.blockPosition();
                    Vec3 cam = mc.gameRenderer.getMainCamera().position();

                    PoseStack poseStack = new PoseStack();
                    poseStack.translate(-cam.x, -cam.y, -cam.z);

                    MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
                    VertexConsumer vc = buf.getBuffer(RenderTypes.debugQuads());
                    Matrix4f mat = poseStack.last().pose();

                    // 1. 地面网格线
                    int playerY = playerPos.getY();
                    for (int dx = -GRID_RANGE; dx <= GRID_RANGE; dx++) {
                        for (int dz = -GRID_RANGE; dz <= GRID_RANGE; dz++) {
                            int bx = playerPos.getX() + dx;
                            int bz = playerPos.getZ() + dz;
                            float y0 = playerY + 0.005f;
                            drawLine(mat, vc, bx, y0, bz, bx + 1, y0, bz, GRID_COLOR);
                            drawLine(mat, vc, bx, y0, bz, bx, y0, bz + 1, GRID_COLOR);
                        }
                    }

                    // 2. 水平高度线
                    float levelY = playerY + 0.01f;
                    for (int d = -LEVEL_RANGE; d <= LEVEL_RANGE; d++) {
                        int bx = playerPos.getX() + d;
                        int bz = playerPos.getZ() + d;
                        drawLine(mat, vc, bx, levelY, playerPos.getZ() - LEVEL_RANGE,
                                          bx + 1, levelY, playerPos.getZ() - LEVEL_RANGE, LEVEL_COLOR);
                        drawLine(mat, vc, bx, levelY, playerPos.getZ() + LEVEL_RANGE,
                                          bx + 1, levelY, playerPos.getZ() + LEVEL_RANGE, LEVEL_COLOR);
                        drawLine(mat, vc, playerPos.getX() - LEVEL_RANGE, levelY, bz,
                                          playerPos.getX() - LEVEL_RANGE, levelY, bz + 1, LEVEL_COLOR);
                        drawLine(mat, vc, playerPos.getX() + LEVEL_RANGE, levelY, bz,
                                          playerPos.getX() + LEVEL_RANGE, levelY, bz + 1, LEVEL_COLOR);
                    }

                    // 3. 准心方块高亮
                    if (mc.hitResult != null &&
                            mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                        net.minecraft.world.phys.BlockHitResult bhr =
                                (net.minecraft.world.phys.BlockHitResult) mc.hitResult;
                        BlockPos tp = bhr.getBlockPos();
                        float tx = tp.getX(), ty = tp.getY(), tz = tp.getZ();
                        drawQuad(mat, vc, tx, ty + 1.003f, tz, tx + 1, ty + 1.003f, tz + 1, TARGET_COLOR);
                    }

                    buf.endBatch(RenderTypes.debugQuads());
                }
            }
        );
    }

    // ==================== HUD 信息叠加（高度 + 坐标提示） ====================
    public static final ForgeLayer HUD_LAYER = (guiGraphics, deltaTracker) -> {
        int screenWidth = guiGraphics.guiWidth();
        int screenHeight = guiGraphics.guiHeight();
        if (!MCHelperConfig.showBuildingAssist) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.getDebugOverlay().showDebugScreen()) return;

        BlockPos pos = mc.player.blockPosition();

        // 右下角显示当前 Y 高度
        String yText = String.format("§bY: §f%d", pos.getY());
        int tw = mc.font.width("Y: " + pos.getY());
        int bx = screenWidth - tw - 12;
        int by = screenHeight - 90;

        guiGraphics.fill(bx - 4, by - 2, bx + tw + 4, by + 11, 0x88000000);
        guiGraphics.text(mc.font, yText, bx, by, 0xFFAABBCC, true);

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
            guiGraphics.text(mc.font, tCoord, bx2, by2, 0xFFAABBCC, true);
        }
    };

    // ==================== 工具方法 ====================

    private static void drawLine(Matrix4f mat, VertexConsumer vc,
                                  float x0, float y0, float z0,
                                  float x1, float y1, float z1, int[] c) {
        drawQuad(mat, vc, x0, y0, z0, x1, y1 + 0.02f, z1, c);
    }

    private static void drawQuad(Matrix4f mat, VertexConsumer vc,
                                  float x0, float y, float z0,
                                  float x1, float y1, float z1, int[] c) {
        vc.addVertex(mat, x0, y,  z0).setColor(c[0], c[1], c[2], c[3]);
        vc.addVertex(mat, x0, y,  z1).setColor(c[0], c[1], c[2], c[3]);
        vc.addVertex(mat, x1, y,  z1).setColor(c[0], c[1], c[2], c[3]);
        vc.addVertex(mat, x1, y,  z0).setColor(c[0], c[1], c[2], c[3]);
    }
}
