package dev.emi.trinkets.api;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Function3;
import dev.emi.trinkets.compat.*;
import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistryV3;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentInitializer;
import dev.onyxstudios.cca.api.v3.entity.RespawnCopyStrategy;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.data.EntitySlotLoader;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.*;

public class TrinketsApi implements EntityComponentInitializer {
    public static final ComponentKey<TrinketComponent> TRINKET_COMPONENT = ComponentRegistryV3.INSTANCE
            .getOrCreate(new ResourceLocation(TrinketConstants.MOD_ID, "trinkets"), TrinketComponent.class);
    private static final Map<ResourceLocation, Function3<ItemStack, SlotReference, LivingEntity, TriState>> PREDICATES = new HashMap<>();

    private static final Map<Item, Trinket> TRINKETS = new HashMap<>();
    private static final Trinket DEFAULT_TRINKET;

    public static void registerTrinket(Item item, Trinket trinket) {
        AccessoriesAPI.registerAccessory(item, new WrappedTrinket(trinket));
        TRINKETS.put(item, trinket);
    }

    public static Trinket getTrinket(Item item) {
        var trinket = TRINKETS.get(item);

        if(trinket == null) {
            var accessory = AccessoriesAPI.getAccessory(item);

            if(accessory.isPresent()) trinket = new WrappedAccessory(accessory.get());
        }

        //TODO: Maybe check for valid slots if any and return different wrapped accessory as a way of indicating this item is for sure a valid trinket for things like Compound Accessories
        if(trinket == null) return DEFAULT_TRINKET;

        return trinket;
    }

    public static Trinket getDefaultTrinket() {
        return DEFAULT_TRINKET;
    }

    public static Optional<TrinketComponent> getTrinketComponent(LivingEntity livingEntity) {
        if(livingEntity == null) return Optional.empty();

        return AccessoriesAPI.getCapability(livingEntity).<TrinketComponent>map(LivingEntityTrinketComponent::new).or(() -> Optional.of(new EmptyComponent(livingEntity)));
    }

    public static void onTrinketBroken(ItemStack stack, SlotReference ref, LivingEntity entity) {
        throw new IllegalStateException("onTrinketBroken not implemented");
    }

    @Deprecated
    public static Map<String, SlotGroup> getPlayerSlots() {
        return getEntitySlots(EntityType.PLAYER);
    }

    public static Map<String, SlotGroup> getPlayerSlots(Level world) {
        return getEntitySlots(world, EntityType.PLAYER);
    }

    public static Map<String, SlotGroup> getPlayerSlots(Player player) {
        return getEntitySlots(player);
    }

//    @Deprecated
////    public static Map<String, SlotGroup> getEntitySlots(EntityType<?> type) {
////        var validEntitySlots = EntitySlotLoader.INSTANCE.getSlotTypes(false, type);
////
////        if(validEntitySlots == null) validEntitySlots = Map.of();
////
////        var convertedGroups = new HashMap<String, SlotGroup>();
////
////        for (var entry : SlotGroupLoader.INSTANCE.getGroups(false).entrySet()) {
////            Map<String, io.wispforest.accessories.api.SlotType> validSlots = new HashMap<>();
////
////            for (String slot : entry.getValue().slots()) {
////                var slotType = validEntitySlots.get(slot);
////
////                if(slotType == null) continue;
////
////                validSlots.put(slot, slotType);
////            }
////
////            convertedGroups.put(
////                    entry.getKey(),
////                    new WrappedSlotGroup(entry.getValue(), s -> Optional.ofNullable(validSlots.get(s)))
////            );
////        }
////
////        return convertedGroups;
////    }
////    public static Map<String, SlotGroup> getEntitySlots(Level world, EntityType<?> type) {
////        var validEntitySlots = EntitySlotLoader.INSTANCE.getSlotTypes(false, type);
////
////        if(validEntitySlots == null) validEntitySlots = Map.of();
////
////        var convertedGroups = new HashMap<String, SlotGroup>();
////
////        for (var entry : SlotGroupLoader.INSTANCE.getGroups(world.isClientSide()).entrySet()) {
////            Map<String, io.wispforest.accessories.api.SlotType> validSlots = new HashMap<>();
////
////            for (String slot : entry.getValue().slots()) {
////                var slotType = validEntitySlots.get(slot);
////
////                if(slotType == null) continue;
////
////                validSlots.put(slot, slotType);
////            }
////
////            convertedGroups.put(
////                    entry.getKey(),
////                    new WrappedSlotGroup(entry.getValue(), s -> Optional.ofNullable(validSlots.get(s)))
////            );
////        }
////
////        return convertedGroups;
////    }

    @Deprecated
    public static Map<String, SlotGroup> getEntitySlots(EntityType<?> type) {
        Map<String, SlotType> convertedSlots = new HashMap<>();

        for (var entry : EntitySlotLoader.INSTANCE.getSlotTypes(false, type).entrySet()) {
            convertedSlots.put(entry.getKey(), new WrappedSlotType(entry.getValue()));
        }

        return Map.of("", new WrappedSlotGroup(convertedSlots));
    }

    public static Map<String, SlotGroup> getEntitySlots(Level world, EntityType<?> type) {
        Map<String, SlotType> convertedSlots = new HashMap<>();

        for (var entry : AccessoriesAPI.getEntitySlots(world, type).entrySet()) {
            convertedSlots.put(entry.getKey(), new WrappedSlotType(entry.getValue()));
        }

        return Map.of("", new WrappedSlotGroup(convertedSlots));
    }

    public static Map<String, SlotGroup> getEntitySlots(Entity entity) {
        if (entity != null) return getEntitySlots(entity.level(), entity.getType());

        return ImmutableMap.of();
    }

    public static void registerTrinketPredicate(ResourceLocation id, Function3<ItemStack, SlotReference, LivingEntity, TriState> predicate) {
        PREDICATES.put(id, predicate);

        AccessoriesAPI.registerPredicate(id, (reference, stack) -> {
            var capability = AccessoriesAPI.getCapability(reference.entity());

            if(capability.isEmpty()) return TriState.DEFAULT;

            var container = capability.get().getContainers().get(reference.slotName());

            if(container == null) return TriState.DEFAULT;

            var ref = new SlotReference(
                    new WrappedTrinketInventory(
                            new LivingEntityTrinketComponent(capability.get()),
                            container,
                            container.slotType().get()
                    ),
                    0
            );

            return predicate.apply(stack, ref, reference.entity());
        });
    }

    public static Optional<Function3<ItemStack, SlotReference, LivingEntity, TriState>> getTrinketPredicate(ResourceLocation id) {
        return Optional.ofNullable(PREDICATES.get(id));
    }

    public static boolean evaluatePredicateSet(Set<ResourceLocation> set, ItemStack stack, SlotReference ref, LivingEntity entity) {
        var convertedSet = new HashSet<ResourceLocation>();

        for (var location : set) {
            var converetdLocation = switch (location.toString()){
                case "trinkets:all" -> Accessories.of("all");
                case "trinkets:none" -> Accessories.of("none");
                case "trinkets:tag" -> Accessories.of("tag");
                case "trinkets:relevant" -> Accessories.of("relevant");
                default -> location;
            };

            convertedSet.add(converetdLocation);
        }

        var slotName = ((WrappedTrinketInventory) ref.inventory()).container.getSlotName();

        var reference = new io.wispforest.accessories.api.SlotReference(slotName, entity, ref.index());

        return AccessoriesAPI.getPredicateResults(convertedSet, reference, stack);
    }

    static {
        TrinketsApi.registerTrinketPredicate(new ResourceLocation("trinkets", "all"), (stack, ref, entity) -> TriState.TRUE);
        TrinketsApi.registerTrinketPredicate(new ResourceLocation("trinkets", "none"), (stack, ref, entity) -> TriState.FALSE);
        TagKey<Item> trinketsAll = TagKey.create(Registries.ITEM, new ResourceLocation("trinkets", "all"));

        TrinketsApi.registerTrinketPredicate(new ResourceLocation("trinkets", "tag"), (stack, ref, entity) -> {
            SlotType slot = ref.inventory().getSlotType();
            TagKey<Item> tag = TagKey.create(Registries.ITEM, new ResourceLocation("trinkets", slot.getGroup() + "/" + slot.getName()));

            if (stack.is(tag) || stack.is(trinketsAll)) {
                return TriState.TRUE;
            }
            return TriState.DEFAULT;
        });
        TrinketsApi.registerTrinketPredicate(new ResourceLocation("trinkets", "relevant"), (stack, ref, entity) -> {
            UUID uuid = UUID.nameUUIDFromBytes((ref.inventory().getSlotType().getName() + ref.index()).getBytes());
            var accessory = AccessoriesAPI.getAccessory(stack).get();

            var map = accessory.getModifiers(stack, new io.wispforest.accessories.api.SlotReference(ref.inventory().getSlotType().getName(), entity, ref.index()), uuid);
            if (!map.isEmpty()) {
                return TriState.TRUE;
            }
            return TriState.DEFAULT;
        });
        DEFAULT_TRINKET = new WrappedAccessory(AccessoriesAPI.defaultAccessory());
    }

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        registry.registerFor(LivingEntity.class, TrinketsApi.TRINKET_COMPONENT, livingEntity -> getTrinketComponent(livingEntity).get());
        registry.registerForPlayers(TrinketsApi.TRINKET_COMPONENT, player -> getTrinketComponent(player).get(), RespawnCopyStrategy.ALWAYS_COPY);
    }
}
