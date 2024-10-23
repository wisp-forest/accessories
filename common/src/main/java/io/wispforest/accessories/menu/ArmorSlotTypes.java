package io.wispforest.accessories.menu;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.SoundEventData;
import io.wispforest.accessories.api.slot.*;
import it.unimi.dsi.fastutil.Pair;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.util.TriConsumer;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ArmorSlotTypes implements UniqueSlotHandling.RegistrationCallback {

    private static final Accessory armorAccessory = new Accessory() {};

    public static final Map<EquipmentSlot, ResourceLocation> TEXTURE_EMPTY_SLOTS = Map.of(
            EquipmentSlot.FEET, ResourceLocation.withDefaultNamespace("item/empty_armor_slot_boots"),
            EquipmentSlot.LEGS, ResourceLocation.withDefaultNamespace("item/empty_armor_slot_leggings"),
            EquipmentSlot.CHEST, ResourceLocation.withDefaultNamespace("item/empty_armor_slot_chestplate"),
            EquipmentSlot.HEAD, ResourceLocation.withDefaultNamespace("item/empty_armor_slot_helmet")
    );

    public static final EquipmentSlot[] SLOT_IDS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public static final ArmorSlotTypes INSTANCE = new ArmorSlotTypes();

    private static final ResourceLocation HEAD_PREDICATE_LOCATION = Accessories.of("head");
    private static final ResourceLocation CHEST_PREDICATE_LOCATION = Accessories.of("chest");
    private static final ResourceLocation LEGS_PREDICATE_LOCATION = Accessories.of("legs");
    private static final ResourceLocation FEET_PREDICATE_LOCATION = Accessories.of("feet");
    private static final ResourceLocation ANIMAL_BODY_PREDICATE_LOCATION  = Accessories.of("animal_body");

    private SlotTypeReference headSlotReference = null;
    private SlotTypeReference chestSlotReference = null;
    private SlotTypeReference legsSlotReference = null;
    private SlotTypeReference feetSlotReference = null;
    private SlotTypeReference animalBodySlotReference = null;

    private ArmorSlotTypes() {}

    public static boolean isArmorType(String slotType) {
        return headSlot().slotName().equals(slotType)
                || chestSlot().slotName().equals(slotType)
                || legsSlot().slotName().equals(slotType)
                || feetSlot().slotName().equals(slotType);
    }

    public static SlotTypeReference headSlot() {
        return ArmorSlotTypes.INSTANCE.headSlotReference;
    }

    public static SlotTypeReference chestSlot() {
        return ArmorSlotTypes.INSTANCE.chestSlotReference;
    }

    public static SlotTypeReference legsSlot() {
        return ArmorSlotTypes.INSTANCE.legsSlotReference;
    }

    public static SlotTypeReference feetSlot() {
        return ArmorSlotTypes.INSTANCE.feetSlotReference;
    }

    public static SlotTypeReference animalBody() {
        return ArmorSlotTypes.INSTANCE.animalBodySlotReference;
    }

    public static List<SlotTypeReference> getArmorReferences() {
        return List.of(headSlot(), chestSlot(), legsSlot(), feetSlot());
    }

    public static final ResourceLocation SPRITE_ATLAS_LOCATION = ResourceLocation.withDefaultNamespace("textures/atlas/gui.png");

    private static final ResourceLocation LLAMA_ARMOR_SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/horse/llama_armor_slot");
    private static final ResourceLocation HORSE_ARMOR_SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/horse/armor_slot");

    @Nullable
    public static Pair<@Nullable ResourceLocation, ResourceLocation> getEmptyTexture(EquipmentSlot slot, LivingEntity living) {
        var texture = TEXTURE_EMPTY_SLOTS.get(slot);

        if (texture != null) return Pair.of(null, texture);


        if (living instanceof AbstractHorse horse) {
            if (horse.canUseSlot(EquipmentSlot.BODY)) {
                if (horse instanceof Llama) return Pair.of(SPRITE_ATLAS_LOCATION, LLAMA_ARMOR_SLOT_SPRITE);

                return Pair.of(SPRITE_ATLAS_LOCATION, HORSE_ARMOR_SLOT_SPRITE);
            }
        }

        return null;
    }

    @Nullable
    public static SlotTypeReference getReferenceFromSlot(EquipmentSlot equipmentSlot) {
        return switch (equipmentSlot) {
            case HEAD -> headSlot();
            case CHEST -> chestSlot();
            case LEGS -> legsSlot();
            case FEET -> feetSlot();
            case BODY -> animalBody();
            default -> null;
        };
    }

    public static boolean isValidEquipable(EquipmentSlot equipmentSlot) {
        return switch (equipmentSlot) {
            case HEAD, LEGS, CHEST, FEET, BODY -> true;
            default -> false;
        };
    }

    public void init() {
        UniqueSlotHandling.EVENT.register(this);

        SlotPredicateRegistry.registerPredicate(HEAD_PREDICATE_LOCATION, (EntityBasedPredicate) ((level, entity, slotType, slot, stack) -> isValid(entity, stack, EquipmentSlot.HEAD)));
        SlotPredicateRegistry.registerPredicate(CHEST_PREDICATE_LOCATION, (EntityBasedPredicate) ((level, entity, slotType, slot, stack) -> isValid(entity, stack, EquipmentSlot.CHEST)));
        SlotPredicateRegistry.registerPredicate(LEGS_PREDICATE_LOCATION,  (EntityBasedPredicate) ((level, entity, slotType, slot, stack) -> isValid(entity, stack, EquipmentSlot.LEGS)));
        SlotPredicateRegistry.registerPredicate(FEET_PREDICATE_LOCATION,  (EntityBasedPredicate) ((level, entity, slotType, slot, stack) -> isValid(entity, stack, EquipmentSlot.FEET)));
        SlotPredicateRegistry.registerPredicate(ANIMAL_BODY_PREDICATE_LOCATION,  (EntityBasedPredicate) ((level, entity, slotType, slot, stack) -> isValid(entity, stack, EquipmentSlot.BODY)));
    }

    @Override
    public void registerSlots(UniqueSlotHandling.UniqueSlotBuilderFactory factory) {
        headSlotReference = factory.create(Accessories.of("head"), 1)
                .allowTooltipInfo(false)
                .slotPredicates(HEAD_PREDICATE_LOCATION)
                .strictMode(true)
                .allowResizing(false)
                .allowEquipFromUse(false)
                .validTypes(EntityType.PLAYER, EntityType.ARMOR_STAND)
                .build();

        chestSlotReference = factory.create(Accessories.of("chest"), 1)
                .allowTooltipInfo(false)
                .slotPredicates(CHEST_PREDICATE_LOCATION)
                .strictMode(true)
                .allowResizing(false)
                .allowEquipFromUse(false)
                .validTypes(EntityType.PLAYER, EntityType.ARMOR_STAND)
                .build();

        legsSlotReference = factory.create(Accessories.of("legs"), 1)
                .allowTooltipInfo(false)
                .slotPredicates(LEGS_PREDICATE_LOCATION)
                .strictMode(true)
                .allowResizing(false)
                .allowEquipFromUse(false)
                .validTypes(EntityType.PLAYER, EntityType.ARMOR_STAND)
                .build();

        feetSlotReference = factory.create(Accessories.of("feet"), 1)
                .allowTooltipInfo(false)
                .slotPredicates(FEET_PREDICATE_LOCATION)
                .strictMode(true)
                .allowResizing(false)
                .allowEquipFromUse(false)
                .validTypes(EntityType.PLAYER, EntityType.ARMOR_STAND)
                .build();

        animalBodySlotReference = factory.create(Accessories.of("animal_body"), 1)
                .allowTooltipInfo(false)
                .slotPredicates(ANIMAL_BODY_PREDICATE_LOCATION)
                .strictMode(true)
                .allowResizing(false)
                .allowEquipFromUse(false)
                .validTypes(EntityType.HORSE, EntityType.WOLF)
                .build();
    }

    private static TriState isValid(LivingEntity livingEntity, ItemStack stack, EquipmentSlot equipmentSlot) {
        EquipmentSlot stackEquipmentSlot = null;

        if(livingEntity == null) {
            var equipable = stack.get(DataComponents.EQUIPPABLE);

            if(equipable != null) stackEquipmentSlot = equipable.slot();
        } else {
            stackEquipmentSlot = livingEntity.getEquipmentSlotForItem(stack);
        }

        return equipmentSlot.equals(stackEquipmentSlot) ? TriState.TRUE : TriState.DEFAULT;
    }

    @Nullable
    public static ItemStack getAlternativeStack(LivingEntity instance, EquipmentSlot equipmentSlot) {
        var capability = instance.accessoriesCapability();

        if (capability != null) {
            var reference = ArmorSlotTypes.getReferenceFromSlot(equipmentSlot);

            if (reference != null) {
                var container = capability.getContainer(reference);

                if (container != null) {
                    if(!container.shouldRender(0)) return ItemStack.EMPTY;

                    var stack = container.getCosmeticAccessories().getItem(0);

                    if (!stack.isEmpty() && Accessories.config().clientOptions.showCosmeticAccessories()) return stack;
                }
            }
        }

        return null;
    }
}
