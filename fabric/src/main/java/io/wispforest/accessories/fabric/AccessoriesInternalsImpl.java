package io.wispforest.accessories.fabric;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.ArgumentType;
import io.wispforest.accessories.api.AccessoriesHolder;
import io.wispforest.accessories.client.AccessoriesMenu;
import io.wispforest.accessories.client.AccessoriesMenuData;
import io.wispforest.accessories.endec.CodecUtils;
import io.wispforest.accessories.impl.AccessoriesHolderImpl;
import io.wispforest.accessories.networking.base.BaseNetworkHandler;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.impl.resource.conditions.ResourceConditionsImpl;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class AccessoriesInternalsImpl {

    public static boolean isDevelopmentEnv() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    public static AccessoriesHolder getHolder(LivingEntity livingEntity){
        return livingEntity.getAttachedOrCreate(AccessoriesFabric.HOLDER_ATTACHMENT_TYPE);
    }

    public static void modifyHolder(LivingEntity livingEntity, UnaryOperator<AccessoriesHolderImpl> modifier){
        var holder = (AccessoriesHolderImpl) getHolder(livingEntity);

        holder = modifier.apply(holder);

        livingEntity.setAttached(AccessoriesFabric.HOLDER_ATTACHMENT_TYPE, holder);
    }

    public static BaseNetworkHandler getNetworkHandler(){
        return AccessoriesFabricNetworkHandler.INSTANCE;
    }

    public static final ThreadLocal<Map<ResourceKey<?>, Map<ResourceLocation, Collection<Holder<?>>>>> LOADED_TAGS = new ThreadLocal<>();

    public static void setTags(List<TagManager.LoadResult<?>> tags) {
        Map<ResourceKey<?>, Map<ResourceLocation, Collection<Holder<?>>>> tagMap = new IdentityHashMap<>();

        for (TagManager.LoadResult<?> registryTags : tags) {
            tagMap.put(registryTags.key(), (Map) registryTags.tags());
        }

        LOADED_TAGS.set(tagMap);
    }

    public static <T> Optional<Collection<Holder<T>>> getHolder(TagKey<T> tagKey){
        var tags = LOADED_TAGS.get().get(tagKey.registry());

        if(tags == null) return Optional.empty();

        var holders = tags.get(tagKey.location());

        if(holders == null) return Optional.empty();

        var converted = holders
                .stream()
                .map(holder -> (Holder<T>) holder)
                .collect(Collectors.toUnmodifiableSet());

        return Optional.of(converted);
    }

    //--

    public static void giveItemToPlayer(ServerPlayer player, ItemStack stack) {
        if(stack.isEmpty()) return;

        try(var transaction = Transaction.openOuter()) {
            PlayerInventoryStorage.of(player).offerOrDrop(ItemVariant.of(stack), stack.getCount(), transaction);
            transaction.commit();
        }
    }

    public static boolean isValidOnConditions(JsonObject object, String dataType, ResourceLocation key, @Nullable HolderLookup.Provider registryLookup) {
        return ResourceConditionsImpl.applyResourceConditions(object, dataType, key, registryLookup);
    }

    public static <T extends AbstractContainerMenu> MenuType<T> registerMenuType(ResourceLocation location, TriFunction<Integer, Inventory, AccessoriesMenuData, T> func) {
        return Registry.register(BuiltInRegistries.MENU, location, new ExtendedScreenHandlerType<>(func::apply, CodecUtils.packetCodec(AccessoriesMenuData.ENDEC)));
    }

    public static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>, I extends ArgumentTypeInfo<A, T>> I registerCommandArgumentType(ResourceLocation location, Class<A> clazz, I info) {
        ArgumentTypeRegistry.registerArgumentType(location, clazz, info);

        return info;
    }

    public static void openAccessoriesMenu(Player player, @Nullable LivingEntity targetEntity, @Nullable ItemStack carriedStack) {
        player.openMenu(new ExtendedScreenHandlerFactory<AccessoriesMenuData>() {
            @Override
            public AccessoriesMenuData getScreenOpeningData(ServerPlayer player) {
                return AccessoriesMenuData.of(targetEntity);
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
                var menu = new AccessoriesMenu(i, inventory, targetEntity);

                if(carriedStack != null) menu.setCarried(carriedStack);

                return menu;
            }
        });
    }
}
