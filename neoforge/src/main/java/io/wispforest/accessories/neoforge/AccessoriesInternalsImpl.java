package io.wispforest.accessories.neoforge;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import io.wispforest.accessories.api.AccessoriesHolder;
import io.wispforest.accessories.client.AccessoriesMenu;
import io.wispforest.accessories.client.AccessoriesMenuData;
import io.wispforest.accessories.endec.RegistriesAttribute;
import io.wispforest.accessories.impl.AccessoriesHolderImpl;
import io.wispforest.accessories.networking.base.BaseNetworkHandler;
import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.format.bytebuf.ByteBufDeserializer;
import io.wispforest.endec.format.bytebuf.ByteBufSerializer;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
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

    public static BaseNetworkHandler getNetworkHandler(){
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

    public static boolean isValidOnConditions(JsonObject object, String dataType, ResourceLocation key, @Nullable HolderLookup.Provider registryLookup) {
        DynamicOps<JsonElement> ops = JsonOps.INSTANCE;

        if(registryLookup != null) ops = registryLookup.createSerializationContext(ops);

        return ICondition.conditionsMatched(ops, object);
    }

    public static <T extends AbstractContainerMenu> MenuType<T> registerMenuType(ResourceLocation location, TriFunction<Integer, Inventory, AccessoriesMenuData, T> func) {
        return Registry.register(BuiltInRegistries.MENU, location, IMenuTypeExtension.create((i, arg, arg2) -> {
            return func.apply(i, arg, AccessoriesMenuData.ENDEC.decodeFully(SerializationContext.attributes(RegistriesAttribute.of(arg2.registryAccess())), ByteBufDeserializer::of, arg2));
        }));
    }

    public static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>, I extends ArgumentTypeInfo<A, T>> I registerCommandArgumentType(ResourceLocation location, Class<A> clazz, I info) {
        ArgumentTypeInfos.registerByClass(clazz, info);

        return Registry.register(BuiltInRegistries.COMMAND_ARGUMENT_TYPE, location, info);
    }

    public static void openAccessoriesMenu(Player player, @Nullable LivingEntity targetEntity, @Nullable ItemStack carriedStack) {
        player.openMenu(
                new SimpleMenuProvider((i, arg, arg2) -> {
                    var menu = new AccessoriesMenu(i, arg, targetEntity);

                    if(carriedStack != null) menu.setCarried(carriedStack);

                    return menu;
                }, Component.empty()),
                buf -> {
                    AccessoriesMenuData.ENDEC.encode(SerializationContext.attributes(RegistriesAttribute.of(buf.registryAccess())), ByteBufSerializer.of(buf), AccessoriesMenuData.of(targetEntity));
                });
    }
}
