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

package top.theillusivec4.curios.common.inventory;

import javax.annotation.Nonnull;

import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.api.menu.AccessoriesBasedSlot;
import io.wispforest.accessories.client.gui.AccessoriesInternalSlot;
import io.wispforest.accessories.data.SlotTypeLoader;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.SlotItemHandler;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;
import top.theillusivec4.curios.compat.CuriosWrappingUtils;
import top.theillusivec4.curios.compat.WrappedCurioStackHandler;
import top.theillusivec4.curios.mixin.core.AccessorEntity;

public class CurioSlot extends AccessoriesInternalSlot {

    private final String identifier;
    private final Player player;
    private final SlotContext slotContext;

    private NonNullList<Boolean> renderStatuses;
    private boolean canToggleRender;
    private boolean showCosmeticToggle;
    private boolean isCosmetic = false;

    public CurioSlot(Player player, IDynamicStackHandler handler, int index, String identifier, int xPosition, int yPosition, NonNullList<Boolean> renders, boolean canToggleRender, boolean showCosmeticToggle, boolean isCosmetic) {
        this(player, handler, index, identifier, xPosition, yPosition, renders, canToggleRender);
        this.showCosmeticToggle = showCosmeticToggle;
        this.isCosmetic = isCosmetic;
    }

    public CurioSlot(Player player, IDynamicStackHandler handler, int index, String identifier, int xPosition, int yPosition, NonNullList<Boolean> renders, boolean canToggleRender) {
        super(0,
                getContainer(player, identifier),
                (handler instanceof WrappedCurioStackHandler.HandlerImpl wrapped ? wrapped.isCosmetic : throwException("Unable to handle passed IDynamicStackHandler as such is not a type that is valid")),
                index,
                xPosition,
                yPosition);

        this.identifier = identifier;
        this.renderStatuses = renders;
        this.player = player;
        this.canToggleRender = canToggleRender;
        this.slotContext = new SlotContext(identifier, player, index, this instanceof CosmeticCurioSlot, this instanceof CosmeticCurioSlot || renders.get(index));
    }

    public static final <T> T throwException(String message) {
        throw new IllegalStateException(message);
    }

    private static AccessoriesContainer getContainer(Player player, String curiosId) {
        return player.accessoriesCapability().getContainer(SlotTypeLoader.getSlotType(player.level(), CuriosWrappingUtils.curiosToAccessories(curiosId)));
    }

    public String getIdentifier() {
        return this.identifier;
    }

    public boolean canToggleRender() {
        return this.canToggleRender;
    }

    public boolean isCosmetic() {
        return this.isCosmetic;
    }

    public boolean showCosmeticToggle() {
        return this.showCosmeticToggle;
    }

    public boolean getRenderStatus() {
        return this.accessoriesContainer.shouldRender(this.getContainerSlot());
    }

    @OnlyIn(Dist.CLIENT)
    public String getSlotName() {
        //NO-OP
        return "";
    }

    @Override
    public void set(@Nonnull ItemStack stack) {
        super.set(stack);
    }

    @Override
    public boolean allowModification(@Nonnull Player pPlayer) {
        return super.allowModification(pPlayer);
    }
}