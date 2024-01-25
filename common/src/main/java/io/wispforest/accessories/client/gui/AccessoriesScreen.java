package io.wispforest.accessories.client.gui;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.client.AccessoriesMenu;
import io.wispforest.accessories.impl.ExpandedSimpleContainer;
import io.wispforest.accessories.mixin.ScreenAccessor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class AccessoriesScreen extends EffectRenderingInventoryScreen<AccessoriesMenu> {

    public static final ResourceLocation SLOT_FRAME = Accessories.of("textures/gui/slot.png");

    private float xMouse;
    private float yMouse;
    private boolean widthTooNarrow;

    protected AccessoriesViewComponent viewComponent = new AccessoriesViewComponent();

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
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {

        this.viewComponent.render(guiGraphics, mouseX, mouseY, partialTick);

        for (Slot slot : this.menu.slots) {
            if(!(slot.container instanceof ExpandedSimpleContainer)) continue;

            var pose = guiGraphics.pose();

            pose.pushPose();

            pose.translate(-1, -1, 100);

            guiGraphics.blit(SLOT_FRAME, slot.x + this.leftPos, slot.y + this.topPos, 0, 0, 18, 18, 18, 18);

            pose.popPose();
        }

        if (this.widthTooNarrow) {
            this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        } else {
            super.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        this.xMouse = (float)mouseX;
        this.yMouse = (float)mouseY;
    }

    @Override
    protected void init() {
        super.init();
        this.widthTooNarrow = this.width < 379;
        this.viewComponent.init(this.width, this.height, this.minecraft, this.widthTooNarrow, this.menu);
        this.leftPos = this.viewComponent.updateScreenPosition(this.width, this.imageWidth);
//            this.addRenderableWidget(new ImageButton(this.leftPos + 104, this.height / 2 - 22, 20, 18, RecipeBookComponent.RECIPE_BUTTON_SPRITES, button -> {
//                //this.viewComponent.toggleVisibility();
//                this.leftPos = this.viewComponent.updateScreenPosition(this.width, this.imageWidth);
//                button.setPosition(this.leftPos + 104, this.height / 2 - 22);
//                //this.buttonClicked = true;
//            }));
        this.addWidget(this.viewComponent);
        this.setInitialFocus(this.viewComponent);
    }
}
