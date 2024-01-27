package io.wispforest.accessories.client.gui;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.client.AccessoriesMenu;
import io.wispforest.accessories.impl.ExpandedSimpleContainer;
import io.wispforest.accessories.mixin.ScreenAccessor;
import io.wispforest.accessories.pond.ContainerScreenExtension;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AccessoriesScreen extends EffectRenderingInventoryScreen<AccessoriesMenu> implements ContainerScreenExtension {

    public static final ResourceLocation SLOT_FRAME = Accessories.of("textures/gui/slot.png");

    protected static final ResourceLocation ACCESSORIES_PANEL_LOCATION = Accessories.of("textures/gui/accessories_panel.png");

    protected static final ResourceLocation BACKGROUND_PATCH = Accessories.of("background_patch");
    protected static final ResourceLocation SCROLL_BAR_PATCH = Accessories.of("scroll_bar_patch");

    private List<Renderable> cosmeticButtons = new ArrayList<>();

    private float xMouse;
    private float yMouse;

    public AccessoriesScreen(AccessoriesMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);

        ((ScreenAccessor) this).accessories$setTitle(component);

        //this.titleLabelX = 42069;
        this.inventoryLabelX = 42069;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        var bl = super.mouseClicked(mouseX, mouseY, button);

        if(this.getFocused() instanceof Button) this.clearFocus();

        return bl;
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
        int x = getStartingPanelX();
        int y = this.topPos;

        int upperPadding = 8;

        var height = getPanelHeight(upperPadding);

        int width = getPanelWidth();

        guiGraphics.blitSprite(AccessoriesScreen.BACKGROUND_PATCH, x + 6, y, width, height); //147

        if(menu.overMaxVisibleSlots) {
            guiGraphics.blitSprite(AccessoriesScreen.SCROLL_BAR_PATCH, x + 13, y + 7 + upperPadding, 8, height - 22);
        }

        guiGraphics.pose().popPose();

        for (Slot slot : this.menu.slots) {
            if(!(slot.container instanceof ExpandedSimpleContainer) || !slot.isActive()) continue;

            var pose = guiGraphics.pose();

            pose.pushPose();

            pose.translate(-1, -1, 0);

            guiGraphics.blit(SLOT_FRAME, slot.x + this.leftPos, slot.y + this.topPos, 0, 0, 18, 18, 18, 18);

            //InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, slot.x + this.leftPos, slot.y + this.topPos, slot.x + this.leftPos + 18, slot.y + this.topPos + 18, 8, 0.0625F, this.xMouse, this.yMouse, this.minecraft.player);

            pose.popPose();
        }
    }

    private int getPanelHeight(int upperPadding){
        return 14 + (menu.totalSlots * 18) + upperPadding;
    }

    private int getPanelWidth(){
        int width = 8;

        if(menu.isCosmeticsOpen()){
            width += (18 * 2) + (2 + 8 + 8 + 2);
        } else {
            width += 18 + (2 + 8 + 8);
        }

        if(!menu.overMaxVisibleSlots) {
            width -= 12;
        }

        return width;
    }

    private int getStartingPanelX(){
        int x = this.leftPos;

        if(menu.isCosmeticsOpen()){
            x -= 72;
        } else {
            x -= 52;
        }

        if(!menu.overMaxVisibleSlots) {
            x += 12;
        }

        return x;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    protected boolean insideScrollbar(double mouseX, double mouseY) {
        int upperPadding = 8;

        int x = getStartingPanelX() + 13;
        int y = this.topPos + 7 + upperPadding;

        int height = getPanelHeight(upperPadding) - 22;
        int width = 8;

        return mouseX >= x
                && mouseY >= y
                && mouseX < (x + width)
                && mouseY < (y + height);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        //this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        for (Renderable cosmeticButton : this.cosmeticButtons) {
            cosmeticButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }

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

        cosmeticButtons.clear();

        var button = Button.builder(Component.empty(), (btn) -> {
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);

                    btn.setTooltip(cosmeticsToggleTooltip(this.menu.isCosmeticsOpen()));
                })
                .tooltip(cosmeticsToggleTooltip(this.menu.isCosmeticsOpen()))
                .bounds(this.leftPos - 27, this.topPos + 7, 18, 6)
                .build();

        this.addRenderableWidget(button);

        for (Slot slot : menu.slots) {
            if(!(slot instanceof AccessoriesSlot accessoriesSlot && !accessoriesSlot.isCosmetic)) continue;

            var slotButton = ToggleButton.toggleBuilder(Component.empty(), btn -> {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, slot.index);
                    })
                    .onRender(btn -> {
                        var bl = accessoriesSlot.container.shouldRender(accessoriesSlot.getContainerSlot());

                        if(bl != btn.toggled()){
                            btn.toggled(bl);
                            btn.setTooltip(toggleTooltip(bl));
                        }
                    })
                    .tooltip(toggleTooltip(accessoriesSlot.container.shouldRender(accessoriesSlot.getContainerSlot())))
                    .zIndex(300)
                    .bounds(slot.x + this.leftPos + 13, slot.y + this.topPos - 2, 5, 5)
                    .build()
                    .toggled(accessoriesSlot.container.shouldRender(accessoriesSlot.getContainerSlot()));

            cosmeticButtons.add(this.addWidget(slotButton));
        }
    }

    private Tooltip cosmeticsToggleTooltip(boolean value){
        var key = "slot.cosmetics.toggle." + (!value ? "shown" : "hidden");

        return Tooltip.create(Component.translatable(Accessories.translation(key)));
    }

    private Tooltip toggleTooltip(boolean value){
        var key = "slot.display.toggle." + (!value ? "shown" : "hidden");

        return Tooltip.create(Component.translatable(Accessories.translation(key)));
    }

    @Override
    public @Nullable Boolean isHovering(Slot slot, double mouseX, double mouseY) {
        for (GuiEventListener child : this.children()) {
            if(child instanceof ToggleButton toggleButton && toggleButton.isMouseOver(mouseX, mouseY)){
                return false;
            }
        }

        return ContainerScreenExtension.super.isHovering(slot, mouseX, mouseY);
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

    /**
     * From <a href="https://github.com/wisp-forest/owo-lib/blob/d47a040240eab222362e1fa096575a816291cc2f/src/main/java/io/wispforest/owo/ui/core/PositionedRectangle.java#L37">PositionedRectangle.java</a>
     */
    private static boolean isIntersecting(int x1, int y1, int width1, int height1, int x2, int y2, int width2, int height2) {
        return x2 < x1 + width1 && x2 + width2 >= x1 && y2 < y1 + height1 && y2 + height2 >= y1;
    }
}