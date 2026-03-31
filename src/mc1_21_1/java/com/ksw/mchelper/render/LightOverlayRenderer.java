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
 * 光照等级叠加层渲染器 — 1.21.1 版本
 * 使用 RenderLevelStageEvent（1.21.1 尚无 AddFramePassEvent）
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

    private final List<LightBlock> cachedBlocks = new ArrayList<>();
    private BlockPos lastScanPos = null;
    private int tickCounter = 0;
    private static final int SCAN_INTERVAL = 10;

    private static final int[] COLOR_SAFE   = {0, 200, 0, 100};
    private static final int[] COLOR_WARN   = {255, 220, 0, 120};
    private static final int[] COLOR_DANGER = {255, 40, 40, 140};

    private boolean forceRefresh = false;

    public void requestRefresh() { forceRefresh = true; }

    public void clearCache() {
        synchronized (cachedBlocks) { cachedBlocks.clear(); }
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
        boolean needScan = forceRefresh;
        if (forceRefresh) {
            forceRefresh = false;
            tickCounter = 0;
        }

        if (!needScan) {
            if (tickCounter < SCAN_INTERVAL) return;
            tickCounter = 0;
            BlockPos playerPos = mc.player.blockPosition();
            if (lastScanPos != null && playerPos.distManhattan(lastScanPos) < 2) return;
        }

        BlockPos playerPos = mc.player.blockPosition();
        lastScanPos = playerPos;
        scanLightLevels(mc, playerPos);
    }

    private void scanLightLevels(Minecraft mc, BlockPos center) {
        List<LightBlock> newBlocks = new ArrayList<>();
        int renderDist = MCHelperConfig.lightRenderDistance;
        int distSq = renderDist * renderDist;

        for (int dx = -renderDist; dx <= renderDist; dx++) {
            for (int dz = -renderDist; dz <= renderDist; dz++) {
                if (dx * dx + dz * dz > distSq) continue;
                for (int dy = -4; dy <= 4; dy++) {
                    int bx = center.getX() + dx;
                    int by = center.getY() + dy;
                    int bz = center.getZ() + dz;
                    BlockPos pos = new BlockPos(bx, by, bz);

                    BlockState stateBelow = mc.level.getBlockState(pos);
                    if (!stateBelow.isSolidRender(mc.level, pos)) continue;

                    BlockPos abovePos = pos.above();
                    BlockState stateAbove = mc.level.getBlockState(abovePos);
                    if (!stateAbove.isAir()) continue;

                    BlockPos above2 = abovePos.above();
                    BlockState stateAbove2 = mc.level.getBlockState(above2);
                    if (!stateAbove2.isAir() && stateAbove2.isSolidRender(mc.level, above2)) continue;

                    int blockLight = mc.level.getBrightness(LightLayer.BLOCK, abovePos);
                    int level = blockLight >= 8 ? 2 : blockLight >= 1 ? 1 : 0;
                    newBlocks.add(new LightBlock(bx, by, bz, level));
                }
            }
        }

        synchronized (cachedBlocks) {
            cachedBlocks.clear();
            cachedBlocks.addAll(newBlocks);
        }
    }

    // ==================== 渲染事件：RenderLevelStageEvent（1.21.1 方式）====================
    @SubscribeEvent
    public void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        if (!MCHelperConfig.showLightOverlay) return;

        List<LightBlock> blocks;
        synchronized (cachedBlocks) {
            if (cachedBlocks.isEmpty()) return;
            blocks = new ArrayList<>(cachedBlocks);
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // 1.21.1: getPoseStack() 返回 Matrix4f，需要自己创建 PoseStack 并应用相机偏移
        PoseStack poseStack = new PoseStack();
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.debugQuads());
        Matrix4f matrix = poseStack.last().pose();

        for (LightBlock lb : blocks) {
            int[] color;
            switch (lb.level) {
                case 0: color = COLOR_DANGER; break;
                case 1: color = COLOR_WARN;   break;
                default: color = COLOR_SAFE;  break;
            }

            float x0 = lb.x;
            float y0 = lb.y + 1.005f;
            float z0 = lb.z;

            consumer.addVertex(matrix, x0,     y0, z0    ).setColor(color[0], color[1], color[2], color[3]);
            consumer.addVertex(matrix, x0,     y0, z0 + 1).setColor(color[0], color[1], color[2], color[3]);
            consumer.addVertex(matrix, x0 + 1, y0, z0 + 1).setColor(color[0], color[1], color[2], color[3]);
            consumer.addVertex(matrix, x0 + 1, y0, z0    ).setColor(color[0], color[1], color[2], color[3]);
        }

        bufferSource.endBatch(RenderType.debugQuads());
    }
}
