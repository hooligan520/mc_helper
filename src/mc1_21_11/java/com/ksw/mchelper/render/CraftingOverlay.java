package com.ksw.mchelper.render;

import com.ksw.mchelper.config.MCHelperConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeLayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.*;
import net.minecraft.util.context.ContextMap;

import java.util.ArrayList;
import java.util.List;

/**
 * 合成查询覆盖层（R 键切换）
 * 1.21.4 适配版 — 使用 RecipeHolder 和 SlotDisplay 系统
 */
public class CraftingOverlay {

    // ==================== 颜色常量 ====================
    private static final int BG_TOP      = 0xCC1A1A2E;
    private static final int BG_BOTTOM   = 0xCC16213E;
    private static final int BORDER_OUTER = 0xFF0F3460;
    private static final int BORDER_INNER = 0xFF533483;
    private static final int SEPARATOR   = 0x40FFFFFF;
    private static final int TEXT_TITLE  = 0xFF00D2FF;
    private static final int TEXT_GRAY   = 0xFFAABBCC;

    // ==================== 布局常量 ====================
    private static final float SCALE      = 0.4f;
    private static final int PADDING      = 8;
    private static final int LINE_HEIGHT  = 13;
    private static final int SLOT_SIZE    = 18;
    private static final int BOX_WIDTH    = 260;

    /** 创建包含注册表信息的 ContextMap，用于 SlotDisplay 解析 Tag 类型食材 */
    private static ContextMap makeContext(net.minecraft.world.level.Level level) {
        return net.minecraft.world.item.crafting.display.SlotDisplayContext.fromLevel(level);
    }

    public static final ForgeLayer LAYER = (guiGraphics, deltaTracker) -> {
        int screenWidth = guiGraphics.guiWidth();
        int screenHeight = guiGraphics.guiHeight();
        if (!MCHelperConfig.showCrafting) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.getDebugOverlay().showDebugScreen()) return;

        // 手持物品
        ItemStack heldItem = mc.player.getMainHandItem();
        if (heldItem.isEmpty()) heldItem = mc.player.getOffhandItem();
        if (heldItem.isEmpty()) return;

        // 1.21.4: RecipeManager 在单人模式通过 getSingleplayerServer() 获取
        var server = mc.getSingleplayerServer();
        if (server == null) return;
        RecipeManager recipeManager = server.getRecipeManager();

        List<RecipeEntry> entries = findRecipes(recipeManager, heldItem, mc.level);
        if (entries.isEmpty()) return;

        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().scale(SCALE, SCALE);

        int boxX = (int) ((screenWidth - BOX_WIDTH * SCALE) / 2 / SCALE);
        int boxY = (int) ((screenHeight * 0.65f) / SCALE);

        renderRecipeBox(guiGraphics, mc, entries.get(0), boxX, boxY, entries.size());

        guiGraphics.pose().popMatrix();
    };

    // ==================== 渲染 ====================
    private static void renderRecipeBox(GuiGraphics g, Minecraft mc,
                                         RecipeEntry entry, int x, int y, int totalCount) {
        int boxHeight = calculateHeight(entry);
        g.fillGradient(x, y, x + BOX_WIDTH, y + boxHeight, BG_TOP, BG_BOTTOM);
        drawBorder(g, x, y, BOX_WIDTH, boxHeight);
        g.fill(x + 1, y + 1, x + BOX_WIDTH - 1, y + 2, BORDER_INNER);

        int textX = x + PADDING;
        int textY = y + PADDING;

        String suffix = totalCount > 1 ? " §7(" + totalCount + "种配方)" : "";
        g.drawString(mc.font, "§b🔨 合成配方" + suffix, textX, textY, TEXT_TITLE, true);
        textY += LINE_HEIGHT;
        g.fill(x + 4, textY, x + BOX_WIDTH - 4, textY + 1, SEPARATOR);
        textY += 5;

        if (entry.type == RecipeType.CRAFTING) {
            renderCraftingGrid(g, mc, entry, textX, textY);
        } else {
            renderSmeltingRecipe(g, mc, entry, textX, textY);
        }
    }

    private static void renderCraftingGrid(GuiGraphics g, Minecraft mc,
                                            RecipeEntry entry, int textX, int textY) {
        for (int row = 0; row < entry.gridSize; row++) {
            for (int col = 0; col < entry.gridSize; col++) {
                int idx = row * entry.gridSize + col;
                int px = textX + col * SLOT_SIZE;
                int py = textY + row * SLOT_SIZE;

                g.fill(px, py, px + SLOT_SIZE - 2, py + SLOT_SIZE - 2, 0x60000000);
                g.fill(px, py, px + SLOT_SIZE - 2, py + 1, 0xFF404060);
                g.fill(px, py, px + 1, py + SLOT_SIZE - 2, 0xFF404060);

                if (idx < entry.ingredients.size() && !entry.ingredients.get(idx).isEmpty()) {
                    g.renderItem(entry.ingredients.get(idx), px + 1, py + 1);
                }
            }
        }

        int arrowX = textX + entry.gridSize * SLOT_SIZE + 4;
        int arrowY = textY + (entry.gridSize * SLOT_SIZE) / 2 - 4;
        g.drawString(mc.font, "§7➜", arrowX, arrowY, TEXT_GRAY, true);

        int resultX = arrowX + 16;
        int resultY = textY + (entry.gridSize * SLOT_SIZE) / 2 - 8;
        g.fill(resultX, resultY, resultX + SLOT_SIZE - 2, resultY + SLOT_SIZE - 2, 0x60000000);
        g.renderItem(entry.result, resultX + 1, resultY + 1);
        if (entry.result.getCount() > 1) {
            g.renderItemDecorations(mc.font, entry.result, resultX + 1, resultY + 1);
        }
    }

    private static void renderSmeltingRecipe(GuiGraphics g, Minecraft mc,
                                              RecipeEntry entry, int textX, int textY) {
        String typeLabel = switch (entry.smeltingType) {
            case "blast" -> "§7高炉烧炼";
            case "smoke" -> "§7烟熏炉";
            default -> "§7熔炉烧炼";
        };
        g.drawString(mc.font, typeLabel, textX, textY, TEXT_GRAY, true);
        textY += LINE_HEIGHT + 2;

        if (!entry.ingredients.isEmpty()) {
            ItemStack input = entry.ingredients.get(0);
            g.fill(textX, textY, textX + SLOT_SIZE - 2, textY + SLOT_SIZE - 2, 0x60000000);
            g.renderItem(input, textX + 1, textY + 1);
            g.drawString(mc.font, "§7 ➜ ", textX + SLOT_SIZE + 2, textY + 4, TEXT_GRAY, true);
            int rx = textX + SLOT_SIZE + 22;
            g.fill(rx, textY, rx + SLOT_SIZE - 2, textY + SLOT_SIZE - 2, 0x60000000);
            g.renderItem(entry.result, rx + 1, textY + 1);
            if (entry.experience > 0) {
                g.drawString(mc.font, String.format("§6+%.1f 经验", entry.experience),
                        rx + SLOT_SIZE + 4, textY + 4, TEXT_GRAY, true);
            }
        }
    }

    private static int calculateHeight(RecipeEntry entry) {
        int contentH = (entry.type == RecipeType.CRAFTING)
                ? entry.gridSize * SLOT_SIZE + 8
                : LINE_HEIGHT + SLOT_SIZE + 12;
        return LINE_HEIGHT + 5 + contentH + PADDING * 2;
    }

    // ==================== 配方查找 ====================
    private static List<RecipeEntry> findRecipes(RecipeManager manager, ItemStack item, net.minecraft.world.level.Level level) {
        List<RecipeEntry> results = new ArrayList<>();
        ContextMap ctx = makeContext(level);
        for (RecipeHolder<?> holder : manager.getRecipes()) {
            RecipeEntry entry = toEntry(holder.value(), ctx);
            if (entry != null && !entry.result.isEmpty() && entry.result.is(item.getItem())) {
                results.add(entry);
            }
        }
        return results;
    }

    private static RecipeEntry toEntry(Recipe<?> recipe, ContextMap ctx) {
        List<RecipeDisplay> displays = recipe.display();
        if (displays.isEmpty()) return null;

        RecipeDisplay display = displays.get(0);
        RecipeEntry entry = new RecipeEntry();

        // 获取结果物品
        entry.result = display.result().resolveForFirstStack(ctx);
        if (entry.result.isEmpty()) return null;

        if (display instanceof ShapedCraftingRecipeDisplay shaped) {
            entry.type = RecipeType.CRAFTING;
            entry.gridSize = Math.max(shaped.width(), shaped.height());
            List<SlotDisplay> ings = shaped.ingredients();
            for (int r = 0; r < entry.gridSize; r++) {
                for (int c = 0; c < entry.gridSize; c++) {
                    if (r < shaped.height() && c < shaped.width()) {
                        int idx = r * shaped.width() + c;
                        entry.ingredients.add(idx < ings.size()
                                ? ings.get(idx).resolveForFirstStack(ctx) : ItemStack.EMPTY);
                    } else {
                        entry.ingredients.add(ItemStack.EMPTY);
                    }
                }
            }
            return entry;
        } else if (display instanceof ShapelessCraftingRecipeDisplay shapeless) {
            entry.type = RecipeType.CRAFTING;
            entry.gridSize = 3;
            List<SlotDisplay> ings = shapeless.ingredients();
            for (int i = 0; i < 9; i++) {
                entry.ingredients.add(i < ings.size()
                        ? ings.get(i).resolveForFirstStack(ctx) : ItemStack.EMPTY);
            }
            return entry;
        } else if (recipe instanceof AbstractCookingRecipe cooking) {
            entry.type = RecipeType.SMELTING;
            entry.experience = cooking.experience();
            entry.smeltingType = (recipe instanceof BlastingRecipe) ? "blast"
                    : (recipe instanceof SmokingRecipe) ? "smoke" : "furnace";
            var ings = cooking.placementInfo().ingredients();
            if (!ings.isEmpty()) {
                ings.get(0).items().findFirst()
                        .map(h -> new ItemStack(h))
                        .ifPresent(s -> entry.ingredients.add(s));
            }
            if (entry.ingredients.isEmpty()) entry.ingredients.add(ItemStack.EMPTY);
            return entry;
        }
        return null;
    }

    // ==================== 数据结构 ====================
    private enum RecipeType { CRAFTING, SMELTING }

    private static class RecipeEntry {
        RecipeType type = RecipeType.CRAFTING;
        int gridSize = 3;
        ItemStack result = ItemStack.EMPTY;
        List<ItemStack> ingredients = new ArrayList<>();
        float experience = 0f;
        String smeltingType = "furnace";
    }

    private static void drawBorder(GuiGraphics g, int x, int y, int width, int height) {
        g.fill(x, y, x + width, y + 1, BORDER_OUTER);
        g.fill(x, y + height - 1, x + width, y + height, BORDER_OUTER);
        g.fill(x, y, x + 1, y + height, BORDER_OUTER);
        g.fill(x + width - 1, y, x + width, y + height, BORDER_OUTER);
    }
}
