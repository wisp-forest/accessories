package io.wispforest.accessories;

import com.google.common.collect.Multimap;
import com.google.gson.JsonObject;
import dev.architectury.injectables.annotations.ExpectPlatform;
import io.wispforest.accessories.data.api.EndecDataLoader;
import io.wispforest.accessories.impl.core.AccessoriesHolderImpl;
import io.wispforest.accessories.impl.option.AccessoriesPlayerOptionsHolder;
import io.wispforest.accessories.menu.AccessoriesMenuVariant;
import io.wispforest.endec.Endec;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
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
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Util Class implemented though Architectury Plugin allowing for various access to platform specific way
 * of getting class instances
 */
@ApiStatus.Internal
public class AccessoriesInternals {

    @Nullable
    public static EquipmentSlot INTERNAL_SLOT = null;

    @Nullable
    public static EquipmentSlot.Type ACCESSORIES_TYPE = null;

    /**
     * @return {@link AccessoriesHolderImpl} attached to a given {@link LivingEntity} based on the Platforms method for getting it
     */
    @ExpectPlatform
    public static AccessoriesHolderImpl getHolder(LivingEntity livingEntity) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void modifyHolder(LivingEntity livingEntity, UnaryOperator<AccessoriesHolderImpl> modifier) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static AccessoriesPlayerOptionsHolder getPlayerOptions(Player player) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void modifyPlayerOptions(Player player, UnaryOperator<AccessoriesPlayerOptionsHolder> modifier) {
        throw new AssertionError();
    }

    //--

    @ExpectPlatform
    public static void giveItemToPlayer(ServerPlayer player, ItemStack stack) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean isValidOnConditions(JsonObject object, String dataType, ResourceLocation key, SimplePreparableReloadListener listener, @Nullable RegistryOps.RegistryInfoLookup registryInfo) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T extends AbstractContainerMenu, D> MenuType<T> registerMenuType(ResourceLocation location, Endec<D> endec, TriFunction<Integer, Inventory, D, T> func) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void openAccessoriesMenu(Player player, AccessoriesMenuVariant variant, @Nullable LivingEntity targetEntity, @Nullable ItemStack carriedStack) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void addAttributeTooltips(@Nullable Player player, ItemStack stack, Multimap<Holder<Attribute>, AttributeModifier> multimap, Consumer<Component> tooltipAddCallback, TooltipDisplay display, Item.TooltipContext context, TooltipFlag flag) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static Function<PreparableReloadListener.SharedState, HolderLookup.@Nullable Provider> registerLoader(PackType type, EndecDataLoader<?> loader) {
        throw new AssertionError();
    }
}
