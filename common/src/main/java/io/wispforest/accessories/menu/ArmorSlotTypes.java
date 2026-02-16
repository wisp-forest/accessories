package io.wispforest.accessories.menu;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.action.ActionResponse;
import io.wispforest.accessories.api.action.ActionResponseBuffer;
import io.wispforest.accessories.api.core.Accessory;
import io.wispforest.accessories.api.slot.SlotTypeReference;
import io.wispforest.accessories.api.slot.UniqueSlotHandling;
import io.wispforest.accessories.api.slot.validator.EntitySlotValidator;
import io.wispforest.accessories.api.slot.validator.SlotValidatorRegistry;
import io.wispforest.accessories.impl.slot.StrictMode;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class ArmorSlotTypes implements UniqueSlotHandling.RegistrationCallback {

    private static final ResourceLocation SADDLE_SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/slot/saddle");

    private static final ResourceLocation LLAMA_ARMOR_SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/slot/llama_armor");
    private static final ResourceLocation HORSE_ARMOR_SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/slot/horse_armor");

    private static final Accessory armorAccessory = new Accessory() {};

    public static final Map<EquipmentSlot, ResourceLocation> TEXTURE_EMPTY_SLOTS = Map.of(
            EquipmentSlot.FEET, InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS,
            EquipmentSlot.LEGS, InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS,
            EquipmentSlot.CHEST, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
            EquipmentSlot.HEAD, InventoryMenu.EMPTY_ARMOR_SLOT_HELMET,
            EquipmentSlot.SADDLE, SADDLE_SLOT_SPRITE
    );

    public static final EquipmentSlot[] SLOT_IDS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public static final ArmorSlotTypes INSTANCE = new ArmorSlotTypes();

    private static final ResourceLocation HEAD_PREDICATE_LOCATION = of("head");
    private static final ResourceLocation CHEST_PREDICATE_LOCATION = of("chest");
    private static final ResourceLocation LEGS_PREDICATE_LOCATION = of("legs");
    private static final ResourceLocation FEET_PREDICATE_LOCATION = of("feet");
    private static final ResourceLocation ANIMAL_BODY_PREDICATE_LOCATION  = of("animal_body");
    private static final ResourceLocation SADDLE_PREDICATE_LOCATION  = of("saddle");

    public static ResourceLocation of(String path) {
        return ResourceLocation.fromNamespaceAndPath(Accessories.MODID + "_" + "cosmetics", path);
    }

    private SlotTypeReference headSlotReference = null;
    private SlotTypeReference chestSlotReference = null;
    private SlotTypeReference legsSlotReference = null;
    private SlotTypeReference feetSlotReference = null;
    private SlotTypeReference animalBodySlotReference = null;
    private SlotTypeReference saddleSlotReference = null;

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

    public static SlotTypeReference saddleSlot() {
        return ArmorSlotTypes.INSTANCE.saddleSlotReference;
    }

    public static List<SlotTypeReference> getArmorReferences() {
        return List.of(headSlot(), chestSlot(), legsSlot(), feetSlot());
    }

    @Nullable
    public static ResourceLocation getEmptyTexture(EquipmentSlot slot, LivingEntity living) {
        var texture = TEXTURE_EMPTY_SLOTS.get(slot);

        if (texture != null) return texture;

        if (living instanceof AbstractHorse horse) {
            if (horse.canUseSlot(EquipmentSlot.BODY)) {
                return (horse instanceof Llama) ? LLAMA_ARMOR_SLOT_SPRITE : HORSE_ARMOR_SLOT_SPRITE;
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
            case SADDLE -> saddleSlot();
            default -> null;
        };
    }

    public static boolean isValidEquipable(EquipmentSlot equipmentSlot) {
        return switch (equipmentSlot) {
            case HEAD, LEGS, CHEST, FEET, BODY, SADDLE -> true;
            default -> false;
        };
    }

    public void init() {
        UniqueSlotHandling.EVENT.register(this);

        SlotValidatorRegistry.register(HEAD_PREDICATE_LOCATION, (EntitySlotValidator) ((entity, level, slotType, slot, stack, buffer) -> isValid(entity, stack, EquipmentSlot.HEAD, buffer)));
        SlotValidatorRegistry.register(CHEST_PREDICATE_LOCATION, (EntitySlotValidator) ((entity, level, slotType, slot, stack, buffer) -> isValid(entity, stack, EquipmentSlot.CHEST, buffer)));
        SlotValidatorRegistry.register(LEGS_PREDICATE_LOCATION,  (EntitySlotValidator) ((entity, level, slotType, slot, stack, buffer) -> isValid(entity, stack, EquipmentSlot.LEGS, buffer)));
        SlotValidatorRegistry.register(FEET_PREDICATE_LOCATION,  (EntitySlotValidator) ((entity, level, slotType, slot, stack, buffer) -> isValid(entity, stack, EquipmentSlot.FEET, buffer)));
        SlotValidatorRegistry.register(ANIMAL_BODY_PREDICATE_LOCATION,  (EntitySlotValidator) ((entity, level, slotType, slot, stack, buffer) -> isValid(entity, stack, EquipmentSlot.BODY, buffer)));
        SlotValidatorRegistry.register(SADDLE_PREDICATE_LOCATION,  (EntitySlotValidator) ((entity, level, slotType, slot, stack, buffer) -> isValid(entity, stack, EquipmentSlot.SADDLE, buffer)));
    }

    @Override
    public void registerSlots(UniqueSlotHandling.UniqueSlotBuilderFactory factory) {
        headSlotReference = factory.create(of("head"), 1)
                .allowTooltipInfo(false)
                .slotPredicates(HEAD_PREDICATE_LOCATION)
                .strictMode(StrictMode.PARTIAL)
                .allowResizing(false)
                .allowEquipFromUse(false)
                .build();

        chestSlotReference = factory.create(of("chest"), 1)
                .allowTooltipInfo(false)
                .slotPredicates(CHEST_PREDICATE_LOCATION)
                .strictMode(StrictMode.PARTIAL)
                .allowResizing(false)
                .allowEquipFromUse(false)
                .build();

        legsSlotReference = factory.create(of("legs"), 1)
                .allowTooltipInfo(false)
                .slotPredicates(LEGS_PREDICATE_LOCATION)
                .strictMode(StrictMode.PARTIAL)
                .allowResizing(false)
                .allowEquipFromUse(false)
                .build();

        feetSlotReference = factory.create(of("feet"), 1)
                .allowTooltipInfo(false)
                .slotPredicates(FEET_PREDICATE_LOCATION)
                .strictMode(StrictMode.PARTIAL)
                .allowResizing(false)
                .allowEquipFromUse(false)
                .build();

        animalBodySlotReference = factory.create(of("animal_body"), 1)
                .allowTooltipInfo(false)
                .slotPredicates(ANIMAL_BODY_PREDICATE_LOCATION)
                .strictMode(StrictMode.PARTIAL)
                .allowResizing(false)
                .allowEquipFromUse(false)
                .build();

        saddleSlotReference = factory.create(of("saddle"), 1)
                .allowTooltipInfo(false)
                .slotPredicates(SADDLE_PREDICATE_LOCATION)
                .strictMode(StrictMode.PARTIAL)
                .allowResizing(false)
                .allowEquipFromUse(false)
                .build();
    }

    private static void isValid(@Nullable LivingEntity livingEntity, ItemStack stack, EquipmentSlot equipmentSlot, ActionResponseBuffer buffer) {
        EquipmentSlot stackEquipmentSlot = null;

        if(livingEntity == null) {
            var equipable = stack.get(DataComponents.EQUIPPABLE);

            if(equipable != null) stackEquipmentSlot = equipable.slot();
        } else {
            stackEquipmentSlot = livingEntity.getEquipmentSlotForItem(stack);
        }

        if (equipmentSlot.equals(stackEquipmentSlot)) {
            buffer.respondWith(ActionResponse.of(true, Component.literal("Given stack fits within '" + equipmentSlot.getName() + "' slot.")));
        }
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
