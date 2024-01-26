package io.wispforest.accessories.client.gui;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.client.AccessoriesMenu;
import io.wispforest.accessories.impl.ExpandedSimpleContainer;
import io.wispforest.accessories.mixin.ScreenAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AccessoriesScreen extends EffectRenderingInventoryScreen<AccessoriesMenu> {

    public static final ResourceLocation SLOT_FRAME = Accessories.of("textures/gui/slot.png");

    protected static final ResourceLocation ACCESSORIES_PANEL_LOCATION = Accessories.of("textures/gui/accessories_panel.png");

    protected static final ResourceLocation BACKGROUND_PATCH = Accessories.of("background_patch");
    protected static final ResourceLocation SCROLL_BAR_PATCH = Accessories.of("scroll_bar_patch");

    private float xMouse;
    private float yMouse;

    public AccessoriesScreen(AccessoriesMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);

        ((ScreenAccessor) this).accessories$setTitle(component);

        //this.titleLabelX = 42069;
        this.inventoryLabelX = 42069;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int i = this.leftPos;
        int j = this.topPos;
        guiGraphics.blit(INVENTORY_LOCATION, i, j, 0, 0, this.imageWidth, this.imageHeight);
        InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, i + 26, j + 8, i + 75, j + 78, 30, 0.0625F, this.xMouse, this.yMouse, this.minecraft.player);

        //if (!this.isVisible()) return;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 0);
        int x = this.leftPos - 72;
        int y = this.topPos;

        int upperPadding = 8;

        guiGraphics.blitSprite(AccessoriesScreen.BACKGROUND_PATCH, x + 6/*+83*/, y, 64, 158 + upperPadding); //147
        guiGraphics.blitSprite(AccessoriesScreen.SCROLL_BAR_PATCH, x + 13/* + 90*/, y + 7 + upperPadding, 8, 144);

        guiGraphics.pose().popPose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        for (Slot slot : this.menu.slots) {
            if(!(slot.container instanceof ExpandedSimpleContainer)) continue;

            var pose = guiGraphics.pose();

            pose.pushPose();

            pose.translate(-1, -1, 100);

            guiGraphics.blit(SLOT_FRAME, slot.x + this.leftPos, slot.y + this.topPos, 0, 0, 18, 18, 18, 18);

            //InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, slot.x + this.leftPos, slot.y + this.topPos, slot.x + this.leftPos + 18, slot.y + this.topPos + 18, 8, 0.0625F, this.xMouse, this.yMouse, this.minecraft.player);

            pose.popPose();
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        this.xMouse = (float)mouseX;
        this.yMouse = (float)mouseY;

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean handleComponentClicked(@Nullable Style style) {
        return super.handleComponentClicked(style);
    }

    @Override
    protected void init() {
        super.init();

        var button = Button.builder(Component.empty(), (btn) -> {})
                .bounds(this.leftPos - 59, this.topPos + 7, 8, 6)
                .build();

        this.addRenderableWidget(button);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        if(this.hoveredSlot instanceof AccessoriesSlot accessoriesSlot && accessoriesSlot.getItem().isEmpty()){
            var slotType = accessoriesSlot.container.slotType();

            if(slotType.isPresent()){
                List<Component> tooltipData = new ArrayList<>();

                var key = accessoriesSlot.isCosmetic ? "cosmetic_" : "";

                tooltipData.add(
                        Component.translatable(Accessories.translation(key + "slot.tooltip.singular")).withStyle(ChatFormatting.GRAY)
                                .append(Component.translatable(slotType.get().translation()).withStyle(ChatFormatting.BLUE))
                );

                guiGraphics.renderTooltip(Minecraft.getInstance().font, tooltipData, Optional.empty(), x, y);

                return;
            }
        }

        super.renderTooltip(guiGraphics, x, y);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int x, int y, int mouseButton) {
        boolean flag = mouseX < (double)x || mouseY < (double)y || mouseX >= (double)(x + width) || mouseY >= (double)(y + height);
        boolean flag1 = (double)(x - 147) < mouseX && mouseX < (double)x && (double)y < mouseY && mouseY < (double)(y + height);
        return flag && !flag1;
    }
}