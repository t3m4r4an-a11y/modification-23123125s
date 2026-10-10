package net.macos.client.render;

import net.macos.client.config.ConfigManager;
import net.macos.client.gui.font.AetherionFont;
import net.macos.client.hud.glass.GlassRenderer;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.DyeColor;

import java.util.ArrayList;
import java.util.List;

/**
 * 2026 Luxury Liquid Glass Shulker & Container Preview.
 * Renders anti-aliased squircle item capsules, dye-themed neon accents, and smooth drop shadows.
 */
public final class ShulkerPreviewRenderer {

    private ShulkerPreviewRenderer() {}

    public static boolean isShulkerBox(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.getItem() instanceof BlockItem bi) {
            return bi.getBlock() instanceof ShulkerBoxBlock;
        }
        return false;
    }

    public static int getShulkerThemeColor(ItemStack stack) {
        if (stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof ShulkerBoxBlock sb) {
            DyeColor dye = sb.getColor();
            if (dye != null) {
                return dye.getFireworkColor();
            }
        }
        return 0xA055E8; // Default vibrant shulker purple
    }

    public static List<ItemStack> getItems(ItemStack shulker) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < 27; i++) items.add(ItemStack.EMPTY);

        NbtCompound root = shulker.getNbt();
        if (root != null && root.contains("BlockEntityTag", NbtElement.COMPOUND_TYPE)) {
            NbtCompound tag = root.getCompound("BlockEntityTag");
            if (tag.contains("Items", NbtElement.LIST_TYPE)) {
                NbtList list = tag.getList("Items", NbtElement.COMPOUND_TYPE);
                for (int i = 0; i < list.size(); i++) {
                    NbtCompound itemTag = list.getCompound(i);
                    int slot = itemTag.getByte("Slot") & 0xFF;
                    if (slot < 27) {
                        items.set(slot, ItemStack.fromNbt(itemTag));
                    }
                }
            }
        }
        return items;
    }

    public static void renderPreview(DrawContext ctx, ItemStack stack, int mouseX, int mouseY) {
        if (!ConfigManager.INSTANCE.enableShulkerPreview || !isShulkerBox(stack)) return;

        List<ItemStack> items = getItems(stack);
        boolean hasItems = items.stream().anyMatch(it -> !it.isEmpty());
        if (!hasItems) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;

        int cols = 9;
        int rows = 3;
        int slotSize = 19;
        int pad = 8;
        int headerH = 18;

        int previewW = cols * slotSize + pad * 2 - 1;
        int previewH = rows * slotSize + pad * 2 + headerH;

        // Position above or next to cursor
        int x = mouseX + 12;
        int y = mouseY - previewH - 6;
        if (y < 6) y = mouseY + 18;
        if (x + previewW > mc.getWindow().getScaledWidth()) {
            x = mouseX - previewW - 6;
        }

        int themeColor = getShulkerThemeColor(stack);

        ctx.getMatrices().push();
        ctx.getMatrices().translate(0, 0, 400); // Above screen items

        // Deep drop shadow & Obsidian Liquid Glass backing
        GlassRenderer.dropShadow(ctx, x, y, previewW, previewH, 14, 10, 0x80);
        SquircleRenderer.fill(ctx, x, y, previewW, previewH, 10, 0xD8080C16);
        SquircleRenderer.border(ctx, x, y, previewW, previewH, 10, 1.0f, (0x30 << 24) | themeColor);
        GlassRenderer.specular(ctx, x, y, previewW, previewH, 10, 0x22FFFFFF);

        // Header Accent Pip & Title
        int pipW = 4;
        SquircleRenderer.pill(ctx, x + pad, y + 6, pipW, 9, (0xEA << 24) | themeColor);
        AetherionFont.draw(ctx, stack.getName().getString(), x + pad + pipW + 6, y + 6, 0xF5F5F7);

        // Slots grid
        int startX = x + pad;
        int startY = y + pad + headerH;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int slotX = startX + c * slotSize;
                int slotY = startY + r * slotSize;
                int idx = r * cols + c;

                // Squircle slot tile
                SquircleRenderer.fill(ctx, slotX, slotY, 17, 17, 4, 0x1EFFFFFF);
                SquircleRenderer.border(ctx, slotX, slotY, 17, 17, 4, 1.0f, 0x14FFFFFF);

                ItemStack item = items.get(idx);
                if (!item.isEmpty()) {
                    ctx.drawItem(item, slotX + 1, slotY + 1);
                    ctx.drawItemInSlot(mc.textRenderer, item, slotX + 1, slotY + 1);
                }
            }
        }

        ctx.draw();
        ctx.getMatrices().pop();
    }
}
