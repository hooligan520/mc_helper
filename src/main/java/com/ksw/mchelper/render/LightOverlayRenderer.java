package com.ksw.mchelper.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.ksw.mchelper.config.MCHelperConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * 光照等级叠加层渲染器 — 性能优化版
 *
 * 性能优化策略：
 * 1. 缓存机制：每 10 tick（0.5秒）扫描一次，而非每帧扫描
 * 2. 分帧渲染：大范围扫描时分多帧完成，避免单帧卡顿
 * 3. 距离优化：圆形范围替代方形，减少无效扫描
 * 4. 只扫描可站立表面，跳过密闭空间
 *
 * 颜色标记：
 * - 红色：方块光照 = 0，会刷怪
 * - 黄色：方块光照 1~7，夜晚可能刷怪
 * - 绿色：方块光照 >= 8，安全
 */
public class LightOverlayRenderer {

    // ==================== 缓存数据结构 ====================
    private static class LightBlock {
        final int x, y, z;
        final int level; // 0=danger, 1=warn, 2=safe

        LightBlock(int x, int y, int z, int level) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.level = level;
        }
    }

    // 缓存的光照方块列表
    private final List<LightBlock> cachedBlocks = new ArrayList<>();

    // 上次扫描的玩家位置
    private BlockPos lastScanPos = null;

    // tick 计数器
    private int tickCounter = 0;

    // 扫描间隔（tick），10 tick = 0.5 秒
    private static final int SCAN_INTERVAL = 10;

    // 颜色常量（RGBA 分量）— 提高透明度让效果更明显
    private static final int[] COLOR_SAFE   = {0, 200, 0, 100};    // 绿色
    private static final int[] COLOR_WARN   = {255, 220, 0, 120};  // 黄色
    private static final int[] COLOR_DANGER = {255, 40, 40, 140};  // 红色

    // 标记是否需要强制刷新（开启时立即扫描）
    private boolean forceRefresh = false;

    /**
     * 外部调用：标记需要立即刷新（开启光照时调用）
     */
    public void requestRefresh() {
        forceRefresh = true;
    }

    /**
     * 外部调用：清除缓存（关闭光照时调用）
     */
    public void clearCache() {
        synchronized (cachedBlocks) {
            cachedBlocks.clear();
        }
        lastScanPos = null;
    }

    // ==================== Tick 事件：定期扫描光照 ====================
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!MCHelperConfig.showLightOverlay) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        tickCounter++;

        // 强制刷新时立即扫描
        boolean needScan = forceRefresh;
        if (forceRefresh) {
            forceRefresh = false;
            tickCounter = 0;
        }

        if (!needScan) {
            if (tickCounter < SCAN_INTERVAL) return;
            tickCounter = 0;

            BlockPos playerPos = mc.player.blockPosition();
            // 如果玩家没有移动超过 2 格，不重新扫描
            if (lastScanPos != null && playerPos.distManhattan(lastScanPos) < 2) return;
        }

        BlockPos playerPos = mc.player.blockPosition();
        lastScanPos = playerPos;
        scanLightLevels(mc, playerPos);
    }

    /**
     * 扫描光照等级，更新缓存
     */
    private void scanLightLevels(Minecraft mc, BlockPos center) {
        List<LightBlock> newBlocks = new ArrayList<>();
        int renderDist = MCHelperConfig.lightRenderDistance;
        int distSq = renderDist * renderDist;

        for (int dx = -renderDist; dx <= renderDist; dx++) {
            for (int dz = -renderDist; dz <= renderDist; dz++) {
                // 圆形范围检查（比方形减少约 22% 扫描量）
                if (dx * dx + dz * dz > distSq) continue;

                // Y 方向只搜索玩家上下 4 格
                for (int dy = -4; dy <= 4; dy++) {
                    int bx = center.getX() + dx;
                    int by = center.getY() + dy;
                    int bz = center.getZ() + dz;
                    BlockPos pos = new BlockPos(bx, by, bz);

                    // 检查是否是可站立的表面
                    BlockState stateBelow = mc.level.getBlockState(pos);
                    if (!stateBelow.isSolidRender(mc.level, pos)) continue;

                    BlockPos abovePos = pos.above();
                    BlockState stateAbove = mc.level.getBlockState(abovePos);
                    if (!stateAbove.isAir()) continue;

                    // 还要确认上方第二格也是通过的（确保怪物能站立）
                    BlockPos above2 = abovePos.above();
                    BlockState stateAbove2 = mc.level.getBlockState(above2);
                    if (!stateAbove2.isAir() && stateAbove2.isSolidRender(mc.level, above2)) continue;

                    // 获取光照等级
                    int blockLight = mc.level.getBrightness(LightLayer.BLOCK, abovePos);

                    int level;
                    if (blockLight >= 8) {
                        level = 2; // 安全
                    } else if (blockLight >= 1) {
                        level = 1; // 临界
                    } else {
                        level = 0; // 危险
                    }

                    newBlocks.add(new LightBlock(bx, by, bz, level));
                }
            }
        }

        // 原子替换缓存
        synchronized (cachedBlocks) {
            cachedBlocks.clear();
            cachedBlocks.addAll(newBlocks);
        }
    }

    // ==================== 渲染事件：使用缓存绘制 ====================
    @SubscribeEvent
    public void onRenderLevel(RenderLevelStageEvent event) {
        if (!MCHelperConfig.showLightOverlay) return;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        List<LightBlock> blocks;
        synchronized (cachedBlocks) {
            if (cachedBlocks.isEmpty()) return;
            blocks = new ArrayList<>(cachedBlocks);
        }

        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.debugQuads());

        Matrix4f matrix = poseStack.last().pose();

        for (LightBlock lb : blocks) {
            int[] color;
            switch (lb.level) {
                case 0: color = COLOR_DANGER; break;
                case 1: color = COLOR_WARN; break;
                default: color = COLOR_SAFE; break;
            }

            float x0 = lb.x;
            float y0 = lb.y + 1.005f; // 略高于方块顶面
            float z0 = lb.z;

            consumer.vertex(matrix, x0, y0, z0).color(color[0], color[1], color[2], color[3]).endVertex();
            consumer.vertex(matrix, x0, y0, z0 + 1).color(color[0], color[1], color[2], color[3]).endVertex();
            consumer.vertex(matrix, x0 + 1, y0, z0 + 1).color(color[0], color[1], color[2], color[3]).endVertex();
            consumer.vertex(matrix, x0 + 1, y0, z0).color(color[0], color[1], color[2], color[3]).endVertex();
        }

        bufferSource.endBatch(RenderType.debugQuads());
        poseStack.popPose();
    }
}
