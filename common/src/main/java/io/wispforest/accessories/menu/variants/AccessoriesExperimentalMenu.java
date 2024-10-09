package io.wispforest.accessories.menu.variants;

import com.mojang.datafixers.util.Pair;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.api.AccessoriesHolder;
import io.wispforest.accessories.api.menu.AccessoriesBasedSlot;
import io.wispforest.accessories.api.slot.SlotGroup;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.api.slot.SlotTypeReference;
import io.wispforest.accessories.api.slot.UniqueSlotHandling;
import io.wispforest.accessories.data.SlotGroupLoader;
import io.wispforest.accessories.menu.*;
import io.wispforest.accessories.menu.networking.ToggledSlots;
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
import java.util.stream.Collectors;

public class AccessoriesExperimentalMenu extends AccessoriesMenuBase {

    private final Set<SlotType> usedSlots = new HashSet<>();

    private final Set<SlotGroup> selectedGroups = new HashSet<>();

    private final List<AccessoriesBasedSlot> accessoriesSpecificSlots = new ArrayList<>();

    private int addedArmorSlots = 0;

    private int startingAccessoriesSlot = 0;

    private boolean includeSaddle = false;

    public static AccessoriesExperimentalMenu of(int containerId, Inventory inventory, AccessoriesMenuData data) {
        var targetEntity = data.targetEntityId()
                .map(i -> (inventory.player.level().getEntity(i) instanceof LivingEntity livingEntity)
                        ? livingEntity
                        : null
                ).orElse(null);

        return new AccessoriesExperimentalMenu(containerId, inventory, targetEntity);
    }

    public AccessoriesExperimentalMenu(int containerId, Inventory inventory, @Nullable LivingEntity targetEntity) {
        super(AccessoriesMenuTypes.EXPERIMENTAL_MENU, containerId, inventory, targetEntity);

        var accessoryTarget = targetEntity != null ? targetEntity : owner;

        var capability = AccessoriesCapability.get(accessoryTarget);

        if (capability == null) return;

        this.updateUsedSlots();

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
                            return stack.getItem() instanceof SaddleItem && super.mayPlace(stack);
                        }

                        @Override
                        public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                            return Pair.of(ArmorSlotTypes.SPRITE_ATLAS_LOCATION, ResourceLocation.withDefaultNamespace(iconPath));
                        }
                    }
            );
        }

        this.startingAccessoriesSlot = this.slots.size();

        var containers = capability.getContainers();

        var validEquipmentSlots = new ArrayList<EquipmentSlot>();

        for (var value : EquipmentSlot.values()) {
            if (!accessoryTarget.canUseSlot(value)) continue;

            var armorRef = ArmorSlotTypes.getReferenceFromSlot(value);

            if (armorRef == null || containers.get(armorRef.slotName()) == null) continue;

            validEquipmentSlots.add(value);
        }

        for (var equipmentSlot : validEquipmentSlots.reversed()) {
            if (addArmorSlot(equipmentSlot, accessoryTarget, ArmorSlotTypes.getReferenceFromSlot(equipmentSlot), containers)) {
                addedArmorSlots += 2;
            }
        }

        //--

        var validGroupData = SlotGroupLoader.getValidGroups(accessoryTarget);

        var slotTypes = validGroupData.values()
                .stream()
                .flatMap(Collection::stream)
                .toList();

        for (var slot : slotTypes) {
            var accessoryContainer = containers.get(slot.name());

            if (accessoryContainer == null || accessoryContainer.slotType() == null) continue;

            for (int i = 0; i < accessoryContainer.getSize(); i++) {
                var cosmeticSlot = new AccessoriesInternalSlot(accessoryContainer, true, i, -300, -300)
                        .useCosmeticIcon(false);

                this.addSlot(cosmeticSlot);
                this.accessoriesSpecificSlots.add(cosmeticSlot);

                var baseSlot = new AccessoriesInternalSlot(accessoryContainer, false, i, -300, -300);

                this.addSlot(baseSlot);
                this.accessoriesSpecificSlots.add(baseSlot);
            }
        }

        ToggledSlots.initMenu(this);
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
        };

        this.addSlot(cosmeticSlot);

        return true;
    }

    public final LivingEntity targetEntityDefaulted() {
        var targetEntity = this.targetEntity();

        return (targetEntity != null) ? targetEntity : this.owner();
    }

    public int startingAccessoriesSlot() {
        return this.startingAccessoriesSlot;
    }

    public List<AccessoriesBasedSlot> getAccessoriesSlots() {
        return this.accessoriesSpecificSlots;
    }

    public List<Slot> getVisibleAccessoriesSlots() {
        var filteredList = new ArrayList<Slot>();

        var groups = SlotGroupLoader.getValidGroups(this.targetEntityDefaulted());

        var usedSlots = this.getUsedSlots();

        if (usedSlots != null) {
            groups.forEach((group, groupSlots) -> {
                if (groupSlots.stream().noneMatch(usedSlots::contains)) this.removeSelectedGroup(group);
            });
        }

        var selectedGroupedSlots = SlotGroupLoader.getValidGroups(this.targetEntityDefaulted()).entrySet()
                .stream()
                .filter(entry -> this.selectedGroups.isEmpty() || this.selectedGroups.contains(entry.getKey()))
                .flatMap(entry -> entry.getValue().stream())
                .toList();

        for (int i = 0; i < (this.accessoriesSpecificSlots.size() / 2); i++) {
            var cosmetic = (i * 2);
            var accessory = cosmetic + 1;

            var cosmeticSlot = this.accessoriesSpecificSlots.get(cosmetic);
            var accessorySlot = this.accessoriesSpecificSlots.get(accessory);

            var slotType = accessorySlot.slotType();

            var isVisible = (this.usedSlots.isEmpty() || this.usedSlots.contains(slotType))
                    && (selectedGroupedSlots.isEmpty() || selectedGroupedSlots.contains(slotType));

            if(isVisible){
                filteredList.add(cosmeticSlot);
                filteredList.add(accessorySlot);
            }
        }

        return filteredList;
    }

    @Nullable
    public Set<SlotType> getUsedSlots() {
        return this.areUnusedSlotsShown() ? null : this.usedSlots;
    }

    public void updateUsedSlots() {
        this.usedSlots.clear();

        if(!this.areUnusedSlotsShown()) {
            var currentlyUsedSlots = AccessoriesAPI.getUsedSlotsFor(this.targetEntity != null ? this.targetEntity : this.owner, this.owner.getInventory());

            if(!currentlyUsedSlots.isEmpty()) {
                this.usedSlots.addAll(currentlyUsedSlots);
            } else {
                this.usedSlots.add(null);
            }
        }
    }

    public Set<SlotGroup> usedGroups() {
        var groups = SlotGroupLoader.getValidGroups(this.targetEntityDefaulted()).entrySet().stream();

        var usedSlots = this.getUsedSlots();

        groups = groups
                .filter(entry -> {
                    var groupSlots = entry.getValue()
                            .stream()
                            .filter(slotType -> {
                                if (UniqueSlotHandling.isUniqueSlot(slotType.name())) return false;

                                var capability = this.targetEntityDefaulted().accessoriesCapability();

                                if (capability == null) return false;

                                var container = capability.getContainer(slotType);

                                if (container == null) return false;

                                return container.getSize() > 0;
                            })
                            .collect(Collectors.toSet());

                    return !groupSlots.isEmpty() && (usedSlots == null || groupSlots.stream().anyMatch(usedSlots::contains));
                });

        return groups.map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    public Set<SlotGroup> selectedGroups() {
        return this.selectedGroups;
    }

    public boolean isGroupSelected(SlotGroup slotGroup) {
        return this.selectedGroups.contains(slotGroup);
    }

    public void addSelectedGroup(SlotGroup slotGroup) {
        this.selectedGroups.add(slotGroup);

        if (this.selectedGroups.containsAll(usedGroups())) {
            this.selectedGroups.clear();
        }
    }

    public void removeSelectedGroup(SlotGroup slotGroup) {
        this.selectedGroups.remove(slotGroup);
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

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        var slot = this.slots.get(index);

        if(!slot.hasItem()) return ItemStack.EMPTY;

        var itemStack2 = slot.getItem();
        var itemStack = itemStack2.copy();

        // 0 1 2 3 : 6 - 7 / 4 - 5 / 2 - 3 / 0 - 1
        var equipmentSlot = player.getEquipmentSlotForItem(itemStack);
        int bottomEquipmentIndex = 8 - ((equipmentSlot.getIndex() + 1) * 2);
        int topEquipmentIndex = bottomEquipmentIndex + 1;

        var upperInventorySize = this.startingAccessoriesSlot;

        /*
         * Player Indies
         *       0: Result slot
         *  1 -  5: Crafting Grid
         *  5 - 41: Player Inv
         *      41: Offhand Slot
         * 41 - 49: Armor Slots
         * 49 -   : Accessories Slots
         */

        if (index == 0) {
            if (!this.moveItemStackTo(itemStack2, 5, 41, true)) return ItemStack.EMPTY;

            slot.onQuickCraft(itemStack2, itemStack);
        }
        else if (index >= 1 && index < 5) {
            if (!this.moveItemStackTo(itemStack2, 5, 41, false)) return ItemStack.EMPTY;
        }
        else if(index >= upperInventorySize) {
            if (!moveItemStackTo(itemStack2, 5, 41, false)) return ItemStack.EMPTY;
        }
        else if (equipmentSlot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && (!this.slots.get(42 + bottomEquipmentIndex).hasItem() || !this.slots.get(42 + topEquipmentIndex).hasItem())) {
            if(!this.moveItemStackTo(itemStack2, 42 + bottomEquipmentIndex, 42 + topEquipmentIndex + 1, false)) return ItemStack.EMPTY;
        }
        else if (equipmentSlot == EquipmentSlot.OFFHAND && !this.slots.get(45).hasItem()) {
            if(!this.moveItemStackTo(itemStack2, 41, 42, false)) return ItemStack.EMPTY;
        } else {
            boolean changeOccured = false;

            if (canMoveToAccessorySlot(itemStack2, this.targetEntityDefaulted()) && index < this.startingAccessoriesSlot) {
                moveItemStackTo(itemStack2, upperInventorySize, slots.size(), false);

                if (itemStack2.getCount() != itemStack.getCount() || itemStack2.isEmpty()) {
                    changeOccured = true;
                }
            }

            if(!changeOccured) {
                if (index >= 5 && index < 32) {
                    if (!this.moveItemStackTo(itemStack2, 32, 41, false)) return ItemStack.EMPTY;
                } else if (index >= 32 && index < 41) {
                    if (!this.moveItemStackTo(itemStack2, 5, 32, false)) return ItemStack.EMPTY;
                }
            }
        }

        if (itemStack2.getCount() == itemStack.getCount()) return ItemStack.EMPTY;

        if (itemStack2.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY, itemStack);
        } else {
            slot.setChanged();
        }

        if (itemStack2.getCount() == itemStack.getCount()) return ItemStack.EMPTY;

        slot.onTake(player, itemStack2);

        if (index == 0) player.drop(itemStack2, false);

        return itemStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    protected boolean canMoveToAccessorySlot(ItemStack stack, LivingEntity living) {
        var capability = living.accessoriesCapability();

        if (capability == null) return false;

        var validSlotTypes = AccessoriesAPI.getStackSlotTypes(living, stack);

        for (var slot : this.slots.subList(this.startingAccessoriesSlot, this.slots.size())) {
            if (slot instanceof SlotTypeAccessible accessible && validSlotTypes.contains(accessible.slotType())) return true;
        }

        return false;
    }

    protected boolean moveItemStackTo(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection) {
        boolean bl = false;
        int i = reverseDirection ? endIndex - 1 : startIndex;

        if (stack.isStackable()) {
            while (!stack.isEmpty() && (reverseDirection ? i >= startIndex : i < endIndex)) {
                var slot = this.slots.get(i);

                if (slot.isActive()) {
                    var itemStack = slot.getItem();

                    if (!itemStack.isEmpty() && ItemStack.isSameItemSameComponents(stack, itemStack)) {
                        int j = itemStack.getCount() + stack.getCount();
                        int k = slot.getMaxStackSize(itemStack);

                        if (j <= k) {
                            stack.setCount(0);
                            itemStack.setCount(j);
                            slot.setChanged();
                            bl = true;
                        } else if (itemStack.getCount() < k) {
                            stack.shrink(k - itemStack.getCount());
                            itemStack.setCount(k);
                            slot.setChanged();
                            bl = true;
                        }
                    }
                }

                i += (reverseDirection) ? -1 : 1;
            }
        }

        if (!stack.isEmpty()) {
            i = reverseDirection ? endIndex - 1 : startIndex;

            while (reverseDirection ? i >= startIndex : i < endIndex) {
                var slot = this.slots.get(i);

                if(slot.isActive()) {
                    var itemStack = slot.getItem();

                    if (itemStack.isEmpty() && slot.mayPlace(stack)) {
                        int j = slot.getMaxStackSize(stack);

                        slot.setByPlayer(stack.split(Math.min(stack.getCount(), j)));
                        slot.setChanged();

                        bl = true;
                        break;
                    }
                }

                i += (reverseDirection) ? -1 : 1;
            }
        }

        return bl;
    }

}
