package io.wispforest.accessories.menu.variants;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.api.AccessoriesHolder;
import io.wispforest.accessories.api.slot.SlotGroup;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.api.slot.SlotTypeReference;
import io.wispforest.accessories.data.SlotGroupLoader;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.menu.*;
import io.wispforest.owo.client.screens.ScreenUtils;
import io.wispforest.owo.client.screens.SlotGenerator;
import io.wispforest.owo.util.pond.OwoSlotExtension;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SaddleItem;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Stream;

public class AccessoriesExperimentalMenu extends AccessoriesMenuBase {

    private final Map<Integer, Boolean> slotToView = new HashMap<>();

    private final Set<SlotGroup> validGroups = new HashSet<>();

    @Nullable
    private Set<SlotType> usedSlots = null;

    private final List<SlotType> addedSlots = new ArrayList<>();

    private int addedArmorSlots = 0;

    private int startingAccessoriesSlot = 0;

    private boolean includeSaddle = false;

    public static AccessoriesExperimentalMenu of(int containerId, Inventory inventory, AccessoriesMenuData data) {
        var targetEntity = data.targetEntityId().map(i -> {
            var entity = inventory.player.level().getEntity(i);

            if(entity instanceof LivingEntity livingEntity) return livingEntity;

            return null;
        }).orElse(null);

        return new AccessoriesExperimentalMenu(containerId, inventory, targetEntity);
    }

    public AccessoriesExperimentalMenu(int containerId, Inventory inventory, @Nullable LivingEntity targetEntity) {
        super(AccessoriesMenuTypes.EXPERIMENTAL_MENU, containerId, inventory, targetEntity);

        var accessoryTarget = targetEntity != null ? targetEntity : owner;

        var capability = AccessoriesCapability.get(accessoryTarget);

        if (capability == null) return;

        if(!this.areUnusedSlotsShown()) {
            this.usedSlots = ImmutableSet.copyOf(AccessoriesAPI.getUsedSlotsFor(targetEntity != null ? targetEntity : owner, owner.getInventory()));
        }

        //--

        SlotGenerator.begin(this::addSlot, -300, -300)
                .playerInventory(inventory);

        //--

        this.addSlot(new Slot(inventory, 40, -300, -300) {
            @Override
            public void setByPlayer(ItemStack itemStack, ItemStack itemStack2) {
                inventory.player.onEquipItem(EquipmentSlot.OFFHAND, itemStack2, itemStack);
                super.setByPlayer(itemStack, itemStack2);
            }

            @Override
            public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                return Pair.of(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
            }
        });

        var saddleInv = SlotAccessContainer.ofSaddleSlot(accessoryTarget);

        if(saddleInv != null) {
            this.includeSaddle = true;

            var iconPath = targetEntity instanceof Llama ? "container/horse/llama_armor_slot" : "container/horse/saddle_slot" ;

            this.addSlot(
                    new Slot(saddleInv, 0, -300, -300){
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            if(!(stack.getItem() instanceof SaddleItem)) return false;

                            return super.mayPlace(stack);
                        }

                        @Override
                        public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                            return Pair.of(ArmorSlotTypes.SPRITE_ATLAS_LOCATION, ResourceLocation.withDefaultNamespace(iconPath));
                        }
                    }
            );
        }

        startingAccessoriesSlot = this.slots.size();

        var containers = capability.getContainers();

        var validEquipmentSlots = new ArrayList<EquipmentSlot>();

        for (EquipmentSlot value : EquipmentSlot.values()) {
            if (!accessoryTarget.canUseSlot(value)) continue;

            var armorRef = ArmorSlotTypes.getReferenceFromSlot(value);

            if (armorRef == null || containers.get(armorRef.slotName()) == null) continue;

            validEquipmentSlots.add(value);
        }

        for (EquipmentSlot equipmentSlot : validEquipmentSlots.reversed()) {
            if (addArmorSlot(equipmentSlot, accessoryTarget, ArmorSlotTypes.getReferenceFromSlot(equipmentSlot), containers)) {
                addedArmorSlots += 2;
            }
        }

        //--

        var slotTypes = SlotGroupLoader.getGroups(inventory.player.level(), !this.areUniqueSlotsShown()).stream()
                .sorted(Comparator.comparingInt(SlotGroup::order).reversed())
                .flatMap(slotGroup -> {
                    var slots = slotGroup.slots()
                            .stream()
                            .map(slot -> SlotTypeLoader.getSlotType(owner.level(), slot))
                            .filter(slotType -> this.usedSlots == null || this.usedSlots.contains(slotType))
                            .sorted(Comparator.comparingInt(SlotType::order).reversed())
                            .toList();

                    if(slots.isEmpty()) return Stream.of();

                    this.validGroups.add(slotGroup);

                    return slots.stream();
                }).toList();

        for (var slot : slotTypes) {
            this.addedSlots.add(slot);

            var accessoryContainer = containers.get(slot.name());

            if (accessoryContainer == null || accessoryContainer.slotType() == null) continue;

            for (int i = 0; i < accessoryContainer.getSize(); i++) {
                var cosmeticSlot = new AccessoriesInternalSlot(accessoryContainer, true, i, -300, -300)
                        .useCosmeticIcon(false)
                        .isActive((slot1) -> /*this.isCosmeticsOpen() &&*/ this.slotToView.getOrDefault(slot1.index, true))
                        .isAccessible((slot1) -> /*this.isCosmeticsOpen() &&*/ slot1.isCosmetic);

                this.addSlot(cosmeticSlot);

                var baseSlot = new AccessoriesInternalSlot(accessoryContainer, false, i, -300, -300)
                        .isActive(slot1 -> this.slotToView.getOrDefault(slot1.index, true));

                this.addSlot(baseSlot);
            }
        }

        this.addServerboundMessage(ToggledSlots.class, (message) -> {
            message.changedSlotStates().forEach((index, state) -> {
                var slot = ((OwoSlotExtension) this.getSlot(index));

                if(state != slot.owo$getDisabledOverride()) {
                    slot.owo$setDisabledOverride(state);
                }
            });
        });
    }

    private boolean addArmorSlot(EquipmentSlot equipmentSlot, LivingEntity targetEntity, SlotTypeReference armorReference, Map<String, AccessoriesContainer> containers) {
        var location = ArmorSlotTypes.getEmptyTexture(equipmentSlot, targetEntity);

        var armorContainer = containers.get(armorReference.slotName());

        if(armorContainer == null) return false;

        var armorSlot = new AccessoriesArmorSlot(armorContainer, SlotAccessContainer.ofArmor(equipmentSlot, targetEntity), targetEntity, equipmentSlot, 0, -300, -300, location != null ? location.second() : null)
                .setAtlasLocation(location != null ? location.first() : null); // 39 - i

        this.addSlot(armorSlot);

        var cosmeticSlot = new AccessoriesInternalSlot(armorContainer, true, 0, -300, -300){
            @Override
            public @Nullable Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                if(location == null) return null;

                var atlasLocation = location.first();

                if(atlasLocation == null) atlasLocation = ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png");

                return Pair.of(atlasLocation, location.second());
            }
        }.isActive((slot1) -> /*this.isCosmeticsOpen() &&*/ this.slotToView.getOrDefault(slot1.index, true))
                .isAccessible((slot1) -> /*this.isCosmeticsOpen() &&*/ slot1.isCosmetic);

        this.addSlot(cosmeticSlot);

        return true;
    }

    public int startingAccessoriesSlot() {
        return this.startingAccessoriesSlot;
    }

    public int addedArmorSlots() {
        return this.addedArmorSlots;
    }

    public boolean includeSaddle() {
        return this.includeSaddle;
    }

    public boolean areUnusedSlotsShown() {
        return Optional.ofNullable(AccessoriesHolder.get(owner)).map(AccessoriesHolder::showUnusedSlots).orElse(false);
    }

    public boolean areUniqueSlotsShown() {
        return Optional.ofNullable(AccessoriesHolder.get(owner)).map(AccessoriesHolder::showUniqueSlots).orElse(false);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = (Slot)this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack itemStack2 = slot.getItem();
            itemStack = itemStack2.copy();
            EquipmentSlot equipmentSlot = player.getEquipmentSlotForItem(itemStack);
            if (equipmentSlot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && !((Slot)this.slots.get(8 - equipmentSlot.getIndex())).hasItem()) {
                int i = 8 - equipmentSlot.getIndex();
                if (!this.moveItemStackTo(itemStack2, i, i + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (equipmentSlot == EquipmentSlot.OFFHAND && !((Slot)this.slots.get(45)).hasItem()) {
                if (!this.moveItemStackTo(itemStack2, 45, 46, false)) {
                    return ItemStack.EMPTY;
                }
            }
        }

        return ScreenUtils.handleSlotTransfer(this, index, this.startingAccessoriesSlot);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    protected boolean moveItemStackTo(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection) {
        boolean bl = false;
        int i = startIndex;
        if (reverseDirection) {
            i = endIndex - 1;
        }

        if (stack.isStackable()) {
            while(!stack.isEmpty() && (reverseDirection ? i >= startIndex : i < endIndex)) {
                Slot slot = this.slots.get(i);
                ItemStack itemStack = slot.getItem();

                if(slot.isActive()) {
                    //Check if the slot dose not permit the given amount
                    if (slot.getMaxStackSize(itemStack) < itemStack.getCount()) {
                        if (!itemStack.isEmpty() && ItemStack.isSameItemSameComponents(stack, itemStack)) {
                            int j = itemStack.getCount() + stack.getCount();
                            if (j <= stack.getMaxStackSize()) {
                                stack.setCount(0);
                                itemStack.setCount(j);
                                slot.setChanged();
                                bl = true;
                            } else if (itemStack.getCount() < stack.getMaxStackSize()) {
                                stack.shrink(stack.getMaxStackSize() - itemStack.getCount());
                                itemStack.setCount(stack.getMaxStackSize());
                                slot.setChanged();
                                bl = true;
                            }
                        }
                    }
                }

                if (reverseDirection) {
                    --i;
                } else {
                    ++i;
                }
            }
        }

        if (!stack.isEmpty()) {
            if (reverseDirection) {
                i = endIndex - 1;
            } else {
                i = startIndex;
            }

            while(reverseDirection ? i >= startIndex : i < endIndex) {
                Slot slot = this.slots.get(i);

                if(slot.isActive()) {
                    ItemStack itemStack = slot.getItem();
                    if (itemStack.isEmpty() && slot.mayPlace(stack)) {
                        //Use Stack aware form of getMaxStackSize
                        if (stack.getCount() > slot.getMaxStackSize(stack)) {
                            slot.setByPlayer(stack.split(slot.getMaxStackSize(stack)));
                        } else {
                            slot.setByPlayer(stack.split(stack.getCount()));
                        }

                        slot.setChanged();
                        bl = true;
                        break;
                    }
                }

                if (reverseDirection) {
                    --i;
                } else {
                    ++i;
                }
            }
        }

        return bl;
    }

    public record ToggledSlots(Map<Integer, Boolean> changedSlotStates) {}
}
