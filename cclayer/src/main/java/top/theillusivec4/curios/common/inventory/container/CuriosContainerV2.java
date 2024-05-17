package top.theillusivec4.curios.common.inventory.container;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.common.CuriosRegistry;
import top.theillusivec4.curios.mixin.core.AbstractContainerMenuAccessor;

import javax.annotation.Nonnull;
import java.util.List;

public class CuriosContainerV2 extends CuriosContainer {

    private static final ResourceLocation[] ARMOR_SLOT_TEXTURES = new ResourceLocation[] {
            InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS, InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS,
            InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE, InventoryMenu.EMPTY_ARMOR_SLOT_HELMET};

    private static final EquipmentSlot[] VALID_EQUIPMENT_SLOTS = new EquipmentSlot[] {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
            EquipmentSlot.FEET};

    public final ICuriosItemHandler curiosHandler;
    public final Player player;

    private final boolean isLocalWorld;

    private final CraftingContainer craftMatrix = new TransientCraftingContainer(this, 2, 2);
    private final ResultContainer craftResult = new ResultContainer();
    public int currentPage;
    public int totalPages;
    public List<Integer> grid;
    private List<ProxySlot> proxySlots;
    private int moveToPage = -1;
    private int moveFromIndex = -1;
    public boolean hasCosmetics;
    public boolean isViewingCosmetics;
    public int panelWidth;

    public CuriosContainerV2(int windowId, Inventory playerInventory, FriendlyByteBuf packetBuffer) {
        this(windowId, playerInventory);
    }

    public CuriosContainerV2(int windowId, Inventory playerInventory) {
        super(windowId, playerInventory);
        ((AbstractContainerMenuAccessor) this).setMenuType(CuriosRegistry.CURIO_MENU_NEW.get());
        this.player = playerInventory.player;
        this.isLocalWorld = this.player.level().isClientSide;
        this.curiosHandler = CuriosApi.getCuriosInventory(this.player).orElse(null);
        this.resetSlots();
    }

    public void setPage(int page) {
        // NO-OP
    }

    public void resetSlots() {
        // NO-OP
    }

    public void toggleCosmetics() {
        // NO-OP
    }

    @Override
    public void slotsChanged(@Nonnull Container inventoryIn) {
        // NO-OP
    }

    @Override
    public void removed(@Nonnull Player playerIn) {
        // NO-OP
    }

    @Override
    public void setItem(int pSlotId, int pStateId, @Nonnull ItemStack pStack) {
        // NO-OP
    }

    @Override
    public boolean stillValid(@Nonnull Player player) {
        return true;
    }

    @Nonnull
    @Override
    public ItemStack quickMoveStack(@Nonnull Player playerIn, int index) {
        // NO-OP
        return ItemStack.EMPTY;
    }

    protected int findAvailableSlot(ItemStack stack) {
        // NO-OP
        return -1;
    }

    @Nonnull
    @Override
    public RecipeBookType getRecipeBookType() {
        return RecipeBookType.CRAFTING;
    }

    @Override
    public boolean shouldMoveToInventory(int index) {
        return index != this.getResultSlotIndex();
    }

    @Override
    public void fillCraftSlotsStackedContents(@Nonnull StackedContents itemHelperIn) {
        // NO-OP
    }

    @Override
    public void clearCraftingContent() {
        // NO-OP
    }

    @Override
    public boolean recipeMatches(Recipe<? super CraftingContainer> recipeHolder) {
        return recipeHolder.matches(this.craftMatrix, this.player.level());
    }

    @Override
    public int getResultSlotIndex() {
        return 0;
    }

    @Override
    public int getGridWidth() {
        return this.craftMatrix.getWidth();
    }

    @Override
    public int getGridHeight() {
        return this.craftMatrix.getHeight();
    }

    @Override
    public int getSize() {
        return 5;
    }

    public void nextPage() {
        this.setPage(Math.min(this.currentPage + 1, this.totalPages - 1));
    }

    public void prevPage() {
        this.setPage(Math.max(this.currentPage - 1, 0));
    }

    public void checkQuickMove() {
        // NO-OP
    }

    private record ProxySlot(int page, Slot slot) { }
}