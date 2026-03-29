package com.ksw.mchelper.render;

import com.ksw.mchelper.config.MCHelperConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 合成查询覆盖层（R 键切换）
 *
 * 手持物品时显示该物品的合成配方，包括：
 * - 合成表配方（3x3 / 2x2）
 * - 烧炼配方（熔炉 / 高炉 / 烟熏炉）
 * 若有多个配方，显示第一个，并标注总数量。
 */
public class CraftingOverlay {

    // ==================== 颜色常量 ====================
    private static final int BG_TOP      = 0xCC1A1A2E;
    private static final int BG_BOTTOM   = 0xCC16213E;
    private static final int BORDER_OUTER = 0xFF0F3460;
    private static final int BORDER_INNER = 0xFF533483;
    private static final int SEPARATOR   = 0x40FFFFFF;
    private static final int TEXT_TITLE  = 0xFF00D2FF;
    private static final int TEXT_WHITE  = 0xFFFFFFFF;
    private static final int TEXT_GRAY   = 0xFFAABBCC;
    private static final int TEXT_DARK   = 0xFF778899;

    // ==================== 布局常量 ====================
    private static final float SCALE      = 0.4f;
    private static final int PADDING      = 8;
    private static final int LINE_HEIGHT  = 13;
    private static final int SLOT_SIZE    = 18;   // 配方格子大小（含间距）
    private static final int BOX_WIDTH    = 260;

    public static final IGuiOverlay HUD_CRAFTING = (ForgeGui gui, GuiGraphics guiGraphics,
                                                     float partialTick, int screenWidth, int screenHeight) -> {
        if (!MCHelperConfig.showCrafting) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.options.renderDebug) return;

        // 获取手持物品
        ItemStack heldItem = mc.player.getMainHandItem();
        if (heldItem.isEmpty()) {
            // 手持为空时尝试副手
            heldItem = mc.player.getOffhandItem();
        }
        if (heldItem.isEmpty()) return;

        // 查找合成配方
        RecipeManager recipeManager = mc.level.getRecipeManager();
        List<RecipeEntry> entries = findRecipes(recipeManager, heldItem);
        if (entries.isEmpty()) return;

        // 应用缩放
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(SCALE, SCALE, 1.0f);

        // 居中显示（屏幕下方 1/4 处）
        int boxX = (int) ((screenWidth - BOX_WIDTH * SCALE) / 2 / SCALE);
        int boxY = (int) ((screenHeight * 0.65f) / SCALE);

        RecipeEntry entry = entries.get(0);
        int totalHeight = renderRecipeBox(guiGraphics, mc, entry, boxX, boxY,
                heldItem, entries.size());

        guiGraphics.pose().popPose();
    };

    // ==================== 渲染配方面板 ====================
    private static int renderRecipeBox(GuiGraphics g, Minecraft mc,
                                        RecipeEntry entry, int x, int y,
                                        ItemStack resultItem, int totalCount) {
        int boxHeight = calculateHeight(entry);

        // 背景 + 边框
        g.fillGradient(x, y, x + BOX_WIDTH, y + boxHeight, BG_TOP, BG_BOTTOM);
        drawBorder(g, x, y, BOX_WIDTH, boxHeight);
        g.fill(x + 1, y + 1, x + BOX_WIDTH - 1, y + 2, BORDER_INNER);

        int textX = x + PADDING;
        int textY = y + PADDING;

        // 标题
        String titleSuffix = totalCount > 1 ? " §7(" + totalCount + "种配方)" : "";
        g.drawString(mc.font, "§b🔨 合成配方" + titleSuffix, textX, textY, TEXT_TITLE, true);
        textY += LINE_HEIGHT;
        g.fill(x + 4, textY, x + BOX_WIDTH - 4, textY + 1, SEPARATOR);
        textY += 5;

        if (entry.type == RecipeType.CRAFTING) {
            renderCraftingGrid(g, mc, entry, textX, textY, x, boxHeight);
        } else {
            renderSmeltingRecipe(g, mc, entry, textX, textY);
        }

        return boxHeight;
    }

    // 渲染合成表（3x3 格子）
    private static void renderCraftingGrid(GuiGraphics g, Minecraft mc,
                                            RecipeEntry entry, int textX, int textY,
                                            int boxX, int boxHeight) {
        int gridSize = entry.gridSize; // 2 或 3
        int gridStartX = textX;
        int gridStartY = textY;

        // 绘制配方格子
        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                int idx = row * gridSize + col;
                int px = gridStartX + col * SLOT_SIZE;
                int py = gridStartY + row * SLOT_SIZE;

                // 格子背景
                g.fill(px, py, px + SLOT_SIZE - 2, py + SLOT_SIZE - 2, 0x60000000);
                g.fill(px, py, px + SLOT_SIZE - 2, py + 1, 0xFF404060);
                g.fill(px, py, px + 1, py + SLOT_SIZE - 2, 0xFF404060);

                if (idx < entry.ingredients.size()) {
                    ItemStack ingredient = entry.ingredients.get(idx);
                    if (!ingredient.isEmpty()) {
                        g.renderItem(ingredient, px + 1, py + 1);
                    }
                }
            }
        }

        // 箭头 →
        int arrowX = gridStartX + gridSize * SLOT_SIZE + 4;
        int arrowY = gridStartY + (gridSize * SLOT_SIZE) / 2 - 4;
        g.drawString(mc.font, "§7➜", arrowX, arrowY, TEXT_GRAY, true);

        // 结果物品
        int resultX = arrowX + 16;
        int resultY = gridStartY + (gridSize * SLOT_SIZE) / 2 - 8;
        g.fill(resultX, resultY, resultX + SLOT_SIZE - 2, resultY + SLOT_SIZE - 2, 0x60000000);
        g.renderItem(entry.result, resultX + 1, resultY + 1);
        if (entry.result.getCount() > 1) {
            g.renderItemDecorations(mc.font, entry.result, resultX + 1, resultY + 1);
        }
    }

    // 渲染烧炼配方（线性显示）
    private static void renderSmeltingRecipe(GuiGraphics g, Minecraft mc,
                                              RecipeEntry entry, int textX, int textY) {
        // 类型标签
        String typeLabel = switch (entry.smeltingType) {
            case "blast" -> "§7高炉烧炼";
            case "smoke" -> "§7烟熏炉";
            default -> "§7熔炉烧炼";
        };
        g.drawString(mc.font, typeLabel, textX, textY, TEXT_GRAY, true);
        textY += LINE_HEIGHT + 2;

        if (!entry.ingredients.isEmpty()) {
            ItemStack input = entry.ingredients.get(0);
            // 输入物品
            g.fill(textX, textY, textX + SLOT_SIZE - 2, textY + SLOT_SIZE - 2, 0x60000000);
            g.renderItem(input, textX + 1, textY + 1);

            // 箭头
            g.drawString(mc.font, "§7 ➜ ", textX + SLOT_SIZE + 2, textY + 4, TEXT_GRAY, true);

            // 输出物品
            int resultX = textX + SLOT_SIZE + 22;
            g.fill(resultX, textY, resultX + SLOT_SIZE - 2, textY + SLOT_SIZE - 2, 0x60000000);
            g.renderItem(entry.result, resultX + 1, textY + 1);

            // 经验值
            if (entry.experience > 0) {
                g.drawString(mc.font,
                        String.format("§6+%.1f 经验", entry.experience),
                        resultX + SLOT_SIZE + 4, textY + 4, TEXT_GRAY, true);
            }
        }
    }

    private static int calculateHeight(RecipeEntry entry) {
        int contentHeight;
        if (entry.type == RecipeType.CRAFTING) {
            contentHeight = entry.gridSize * SLOT_SIZE + 8;
        } else {
            contentHeight = LINE_HEIGHT + SLOT_SIZE + 12;
        }
        return LINE_HEIGHT + 5 + contentHeight + PADDING * 2;
    }

    // ==================== 配方查找 ====================
    private static List<RecipeEntry> findRecipes(RecipeManager manager, ItemStack item) {
        List<RecipeEntry> results = new ArrayList<>();
        for (net.minecraft.world.item.crafting.Recipe<?> recipe : manager.getRecipes()) {
            ItemStack result;
            try {
                result = recipe.getResultItem(net.minecraft.core.RegistryAccess.EMPTY);
            } catch (Exception e) {
                continue;
            }
            if (!result.isEmpty() && result.is(item.getItem())) {
                RecipeEntry entry = toEntry(recipe);
                if (entry != null) results.add(entry);
            }
        }
        return results;
    }

    private static RecipeEntry toEntry(net.minecraft.world.item.crafting.Recipe<?> recipe) {
        if (recipe instanceof ShapedRecipe shaped) {
            RecipeEntry entry = new RecipeEntry();
            entry.type = RecipeType.CRAFTING;
            entry.gridSize = Math.max(shaped.getWidth(), shaped.getHeight());
            entry.result = shaped.getResultItem(net.minecraft.core.RegistryAccess.EMPTY);
            entry.ingredients = new ArrayList<>();
            for (int r = 0; r < entry.gridSize; r++) {
                for (int c = 0; c < entry.gridSize; c++) {
                    if (r < shaped.getHeight() && c < shaped.getWidth()) {
                        int idx = r * shaped.getWidth() + c;
                        ItemStack[] stacks = shaped.getIngredients().get(idx).getItems();
                        entry.ingredients.add(stacks.length > 0 ? stacks[0] : ItemStack.EMPTY);
                    } else {
                        entry.ingredients.add(ItemStack.EMPTY);
                    }
                }
            }
            return entry;
        } else if (recipe instanceof ShapelessRecipe shapeless) {
            RecipeEntry entry = new RecipeEntry();
            entry.type = RecipeType.CRAFTING;
            entry.gridSize = 3;
            entry.result = shapeless.getResultItem(net.minecraft.core.RegistryAccess.EMPTY);
            entry.ingredients = new ArrayList<>();
            for (int i = 0; i < 9; i++) {
                if (i < shapeless.getIngredients().size()) {
                    ItemStack[] stacks = shapeless.getIngredients().get(i).getItems();
                    entry.ingredients.add(stacks.length > 0 ? stacks[0] : ItemStack.EMPTY);
                } else {
                    entry.ingredients.add(ItemStack.EMPTY);
                }
            }
            return entry;
        } else if (recipe instanceof AbstractCookingRecipe cooking) {
            RecipeEntry entry = new RecipeEntry();
            entry.type = RecipeType.SMELTING;
            entry.result = cooking.getResultItem(net.minecraft.core.RegistryAccess.EMPTY);
            entry.experience = cooking.getExperience();
            ItemStack[] stacks = cooking.getIngredients().get(0).getItems();
            entry.ingredients = new ArrayList<>();
            entry.ingredients.add(stacks.length > 0 ? stacks[0] : ItemStack.EMPTY);
            entry.smeltingType = (recipe instanceof BlastingRecipe) ? "blast"
                    : (recipe instanceof SmokingRecipe) ? "smoke" : "furnace";
            return entry;
        }
        return null;
    }

    // ==================== 数据结构 ====================
    private enum RecipeType { CRAFTING, SMELTING }

    private static class RecipeEntry {
        RecipeType type;
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
