package io.wispforest.accessories.neoforge;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesHolder;
import io.wispforest.accessories.client.AccessoriesMenu;
import io.wispforest.accessories.impl.AccessoriesHolderImpl;
import io.wispforest.accessories.networking.AccessoriesNetworkHandler;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.network.IContainerFactory;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

public class AccessoriesInternalsImpl {

    public static AccessoriesHolder getHolder(LivingEntity livingEntity){
        return livingEntity.getData(AccessoriesForge.HOLDER_ATTACHMENT_TYPE);
    }

    public static void modifyHolder(LivingEntity livingEntity, UnaryOperator<AccessoriesHolderImpl> modifier){
        var holder = (AccessoriesHolderImpl) getHolder(livingEntity);

        holder = modifier.apply(holder);

        livingEntity.setData(AccessoriesForge.HOLDER_ATTACHMENT_TYPE, holder);
    }

    public static AccessoriesNetworkHandler getNetworkHandler(){
        return AccessoriesForgeNetworkHandler.INSTANCE;
    }

    public static <T> Optional<Collection<Holder<T>>> getHolder(TagKey<T> tagKey){
        return currentContext.map(iContext -> iContext.getTag(tagKey));
    }

    private static Optional<ICondition.IContext> currentContext = Optional.empty();

    public static void setContext(@Nullable ICondition.IContext context){
        currentContext = Optional.ofNullable(context);
    }

    //--

    public static void giveItemToPlayer(ServerPlayer player, ItemStack stack) {
        ItemHandlerHelper.giveItemToPlayer(player, stack);
    }

    public static boolean isValidOnConditions(JsonObject object) {
        return ICondition.conditionsMatched(JsonOps.INSTANCE, object);
    }

    public static Optional<IEventBus> getBus() {
        return Optional.of(NeoForge.EVENT_BUS);
    }

    public static <T extends AbstractContainerMenu> MenuType<T> registerMenuType(ResourceLocation location, TriFunction<Integer, Inventory, FriendlyByteBuf, T> func) {
        return Registry.register(BuiltInRegistries.MENU, location, IMenuTypeExtension.create(func::apply));
    }

    public static void openAccessoriesMenu(Player player, @Nullable LivingEntity targetEntity) {
        player.openMenu(
                new SimpleMenuProvider((i, arg, arg2) -> new AccessoriesMenu(i, arg, true, targetEntity), Component.empty()),
                buf -> AccessoriesMenu.writeBufData(buf, targetEntity));
    }
}
