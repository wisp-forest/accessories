package io.wispforest.accessories.api.events;

import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.DropRule;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.impl.AccessoryNestUtils;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class AccessoriesEvents {

    /**
     * Event used to check if the given {@link LivingEntity} should drop any of
     * the given {@link Accessory} found on the entity
     */
    public static final Event<OnDeathCallback> ON_DEATH_EVENT = EventFactory.createArrayBacked(OnDeathCallback.class,
            (invokers) -> (eventContext) -> {
                for (var invoker : invokers) invoker.shouldDrop(eventContext);
            }
    );

    public interface OnDeathCallback {
        void shouldDrop(OnDeathEvent eventContext);
    }

    public static class OnDeathEvent extends ReturnableEvent<Boolean> {
        private final LivingEntity entity;
        private final AccessoriesCapability capability;
        private final DamageSource damageSource;

        public OnDeathEvent(LivingEntity entity, AccessoriesCapability capability, DamageSource damageSource) {
            this.entity = entity;
            this.capability = capability;
            this.damageSource = damageSource;
        }

        public final LivingEntity entity() {
            return this.entity;
        }

        public final AccessoriesCapability capability() {
            return this.capability;
        }

        public final DamageSource damageSource() {
            return this.damageSource;
        }
    }

    //--

    /**
     * Event used to check what rule should be followed when handling of {@link Accessory} when
     * about to drop such on {@link LivingEntity}'s death
     */
    public static final Event<OnDropCallback> ON_DROP_EVENT = EventFactory.createArrayBacked(OnDropCallback.class,
            (invokers) -> (eventContext) -> {
                for (var invoker : invokers) {
                    invoker.onDrop(eventContext);

                    if (eventContext.getReturn() != DropRule.DEFAULT) return;
                }
            }
    );

    public interface OnDropCallback {
        void onDrop(OnDropEvent eventContext);
    }

    public static class OnDropEvent extends ReturnableEvent<DropRule> implements SlotEntryReferenced {
        private final SlotReference reference;
        private final ItemStack stack;

        public OnDropEvent(DropRule dropRule, ItemStack stack, SlotReference reference) {
            this.setReturn(dropRule);

            this.stack = stack;
            this.reference = reference;
        }

        @Override
        public final SlotReference reference() {
            return this.reference;
        }

        @Override
        public final ItemStack stack() {
            return this.stack;
        }
    }

    //--

    /**
     * Event fired on the Equip of the following {@link Accessory} for the given {@link LivingEntity}
     */
    public static final Event<CanEquipCallback> CAN_EQUIP_EVENT = EventFactory.createArrayBacked(CanEquipCallback.class,
            (invokers) -> (eventContext) -> {
                var finalState = AccessoryNestUtils.recursiveStackHandling(eventContext.stack, eventContext.reference, (stack1, reference1) -> {
                    var innerEventContext = new CanEquipEvent(stack1, reference1);

                    for (var invoker : invokers) {
                        invoker.onEquip(innerEventContext);

                        if(innerEventContext.getReturn() == Boolean.FALSE) return false;
                    }

                    return innerEventContext.getReturn();
                });

                eventContext.setReturn(finalState);
            }
    );

    public interface CanEquipCallback {
        void onEquip(CanEquipEvent eventContext);
    }

    public static class CanEquipEvent extends ReturnableEvent<Boolean> implements SlotEntryReferenced {
        private final SlotReference reference;
        private final ItemStack stack;

        public CanEquipEvent(ItemStack stack, SlotReference reference) {
            this.reference = reference;
            this.stack = stack;
        }

        @Override
        public SlotReference reference() {
            return reference;
        }

        @Override
        public ItemStack stack() {
            return stack;
        }
    }

    //--

    /**
     * Event fired on the Unequip of the following {@link Accessory} for the given {@link LivingEntity}
     */
    public static final Event<CanUnequipCallback> CAN_UNEQUIP_EVENT = EventFactory.createArrayBacked(CanUnequipCallback.class,
            (invokers) -> (eventContext) -> {
                var finalState = AccessoryNestUtils.recursiveStackHandling(eventContext.stack, eventContext.reference, (stack1, reference1) -> {
                    var innerEventContext = new CanUnequipEvent(stack1, reference1);

                    for (var invoker : invokers) {
                        invoker.onUnequip(innerEventContext);

                        if(innerEventContext.getReturn() == Boolean.FALSE) return false;
                    }

                    return innerEventContext.getReturn();
                });

                eventContext.setReturn(finalState);
            }
    );

    public interface CanUnequipCallback {
        void onUnequip(CanUnequipEvent eventContext);
    }

    public static class CanUnequipEvent extends ReturnableEvent<Boolean> implements SlotEntryReferenced {
        private final SlotReference reference;
        private final ItemStack stack;

        public CanUnequipEvent(ItemStack stack, SlotReference reference) {
            this.reference = reference;
            this.stack = stack;
        }

        @Override
        public SlotReference reference() {
            return reference;
        }

        @Override
        public ItemStack stack() {
            return stack;
        }
    }

    //--

    public static final Event<OnEntityModificationCallback> ENTITY_MODIFICATION_CHECK = EventFactory.createArrayBacked(OnEntityModificationCallback.class,
            (invokers) -> (eventContext) -> {
                for (var invoker : invokers) invoker.checkModifiability(eventContext);
            }
    );

    public interface OnEntityModificationCallback {
        void checkModifiability(OnEntityModificationEvent eventContext);
    }

    public static class OnEntityModificationEvent extends ReturnableEvent<Boolean> implements SlotReferenced {
        private final LivingEntity targetEntity;
        private final Player player;

        @Nullable
        private final SlotReference reference;

        public OnEntityModificationEvent(LivingEntity targetEntity, Player player, @Nullable SlotReference reference) {
            this.reference = reference;

            this.targetEntity = targetEntity;
            this.player = player;
        }

        public LivingEntity getTargetEntity() {
            return targetEntity;
        }

        public Player getPlayer() {
            return player;
        }

        @Nullable
        @Override
        public SlotReference reference() {
            return reference;
        }
    }

    //--

    public interface SlotReferenced {
        SlotReference reference();
    }

    public interface SlotEntryReferenced extends SlotReferenced {
        SlotReference reference();
        ItemStack stack();
    }
}