package com.ksw.mchelper.render;

import com.ksw.mchelper.config.MCHelperConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 合成查询覆盖层（R 键切换）— 1.21.1 版本
 * 使用 RecipeHolder + getResultItem/getIngredients（无 RecipeDisplay API）
 * 仅支持单人模式
 */
public class CraftingOverlay {

    private static final int SLOT_SIZE    = 18;
    private static final int BOX_WIDTH    = 260;
    private static final int BOX_PADDING  = 8;
    private static final int ARROW_W      = 20;

    // ==================== 内部配方数据结构 ====================
    private static class RecipeEntry {
        ItemStack result;
        List<ItemStack> ingredients; // 最多 9 个（3x3）
        boolean isShaped;
        int width, height;          // 有序合成时的宽高
        boolean isSmelting;
        float smeltExp;
        String type;                // "crafting_shaped" / "crafting_shapeless" / "smelting" 等

        RecipeEntry(ItemStack result, List<ItemStack> ingredients,
                    boolean isShaped, int w, int h, boolean isSmelting, float exp, String type) {
            this.result = result;
            this.ingredients = ingredients;
            this.isShaped = isShaped;
            this.width = w;
            this.height = h;
            this.isSmelting = isSmelting;
            this.smeltExp = exp;
            this.type = type;
        }
    }

    // ==================== HUD 渲染层 ====================
    public static final LayeredDraw.Layer LAYER = (guiGraphics, deltaTracker) -> {
        if (!MCHelperConfig.showCrafting) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.getDebugOverlay().showDebugScreen()) return;

        ItemStack heldItem = mc.player.getMainHandItem();
        if (heldItem.isEmpty()) heldItem = mc.player.getOffhandItem();
        if (heldItem.isEmpty()) return;

        var server = mc.getSingleplayerServer();
        if (server == null) return;
        RecipeManager recipeManager = server.getRecipeManager();

        List<RecipeEntry> entries = findRecipes(recipeManager, heldItem, server.registryAccess());
        if (entries.isEmpty()) return;

        RecipeEntry entry = entries.get(0);
        int total = entries.size();

        int screenW = guiGraphics.guiWidth();
        int screenH = guiGraphics.guiHeight();

        // 计算 BOX_HEIGHT
        int rows = entry.isSmelting ? 1 : (entry.isShaped ? entry.height : (int)Math.ceil(entry.ingredients.size() / 3.0));
        int boxHeight = BOX_PADDING * 2 + 16 + 4 + Math.max(rows, 1) * SLOT_SIZE + (entry.isSmelting ? 16 : 0);

        int boxX = screenW / 2 - BOX_WIDTH / 2;
        int boxY = screenH - boxHeight - 50;

        // 背景
        guiGraphics.fill(boxX, boxY, boxX + BOX_WIDTH, boxY + boxHeight, 0xCC101828);
        guiGraphics.fill(boxX, boxY, boxX + BOX_WIDTH, boxY + 1, 0xFF1E3A5F);
        guiGraphics.fill(boxX, boxY + boxHeight - 1, boxX + BOX_WIDTH, boxY + boxHeight, 0xFF1E3A5F);
        guiGraphics.fill(boxX, boxY, boxX + 1, boxY + boxHeight, 0xFF1E3A5F);
        guiGraphics.fill(boxX + BOX_WIDTH - 1, boxY, boxX + BOX_WIDTH, boxY + boxHeight, 0xFF1E3A5F);

        // 标题
        String typeLabel = entry.isSmelting ? "§b⚙ 烧炼配方" : (entry.isShaped ? "§b⊞ 有序合成" : "§b⊟ 无序合成");
        if (total > 1) typeLabel += " §7(" + total + " 种)";
        guiGraphics.drawString(mc.font, typeLabel, boxX + BOX_PADDING, boxY + BOX_PADDING, 0xFFFFFFFF, false);

        int contentY = boxY + BOX_PADDING + 16 + 4;

        if (entry.isSmelting) {
            // 烧炼：一个原材料 → 箭头 → 结果
            int startX = boxX + BOX_PADDING;
            renderSlot(guiGraphics, mc, entry.ingredients.get(0), startX, contentY);
            guiGraphics.drawString(mc.font, "§7→", startX + SLOT_SIZE + 4, contentY + 4, 0xFFAAAAAA, false);
            renderSlot(guiGraphics, mc, entry.result, startX + SLOT_SIZE + ARROW_W + 4, contentY);
            if (entry.smeltExp > 0) {
                guiGraphics.drawString(mc.font,
                    String.format("§6%.1f xp", entry.smeltExp),
                    startX + SLOT_SIZE * 2 + ARROW_W + 8, contentY + 4,
                    0xFFFFFFFF, false);
            }
        } else {
            // 合成：网格
            int cols = entry.isShaped ? entry.width : 3;
            int gridX = boxX + BOX_PADDING;
            for (int i = 0; i < entry.ingredients.size(); i++) {
                int col = i % cols;
                int row = i / cols;
                renderSlot(guiGraphics, mc, entry.ingredients.get(i),
                    gridX + col * SLOT_SIZE, contentY + row * SLOT_SIZE);
            }
            // 箭头 + 结果
            int arrowX = gridX + cols * SLOT_SIZE + 6;
            guiGraphics.drawString(mc.font, "§7→", arrowX, contentY + (rows * SLOT_SIZE) / 2 - 4, 0xFFAAAAAA, false);
            renderSlot(guiGraphics, mc, entry.result, arrowX + ARROW_W, contentY + (rows * SLOT_SIZE) / 2 - SLOT_SIZE / 2);
        }
    };

    // ==================== 配方查找 ====================
    @SuppressWarnings("unchecked")
    private static List<RecipeEntry> findRecipes(RecipeManager manager, ItemStack item,
            net.minecraft.core.HolderLookup.Provider registryAccess) {
        List<RecipeEntry> results = new ArrayList<>();

        for (RecipeHolder<?> holder : manager.getRecipes()) {
            Recipe<?> recipe = holder.value();
            ItemStack result = recipe.getResultItem(registryAccess);
            if (result.isEmpty() || !result.is(item.getItem())) continue;

            RecipeEntry entry = toEntry(recipe, result, registryAccess);
            if (entry != null) results.add(entry);
        }
        return results;
    }

    private static RecipeEntry toEntry(Recipe<?> recipe, ItemStack result,
            net.minecraft.core.HolderLookup.Provider registryAccess) {
        if (recipe instanceof ShapedRecipe shaped) {
            List<ItemStack> ings = new ArrayList<>();
            for (var ing : shaped.getIngredients()) {
                ItemStack[] items = ing.getItems();
                ings.add(items.length > 0 ? items[0] : ItemStack.EMPTY);
            }
            return new RecipeEntry(result, ings, true,
                shaped.getWidth(), shaped.getHeight(), false, 0f, "shaped");
        } else if (recipe instanceof ShapelessRecipe shapeless) {
            List<ItemStack> ings = new ArrayList<>();
            for (var ing : shapeless.getIngredients()) {
                ItemStack[] items = ing.getItems();
                ings.add(items.length > 0 ? items[0] : ItemStack.EMPTY);
            }
            return new RecipeEntry(result, ings, false, 3, 1, false, 0f, "shapeless");
        } else if (recipe instanceof AbstractCookingRecipe cooking) {
            List<ItemStack> ings = new ArrayList<>();
            ItemStack[] items = cooking.getIngredients().get(0).getItems();
            ings.add(items.length > 0 ? items[0] : ItemStack.EMPTY);
            return new RecipeEntry(result, ings, false, 1, 1, true, cooking.getExperience(), "smelting");
        }
        return null;
    }

    // ==================== 工具方法 ====================
    private static void renderSlot(GuiGraphics g, Minecraft mc, ItemStack stack, int x, int y) {
        g.fill(x, y, x + 16, y + 16, 0x44FFFFFF);
        if (!stack.isEmpty()) {
            g.renderItem(stack, x, y);
        }
    }
}
