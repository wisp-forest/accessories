package io.wispforest.accessories.menu;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

public interface SaddleableContainer extends Container {

    @Nullable
    static Container of(LivingEntity living) {
        if(living instanceof AbstractHorse abstractHorse) {
            return ofHorse(abstractHorse);
        } else if(living instanceof Saddleable saddleable) {
            return ofSaddleable(saddleable);
        }

        return null;
    }

    static Container ofHorse(AbstractHorse abstractHorse) {
        return new SlotAccessContainer(abstractHorse.getSlot(400));

//        return new SaddleableContainer() {
//            private final SlotAccess slotAccess = abstractHorse.getSlot(400);
//
//            @Override
//            public Saddleable saddleable() {
//                return abstractHorse;
//            }
//
//            @Override
//            public ItemStack getSaddle() {
//                return this.slotAccess.get();
//            }
//
//            @Override
//            public ItemStack removeSaddle() {
//                this.slotAccess.set(ItemStack.EMPTY);
//
//                return this.slotAccess.get().copy();
//            }
//        };
    }

    static Container ofSaddleable(Saddleable saddleable) {
        return new SlotAccessContainer(
                SlotAccess.of(
                        () -> saddleable.isSaddled() ? Items.SADDLE.getDefaultInstance() : ItemStack.EMPTY,
                        stack -> saddleable.equipSaddle(stack, SoundSource.NEUTRAL)
                )
        );

//        return new SaddleableContainer() {
//            @Override
//            public Saddleable saddleable() {
//                return saddleable;
//            }
//
//            @Override
//            public ItemStack getSaddle() {
//                return saddleable.isSaddled() ? Items.SADDLE.getDefaultInstance() : ItemStack.EMPTY;
//            }
//
//            @Override
//            public ItemStack removeSaddle() {
//                saddleable.equipSaddle(ItemStack.EMPTY, null);
//
//                return ItemStack.EMPTY;
//            }
//        };
    }

    Saddleable saddleable();

    ItemStack getSaddle();

    ItemStack removeSaddle();

    @Override
    default int getContainerSize() {
        return 1;
    }

    @Override
    default boolean isEmpty() {
        return getItem(0).isEmpty();
    }

    @Override
    default ItemStack getItem(int slot) {
        return getSaddle();
    }

    @Override
    default ItemStack removeItem(int slot, int amount) {
        if(amount <= 0) return ItemStack.EMPTY;

        return removeSaddle();
    }

    @Override
    default ItemStack removeItemNoUpdate(int slot) {
        return removeSaddle();
    }

    @Override
    default void setItem(int slot, ItemStack stack) {
        saddleable().equipSaddle(stack, SoundSource.NEUTRAL);
    }

    @Override
    default void setChanged() {}

    @Override
    default boolean stillValid(Player player) {
        return true;
    }

    @Override
    default void clearContent() {}
}
