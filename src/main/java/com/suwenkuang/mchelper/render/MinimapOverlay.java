package com.suwenkuang.mchelper.render;

import com.suwenkuang.mchelper.config.MCHelperConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 迷你地图覆盖层（N 键切换）
 *
 * 渲染玩家周围区域的俯视地图，显示地形颜色、玩家位置和朝向。
 *
 * 性能方案：
 * - 定时缓存（每 20 tick = 1 秒刷新一次地图颜色数据）
 * - 玩家移动超过 8 格才刷新
 * - 使用方块的 MapColor 着色（与原版地图一致）
 */
public class MinimapOverlay {

    // ==================== 布局常量 ====================
    private static final float SCALE = 1.0f; // 迷你地图不缩放，自己控制大小
    private static final int BORDER_WIDTH = 2;
    private static final int MARGIN_RIGHT = 6;
    private static final int MARGIN_TOP   = 6;

    // 地图颜色缓存（每个格子一个 ARGB 颜色）
    private int[] mapColors = null;
    private int cachedMapSize = 0;
    private BlockPos lastScanPos = null;
    private int tickCounter = 0;
    private static final int SCAN_INTERVAL = 20; // 每 20 tick 刷新

    // 是否需要立即刷新
    private boolean forceRefresh = false;

    public void requestRefresh() { forceRefresh = true; }
    public void clearCache() { mapColors = null; lastScanPos = null; }

    // ==================== Tick 事件 ====================
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!MCHelperConfig.showMinimap) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        tickCounter++;
        boolean shouldScan = forceRefresh;
        if (forceRefresh) { forceRefresh = false; tickCounter = 0; }

        if (!shouldScan) {
            if (tickCounter < SCAN_INTERVAL) return;
            tickCounter = 0;
            BlockPos playerPos = mc.player.blockPosition();
            if (lastScanPos != null && playerPos.distManhattan(lastScanPos) < 8) return;
        }

        lastScanPos = mc.player.blockPosition();
        cachedMapSize = MCHelperConfig.minimapSize;
        scanMap(mc, lastScanPos, cachedMapSize);
    }

    /**
     * 扫描地图颜色数据
     */
    private void scanMap(Minecraft mc, BlockPos center, int mapSize) {
        int half = mapSize / 2;
        int[] colors = new int[mapSize * mapSize];

        for (int dz = -half; dz < mapSize - half; dz++) {
            for (int dx = -half; dx < mapSize - half; dx++) {
                int worldX = center.getX() + dx;
                int worldZ = center.getZ() + dz;

                // 从顶部向下找第一个非空气的方块（地表）
                int surfaceY = mc.level.getHeight(
                        net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                        worldX, worldZ) - 1;
                surfaceY = Math.max(surfaceY, mc.level.getMinBuildHeight());

                BlockPos pos = new BlockPos(worldX, surfaceY, worldZ);
                BlockState state = mc.level.getBlockState(pos);

                // 用 MapColor 获取颜色（和原版地图一样）
                MapColor mapColor = state.getMapColor(mc.level, pos);
                int argb = mapColorToARGB(mapColor, dx, dz);

                // 高度阴影（让地形有立体感）
                if (surfaceY > center.getY()) {
                    argb = brighten(argb, 20);
                } else if (surfaceY < center.getY() - 2) {
                    argb = darken(argb, 20);
                }

                int idx = (dz + half) * mapSize + (dx + half);
                colors[idx] = argb;
            }
        }

        synchronized (this) {
            mapColors = colors;
        }
    }

    // ==================== 渲染 ====================
    public final IGuiOverlay HUD_MINIMAP = (ForgeGui gui, GuiGraphics guiGraphics,
                                            float partialTick, int screenWidth, int screenHeight) -> {
        if (!MCHelperConfig.showMinimap) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.options.renderDebug) return;

        int mapSize = MCHelperConfig.minimapSize;
        int totalSize = mapSize + BORDER_WIDTH * 2;

        // 右上角定位
        int baseX = screenWidth - totalSize - MARGIN_RIGHT;
        int baseY = MARGIN_TOP;

        int mapX = baseX + BORDER_WIDTH;
        int mapY = baseY + BORDER_WIDTH;

        // 绘制背景边框
        guiGraphics.fill(baseX, baseY, baseX + totalSize, baseY + totalSize, 0xFF000000);
        guiGraphics.fill(baseX + 1, baseY + 1, baseX + totalSize - 1, baseY + totalSize - 1, 0xFF1A1A2E);

        // 绘制地图
        int[] colors;
        synchronized (this) {
            colors = mapColors;
        }

        if (colors != null && colors.length == mapSize * mapSize) {
            for (int pz = 0; pz < mapSize; pz++) {
                for (int px = 0; px < mapSize; px++) {
                    int color = colors[pz * mapSize + px];
                    if (color != 0) {
                        guiGraphics.fill(mapX + px, mapY + pz, mapX + px + 1, mapY + pz + 1, color);
                    }
                }
            }
        } else {
            // 还没有缓存数据，显示加载中
            guiGraphics.drawCenteredString(mc.font, "§7加载中...",
                    mapX + mapSize / 2, mapY + mapSize / 2 - 4, 0xFFAAAAAA);
        }

        // 绘制玩家位置（中心白点 + 方向三角）
        int centerPx = mapX + mapSize / 2;
        int centerPz = mapY + mapSize / 2;

        // 玩家方向箭头
        drawPlayerArrow(guiGraphics, centerPx, centerPz, mc.player.getYRot());

        // 边框装饰
        guiGraphics.fill(baseX, baseY, baseX + totalSize, baseY + 1, 0xFF0F3460);
        guiGraphics.fill(baseX, baseY + totalSize - 1, baseX + totalSize, baseY + totalSize, 0xFF0F3460);
        guiGraphics.fill(baseX, baseY, baseX + 1, baseY + totalSize, 0xFF0F3460);
        guiGraphics.fill(baseX + totalSize - 1, baseY, baseX + totalSize, baseY + totalSize, 0xFF0F3460);

        // 坐标标注（地图下方）
        BlockPos playerPos = mc.player.blockPosition();
        String coordStr = String.format("§7%d, %d", playerPos.getX(), playerPos.getZ());
        guiGraphics.drawCenteredString(mc.font, coordStr,
                baseX + totalSize / 2, baseY + totalSize + 2, 0xFFAABBCC);
    };

    /**
     * 在地图上绘制玩家方向箭头
     */
    private static void drawPlayerArrow(GuiGraphics g, int cx, int cy, float yaw) {
        // 归一化 yaw (-180 ~ 180)
        float angle = (yaw % 360 + 360) % 360; // 0 ~ 360
        double rad = Math.toRadians(angle);

        // 箭头三个顶点（相对中心）
        int size = 4;
        int tx = (int) (cx + size * Math.sin(rad));
        int ty = (int) (cy - size * Math.cos(rad));

        // 画一个小菱形作为玩家标记
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFFFFFFF);   // 白色中心
        g.fill(tx, ty, tx + 1, ty + 1, 0xFFFF5555);              // 红色方向点
    }

    /**
     * MapColor 转 ARGB
     */
    private static int mapColorToARGB(MapColor mapColor, int dx, int dz) {
        if (mapColor == MapColor.NONE) return 0xFF303030;
        // MapColor.col 是 RGB888
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
