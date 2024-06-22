package io.wispforest.accessories.client;

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
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class AccessoriesMenu extends AbstractContainerMenu {
    public static final ResourceLocation BLOCK_ATLAS = new ResourceLocation("textures/atlas/blocks.png");
    public static final ResourceLocation EMPTY_ARMOR_SLOT_HELMET = new ResourceLocation("item/empty_armor_slot_helmet");
    public static final ResourceLocation EMPTY_ARMOR_SLOT_CHESTPLATE = new ResourceLocation("item/empty_armor_slot_chestplate");
    public static final ResourceLocation EMPTY_ARMOR_SLOT_LEGGINGS = new ResourceLocation("item/empty_armor_slot_leggings");
    public static final ResourceLocation EMPTY_ARMOR_SLOT_BOOTS = new ResourceLocation("item/empty_armor_slot_boots");
    public static final ResourceLocation EMPTY_ARMOR_SLOT_SHIELD = new ResourceLocation("item/empty_armor_slot_shield");
    static final ResourceLocation[] TEXTURE_EMPTY_SLOTS;
    private static final EquipmentSlot[] SLOT_IDS;
    public final boolean active;

    public final Player owner;
    @Nullable
    private final LivingEntity targetEntity;

    public int totalSlots = 0;
    public boolean overMaxVisibleSlots = false;

    public int scrolledIndex = 0;

    public float smoothScroll = 0;

    public int maxScrollableIndex = 0;

    public int accessoriesSlotStartIndex = 0;
    public int cosmeticSlotStartIndex = 0;

    public final Set<SlotGroup> validGroups = new HashSet<>();

    private final Map<Integer, Boolean> slotToView = new HashMap<>();

    public Runnable onScrollToEvent = () -> {};

    @Nullable
    public final LivingEntity targetEntity() {
        return this.targetEntity;
    }

    public static void writeBufData(FriendlyByteBuf buf, @Nullable LivingEntity targetEntity){
        var hasTargetEntity = targetEntity != null;

        buf.writeBoolean(hasTargetEntity);

        if(hasTargetEntity) buf.writeInt(targetEntity.getId());
    }

    @Nullable
    public static LivingEntity readBufData(FriendlyByteBuf buf, Level level) {
        LivingEntity targetEntity = null;

        var hasTargetEntity = buf.readBoolean();

        if(hasTargetEntity && level.getEntity(buf.readInt()) instanceof LivingEntity entity) targetEntity = entity;

        return targetEntity;
    }

    public static AccessoriesMenu of(int containerId, Inventory inventory, boolean active, AccessoriesMenuData data) {
        @Nullable var targetEntity = data.targetEntityId().map(i -> {
            var entity = inventory.player.level().getEntity(i);

            if(entity instanceof LivingEntity livingEntity) return livingEntity;

            return null;
        }).orElse(null);

        return new AccessoriesMenu(containerId, inventory, active, targetEntity);
    }

    @Nullable
    public final Set<SlotType> usedSlots;

    public boolean showingSlots() {
        return this.usedSlots == null || !this.usedSlots.isEmpty();
    }

    public AccessoriesMenu(int containerId, Inventory inventory, boolean active, @Nullable LivingEntity targetEntity) {
        super(Accessories.ACCESSORIES_MENU_TYPE, containerId);

        this.active = active;
        this.owner = inventory.player;

        this.targetEntity = targetEntity;

        //--

        this.usedSlots = !this.areUnusedSlotsShown()
                ? new HashSet<>(AccessoriesAPI.getUsedSlotsFor(targetEntity != null ? targetEntity : owner, owner.getInventory()))
                : null;

        //--

        for (int i = 0; i < 4; ++i) {
            final EquipmentSlot equipmentSlot = SLOT_IDS[i];
            this.addSlot(new Slot(inventory, 39 - i, 8, 8 + i * 18) {
                public void setByPlayer(ItemStack newStack, ItemStack oldStack) {
                    AccessoriesMenu.this.owner.onEquipItem(equipmentSlot, oldStack, newStack);
                    super.setByPlayer(newStack, oldStack);
                }

                public int getMaxStackSize() {
                    return 1;
                }

                public boolean mayPlace(ItemStack stack) {
                    return equipmentSlot == Mob.getEquipmentSlotForItem(stack);
                }

                public boolean mayPickup(Player player) {
                    var itemStack = this.getItem();
                    return !itemStack.isEmpty() && !player.isCreative() && EnchantmentHelper.hasBindingCurse(itemStack) ? false : super.mayPickup(player);
                }

                public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                    return Pair.of(BLOCK_ATLAS, TEXTURE_EMPTY_SLOTS[equipmentSlot.getIndex()]);
                }
            });
        }

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(inventory, j + (i + 1) * 9, 8 + j * 18, 84 + i * 18));
            }
        }

        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(inventory, i, 8 + i * 18, 142));
        }

        this.addSlot(new Slot(inventory, 40, 152, 62) {
            public void setByPlayer(ItemStack newStack, ItemStack oldStack) {
                AccessoriesMenu.this.owner.onEquipItem(EquipmentSlot.OFFHAND, oldStack, newStack);
                super.setByPlayer(newStack, oldStack);
            }

            public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                return Pair.of(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
            }
        });

        var player = inventory.player;

        var accessoryTarget = targetEntity != null ? targetEntity : owner;

        var capability = AccessoriesCapability.get(accessoryTarget);

        if (capability == null) return;

        int slotScale = 18;

        int minX = -46;
        int maxX = 60;
        int minY = 8;
        int maxY = 152;

        int cosmeticPadding = 2;

        int yOffset = 8;

        var containers = capability.getContainers();

        int yIndex = 0;

        this.accessoriesSlotStartIndex = this.slots.size();

        var slotVisibility = new HashMap<Slot, Boolean>();

        var accessoriesSlots = new ArrayList<AccessoriesInternalSlot>();
        var cosmeticSlots = new ArrayList<AccessoriesInternalSlot>();

        var groups = SlotGroupLoader.getGroups(inventory.player.level(), !this.areUniqueSlotsShown());

        for (var group : groups.stream().sorted(Comparator.comparingInt(SlotGroup::order).reversed()).toList()) {
            var slotNames = group.slots();

            var slotTypes = slotNames.stream()
                    .map(s -> SlotTypeLoader.getSlotType(player.level(), s)) // TODO: FILTER NULLS?
                    .sorted(Comparator.comparingInt(SlotType::order).reversed())
                    .toList();

            for (var slot : slotTypes) {
                if(this.usedSlots != null && !this.usedSlots.contains(slot)) {
                    continue;
                }

                this.validGroups.add(group);

                var accessoryContainer = containers.get(slot.name());

                if (accessoryContainer == null || accessoryContainer.slotType() == null) continue;

                var size = accessoryContainer.getSize();

                for (int i = 0; i < size; i++) {
                    int currentY = (yIndex * Math.max(18, slotScale)) + minY + yOffset;

                    int currentX = minX;

                    var cosmeticSlot = new AccessoriesInternalSlot(yIndex, accessoryContainer, true, i, currentX, currentY)
                                    .isActive((slot1) -> this.isCosmeticsOpen() && this.slotToView.getOrDefault(slot1.index, true))
                                    .isAccessible(slot1 -> slot1.isCosmetic && isCosmeticsOpen());

                    cosmeticSlots.add(cosmeticSlot);

                    slotVisibility.put(cosmeticSlot, !this.overMaxVisibleSlots);

                    currentX += slotScale + cosmeticPadding;

                    var baseSlot = new AccessoriesInternalSlot(yIndex, accessoryContainer, false, i, currentX, currentY)
                                    .isActive(slot1 -> this.slotToView.getOrDefault(slot1.index, true));

                    accessoriesSlots.add(baseSlot);

                    slotVisibility.put(baseSlot, !this.overMaxVisibleSlots);

                    yIndex++;

                    if (!this.overMaxVisibleSlots && currentY + Math.max(18, slotScale) > maxY) {
                        this.overMaxVisibleSlots = true;
                    }
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
            clickedSlot.setByPlayer(ItemStack.EMPTY, oldStack);
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
    public boolean stillValid(Player player) {
        return true;
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

    static {
        TEXTURE_EMPTY_SLOTS = new ResourceLocation[]{EMPTY_ARMOR_SLOT_BOOTS, EMPTY_ARMOR_SLOT_LEGGINGS, EMPTY_ARMOR_SLOT_CHESTPLATE, EMPTY_ARMOR_SLOT_HELMET};
        SLOT_IDS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    }

    //--

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