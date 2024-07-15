package io.wispforest.accessories.menu;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.slot.SlotTypeReference;
import io.wispforest.accessories.api.slot.UniqueSlotHandling;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class ArmorSlotTypes implements UniqueSlotHandling.RegistrationCallback {

    public static final Map<EquipmentSlot, ResourceLocation> TEXTURE_EMPTY_SLOTS = Map.of(
            EquipmentSlot.FEET, ResourceLocation.withDefaultNamespace("item/empty_armor_slot_boots"),
            EquipmentSlot.LEGS, ResourceLocation.withDefaultNamespace("item/empty_armor_slot_leggings"),
            EquipmentSlot.CHEST, ResourceLocation.withDefaultNamespace("item/empty_armor_slot_chestplate"),
            EquipmentSlot.HEAD, ResourceLocation.withDefaultNamespace("item/empty_armor_slot_helmet"));

    public static final EquipmentSlot[] SLOT_IDS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public static final ArmorSlotTypes INSTANCE = new ArmorSlotTypes();

    private static final ResourceLocation HEAD_PREDICATE_LOCATION = Accessories.of("head");
    private static final ResourceLocation CHEST_PREDICATE_LOCATION = Accessories.of("chest");
    private static final ResourceLocation LEGS_PREDICATE_LOCATION = Accessories.of("legs");
    private static final ResourceLocation FEET_PREDICATE_LOCATION = Accessories.of("feet");

    private SlotTypeReference headSlotReference = null;
    private SlotTypeReference chestSlotReference = null;
    private SlotTypeReference legsSlotReference = null;
    private SlotTypeReference feetSlotReference = null;

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

    public static List<SlotTypeReference> getArmorReferences() {
        return List.of(headSlot(), chestSlot(), legsSlot(), feetSlot());
    }

    @Nullable
    public static SlotTypeReference getReferenceFromSlot(EquipmentSlot equipmentSlot) {
        return switch (equipmentSlot) {
            case HEAD -> headSlot();
            case CHEST -> chestSlot();
            case LEGS -> legsSlot();
            case FEET -> feetSlot();
            default -> null;
        };
    }

    public void init() {
        UniqueSlotHandling.EVENT.register(this);

        AccessoriesAPI.registerPredicate(HEAD_PREDICATE_LOCATION, (level, slotType, slot, stack) -> isValid(stack, EquipmentSlot.HEAD));
        AccessoriesAPI.registerPredicate(CHEST_PREDICATE_LOCATION, (level, slotType, slot, stack) -> isValid(stack, EquipmentSlot.CHEST));
        AccessoriesAPI.registerPredicate(LEGS_PREDICATE_LOCATION, (level, slotType, slot, stack) -> isValid(stack, EquipmentSlot.LEGS));
        AccessoriesAPI.registerPredicate(FEET_PREDICATE_LOCATION, (level, slotType, slot, stack) -> isValid(stack, EquipmentSlot.FEET));
    }

    @Override
    public void registerSlots(UniqueSlotHandling.UniqueSlotBuilderFactory factory) {
        headSlotReference = factory.create(Accessories.of("head"), 1)
                .slotPredicates(HEAD_PREDICATE_LOCATION)
                .strictMode(true)
                .allowResizing(false)
                .validTypes(EntityType.PLAYER, EntityType.ARMOR_STAND)
                .build();

        chestSlotReference = factory.create(Accessories.of("chest"), 1)
                .slotPredicates(CHEST_PREDICATE_LOCATION)
                .strictMode(true)
                .allowResizing(false)
                .validTypes(EntityType.PLAYER, EntityType.ARMOR_STAND)
                .build();

        legsSlotReference = factory.create(Accessories.of("legs"), 1)
                .slotPredicates(LEGS_PREDICATE_LOCATION)
                .strictMode(true)
                .allowResizing(false)
                .validTypes(EntityType.PLAYER, EntityType.ARMOR_STAND)
                .build();

        feetSlotReference = factory.create(Accessories.of("feet"), 1)
                .slotPredicates(FEET_PREDICATE_LOCATION)
                .strictMode(true)
                .allowResizing(false)
                .validTypes(EntityType.PLAYER, EntityType.ARMOR_STAND)
                .build();
    }

    private static TriState isValid(ItemStack stack, EquipmentSlot equipmentSlot) {
        var bl = stack.getItem() instanceof Equipable equipable
                && equipable.getEquipmentSlot().equals(equipmentSlot);

        return bl ? TriState.TRUE : TriState.DEFAULT;
    }
}
