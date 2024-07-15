package io.wispforest.accessories.menu.variants;

import com.google.common.collect.ImmutableSet;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesHolder;
import io.wispforest.accessories.api.slot.SlotGroup;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.data.SlotGroupLoader;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.menu.*;
import io.wispforest.owo.client.screens.ScreenUtils;
import io.wispforest.owo.client.screens.SlotGenerator;
import io.wispforest.owo.util.pond.OwoSlotExtension;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Stream;

public class AccessoriesExperimentalMenu extends AccessoriesMenuBase {

    private final Map<Integer, Boolean> slotToView = new HashMap<>();

    private final Set<SlotGroup> validGroups = new HashSet<>();

    @Nullable
    private Set<SlotType> usedSlots = null;

    public final List<SlotType> addedSlots = new ArrayList<>();

    public int startingAccessoriesSlot = 0;

    public AccessoriesExperimentalMenu(int containerId, Inventory inventory, @Nullable LivingEntity targetEntity) {
        super(AccessoriesMenuTypes.EXPERIMENTAL_MENU, containerId, inventory, targetEntity);

        var accessoryTarget = targetEntity != null ? targetEntity : owner;

        var capability = AccessoriesCapability.get(accessoryTarget);

        if (capability == null) return;

        if(!this.areUnusedSlotsShown()) {
            this.usedSlots = ImmutableSet.copyOf(AccessoriesAPI.getUsedSlotsFor(targetEntity != null ? targetEntity : owner, owner.getInventory()));
        }

        //--

        SlotGenerator.begin(this::addSlot, 0, 0)
                .playerInventory(inventory);

        //--

        startingAccessoriesSlot = this.slots.size();

        var armorTypes = ArmorSlotTypes.getArmorReferences();

        var containers = capability.getContainers();

        for (int i = 0; i < 4; i++) {
            var equipmentSlot = ArmorSlotTypes.SLOT_IDS[i];
            var location = ArmorSlotTypes.TEXTURE_EMPTY_SLOTS.get(equipmentSlot);

            var armorReference = armorTypes.get(i);

            var armorContainer = containers.get(armorReference.slotName());

            this.addSlot(new AccessoriesArmorSlot(armorContainer, inventory, owner, equipmentSlot, 39 - i, 0, 0, location)); // 39 - i

            var cosmeticSlot = new AccessoriesInternalSlot(armorContainer, true, 0, 0, 0){
                @Override protected ResourceLocation icon() { return location; }
            }.isActive((slot1) -> /*this.isCosmeticsOpen() &&*/ this.slotToView.getOrDefault(slot1.index, true))
                    .isAccessible((slot1) -> /*this.isCosmeticsOpen() &&*/ slot1.isCosmetic);

            this.addSlot(cosmeticSlot);
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
            addedSlots.add(slot);

            var accessoryContainer = containers.get(slot.name());

            if (accessoryContainer == null || accessoryContainer.slotType() == null) continue;

            for (int i = 0; i < accessoryContainer.getSize(); i++) {
                var cosmeticSlot = new AccessoriesInternalSlot(accessoryContainer, true, i, 0, 0)
                        .useCosmeticIcon(false)
                        .isActive((slot1) -> /*this.isCosmeticsOpen() &&*/ this.slotToView.getOrDefault(slot1.index, true))
                        .isAccessible((slot1) -> /*this.isCosmeticsOpen() &&*/ slot1.isCosmetic);

                this.addSlot(cosmeticSlot);

                var baseSlot = new AccessoriesInternalSlot(accessoryContainer, false, i, 0, 0)
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

    public static AccessoriesExperimentalMenu of(int containerId, Inventory inventory, AccessoriesMenuData data) {
        var targetEntity = data.targetEntityId().map(i -> {
            var entity = inventory.player.level().getEntity(i);

            if(entity instanceof LivingEntity livingEntity) return livingEntity;

            return null;
        }).orElse(null);

        return new AccessoriesExperimentalMenu(containerId, inventory, targetEntity);
    }

    public boolean isCosmeticsOpen() {
        return Optional.ofNullable(AccessoriesHolder.get(owner)).map(AccessoriesHolder::cosmeticsShown).orElse(false);
    }

    public boolean areLinesShown() {
        return Optional.ofNullable(AccessoriesHolder.get(owner)).map(AccessoriesHolder::linesShown).orElse(false);
    }

    public boolean areUnusedSlotsShown() {
        return Optional.ofNullable(AccessoriesHolder.get(owner)).map(AccessoriesHolder::showUnusedSlots).orElse(false);
    }

    public boolean areUniqueSlotsShown() {
        return Optional.ofNullable(AccessoriesHolder.get(owner)).map(AccessoriesHolder::showUniqueSlots).orElse(false);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ScreenUtils.handleSlotTransfer(this, index, this.startingAccessoriesSlot);
        //return ItemStack.EMPTY;
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
