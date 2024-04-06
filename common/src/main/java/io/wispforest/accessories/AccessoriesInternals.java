package io.wispforest.accessories;

import com.google.gson.JsonObject;
import dev.architectury.injectables.annotations.ExpectPlatform;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesHolder;
import io.wispforest.accessories.networking.AccessoriesNetworkHandler;
import net.minecraft.core.Holder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

/**
 * Util Class implemented though Architectury Plugin allowing for various access to platform specific way
 * of getting class instances
 */
public class AccessoriesInternals {

    /**
     * @return {@link AccessoriesHolder} attached to a given {@link LivingEntity} based on the Platforms method for getting such
     */
    @ExpectPlatform
    public static AccessoriesHolder getHolder(LivingEntity livingEntity){
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void modifyHolder(LivingEntity livingEntity, UnaryOperator<AccessoriesHolder> modifier){
        throw new AssertionError();
    }

    /**
     * @return {@link AccessoriesNetworkHandler} based on the Platforms method for getting such
     */
    @ExpectPlatform
    public static AccessoriesNetworkHandler getNetworkHandler(){
        throw new AssertionError();
    }

    //--

    @ExpectPlatform
    public static <T> Optional<Collection<Holder<T>>> getHolder(TagKey<T> tagKey){
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void giveItemToPlayer(ServerPlayer player, ItemStack stack){
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean isValidOnConditions(JsonObject object){
        throw new AssertionError();
    }

    @ExpectPlatform
    public static Optional<IEventBus> getBus() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T extends AbstractContainerMenu> MenuType<T> registerMenuType(ResourceLocation location, TriFunction<Integer, Inventory, FriendlyByteBuf, T> func){
        throw new AssertionError();
    }

    public static void openAccessoriesMenu(Player player) {
        openAccessoriesMenu(player, null);
    }

    @ExpectPlatform
    public static void openAccessoriesMenu(Player player, @Nullable LivingEntity targetEntity) {
        throw new AssertionError();
    }
}
