package io.wispforest.testccessories.neoforge.client;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.menu.AccessoriesBasedSlot;
import io.wispforest.accessories.client.GuiGraphicsUtils;
import io.wispforest.testccessories.neoforge.TestMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.Optional;

public class TestScreen extends AbstractContainerScreen<TestMenu> implements MenuAccess<TestMenu> {

    private static final ResourceLocation SLOT_FRAME = Accessories.of("textures/gui/slot.png");

    private static final ResourceLocation BACKGROUND_PATCH = Accessories.of("background_patch");

    public TestScreen(TestMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        Integer minX = null, minY = null, maxX = null, maxY = null;

        for (Slot slot : this.menu.slots) {
            if(minX == null || slot.x < minX){
                minX = slot.x;
            } else if(maxX == null || slot.x > maxX){
                maxX = slot.x;
            }

            if(minY == null || slot.y < minY){
                minY = slot.y;
            } else if(maxY == null || slot.y > maxY){
                maxY = slot.y;
            }
        }

        var padding = 4;

        var width = (maxX - minX) + 18 + (padding * 2);
        var height = (maxY - minY) + 18 + (padding * 2);

        var startX = minX - padding;
        var startY = minY - padding;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate((float)leftPos, (float)topPos, 0.0F);

        GuiGraphicsUtils.blitSpriteBatched(guiGraphics, BACKGROUND_PATCH, startX - 1, startY - 1, width + 1, height + 1);

        for (Slot slot : this.menu.slots) {
            guiGraphics.blit(SLOT_FRAME, slot.x - 1, slot.y - 1, 0, 0, 18, 18, 18, 18);
        }

        guiGraphics.pose().popPose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (this.hoveredSlot instanceof AccessoriesBasedSlot slot && slot.getItem().isEmpty() && slot.accessoriesContainer.slotType() != null) {
            var tooltipData = slot.getTooltipData();

            guiGraphics.renderTooltip(Minecraft.getInstance().font, tooltipData, Optional.empty(), mouseX, mouseY);

            return;
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
