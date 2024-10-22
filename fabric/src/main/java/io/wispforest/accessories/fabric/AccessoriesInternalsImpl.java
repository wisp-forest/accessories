package io.wispforest.accessories.fabric;

import com.google.common.collect.Multimap;
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.ArgumentType;

import io.wispforest.accessories.impl.AccessoriesHolderImpl;
import io.wispforest.accessories.menu.AccessoriesMenuData;
import io.wispforest.accessories.menu.AccessoriesMenuVariant;
import io.wispforest.accessories.mixin.ItemStackAccessor;
import io.wispforest.endec.Endec;
import io.wispforest.owo.serialization.CodecUtils;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.impl.resource.conditions.ResourceConditionsImpl;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
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
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class AccessoriesInternalsImpl {

    public static AccessoriesHolderImpl getHolder(LivingEntity livingEntity){
        return livingEntity.getAttachedOrCreate(AccessoriesFabric.HOLDER_ATTACHMENT_TYPE);
    }

    public static void modifyHolder(LivingEntity livingEntity, UnaryOperator<AccessoriesHolderImpl> modifier){
        var holder = (AccessoriesHolderImpl) getHolder(livingEntity);

        holder = modifier.apply(holder);

        livingEntity.setAttached(AccessoriesFabric.HOLDER_ATTACHMENT_TYPE, holder);
    }

    public static final AtomicReference<Map<ResourceKey<?>, Map<ResourceLocation, Supplier<List<Holder<?>>>>>> LOADED_TAGS = new AtomicReference<>();

    public static void setTags(List<Registry.PendingTags<?>> tags) {
        Map<ResourceKey<?>, Map<ResourceLocation, Supplier<List<Holder<?>>>>> tagMap = new IdentityHashMap<>();

        for (Registry.PendingTags<?> registryTags : tags) {
            tagMap.put(registryTags.key(), registryTags.lookup().listTags().collect(Collectors.toMap(holder -> holder.key().location(), holder -> () -> (List<Holder<?>>) (Object) holder.stream().toList())));
        }

        if (LOADED_TAGS.getAndSet(tagMap) != null) {
            throw new IllegalStateException("Tags already captured, this should not happen");
        }
    }

    public static <T> Optional<Collection<Holder<T>>> getHolder(TagKey<T> tagKey){
        var tags = LOADED_TAGS.get().get(tagKey.registry());

        if(tags == null) return Optional.empty();

        var holders = tags.get(tagKey.location());

        if(holders == null) return Optional.empty();

        var converted = holders.get()
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

    public static boolean isValidOnConditions(JsonObject object, String dataType, ResourceLocation key, @Nullable RegistryOps.RegistryInfoLookup registryInfo) {
        return ResourceConditionsImpl.applyResourceConditions(object, dataType, key, registryInfo);
    }

    public static <T extends AbstractContainerMenu, D> MenuType<T> registerMenuType(ResourceLocation location, Endec<D> endec, TriFunction<Integer, Inventory, D, T> func){
        return Registry.register(BuiltInRegistries.MENU, location, new ExtendedScreenHandlerType<>(func::apply, CodecUtils.toPacketCodec(endec)));
    }

    public static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>, I extends ArgumentTypeInfo<A, T>> I registerCommandArgumentType(ResourceLocation location, Class<A> clazz, I info) {
        ArgumentTypeRegistry.registerArgumentType(location, clazz, info);

        return info;
    }

    public static void openAccessoriesMenu(Player player, AccessoriesMenuVariant variant, @Nullable LivingEntity targetEntity, @Nullable ItemStack carriedStack) {
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
                return AccessoriesMenuVariant.openMenu(i, inventory, variant, targetEntity, carriedStack);
            }
        });
    }

    public static void addAttributeTooltips(@Nullable Player player, ItemStack stack, Multimap<Holder<Attribute>, AttributeModifier> multimap, Consumer<Component> tooltipAddCallback, Item.TooltipContext context, TooltipFlag flag) {
        for (Map.Entry<Holder<Attribute>, AttributeModifier> entry : multimap.entries()) {
            ((ItemStackAccessor) (Object) ItemStack.EMPTY).accessories$addModifierTooltip(tooltipAddCallback, player, entry.getKey(), entry.getValue());
        }
    }
}
