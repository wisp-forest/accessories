package io.wispforest.accessories.api.events;

import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.api.*;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.impl.event.EventUtils;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.ICancellableEvent;

public class AccessoriesEvents {

    /**
     * Event used to check if the given {@link LivingEntity} should drop any of
     * the given {@link Accessory} found on the entity
     */
    public static final Event<OnDeath> ON_DEATH_EVENT = EventUtils.createEventWithBus(OnDeath.class, AccessoriesInternals::getBus,
            (bus, invokers) -> (livingEntity, capability) -> {
                var state = TriState.DEFAULT;

                for (var invoker : invokers) {
                    state = invoker.shouldDrop(livingEntity, capability);

                    if (state != TriState.DEFAULT) return state;
                }

                if(bus.isEmpty()) return state;

                return bus.get()
                        .post(new OnDeathEvent(livingEntity, capability))
                        .getReturn();
            }
    );

    public interface OnDeath {
        TriState shouldDrop(LivingEntity livingEntity, AccessoriesCapability capability);
    }

    /**
     * Neoforge Ecosystem event in which fired directly from {@link #ON_DEATH_EVENT} call using the main Neoforge Event Bus
     */
    public static class OnDeathEvent extends ReturnableEvent {
        private final LivingEntity entity;
        private final AccessoriesCapability capability;

        public OnDeathEvent(LivingEntity entity, AccessoriesCapability capability) {
            this.entity = entity;
            this.capability = capability;
        }

        public final LivingEntity entity() {
            return this.entity;
        }

        public final AccessoriesCapability capability() {
            return this.capability;
        }
    }

    //--

    /**
     * Event used to check what rule should be followed when handling of {@link Accessory} when
     * about to drop such on {@link LivingEntity}'s death
     */
    public static final Event<OnDrop> ON_DROP_EVENT = EventUtils.createEventWithBus(OnDrop.class, AccessoriesInternals::getBus,
            (bus, invokers) -> (dropRule, stack, reference) -> {
                var currentRule = dropRule;

                for (var invoker : invokers) {
                    currentRule = invoker.onDrop(dropRule, stack, reference);

                    if (currentRule != DropRule.DEFAULT) return currentRule;
                }

                if(bus.isEmpty()) return currentRule;

                return bus.get()
                        .post(new OnDropEvent(dropRule, stack, reference))
                        .dropRule();
            }
    );

    public interface OnDrop {
        DropRule onDrop(DropRule dropRule, ItemStack stack, SlotReference reference);
    }

    /**
     * Neoforge Ecosystem event in which fired directly from {@link #ON_DROP_EVENT} call using the main Neoforge Event Bus
     */
    public static class OnDropEvent extends net.neoforged.bus.api.Event implements ICancellableEvent, SlotReferenced {
        private DropRule dropRule;

        private final SlotReference reference;

        private final ItemStack stack;

        public OnDropEvent(DropRule dropRule, ItemStack stack, SlotReference reference) {
            this.dropRule = dropRule;

            this.reference = reference;

            this.stack = stack;
        }

        public final DropRule dropRule() {
            return this.dropRule;
        }

        private void setDropRule(DropRule dropRule) {
            this.dropRule = dropRule;

            this.setCanceled(true);
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
    public static final Event<CanEquip> CAN_EQUIP_EVENT = EventUtils.createEventWithBus(CanEquip.class, AccessoriesInternals::getBus,
            (bus, invokers) -> (stack, reference) -> {
                var state = TriState.DEFAULT;

                if(AccessoriesAPI.getAccessory(stack.getItem()) instanceof AccessoryNest holdable){
                    var innerStacks = holdable.getInnerStacks(stack);

                    for (ItemStack innerStack : innerStacks) {
                        for (var invoker : invokers) {
                            state = invoker.onEquip(innerStack, reference);

                            if(state == TriState.FALSE) return state;
                        }

                        if(bus.isPresent()) {
                            state = bus.get()
                                    .post(new CanUnequipEvent(innerStack, reference))
                                    .getReturn();
                        }

                        if(state == TriState.FALSE) return state;
                    }
                }

                for (var invoker : invokers) {
                    state = invoker.onEquip(stack, reference);

                    if(state != TriState.DEFAULT) return state;
                }

                if(bus.isEmpty()) return state;

                return bus.get()
                        .post(new CanEquipEvent(stack, reference))
                        .getReturn();
            }
    );

    public interface CanEquip {
        TriState onEquip(ItemStack stack, SlotReference reference);
    }

    /**
     * Neoforge Ecosystem event in which fired directly from {@link #CAN_EQUIP_EVENT} call using the main Neoforge Event Bus
     */
    public static class CanEquipEvent extends ReturnableEvent implements SlotReferenced {
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
    public static final Event<CanUnequip> CAN_UNEQUIP_EVENT = EventUtils.createEventWithBus(CanUnequip.class, AccessoriesInternals::getBus,
            (bus, invokers) -> (stack, reference) -> {
                var state = TriState.DEFAULT;

                if(AccessoriesAPI.getAccessory(stack.getItem()) instanceof AccessoryNest holdable){
                    var innerStacks = holdable.getInnerStacks(stack);

                    for (ItemStack innerStack : innerStacks) {
                        for (var invoker : invokers) {
                            state = invoker.onUnequip(innerStack, reference);

                            if(state == TriState.FALSE) return state;
                        }

                        if(bus.isPresent()) {
                            state = bus.get()
                                    .post(new CanUnequipEvent(innerStack, reference))
                                    .getReturn();
                        }

                        if(state == TriState.FALSE) return state;
                    }
                }

                for (var invoker : invokers) {
                    state = invoker.onUnequip(stack, reference);

                    if(state != TriState.DEFAULT) return state;
                }

                if(bus.isEmpty()) return state;

                return bus.get()
                        .post(new CanUnequipEvent(stack, reference))
                        .getReturn();
            }
    );

    public interface CanUnequip {
        TriState onUnequip(ItemStack stack, SlotReference reference);
    }

    /**
     * Neoforge Ecosystem event in which fired directly from {@link #CAN_UNEQUIP_EVENT} call using the main Neoforge Event Bus
     */
    public static class CanUnequipEvent extends ReturnableEvent implements SlotReferenced {
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

    public interface SlotReferenced {
        SlotReference reference();
        ItemStack stack();
    }

    private static class ReturnableEvent extends net.neoforged.bus.api.Event implements ICancellableEvent {
        private TriState returnState = TriState.DEFAULT;

        public final ReturnableEvent setReturn(TriState returnState){
            this.returnState = returnState;

            if(returnState != TriState.DEFAULT) this.setCanceled(true);

            return this;
        }

        public final TriState getReturn(){
            return this.returnState;
        }

        @Deprecated
        @Override
        public final void setResult(Result value) {
            super.setResult(value);

            switch (value) {
                case DEFAULT -> setReturn(TriState.DEFAULT);
                case ALLOW -> setReturn(TriState.TRUE);
                case DENY -> setReturn(TriState.FALSE);
            }
        }
    }
}