package io.wispforest.accessories.menu.variants;

import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.api.menu.AccessoriesBasedSlot;
import io.wispforest.accessories.api.slot.*;
import io.wispforest.accessories.api.slot.validator.SlotValidatorRegistry;
import io.wispforest.accessories.data.SlotGroupLoader;
import io.wispforest.accessories.impl.core.ExpandedContainer;
import io.wispforest.accessories.impl.option.AccessoriesPlayerOptionsHolder;
import io.wispforest.accessories.impl.option.PlayerOptions;
import io.wispforest.accessories.menu.*;
import io.wispforest.accessories.menu.networking.ToggledSlots;
import io.wispforest.owo.client.screens.SlotGenerator;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.ticks.ContainerSingleItem;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class AccessoriesMenu extends AccessoriesMenuBase {

    private final Set<SlotType> usedSlots = new HashSet<>();

    private final Set<SlotGroup> selectedGroups = new HashSet<>();

    private final List<AccessoriesBasedSlot> accessoriesSpecificSlots = new ArrayList<>();

    private int addedArmorSlots = 0;

    private int startArmorSlots = 0;
    private int startingAccessoriesSlot = 0;

    public static AccessoriesMenu of(int containerId, Inventory inventory, AccessoriesMenuData data) {
        var targetEntity = data.targetEntityId()
                .map(i -> (inventory.player.level().getEntity(i) instanceof LivingEntity livingEntity)
                        ? livingEntity
                        : null
                ).orElse(null);

        var menu = new AccessoriesMenu(containerId, inventory, targetEntity, data.carriedStack())
                .isSyncedWithServer(data.slotAmountAdded());

        return (AccessoriesMenu) menu;
    }

    public AccessoriesMenu(int containerId, Inventory inventory, @Nullable LivingEntity targetEntity, @Nullable ItemStack carriedStack) {
        super(AccessoriesMenuTypes.PRIAMRY_MENU, containerId, inventory, 2, 2, targetEntity);

        if(carriedStack != null) this.setCarried(carriedStack);

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
            public ResourceLocation getNoItemIcon() {
                return InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD;
            }
        });

        //--

        this.startArmorSlots = this.slots.size();

        var containers = capability.getContainers();

        var validEquipmentSlots = new ArrayList<Pair<EquipmentSlot, SlotTypeReference>>();

        for (var value : EquipmentSlot.values()) {
            if (!accessoryTarget.canUseSlot(value)) continue;

            var armorRef = ArmorSlotTypes.getReferenceFromSlot(value);

            if (armorRef == null || containers.get(armorRef.slotName()) == null) continue;

            validEquipmentSlots.add(Pair.of(value, armorRef));
        }

        for (var pair : validEquipmentSlots.reversed()) {
            if (addArmorSlot(pair.left(), accessoryTarget, pair.right(), containers)) addedArmorSlots += 2;
        }

        this.startingAccessoriesSlot = this.slots.size();

        //--

        var validGroupData = SlotGroupLoader.getValidGroups(accessoryTarget);

        var slotTypes = validGroupData.values()
                .stream()
                .flatMap(Collection::stream)
                .toList();

        for (var slot : slotTypes) {
            var container = containers.get(slot.name());

            if (container == null || container.slotType() == null) continue;

            for (int i = 0; i < container.getSize(); i++) {
                var cosmeticSlot = new AccessoriesBasedSlot(container, container.getCosmeticAccessories(), i, -300, -300)
                    .ownerPlayer(this::owner);

                this.addSlot(cosmeticSlot);
                this.accessoriesSpecificSlots.add(cosmeticSlot);

                var baseSlot = new AccessoriesBasedSlot(container, container.getAccessories(), i, -300, -300)
                    .ownerPlayer(this::owner);

                this.addSlot(baseSlot);
                this.accessoriesSpecificSlots.add(baseSlot);
            }
        }

        ToggledSlots.initMenu(this);

        this.slotAmountAdded = this.slots.size() - this.startArmorSlots;
    }

    private static Container createEquipmentSlotContainer(LivingEntity living, EquipmentSlot equipmentSlot) {
        return new ContainerSingleItem() {
            @Override
            public ItemStack getTheItem() {
                return living.getItemBySlot(equipmentSlot);
            }

            @Override
            public void setTheItem(ItemStack item) {
                living.setItemSlot(equipmentSlot, item);
                if (!item.isEmpty() && living instanceof Mob mob) {
                    mob.setGuaranteedDrop(equipmentSlot);
                    mob.setPersistenceRequired();
                }
            }

            @Override
            public boolean stillValid(Player player) {
                return player.getVehicle() == living || player.canInteractWithEntity(living, 4.0);
            }

            @Override public void setChanged() {}
        };
    }

    private boolean addArmorSlot(EquipmentSlot equipmentSlot, LivingEntity targetEntity, SlotTypeReference armorReference, Map<String, AccessoriesContainer> containers) {
        var location = ArmorSlotTypes.getEmptyTexture(equipmentSlot, targetEntity);

        var container = containers.get(armorReference.slotName());

        if(container == null) return false;

        var armorSlot = new AccessoriesArmorSlot(container, SlotAccessContainer.ofArmor(equipmentSlot, targetEntity), targetEntity, equipmentSlot, 0, -300, -300, location);

        this.addSlot(armorSlot);

        var cosmeticSlot = new AccessoriesBasedSlot(container, container.getCosmeticAccessories(), 0, -300, -300){
            @Override
            public @Nullable ResourceLocation getNoItemIcon() {
                return location;
            }
        }.ownerPlayer(this::owner);

        this.addSlot(cosmeticSlot);

        return true;
    }

    public final LivingEntity targetEntityDefaulted() {
        var targetEntity = this.targetEntity();

        return (targetEntity != null) ? targetEntity : this.owner();
    }

    public int startingAccessoriesSlot() {
        return this.startArmorSlots;
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
            var entity = this.targetEntity != null ? this.targetEntity : this.owner;

            var currentlyUsedSlots = AccessoriesCapability.getUsedSlotsFor(entity, this.owner.getInventory());

            currentlyUsedSlots.addAll(SlotValidatorRegistry.getValidSlotTypes(entity, this.getCarried()));

            if(!currentlyUsedSlots.isEmpty()) {
                this.usedSlots.addAll(currentlyUsedSlots);
            } else {
                this.usedSlots.add(null);
            }
        }
    }

    private static Set<SlotGroup> usedGroups(LivingEntity targetEntity, Set<SlotType> usedSlots) {
        var groups = SlotGroupLoader.getValidGroups(targetEntity).entrySet().stream();

        groups = groups
                .filter(entry -> {
                    var groupSlots = entry.getValue()
                            .stream()
                            .filter(slotType -> {
                                if (UniqueSlotHandling.isUniqueSlot(slotType.name())) return false;

                                var capability = targetEntity.accessoriesCapability();

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

    public boolean isGroupSelected(SlotGroup group) {
        return this.selectedGroups.contains(group);
    }

    public void toggleSelectedGroup(SlotGroup group) {
        if(isGroupSelected(group)) {
            removeSelectedGroup(group);
        } else {
            addSelectedGroup(group);
        }
    }

    public void addSelectedGroup(SlotGroup group) {
        this.selectedGroups.add(group);

        if (this.selectedGroups.containsAll(usedGroups(this.targetEntityDefaulted(), this.getUsedSlots()))) {
            this.selectedGroups.clear();
        }
    }

    public void removeSelectedGroup(SlotGroup group) {
        this.selectedGroups.remove(group);
    }

    //--

    public int addedArmorSlots() {
        return this.addedArmorSlots;
    }

    public boolean areUnusedSlotsShown() {
        return AccessoriesPlayerOptionsHolder.getOptions(owner).getDefaultedData(PlayerOptions.SHOW_UNUSED_SLOTS);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
    }

    private int stackIndex = -1;

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        this.stackIndex = index;

        var stack = quickMoveStackInternal(player, index);

        this.stackIndex = -1;

        return stack;
    }

    private ItemStack quickMoveStackInternal(Player player, int index) {
        var slot = this.slots.get(index);

        if(!slot.hasItem()) return ItemStack.EMPTY;

        var itemStack2 = slot.getItem();
        var itemStack = itemStack2.copy();

        // 0 1 2 3 : 6 - 7 / 4 - 5 / 2 - 3 / 0 - 1
        var equipmentSlot = targetEntity.getEquipmentSlotForItem(itemStack);
        int bottomArmorIndex = 42 + (this.addedArmorSlots - ((equipmentSlot.getIndex() + 1) * 2));
        int topArmorIndex = bottomArmorIndex + 1;

        var upperInventorySize = this.startingAccessoriesSlot;

        /*
         * Player Indies
         *       0: Result slot
         *  1 -  5: Crafting Grid
         *  5 - 41: Player Inv
         *      41: Offhand Slot
         * 41 - (41 - 51): Armor Slots
         * (41 - 51) -   : Accessories Slots
         */

        if (index == 0) { // If from Crafting Result move to player inventory
            if (!this.moveItemStackTo(itemStack2, 5, 41, true)) return ItemStack.EMPTY;

            slot.onQuickCraft(itemStack2, itemStack);
        } else if ((index >= 1 && index < 5) || (index >= upperInventorySize) || Objects.equals(41, index) || (index >= 42)) { // If from Crafting Grid move to player inventory
            if (!this.moveItemStackTo(itemStack2, 5, 41, false)) return ItemStack.EMPTY;
        } else if (equipmentSlot.isArmor() && !this.slots.get(bottomArmorIndex).hasItem()) {
            if(!this.moveItemStackTo(itemStack2, bottomArmorIndex, topArmorIndex, false)) return ItemStack.EMPTY;
        } else if (equipmentSlot == EquipmentSlot.OFFHAND && !this.slots.get(41).hasItem()) {
            if(!this.moveItemStackTo(itemStack2, 41, 42, false)) return ItemStack.EMPTY;
        }
        else {
            boolean changeOccured = false;

            if (canMoveToAccessorySlot(itemStack2, this.targetEntityDefaulted())) {
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

        var validSlotTypes = SlotValidatorRegistry.getStackSlotTypes(living, stack);

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

                            // PATCH TO ATTEMPT TO PRESERVE THE INDEX
                            if (stack.isEmpty() && this.stackIndex != -1) {
                                var prevSlot = this.slots.get(this.stackIndex);

                                if (prevSlot.container instanceof ExpandedContainer simpleContainer) {
                                    simpleContainer.setPreviousItem(prevSlot.index, itemStack);
                                }
                            }
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

                        var newStack = stack.split(Math.min(stack.getCount(), j));

                        slot.setByPlayer(newStack);
                        slot.setChanged();

                        // PATCH TO ATTEMPT TO PRESERVE THE INDEX
                        if (stack.isEmpty() && this.stackIndex != -1) {
                            var prevSlot = this.slots.get(this.stackIndex);

                            if (prevSlot.container instanceof ExpandedContainer simpleContainer) {
                                simpleContainer.setPreviousItem(prevSlot.getContainerSlot(), newStack);
                            }
                        }

                        bl = true;
                        break;
                    }
                }

                i += (reverseDirection) ? -1 : 1;
            }
        }

        return bl;
    }

    //initializeContents

    // REQUIRED TO PREVENT THE MENU FROM RESETTING THE CACHE WITH STACKS THAT ARE ALREADY SYNCED TO THE CLIENT
    // SINCE ACCESSORIES CONTAINERS ARE FULLY SYNCED
    @Override
    public void initializeContents(int stateId, List<ItemStack> items, ItemStack carried) {
        if (!this.isValidMenu()) return;

        for(int i = 0; i < items.size(); ++i) {
            var slot = this.getSlot(i);

            if (slot instanceof SlotTypeAccessible) continue;

            slot.set(items.get(i));
        }

        super.initializeContents(stateId, List.of(), carried);
    }
}
