/*
 * Copyright (c) 2018-2023 C4
 *
 * This file is part of Curios, a mod made for Minecraft.
 *
 * Curios is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Curios is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Curios.  If not, see <https://www.gnu.org/licenses/>.
 */

package top.theillusivec4.curios.client.gui;

import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.networking.server.ScreenOpen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.client.ICuriosScreen;
import top.theillusivec4.curios.common.inventory.container.CuriosContainerV2;

import javax.annotation.Nonnull;

public class CuriosScreenV2 extends EffectRenderingInventoryScreen<CuriosContainerV2>
        implements RecipeUpdateListener, ICuriosScreen {

    static final ResourceLocation CURIO_INVENTORY = new ResourceLocation(CuriosApi.MODID,
            "textures/gui/inventory_revamp.png");

    private final RecipeBookComponent recipeBookGui = new RecipeBookComponent();
    public boolean widthTooNarrow;

    private ImageButton recipeBookButton;
    private CuriosButton buttonCurios;
    private CosmeticButton cosmeticButton;
    private PageButton nextPage;
    private PageButton prevPage;
    private boolean buttonClicked;
    private boolean isRenderButtonHovered;
    public int panelWidth = 0;

    public CuriosScreenV2(CuriosContainerV2 curiosContainer, Inventory playerInventory, Component title) {
        super(curiosContainer, playerInventory, title);
    }

    public static Tuple<Integer, Integer> getButtonOffset(boolean isCreative) {
        int x = 0;
        int y = 0;

        // NO-OP

        return new Tuple<>(x, y);
    }

    @Override
    public void init() {
        AccessoriesInternals.getNetworkHandler().sendToServer(new ScreenOpen());
        this.onClose();
        // NO-OP
    }

    public void updateRenderButtons() {
        // NO-OP
    }

    @Override
    public void containerTick() {
        // NO-OP
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // NO-OP
    }

    @Override
    protected void renderTooltip(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // NO-OP
    }

    @Override
    public boolean keyPressed(int p_keyPressed_1_, int p_keyPressed_2_, int p_keyPressed_3_) {
        // NO-OP
        return false;
    }

    @Override
    protected void renderLabels(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // NO-OP
    }

    /**
     * Draws the background layer of this container (behind the item).
     */

    @Override
    protected void renderBg(@Nonnull GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        // NO-OP
    }

    /**
     * Test if the 2D point is in a rectangle (relative to the GUI). Args : rectX, rectY, rectWidth,
     * rectHeight, pointX, pointY
     */
    @Override
    protected boolean isHovering(int rectX, int rectY, int rectWidth, int rectHeight, double pointX, double pointY) {
        // NO-OP
        return false;
    }

    /**
     * Called when the mouse is clicked. Args : mouseX, mouseY, clickedButton
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        // NO-OP
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseReleased1, double mouseReleased3, int mouseReleased5) {
        // NO-OP
        return false;
    }

    private static int scrollCooldown = 0;

    @Override
    public boolean mouseScrolled(double pMouseScrolled1, double pMouseScrolled3, double pMouseScrolled5) {
        // NO-OP
        return false;
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeftIn, int guiTopIn, int mouseButton) {
        // NO-OP
        return false;
    }

    @Override
    protected void slotClicked(@Nonnull Slot slotIn, int slotId, int mouseButton, @Nonnull ClickType type) {
        // NO-OP
    }

    @Override
    public void recipesUpdated() {
        // NO-OP
    }

    @Nonnull
    @Override
    public RecipeBookComponent getRecipeBookComponent() {
        return this.recipeBookGui;
    }
}