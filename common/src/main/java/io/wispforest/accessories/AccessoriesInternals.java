package io.wispforest.accessories;

import com.google.common.collect.Multimap;
import com.google.gson.JsonObject;
import io.wispforest.accessories.data.api.IdentifiedResourceReloadListener;
import io.wispforest.accessories.impl.core.AccessoriesHolderImpl;
import io.wispforest.accessories.impl.option.AccessoriesPlayerOptionsHolder;
import io.wispforest.accessories.menu.AccessoriesMenuVariant;
import io.wispforest.accessories.utils.ServiceLoaderUtils;
import io.wispforest.endec.Endec;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.GameRules;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * Util Class implemented though Architectury Plugin allowing for various access to platform specific way
 * of getting class instances
 */
@ApiStatus.Internal
public abstract class AccessoriesInternals {

    public static final AccessoriesInternals INSTANCE = ServiceLoaderUtils.load(AccessoriesInternals.class);

    //--
    @Nullable
    private EquipmentSlot INTERNAL_SLOT = null;

    @Nullable
    private EquipmentSlot.Type INTERNAL_SLOT_TYPE = null;

    public EquipmentSlot getInternalEquipmentSlot() {
        Objects.requireNonNull(INTERNAL_SLOT, "Unable to get internal EquipmentSlot used within Accessories to run some Minecraft methods!");

        return INTERNAL_SLOT;
    }

    public EquipmentSlot.Type getInternalEquipmentSlotType() {
        Objects.requireNonNull(INTERNAL_SLOT_TYPE, "Unable to get internal EquipmentSlot.Type used within Accessories to run some Minecraft methods!");

        return INTERNAL_SLOT_TYPE;
    }

    public void setInternalEquipmentSlot(EquipmentSlot slot) {
        if (INTERNAL_SLOT != null) {
            throw new IllegalStateException("Unable to set internal EquipmentSlot for Accessories as it has already happened!");
        }

        INTERNAL_SLOT = slot;
    }

    public void setInternalEquipmentSlotType(EquipmentSlot.Type type) {
        if (INTERNAL_SLOT_TYPE != null) {
            throw new IllegalStateException("Unable to set internal EquipmentSlot.Type for Accessories as it has already happened!");
        }

        INTERNAL_SLOT_TYPE = type;
    }

    //--

    /**
     * @return {@link AccessoriesHolderImpl} attached to a given {@link LivingEntity} based on the Platforms method for getting it
     */
    public abstract AccessoriesHolderImpl getHolder(LivingEntity livingEntity);

    public abstract void modifyHolder(LivingEntity livingEntity, UnaryOperator<AccessoriesHolderImpl> modifier);

    public abstract AccessoriesPlayerOptionsHolder getPlayerOptions(Player player);

    public abstract void modifyPlayerOptions(Player player, UnaryOperator<AccessoriesPlayerOptionsHolder> modifier);

    //--

    public abstract void giveItemToPlayer(ServerPlayer player, ItemStack stack);

    public abstract boolean isValidOnConditions(JsonObject object, String dataType, ResourceLocation key, SimplePreparableReloadListener listener, @Nullable RegistryOps.RegistryInfoLookup registryInfo);

    public abstract <T extends AbstractContainerMenu, D> MenuType<T> registerMenuType(ResourceLocation location, Endec<D> endec, TriFunction<Integer, Inventory, D, T> func);

    public abstract void openAccessoriesMenu(Player player, AccessoriesMenuVariant variant, @Nullable LivingEntity targetEntity, @Nullable ItemStack carriedStack);

    public abstract void addAttributeTooltips(@Nullable Player player, ItemStack stack, Multimap<Holder<Attribute>, AttributeModifier> multimap, Consumer<Component> tooltipAddCallback, TooltipDisplay display, Item.TooltipContext context, TooltipFlag flag);

    public abstract void registerLoader(PackType type, IdentifiedResourceReloadListener loader);

    //--

    public abstract <T> String getTagTranslation(TagKey<T> tagKey);

    public abstract <T> String geEntryTranslation(Holder<T> entry);
}
