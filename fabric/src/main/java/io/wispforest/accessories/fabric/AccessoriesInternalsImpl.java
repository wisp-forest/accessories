package io.wispforest.accessories.fabric;

import com.google.common.collect.Multimap;
import com.google.gson.JsonObject;
import io.wispforest.accessories.data.api.EndecDataLoader;
import io.wispforest.accessories.impl.core.AccessoriesHolderImpl;
import io.wispforest.accessories.impl.option.AccessoriesPlayerOptionsHolder;
import io.wispforest.accessories.menu.AccessoriesMenuData;
import io.wispforest.accessories.menu.AccessoriesMenuVariant;
import io.wispforest.accessories.menu.variants.AccessoriesMenuBase;
import io.wispforest.endec.Endec;
import io.wispforest.owo.serialization.CodecUtils;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.impl.resource.conditions.ResourceConditionsImpl;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
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
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public class AccessoriesInternalsImpl {

    public static AccessoriesHolderImpl getHolder(LivingEntity livingEntity){
        return livingEntity.getAttachedOrCreate(AccessoriesFabric.HOLDER_ATTACHMENT_TYPE);
    }

    public static void modifyHolder(LivingEntity livingEntity, UnaryOperator<AccessoriesHolderImpl> modifier){
        var holder = (AccessoriesHolderImpl) getHolder(livingEntity);

        holder = modifier.apply(holder);

        livingEntity.setAttached(AccessoriesFabric.HOLDER_ATTACHMENT_TYPE, holder);
    }

    public static AccessoriesPlayerOptionsHolder getPlayerOptions(Player player) {
        return player.getAttachedOrCreate(AccessoriesFabric.PLAYER_OPTIONS_ATTACHMENT_TYPE);
    }

    public static void modifyPlayerOptions(Player player, UnaryOperator<AccessoriesPlayerOptionsHolder> modifier) {
        var options = getPlayerOptions(player);

        options = modifier.apply(options);

        player.setAttached(AccessoriesFabric.PLAYER_OPTIONS_ATTACHMENT_TYPE, options);
    }

    //--

    public static void giveItemToPlayer(ServerPlayer player, ItemStack stack) {
        if(stack.isEmpty()) return;

        try(var transaction = Transaction.openOuter()) {
            PlayerInventoryStorage.of(player).offerOrDrop(ItemVariant.of(stack), stack.getCount(), transaction);
            transaction.commit();
        }
    }

    public static boolean isValidOnConditions(JsonObject object, String dataType, ResourceLocation key, SimplePreparableReloadListener listener, @Nullable RegistryOps.RegistryInfoLookup registryInfo) {
        return ResourceConditionsImpl.applyResourceConditions(object, dataType, key, registryInfo);
    }

    public static <T extends AbstractContainerMenu, D> MenuType<T> registerMenuType(ResourceLocation location, Endec<D> endec, TriFunction<Integer, Inventory, D, T> func){
        return Registry.register(BuiltInRegistries.MENU, location, new ExtendedScreenHandlerType<>(func::apply, CodecUtils.toPacketCodec(endec)));
    }

    public static void openAccessoriesMenu(Player player, AccessoriesMenuVariant variant, @Nullable LivingEntity targetEntity, @Nullable ItemStack carriedStack) {
        player.openMenu(new ExtendedScreenHandlerFactory<AccessoriesMenuData>() {
            @Override
            public AccessoriesMenuData getScreenOpeningData(ServerPlayer player) {
                return AccessoriesMenuData.of(targetEntity, ((AccessoriesMenuBase) player.containerMenu));
            }

            @Override
            public Component getDisplayName() { return Component.empty(); }

            @Override
            public boolean shouldCloseCurrentScreen() {
                return false;
            }

            @Nullable
            @Override
            public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
                return AccessoriesMenuVariant.openMenu(i, inventory, variant, targetEntity, carriedStack);
            }
        });
    }

    public static void addAttributeTooltips(@Nullable Player player, ItemStack stack, Multimap<Holder<Attribute>, AttributeModifier> multimap, Consumer<Component> tooltipAddCallback, TooltipDisplay display, Item.TooltipContext context, TooltipFlag flag) {
        var itemAttributeDisplay = new ItemAttributeModifiers.Display.Default();

        for (Map.Entry<Holder<Attribute>, AttributeModifier> entry : multimap.entries()) {
            itemAttributeDisplay.apply(tooltipAddCallback, player, entry.getKey(), entry.getValue());
        }
    }

    public static Function<PreparableReloadListener.SharedState, HolderLookup.@Nullable Provider> registerLoader(PackType packType, EndecDataLoader<?> dataLoader) {
        var loader = ResourceLoader.get(packType);

        var id = dataLoader.getId();

        loader.registerReloader(id, dataLoader);

        for (var dependencyId : dataLoader.getDependencyIds()) {
            loader.addReloaderOrdering(dependencyId, id);
        }

        return sharedState -> sharedState.get(ResourceLoader.RELOADER_REGISTRY_LOOKUP_KEY);
    }
}
