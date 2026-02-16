package io.wispforest.accessories.fabric;

import com.google.common.collect.Multimap;
import com.google.gson.JsonObject;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.data.api.EndecDataLoader;
import io.wispforest.accessories.data.api.IdentifiedResourceReloadListener;
import io.wispforest.accessories.impl.core.AccessoriesHolderImpl;
import io.wispforest.accessories.impl.option.AccessoriesPlayerOptionsHolder;
import io.wispforest.accessories.menu.AccessoriesMenuData;
import io.wispforest.accessories.menu.AccessoriesMenuVariant;
import io.wispforest.accessories.menu.variants.AccessoriesMenuBase;
import io.wispforest.endec.Endec;
import io.wispforest.owo.serialization.CodecUtils;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.impl.resource.conditions.ResourceConditionsImpl;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
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
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public class AccessoriesFabricInternals extends AccessoriesInternals {

    public AccessoriesHolderImpl getHolder(LivingEntity livingEntity){
        return livingEntity.getAttachedOrCreate(AccessoriesFabric.HOLDER_ATTACHMENT_TYPE);
    }

    public void modifyHolder(LivingEntity livingEntity, UnaryOperator<AccessoriesHolderImpl> modifier){
        var holder = (AccessoriesHolderImpl) getHolder(livingEntity);

        holder = modifier.apply(holder);

        livingEntity.setAttached(AccessoriesFabric.HOLDER_ATTACHMENT_TYPE, holder);
    }

    public AccessoriesPlayerOptionsHolder getPlayerOptions(Player player) {
        return player.getAttachedOrCreate(AccessoriesFabric.PLAYER_OPTIONS_ATTACHMENT_TYPE);
    }

    public void modifyPlayerOptions(Player player, UnaryOperator<AccessoriesPlayerOptionsHolder> modifier) {
        var options = getPlayerOptions(player);

        options = modifier.apply(options);

        player.setAttached(AccessoriesFabric.PLAYER_OPTIONS_ATTACHMENT_TYPE, options);
    }

    //--

    public void giveItemToPlayer(ServerPlayer player, ItemStack stack) {
        if(stack.isEmpty()) return;

        try(var transaction = Transaction.openOuter()) {
            PlayerInventoryStorage.of(player).offerOrDrop(ItemVariant.of(stack), stack.getCount(), transaction);
            transaction.commit();
        }
    }

    public boolean isValidOnConditions(JsonObject object, String dataType, ResourceLocation key, SimplePreparableReloadListener listener, @Nullable RegistryOps.RegistryInfoLookup registryInfo) {
        return ResourceConditionsImpl.applyResourceConditions(object, dataType, key, registryInfo);
    }

    public <T extends AbstractContainerMenu, D> MenuType<T> registerMenuType(ResourceLocation location, Endec<D> endec, TriFunction<Integer, Inventory, D, T> func){
        return Registry.register(BuiltInRegistries.MENU, location, new ExtendedScreenHandlerType<>(func::apply, CodecUtils.toPacketCodec(endec)));
    }

    public void openAccessoriesMenu(Player player, AccessoriesMenuVariant variant, @Nullable LivingEntity targetEntity, @Nullable ItemStack carriedStack) {
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

    public void addAttributeTooltips(@Nullable Player player, ItemStack stack, Multimap<Holder<Attribute>, AttributeModifier> multimap, Consumer<Component> tooltipAddCallback, TooltipDisplay display, Item.TooltipContext context, TooltipFlag flag) {
        var itemAttributeDisplay = new ItemAttributeModifiers.Display.Default();

        for (Map.Entry<Holder<Attribute>, AttributeModifier> entry : multimap.entries()) {
            itemAttributeDisplay.apply(tooltipAddCallback, player, entry.getKey(), entry.getValue());
        }
    }

    public void registerLoader(PackType packType, IdentifiedResourceReloadListener dataLoader) {
        var loader = ResourceLoader.get(packType);

        var id = dataLoader.getId();

        loader.registerReloader(id, dataLoader);

        for (var dependencyId : dataLoader.getDependencyIds()) {
            loader.addReloaderOrdering(dependencyId, id);
        }

        if (dataLoader instanceof EndecDataLoader<?> endecDataLoader) {
            endecDataLoader.setRegistriesAccess(sharedState -> sharedState.get(ResourceLoader.RELOADER_REGISTRY_LOOKUP_KEY));
        }
    }

    @Override
    public <T> String getTagTranslation(TagKey<T> tagKey) {
        return tagKey.getTranslationKey();
    }

    @Override
    public <T> String geEntryTranslation(Holder<T> entry) {
        var value = entry.value();

        return switch (value) {
            case Item item -> item.getDescriptionId();
            case Block block -> block.getDescriptionId();
            case EntityType<?> type -> type.getDescriptionId();
            case MobEffect effect -> effect.getDescriptionId();
            case Attribute attribute -> attribute.getDescriptionId();
            case Fluid fluid -> {
                var fluidBlock = fluid.defaultFluidState().createLegacyBlock().getBlock();

                // Some non-placeable fluids use air as their fluid block, in that case infer translation key from the fluid id.
                yield fluidBlock == Blocks.AIR
                    ? Util.makeDescriptionId("block", BuiltInRegistries.FLUID.getKey(fluid))
                    : fluidBlock.getDescriptionId();
            }
            default -> value.toString();
        };
    }
}
