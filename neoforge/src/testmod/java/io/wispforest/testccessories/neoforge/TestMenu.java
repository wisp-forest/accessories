package io.wispforest.testccessories.neoforge;

import io.wispforest.accessories.api.menu.AccessoriesSlotGenerator;
import io.wispforest.testccessories.neoforge.Testccessories;
import io.wispforest.testccessories.neoforge.UniqueSlotTest;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class TestMenu extends AbstractContainerMenu {

    public TestMenu(int i, Inventory inventory, Player player) {
        this(i, inventory);
    }

    private int addedSlots = 0;

    public TestMenu(int containerId, Inventory playerInventory) {
        super(Testccessories.TEST_MENU_TYPE, containerId);

        var player = playerInventory.player;

        var baseY = 20;

        var generator = AccessoriesSlotGenerator.of(this::addSlot, 0, baseY, player, UniqueSlotTest.testSlot1Ref(), UniqueSlotTest.testSlot2Ref());

        baseY += 18;

        if(generator != null) {
            this.addedSlots = generator.padding(1)
                    .row();
        }

        for(int i = 0; i < 3; ++i) {
            for(int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 0 + (j * 18), baseY));
            }

            baseY += 18;
        }

        baseY += 4;

        for(int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 0 + (i * 18), baseY));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return handleSlotTransfer(index, this.addedSlots);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    //--

    // Taken from ScreenUtils from owolib which all credit goes to glisco: https://github.com/wisp-forest/owo-lib/blob/623e12553710b3c9086bff84f7e33c558c0176e9/src/main/java/io/wispforest/owo/client/screens/ScreenUtils.java#L11
    private ItemStack handleSlotTransfer(int clickedSlotIndex, int upperInventorySize) {
        final var slots = this.slots;
        final var clickedSlot = slots.get(clickedSlotIndex);
        if (!clickedSlot.hasItem()) return ItemStack.EMPTY;

        final var clickedStack = clickedSlot.getItem();

        if (clickedSlotIndex < upperInventorySize) {
            if (!insertIntoSlotRange(clickedStack, upperInventorySize, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!insertIntoSlotRange(clickedStack, 0, upperInventorySize)) {
                return ItemStack.EMPTY;
            }
        }

        if (clickedStack.isEmpty()) {
            clickedSlot.set(ItemStack.EMPTY);
        } else {
            clickedSlot.setChanged();
        }

        return clickedStack;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean insertIntoSlotRange(ItemStack addition, int beginIndex, int endIndex) {
        return insertIntoSlotRange(addition, beginIndex, endIndex, false);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean insertIntoSlotRange(ItemStack addition, int beginIndex, int endIndex, boolean fromLast) {
        return this.moveItemStackTo(addition, beginIndex, endIndex, fromLast);
    }
}
