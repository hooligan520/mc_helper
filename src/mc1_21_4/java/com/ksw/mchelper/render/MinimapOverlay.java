package com.ksw.mchelper.render;

import com.ksw.mchelper.config.MCHelperConfig;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 迷你地图覆盖层（M 键切换）
 *
 * 性能优化方案：
 * 1. 异步扫描：scanMap 在后台线程执行，不阻塞主线程
 * 2. NativeImage + DynamicTexture：扫描完成后上传为 GPU 纹理，
 *    每帧渲染只需一次 drawTexture，而不是 10000 次 fill()
 * 3. 玩家位置实时从 mc.player 读取，不依赖缓存，无滞后
 * 4. 扫描间隔：每 40 tick（2 秒）或移动超过 16 格才刷新
 */
public class MinimapOverlay {

    // ==================== 布局常量 ====================
    private static final int BORDER_WIDTH = 2;
    private static final int MARGIN_RIGHT = 6;
    private static final int MARGIN_TOP   = 6;
    private static final int SCAN_INTERVAL = 40;   // 每 40 tick 刷新一次
    private static final int MOVE_THRESHOLD = 16;  // 移动超过 16 格才强制刷新

    // ==================== 状态 ====================
    private BlockPos lastScanPos = null;
    private int tickCounter = 0;
    private final AtomicBoolean scanning = new AtomicBoolean(false);
    private boolean forceRefresh = false;

    // 待上传的像素数据（后台线程写 → 主线程读）
    private volatile int[] pendingColors = null;
    private volatile int pendingSize = 0;

    // GPU 纹理（只在主线程操作）
    private DynamicTexture mapTexture = null;
    private ResourceLocation mapTextureId = null;
    private int textureSize = 0;

    public void requestRefresh() { forceRefresh = true; }

    public void clearCache() {
        lastScanPos = null;
        pendingColors = null;
        // 销毁纹理
        if (mapTextureId != null) {
            Minecraft.getInstance().getTextureManager().release(mapTextureId);
            mapTextureId = null;
            mapTexture = null;
            textureSize = 0;
        }
    }

    // ==================== Tick：触发异步扫描 ====================
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!MCHelperConfig.showMinimap) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        tickCounter++;

        boolean shouldScan = forceRefresh;
        if (forceRefresh) {
            forceRefresh = false;
            tickCounter = 0;
        }

        if (!shouldScan) {
            if (tickCounter < SCAN_INTERVAL) return;
            tickCounter = 0;
            BlockPos playerPos = mc.player.blockPosition();
            if (lastScanPos != null && playerPos.distManhattan(lastScanPos) < MOVE_THRESHOLD) return;
        }

        // 只有没有正在扫描时才启动新扫描
        if (scanning.compareAndSet(false, true)) {
            lastScanPos = mc.player.blockPosition();
            final int mapSize = MCHelperConfig.minimapSize;
            final BlockPos center = lastScanPos;
            final Level level = mc.level;

            // 异步执行扫描（不阻塞主线程）
            CompletableFuture.runAsync(() -> {
                try {
                    int[] colors = scanMap(level, center, mapSize);
                    pendingColors = colors;
                    pendingSize = mapSize;
                } finally {
                    scanning.set(false);
                }
            });
        }
    }

    /**
     * 后台扫描地图颜色（在异步线程执行）
     * 注意：只读操作，ClientLevel 的 getHeight/getBlockState 对多线程读是安全的
     */
    private static int[] scanMap(Level level, BlockPos center, int mapSize) {
        int half = mapSize / 2;
        int[] colors = new int[mapSize * mapSize];

        for (int dz = -half; dz < mapSize - half; dz++) {
            for (int dx = -half; dx < mapSize - half; dx++) {
                int worldX = center.getX() + dx;
                int worldZ = center.getZ() + dz;

                // 获取地表高度
                int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, worldX, worldZ) - 1;
                surfaceY = Math.max(surfaceY, level.getMinY());

                BlockPos pos = new BlockPos(worldX, surfaceY, worldZ);
                MapColor mapColor = level.getBlockState(pos).getMapColor(level, pos);

                int argb = mapColorToARGB(mapColor);

                // 高度明暗（立体感）
                int diff = surfaceY - center.getY();
                if (diff > 2) argb = brighten(argb, Math.min(40, diff * 3));
                else if (diff < -2) argb = darken(argb, Math.min(40, -diff * 3));

                colors[(dz + half) * mapSize + (dx + half)] = argb;
            }
        }

        return colors;
    }

    // ==================== 渲染 ====================
    public final LayeredDraw.Layer MINIMAP_LAYER = (guiGraphics, deltaTracker) -> {
        int screenWidth = guiGraphics.guiWidth();
        int screenHeight = guiGraphics.guiHeight();
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
        if (!MCHelperConfig.showMinimap) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.getDebugOverlay().showDebugScreen()) return;

        // 如果有待上传的新数据，在主线程上传纹理
        int[] pending = pendingColors;
        if (pending != null && pendingSize > 0) {
            pendingColors = null;
            uploadTexture(mc, pending, pendingSize);
        }

        int mapSize = MCHelperConfig.minimapSize;
        int totalSize = mapSize + BORDER_WIDTH * 2;
        int baseX = screenWidth - totalSize - MARGIN_RIGHT;
        int baseY = MARGIN_TOP;
        int mapX = baseX + BORDER_WIDTH;
        int mapY = baseY + BORDER_WIDTH;

        // 背景
        guiGraphics.fill(baseX, baseY, baseX + totalSize, baseY + totalSize, 0xEE000000);

        // 渲染地图纹理（一次 draw call）
        if (mapTextureId != null && textureSize == mapSize) {
            // 1.21.4 blit 签名：blit(ResourceLocation, x, y, 0, 0, w, h)
            guiGraphics.blit(net.minecraft.client.renderer.RenderType::guiTextured, mapTextureId,
                    mapX, mapY, 0, 0, mapSize, mapSize, mapSize, mapSize);
        } else {
            // 还没有纹理
            guiGraphics.drawCenteredString(mc.font, "§7加载中...",
                    mapX + mapSize / 2, mapY + mapSize / 2 - 4, 0xFFAAAAAA);
        }

        // 玩家位置（实时，永远在中心）
        int cx = mapX + mapSize / 2;
        int cy = mapY + mapSize / 2;
        drawPlayerArrow(guiGraphics, cx, cy, mc.player.getYRot());

        // 边框
        guiGraphics.fill(baseX, baseY, baseX + totalSize, baseY + 1, 0xFF0F3460);
        guiGraphics.fill(baseX, baseY + totalSize - 1, baseX + totalSize, baseY + totalSize, 0xFF0F3460);
        guiGraphics.fill(baseX, baseY, baseX + 1, baseY + totalSize, 0xFF0F3460);
        guiGraphics.fill(baseX + totalSize - 1, baseY, baseX + totalSize, baseY + totalSize, 0xFF0F3460);

        // 坐标（实时读取，不滞后）
        BlockPos playerPos = mc.player.blockPosition();
        guiGraphics.drawCenteredString(mc.font,
                String.format("§7%d, %d", playerPos.getX(), playerPos.getZ()),
                baseX + totalSize / 2, baseY + totalSize + 2, 0xFFAABBCC);
    };

    /**
     * 将颜色数组上传为 GPU 纹理（只在主线程调用）
     */
    private void uploadTexture(Minecraft mc, int[] colors, int size) {
        // 尺寸变化时重新创建纹理
        if (mapTexture == null || textureSize != size) {
            if (mapTextureId != null) {
                mc.getTextureManager().release(mapTextureId);
            }
            mapTexture = new DynamicTexture(size, size, false);
            mapTextureId = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                    "mchelper", "minimap_" + System.currentTimeMillis());
            mc.getTextureManager().register(mapTextureId, mapTexture);
            textureSize = size;
        }

        NativeImage img = mapTexture.getPixels();
        if (img == null) return;

        for (int i = 0; i < size * size; i++) {
            int argb = colors[i];
            // NativeImage 使用 ABGR 格式
            int a = (argb >> 24) & 0xFF;
            int r = (argb >> 16) & 0xFF;
            int g = (argb >> 8)  & 0xFF;
            int b = argb & 0xFF;
            // NativeImage.setPixelRGBA 在 1.21.4 中已改为 setPixel
            img.setPixel(i % size, i / size, (a << 24) | (b << 16) | (g << 8) | r);
        }

        mapTexture.upload();
    }

    // ==================== 玩家箭头 ====================
    private static void drawPlayerArrow(GuiGraphics g, int cx, int cy, float yaw) {
        double rad = Math.toRadians((yaw % 360 + 360) % 360);
        int size = 5;
        int tx = (int) (cx + size * Math.sin(rad));
        int ty = (int) (cy - size * Math.cos(rad));

        // 白色中心点
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFFFFFFF);
        // 红色方向指示
        g.fill(tx - 1, ty - 1, tx + 1, ty + 1, 0xFFFF4444);
    }

    // ==================== 工具方法 ====================
    private static int mapColorToARGB(MapColor mapColor) {
        if (mapColor == MapColor.NONE) return 0xFF303030;
        int rgb = mapColor.col;
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8)  & 0xFF;
        int b = rgb & 0xFF;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int brighten(int argb, int amount) {
        int r = Math.min(255, ((argb >> 16) & 0xFF) + amount);
        int g = Math.min(255, ((argb >> 8)  & 0xFF) + amount);
        int b = Math.min(255, (argb & 0xFF) + amount);
        return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    private static int darken(int argb, int amount) {
        int r = Math.max(0, ((argb >> 16) & 0xFF) - amount);
        int g = Math.max(0, ((argb >> 8)  & 0xFF) - amount);
        int b = Math.max(0, (argb & 0xFF) - amount);
        return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
    }
}
