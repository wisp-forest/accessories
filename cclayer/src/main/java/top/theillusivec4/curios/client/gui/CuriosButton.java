/*
 * Copyright (c) 2018-2020 C4
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

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;

import javax.annotation.Nonnull;

public class CuriosButton extends ImageButton {

    private final AbstractContainerScreen<?> parentGui;

    CuriosButton(AbstractContainerScreen<?> parentGui, int xIn, int yIn, int widthIn, int heightIn, int textureOffsetX, int textureOffsetY, int yDiffText, ResourceLocation resource) {
        super(xIn, yIn, widthIn, heightIn, textureOffsetX, textureOffsetY, yDiffText, resource, (button) -> {/* NO-OP */});
        this.parentGui = parentGui;
    }

    @Override
    public void renderWidget(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        Tuple<Integer, Integer> offsets = CuriosScreen.getButtonOffset(parentGui instanceof CreativeModeInventoryScreen);

        this.setX(parentGui.getGuiLeft() + offsets.getA());
        int yOffset = parentGui instanceof CreativeModeInventoryScreen ? 68 : 83;
        this.setY(parentGui.getGuiTop() + offsets.getB() + yOffset);

        if (parentGui instanceof CreativeModeInventoryScreen gui) {
            boolean isInventoryTab = gui.isInventoryOpen();
            this.active = isInventoryTab;

            if (!isInventoryTab) {
                return;
            }
        }
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
    }
}