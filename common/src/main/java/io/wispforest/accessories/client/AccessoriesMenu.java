package io.wispforest.accessories.client;

import com.mojang.datafixers.util.Pair;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.api.*;
import io.wispforest.accessories.api.slot.SlotGroup;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.client.gui.AccessoriesInternalSlot;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.data.SlotGroupLoader;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.mixin.AbstractContainerMenuAccessor;
import io.wispforest.accessories.mixin.SlotAccessor;
import io.wispforest.accessories.networking.client.SyncCosmeticsMenuToggle;
import io.wispforest.accessories.networking.client.SyncLinesMenuToggle;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

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
    private final Player owner;

    public int totalSlots = 0;
    public boolean overMaxVisibleSlots = false;

    public int scrolledIndex = 0;

    public float smoothScroll = 0;

    public int maxScrollableIndex = 0;

    public int accessoriesSlotStartIndex = 0;
    public int cosmeticSlotStartIndex = 0;

    private final Map<Integer, Boolean> slotToView = new HashMap<>();

    public Runnable onScrollToEvent = () -> {};

    public AccessoriesMenu(int containerId, Inventory inventory, boolean active, final Player owner) {
        super(Accessories.ACCESSORIES_MENU_TYPE, containerId);

        this.active = active;
        this.owner = owner;

        for (int i = 0; i < 4; ++i) {
            final EquipmentSlot equipmentSlot = SLOT_IDS[i];
            this.addSlot(new Slot(inventory, 39 - i, 8, 8 + i * 18) {
                public void setByPlayer(ItemStack newStack, ItemStack oldStack) {
                    owner.onEquipItem(equipmentSlot, oldStack, newStack);
                    super.setByPlayer(newStack, oldStack);
                }

                public int getMaxStackSize() {
                    return 1;
                }

                public boolean mayPlace(ItemStack stack) {
                    return equipmentSlot == Mob.getEquipmentSlotForItem(stack);
                }

                public boolean mayPickup(Player player) {
                    ItemStack itemStack = this.getItem();
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
                owner.onEquipItem(EquipmentSlot.OFFHAND, oldStack, newStack);
                super.setByPlayer(newStack, oldStack);
            }

            public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                return Pair.of(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
            }
        });


        var player = inventory.player;

        var accessor = (AbstractContainerMenuAccessor) this;

        accessor.accessories$setMenuType(Accessories.ACCESSORIES_MENU_TYPE);
        accessor.accessories$setContainerId(containerId);

        var capability = AccessoriesCapability.get(player);

        var entitySlotTypes = EntitySlotLoader.getEntitySlots(player);

        int slotScale = 18;

        int minX = -46;
        int maxX = 60;
        int minY = 8;
        int maxY = 152;

        int cosmeticPadding = 2;

        int yOffset = 8;

        Map<SlotType, Set<Accessory>> sortAccessories = new HashMap<>();

        for (var item : BuiltInRegistries.ITEM) {
            var accessory = AccessoriesAPI.getAccessory(item);

            if (accessory.isEmpty()) continue;

//            if(item == Items.APPLE){
//                System.out.println("APPLE");
//            }

            for (var value : entitySlotTypes.values()) {
                if (AccessoriesAPI.canInsertIntoSlot(item.getDefaultInstance(), new SlotReference(value.name(), player, 0))) {
                    sortAccessories.computeIfAbsent(value, s -> new HashSet<>()).add(accessory.get());
                }
            }
        }

        if (capability.isEmpty()) return;

        var containers = capability.get().getContainers();

        int yIndex = 0;

        this.accessoriesSlotStartIndex = this.slots.size();

        var slotVisablity = new HashMap<Slot, Boolean>();

        var accessoriesSlots = new ArrayList<AccessoriesInternalSlot>();
        var cosmeticSlots = new ArrayList<AccessoriesInternalSlot>();

        var groups = SlotGroupLoader.INSTANCE.getSharedGroups(inventory.player.level().isClientSide);

        for (var group : groups.stream().sorted(Comparator.comparingInt(SlotGroup::order).reversed()).toList()) {
            var slotNames = group.slots();

            var slotTypes = slotNames.stream()
                    .flatMap(s -> SlotTypeLoader.getSlotType(player.level(), s).stream())
                    .sorted(Comparator.comparingInt(SlotType::order).reversed())
                    .toList();

            for (SlotType slot : slotTypes) {
                var accessoryContainer = containers.get(slot.name());

                if (accessoryContainer == null) continue;

                var slotType = accessoryContainer.slotType();

                if (slotType.isEmpty()) continue;

                var size = accessoryContainer.getSize();

                for (int i = 0; i < size; i++) {
                    int currentY = (yIndex * Math.max(18, slotScale)) + minY + yOffset;

                    int currentX = minX;

                    var cosmeticSlot = new AccessoriesInternalSlot(yIndex, accessoryContainer, true, i, currentX, currentY)
                                    .isActive((slot1) -> this.isCosmeticsOpen() && slotToView.getOrDefault(slot1.index, true))
                                    .isAccessible(slot1 -> slot1.isCosmetic && isCosmeticsOpen());


                    cosmeticSlots.add(cosmeticSlot);

                    slotVisablity.put(cosmeticSlot, !overMaxVisibleSlots);

                    currentX += slotScale + cosmeticPadding;

                    var baseSlot = new AccessoriesInternalSlot(yIndex, accessoryContainer, false, i, currentX, currentY)
                                    .isActive(slot1 -> slotToView.getOrDefault(slot1.index, true));

                    accessoriesSlots.add(baseSlot);

                    slotVisablity.put(baseSlot, !overMaxVisibleSlots);

                    yIndex++;

                    if (!overMaxVisibleSlots && currentY + Math.max(18, slotScale) > maxY) {
                        overMaxVisibleSlots = true;
                    }
                }
            }
        }

        for (var accessoriesSlot : accessoriesSlots) {
            this.addSlot(accessoriesSlot);
            slotToView.put(accessoriesSlot.index, slotVisablity.getOrDefault(accessoriesSlot, false));
        }

        cosmeticSlotStartIndex = this.slots.size();

        for (var cosmeticSlot : cosmeticSlots) {
            this.addSlot(cosmeticSlot);
            slotToView.put(cosmeticSlot.index, slotVisablity.getOrDefault(cosmeticSlot, false));
        }

        totalSlots = yIndex;

        maxScrollableIndex = totalSlots - 8;
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

        onScrollToEvent.run();

        return true;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player.level().isClientSide) return true;

        if (id == 0) {
            AccessoriesInternals.modifyHolder(player, holder -> holder.cosmeticsShown(!isCosmeticsOpen()));

            AccessoriesInternals.getNetworkHandler().sendToPlayer((ServerPlayer) player, new SyncCosmeticsMenuToggle(isCosmeticsOpen()));

            return true;
        }

        if (id == 1) {
            AccessoriesInternals.modifyHolder(player, holder -> holder.linesShown(!areLinesShown()));

            AccessoriesInternals.getNetworkHandler().sendToPlayer((ServerPlayer) player, new SyncLinesMenuToggle(areLinesShown()));

            return true;
        }

        if (this.slots.get(id) instanceof AccessoriesInternalSlot slot) {
            var renderOptions = slot.container.renderOptions();
            renderOptions.set(slot.getContainerSlot(), !slot.container.shouldRender(slot.getContainerSlot()));
            slot.container.markChanged();
        }

        return super.clickMenuButton(player, id);
    }

    public boolean isCosmeticsOpen() {
        return AccessoriesHolder.get(owner).map(AccessoriesHolder::cosmeticsShown).orElse(false);
    }

    public boolean areLinesShown() {
        return AccessoriesHolder.get(owner).map(AccessoriesHolder::linesShown).orElse(false);
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