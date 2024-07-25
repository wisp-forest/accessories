package io.wispforest.accessories.client;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.api.*;
import io.wispforest.accessories.api.slot.SlotGroup;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.client.gui.AccessoriesInternalSlot;
import io.wispforest.accessories.data.SlotGroupLoader;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.mixin.SlotAccessor;
import io.wispforest.accessories.networking.server.ScreenOpen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class AccessoriesMenu extends AbstractContainerMenu {
    public static final ResourceLocation BLOCK_ATLAS = new ResourceLocation("textures/atlas/blocks.png");

    public static final ResourceLocation EMPTY_ARMOR_SLOT_SHIELD = new ResourceLocation("item/empty_armor_slot_shield");

    private static final Map<EquipmentSlot, ResourceLocation> TEXTURE_EMPTY_SLOTS = Map.of(
            EquipmentSlot.FEET, new ResourceLocation("item/empty_armor_slot_boots"),
            EquipmentSlot.LEGS, new ResourceLocation("item/empty_armor_slot_leggings"),
            EquipmentSlot.CHEST, new ResourceLocation("item/empty_armor_slot_chestplate"),
            EquipmentSlot.HEAD, new ResourceLocation("item/empty_armor_slot_helmet"));

    private static final EquipmentSlot[] SLOT_IDS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private final Player owner;

    @Nullable
    private final LivingEntity targetEntity;

    public int totalSlots = 0;
    public boolean overMaxVisibleSlots = false;

    public int scrolledIndex = 0;

    public float smoothScroll = 0;

    private int maxScrollableIndex = 0;

    private int accessoriesSlotStartIndex = 0;
    private int cosmeticSlotStartIndex = 0;

    private final Set<SlotGroup> validGroups = new HashSet<>();

    private final Map<Integer, Boolean> slotToView = new HashMap<>();

    private Runnable onScrollToEvent = () -> {};

    @Nullable
    private Set<SlotType> usedSlots = null;

    public AccessoriesMenu(int containerId, Inventory inventory, @Nullable LivingEntity targetEntity) {
        super(Accessories.ACCESSORIES_MENU_TYPE, containerId);

        this.owner = inventory.player;
        this.targetEntity = targetEntity;

        var accessoryTarget = targetEntity != null ? targetEntity : owner;

        var capability = AccessoriesCapability.get(accessoryTarget);

        if (capability == null) return;

        //-- Vanilla Slot Setup

        for (int i = 0; i < 4; i++) {
            var equipmentSlot = SLOT_IDS[i];
            ResourceLocation resourceLocation = TEXTURE_EMPTY_SLOTS.get(equipmentSlot);
            this.addSlot(new ArmorSlot(inventory, owner, equipmentSlot, 39 - i, 8, 8 + i * 18, resourceLocation));
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(inventory, j + (i + 1) * 9, 8 + j * 18, 84 + i * 18));
            }
        }

        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(inventory, i, 8 + i * 18, 142));
        }

        this.addSlot(new Slot(inventory, 40, 152, 62) {
            @Override
            public void setByPlayer(ItemStack oldStack) {
                var newStack = this.getItem();
                owner.onEquipItem(EquipmentSlot.OFFHAND, oldStack, newStack);
                super.setByPlayer(oldStack);
            }

            @Override
            public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                return Pair.of(BLOCK_ATLAS, EMPTY_ARMOR_SLOT_SHIELD);
            }
        });

        //--

        if(!this.areUnusedSlotsShown()) {
            this.usedSlots = ImmutableSet.copyOf(AccessoriesAPI.getUsedSlotsFor(targetEntity != null ? targetEntity : owner, owner.getInventory()));
        }

        int minX = -46, maxX = 60, minY = 8, maxY = 152;

        int yIndex = 0;

        this.accessoriesSlotStartIndex = this.slots.size();

        var slotVisibility = new HashMap<Slot, Boolean>();

        var accessoriesSlots = new ArrayList<AccessoriesInternalSlot>();
        var cosmeticSlots = new ArrayList<AccessoriesInternalSlot>();

        var groups = SlotGroupLoader.getGroups(inventory.player.level(), !this.areUniqueSlotsShown());

        var containers = capability.getContainers();

        for (var group : groups.stream().sorted(Comparator.comparingInt(SlotGroup::order).reversed()).toList()) {
            var slotNames = group.slots();

            var slotTypes = slotNames.stream()
                    .map(s -> SlotTypeLoader.getSlotType(owner.level(), s)) // TODO: FILTER NULLS?
                    .sorted(Comparator.comparingInt(SlotType::order).reversed())
                    .toList();

            for (var slot : slotTypes) {
                if(this.usedSlots != null && !this.usedSlots.contains(slot)) continue;

                this.validGroups.add(group);

                var accessoryContainer = containers.get(slot.name());

                if (accessoryContainer == null || accessoryContainer.slotType() == null) continue;

                var size = accessoryContainer.getSize();

                for (int i = 0; i < size; i++) {
                    int currentY = (yIndex * 18) + minY + 8;

                    int currentX = minX;

                    var cosmeticSlot = new AccessoriesInternalSlot(yIndex, accessoryContainer, true, i, currentX, currentY)
                                    .isActive((slot1) -> this.isCosmeticsOpen() && this.slotToView.getOrDefault(slot1.index, true))
                                    .isAccessible(slot1 -> slot1.isCosmetic && isCosmeticsOpen());

                    cosmeticSlots.add(cosmeticSlot);

                    slotVisibility.put(cosmeticSlot, !this.overMaxVisibleSlots);

                    currentX += 18 + 2;

                    var baseSlot = new AccessoriesInternalSlot(yIndex, accessoryContainer, false, i, currentX, currentY)
                                    .isActive(slot1 -> this.slotToView.getOrDefault(slot1.index, true));

                    accessoriesSlots.add(baseSlot);

                    slotVisibility.put(baseSlot, !this.overMaxVisibleSlots);

                    yIndex++;

                    if (!this.overMaxVisibleSlots && currentY + 18 > maxY) this.overMaxVisibleSlots = true;
                }
            }
        }

        for (var accessoriesSlot : accessoriesSlots) {
            this.addSlot(accessoriesSlot);

            slotToView.put(accessoriesSlot.index, slotVisibility.getOrDefault(accessoriesSlot, false));
        }

        this.cosmeticSlotStartIndex = this.slots.size();

        for (var cosmeticSlot : cosmeticSlots) {
            this.addSlot(cosmeticSlot);

            this.slotToView.put(cosmeticSlot.index, slotVisibility.getOrDefault(cosmeticSlot, false));
        }

        this.totalSlots = yIndex;

        this.maxScrollableIndex = this.totalSlots - 8;
    }

    public void setScrollEvent(Runnable event) {
        this.onScrollToEvent = event;
    }

    public boolean scrollTo(int i, boolean smooth) {
        var index = Math.min(Math.max(i, 0), this.maxScrollableIndex);

        if (index == this.scrolledIndex) return false;

        var diff = this.scrolledIndex - index;

        if (!smooth) this.smoothScroll = Mth.clamp(index / (float) this.maxScrollableIndex, 0.0f, 1.0f);

        for (Slot slot : this.slots) {
            if (!(slot instanceof AccessoriesInternalSlot accessoriesSlot)) continue;

            ((SlotAccessor) accessoriesSlot).accessories$setY(accessoriesSlot.y + (diff * 18));

            var menuIndex = accessoriesSlot.menuIndex;

            this.slotToView.put(accessoriesSlot.index, (menuIndex >= index && menuIndex < index + 8));
        }

        this.scrolledIndex = index;

        this.onScrollToEvent.run();

        return true;
    }

    public int maxScrollableIndex(){
        return this.maxScrollableIndex;
    }

    @Nullable
    public LivingEntity targetEntity() {
        return this.targetEntity;
    }

    public Player owner() {
        return this.owner;
    }

    public static AccessoriesMenu of(int containerId, Inventory inventory, AccessoriesMenuData data) {
        var targetEntity = data.targetEntityId().map(i -> {
            var entity = inventory.player.level().getEntity(i);

            if(entity instanceof LivingEntity livingEntity) return livingEntity;

            return null;
        }).orElse(null);

        return new AccessoriesMenu(containerId, inventory, targetEntity);
    }

    public boolean showingSlots() {
        return this.usedSlots == null || !this.usedSlots.isEmpty();
    }

    @Nullable
    public Set<SlotType> usedSlots() {
        return this.usedSlots;
    }

    public Set<SlotGroup> validGroups() {
        return this.validGroups;
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

    public void reopenMenu() {
        AccessoriesInternals.getNetworkHandler().sendToServer(ScreenOpen.of(this.targetEntity));
    }

    //--

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int clickedIndex) {
        final var slots = this.slots;
        final var clickedSlot = slots.get(clickedIndex);
        if (!clickedSlot.hasItem()) return ItemStack.EMPTY;

        ItemStack clickedStack = clickedSlot.getItem();
        var oldStack = clickedStack.copy();
        EquipmentSlot equipmentSlot = Mob.getEquipmentSlotForItem(oldStack);

        int armorSlots = 4;
        int hotbarSlots = 9;
        int invSlots = 27;

        int armorStart = 0;
        int armorEnd = armorStart - 1 + armorSlots;
        int invStart = armorEnd + 1;
        int invEnd = invStart - 1 + invSlots;
        int hotbarStart = invEnd + 1;
        int hotbarEnd = hotbarStart - 1 + hotbarSlots;
        int offhand = hotbarEnd + 1;

        // If the clicked slot isn't an accessory slot
        if (clickedIndex < this.accessoriesSlotStartIndex) {
            // Try to move to accessories
            if (!this.moveItemStackTo(clickedStack, this.accessoriesSlotStartIndex, this.slots.size(), false)) {
                // If the clicked slot is one of the armor slots
                if (clickedIndex >= armorStart && clickedIndex <= armorEnd) {
                    // Try to move to the inventory or hotbar
                    if (!this.moveItemStackTo(clickedStack, invStart, hotbarEnd, false)) {
                        return ItemStack.EMPTY;
                    }
                    // If the clicked slot can go into an armor slot and said armor slot is empty
                } else if (equipmentSlot.getType() == EquipmentSlot.Type.ARMOR && !this.slots.get(armorEnd - equipmentSlot.getIndex()).hasItem()) {
                    // Try to move to the armor slot
                    int targetArmorSlotIndex = armorEnd - equipmentSlot.getIndex();
                    if (!this.moveItemStackTo(clickedStack, targetArmorSlotIndex, targetArmorSlotIndex + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                    // If the clicked slot can go into the offhand slot and the offhand slot is empty
                } else if (equipmentSlot == EquipmentSlot.OFFHAND && !this.slots.get(offhand).hasItem()) {
                    // Try to move to the offhand slot
                    if (!this.moveItemStackTo(clickedStack, offhand, offhand + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                    // If the clicked slot is in the hotbar
                } else if (clickedIndex >= hotbarStart && clickedIndex <= hotbarEnd) {
                    // Try to move to the inventory
                    if (!this.moveItemStackTo(clickedStack, invStart, invEnd, false)) {
                        return ItemStack.EMPTY;
                    }
                    // If the clicked slot is in the inventory
                } else if (clickedIndex >= invStart && clickedIndex <= invEnd) {
                    // Try to move to the hotbar
                    if (!this.moveItemStackTo(clickedStack, hotbarStart, hotbarEnd, false)) {
                        return ItemStack.EMPTY;
                    }
                    // Try to move to the inventory or hotbar
                } else if (!this.moveItemStackTo(clickedStack, invStart, hotbarEnd, false)) {
                    return ItemStack.EMPTY;
                }
            }
        } else if (!this.moveItemStackTo(clickedStack, invStart, hotbarEnd, false)) {
            return ItemStack.EMPTY;
        }

        if (clickedStack.isEmpty()) {
            clickedSlot.setByPlayer(ItemStack.EMPTY);
        } else {
            clickedSlot.setChanged();
        }

        if (clickedStack.getCount() == oldStack.getCount()) {
            return ItemStack.EMPTY;
        }

        clickedSlot.onTake(player, clickedStack);

        return oldStack;
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

                //Check if the slot dose not permit the given amount
                if(slot.getMaxStackSize(itemStack) < itemStack.getCount()) {
                    if (!itemStack.isEmpty() && ItemStack.isSameItemSameTags(stack, itemStack)) {
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

                if (reverseDirection) {
                    --i;
                } else {
                    ++i;
                }
            }
        }

        return bl;
    }

    //--
}