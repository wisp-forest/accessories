package io.wispforest.accessories.neoforge;

import com.google.common.collect.HashMultimap;
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
import io.wispforest.accessories.neoforge.mixin.ContextAwareReloadListenerAccessor;
import io.wispforest.endec.Endec;
import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.format.bytebuf.ByteBufDeserializer;
import io.wispforest.endec.format.bytebuf.ByteBufSerializer;
import io.wispforest.owo.serialization.RegistriesAttribute;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.common.util.AttributeTooltipContext;
import net.neoforged.neoforge.common.util.AttributeUtil;
import net.neoforged.neoforge.event.GatherSkippedAttributeTooltipsEvent;
import org.apache.commons.lang3.function.TriFunction;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public class AccessoriesNeoforgeInternals extends AccessoriesInternals {

    public AccessoriesHolderImpl getHolder(LivingEntity livingEntity){
        return livingEntity.getData(AccessoriesForge.HOLDER_ATTACHMENT_TYPE);
    }

    public void modifyHolder(LivingEntity livingEntity, UnaryOperator<AccessoriesHolderImpl> modifier){
        var holder = getHolder(livingEntity);

        holder = modifier.apply(holder);

        livingEntity.setData(AccessoriesForge.HOLDER_ATTACHMENT_TYPE, holder);
    }

    public AccessoriesPlayerOptionsHolder getPlayerOptions(Player player) {
        return player.getData(AccessoriesForge.PLAYER_OPTIONS_ATTACHMENT_TYPE);
    }

    public void modifyPlayerOptions(Player player, UnaryOperator<AccessoriesPlayerOptionsHolder> modifier) {
        var options = getPlayerOptions(player);

        options = modifier.apply(options);

        player.setData(AccessoriesForge.PLAYER_OPTIONS_ATTACHMENT_TYPE, options);
    }

    //--

    public void giveItemToPlayer(ServerPlayer player, ItemStack stack) {
        // ItemHandlerHelper.giveItemToPlayer(player, stack);
        player.getInventory().placeItemBackInInventory(stack);
    }

    public boolean isValidOnConditions(JsonObject object, String dataType, ResourceLocation key, SimplePreparableReloadListener listener, @Nullable RegistryOps.RegistryInfoLookup registryInfo) {
        return ICondition.conditionsMatched(((ContextAwareReloadListenerAccessor) listener).accessories$makeConditionalOps(), object);
    }

    public <T extends AbstractContainerMenu, D> MenuType<T> registerMenuType(ResourceLocation location, Endec<D> endec, TriFunction<Integer, Inventory, D, T> func) {
        return Registry.register(BuiltInRegistries.MENU, location, IMenuTypeExtension.create((i, arg, arg2) -> {
            return func.apply(i, arg, endec.decodeFully(SerializationContext.attributes(RegistriesAttribute.of(arg2.registryAccess())), ByteBufDeserializer::of, arg2));
        }));
    }

    public void openAccessoriesMenu(Player player, AccessoriesMenuVariant variant, @Nullable LivingEntity targetEntity, @Nullable ItemStack carriedStack) {
        player.openMenu(
                new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Component.empty();
                    }

                    @Override
                    @Nullable
                    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player arg2) {
                        return AccessoriesMenuVariant.openMenu(i, inventory, variant, targetEntity, carriedStack);
                    }

                    @Override
                    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buf) {
                        AccessoriesMenuData.ENDEC.encode(SerializationContext.attributes(RegistriesAttribute.of(buf.registryAccess())), ByteBufSerializer.of(buf), AccessoriesMenuData.of(targetEntity, ((AccessoriesMenuBase) menu)));
                    }
                });
    }

    public void addAttributeTooltips(@Nullable Player player, ItemStack stack, Multimap<Holder<Attribute>, AttributeModifier> multimap, Consumer<Component> tooltipAddCallback, TooltipDisplay display, Item.TooltipContext context, TooltipFlag flag) {
        var neoTooltipCtx = AttributeTooltipContext.of(player, context, display, flag);

        var event = NeoForge.EVENT_BUS.post(new GatherSkippedAttributeTooltipsEvent(stack, neoTooltipCtx));

        if (event.isSkippingAll()) return;

        var modifiers = HashMultimap.create(multimap);

        modifiers.values().removeIf(m -> event.isSkipped(m.id()));

        if (modifiers.isEmpty()) return;

        AttributeUtil.applyTextFor(stack, tooltipAddCallback, modifiers, neoTooltipCtx);
    }

    public static final Map<PackType, Set<IdentifiedResourceReloadListener>> TO_BE_LOADED = new HashMap<>();

    public void registerLoader(PackType type, IdentifiedResourceReloadListener loader) {
        TO_BE_LOADED.computeIfAbsent(type, type1 -> new LinkedHashSet<>()).add(loader);
    }

    @Override
    public <T> String getTagTranslation(TagKey<T> tagKey) {
        return Tags.getTagTranslationKey(tagKey);
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
            case Fluid fluid -> fluid.getFluidType().getDescriptionId();
            default -> value.toString();
        };
    }
}
