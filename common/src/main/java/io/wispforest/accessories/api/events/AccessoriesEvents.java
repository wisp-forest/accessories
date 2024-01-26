package io.wispforest.accessories.api.events;

import io.wispforest.accessories.AccessoriesAccess;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.DropRule;
import io.wispforest.accessories.api.SlotReference;
import io.wispforest.accessories.impl.event.MergedEvent;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.ICancellableEvent;

public class AccessoriesEvents {

    /**
     * Event used to check if the given {@link LivingEntity} should drop any of the given {@link Accessory}
     * found on the entity
     */
    public static final Event<OnDeath> ON_DEATH_EVENT = new MergedEvent<>(OnDeath.class, AccessoriesAccess.getInternal()::getBus,
            (bus, invokers) -> {
                return (livingEntity, capability) -> {
                    for (var invoker : invokers) {
                        if(!invoker.shouldDrop(livingEntity, capability)) return false;
                    }

                    return bus.map(bus1 -> {
                        var event = bus1.post(new OnDeathEvent(livingEntity, capability));

                        return event.isCanceled();
                    }).orElse(true);
                };
            }
    );

    /**
     * Fabric Ecosystem event in which fired directly from {@link #ON_DEATH_EVENT} call
     *
     * Can be used for Common sided mods
     */
    public interface OnDeath {
        boolean shouldDrop(LivingEntity livingEntity, AccessoriesCapability capability);
    }

    /**
     * Forge Ecosystem event in which fired indirectly from {@link #ON_DEATH_EVENT} call using the main Neoforge Event Bus
     */
    public static class OnDeathEvent extends net.neoforged.bus.api.Event implements ICancellableEvent {
        private final LivingEntity entity;
        private final AccessoriesCapability capability;

        public OnDeathEvent(LivingEntity entity, AccessoriesCapability capability){
            this.entity = entity;
            this.capability = capability;
        }

        public final LivingEntity entity(){
            return this.entity;
        }

        public final AccessoriesCapability capability(){
            return this.capability;
        }
    }

    //--

    /**
     * Event used to check what rule should be followed when handling of {@link Accessory} when
     * about to drop such on {@link LivingEntity}'s death
     */
    public static final Event<OnDrop> ON_DROP_EVENT = new MergedEvent<>(OnDrop.class, AccessoriesAccess.getInternal()::getBus,
            (bus, invokers) -> {
                return (dropRule, entity, reference, stack) -> {
                    for (var invoker : invokers) {
                        var dropRule2 = invoker.onDrop(dropRule, entity, reference, stack);

                        if(dropRule2 != DropRule.DEFAULT) return dropRule2;
                    }

                    return bus.map(bus1 -> {
                        var event = bus1.post(new OnDropEvent(dropRule, entity, reference, stack));

                        if(event.isCanceled()){
                            var dropRule2 = event.dropRule;

                            if(dropRule2 != DropRule.DEFAULT) return dropRule2;
                        }

                        return dropRule;
                    }).orElse(dropRule);
                };
            }
    );

    public interface OnDrop {
        DropRule onDrop(DropRule dropRule, LivingEntity entity, SlotReference reference, ItemStack stack);
    }

    public static class OnDropEvent extends net.neoforged.bus.api.Event implements ICancellableEvent {
        private DropRule dropRule;

        private final LivingEntity livingEntity;
        private final SlotReference reference;

        private final ItemStack stack;

        public OnDropEvent(DropRule dropRule, LivingEntity entity, SlotReference reference, ItemStack stack){
            this.dropRule = dropRule;

            this.livingEntity = entity;
            this.reference = reference;

            this.stack = stack;
        }

        public final DropRule dropRule() {
            return this.dropRule;
        }

        private final void setDropRule(DropRule dropRule){
            this.dropRule = dropRule;
        }

        public final LivingEntity entity() {
            return this.livingEntity;
        }

        public final SlotReference reference() {
            return this.reference;
        }

        private final ItemStack stack(){
            return this.stack;
        }
    }

    //--

    /**
     * Event fired on the Equip of the following {@link Accessory} for the given {@link LivingEntity}
     */
    public Event<OnEquip> ON_EQUIP_EVENT = new MergedEvent<>(OnEquip.class, AccessoriesAccess.getInternal()::getBus,
            (bus, invokers) -> {
                return (entity, reference, stack) -> {
                    for (var invoker : invokers) {
                       invoker.onEquip(entity, reference, stack);
                    }

                    bus.ifPresent(bus1 -> {
                       bus1.post(new OnEquipEvent(entity, reference, stack));
                    });
                };
            }
    );

    public interface OnEquip {
        void onEquip(LivingEntity entity, SlotReference reference, ItemStack stack);
    }

    public static class OnEquipEvent extends net.neoforged.bus.api.Event {

        private final LivingEntity entity;
        private final SlotReference reference;
        private final ItemStack stack;

        public OnEquipEvent(LivingEntity entity, SlotReference reference, ItemStack stack){
            this.entity = entity;
            this.reference = reference;
            this.stack = stack;
        }

        public LivingEntity entity() {
            return entity;
        }

        public SlotReference reference() {
            return reference;
        }

        public ItemStack stack() {
            return stack;
        }
    }

    //--

    /**
     * Event fired on the Unequip of the following {@link Accessory} for the given {@link LivingEntity}
     */
    public Event<OnUnequip> ON_UNEQUIP_EVENT = new MergedEvent<>(OnUnequip.class, AccessoriesAccess.getInternal()::getBus,
            (bus, invokers) -> {
                return (entity, reference, stack) -> {
                    for (var invoker : invokers) {
                        invoker.onEquip(entity, reference, stack);
                    }

                    bus.ifPresent(bus1 -> {
                        bus1.post(new OnUnequipEvent(entity, reference, stack));
                    });
                };
            }
    );

    public interface OnUnequip {
        void onEquip(LivingEntity entity, SlotReference reference, ItemStack stack);
    }

    public static class OnUnequipEvent extends net.neoforged.bus.api.Event {

        private final LivingEntity entity;
        private final SlotReference reference;
        private final ItemStack stack;

        public OnUnequipEvent(LivingEntity entity, SlotReference reference, ItemStack stack){
            this.entity = entity;
            this.reference = reference;
            this.stack = stack;
        }

        public LivingEntity entity() {
            return entity;
        }

        public SlotReference reference() {
            return reference;
        }

        public ItemStack stack() {
            return stack;
        }
    }
}
