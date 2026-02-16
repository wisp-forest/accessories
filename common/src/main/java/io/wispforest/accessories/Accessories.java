package io.wispforest.accessories;

import com.google.common.reflect.Reflection;
import com.mojang.logging.LogUtils;
import io.wispforest.accessories.api.action.ActionResponse;
import io.wispforest.accessories.api.action.ActionResponseBuffer;
import io.wispforest.accessories.api.data.AccessoriesTags;
import io.wispforest.accessories.api.events.v2.AllowEntityModificationCallback;
import io.wispforest.accessories.api.tooltip.ComponentBuilder;
import io.wispforest.accessories.commands.AccessoriesCommands;
import io.wispforest.accessories.compat.config.AccessoriesConfig;
import io.wispforest.accessories.criteria.AccessoryChangedCriterion;
import io.wispforest.accessories.data.*;
import io.wispforest.accessories.impl.event.VanillaItemPredicates;
import io.wispforest.accessories.impl.option.AccessoriesPlayerOptionsHolder;
import io.wispforest.accessories.menu.AccessoriesMenuVariant;
import io.wispforest.accessories.menu.ArmorSlotTypes;
import io.wispforest.accessories.mixin.CriteriaTriggersAccessor;
import io.wispforest.accessories.networking.AccessoriesNetworking;
import io.wispforest.accessories.networking.client.ScreenVariantPing;
import io.wispforest.accessories.networking.client.SyncServerOverrideOption;
import io.wispforest.accessories.utils.EndecUtils;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;
import org.slf4j.Logger;

import java.util.function.Consumer;
import java.util.function.Function;

public class Accessories {

    public static final Logger LOGGER = LogUtils.getLogger();

    public static final ResourceLocation SLOT_LOADER_LOCATION = Accessories.of("slot_loader");
    public static final ResourceLocation ENTITY_SLOT_LOADER_LOCATION = Accessories.of("entity_slot_loader");
    public static final ResourceLocation SLOT_GROUP_LOADER_LOCATION = Accessories.of("slot_group_loader");
    public static final ResourceLocation DATA_RELOAD_HOOK = Accessories.of("data_reload_hook");

    public static final boolean DEBUG;

    static {
        boolean debug = AccessoriesLoaderInternals.INSTANCE.isDevelopmentEnv();

        if (System.getProperty("accessories.debug") != null) {
            debug = Boolean.getBoolean("accessories.debug");
        }


        DEBUG = debug;
    }

    public static final String MODID = "accessories";

    public static ResourceLocation of(String path){
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    public static ResourceLocation parseLocationOrDefault(String s){
        var location = ResourceLocation.tryParse(s);

        if (location == null) location = Accessories.of(s);

        return location;
    }

    public static String translationKey(String ...path){
        return MODID + "." + String.join(".", path);
    }

    public static MutableComponent translation(String ...path) {
        return Component.translatable(translationKey(path));
    }

    public static ComponentBuilder translationWithArgs(String ...path) {
        return (args) -> Component.translatable(translationKey(path), args);
    }

    //--

    private static final AccessoriesConfig CONFIG = AccessoriesConfig.createAndLoad(serializationBuilder -> {
        serializationBuilder.addEndec(Vector2i.class, EndecUtils.VECTOR_2_I_ENDEC);
        serializationBuilder.addEndec(AccessoriesPlayerOptionsHolder.class, AccessoriesPlayerOptionsHolder.ENDEC);
    });

    public static AccessoriesConfig config(){
        return CONFIG;
    }

    //--

    public static void askPlayerForVariant(ServerPlayer player) {
        askPlayerForVariant(player, null);
    }

    public static void askPlayerForVariant(ServerPlayer player, @Nullable LivingEntity targetEntity) {
        AccessoriesNetworking.sendToPlayer(player, ScreenVariantPing.of(targetEntity));
    }

    public static boolean attemptOpenScreenPlayer(ServerPlayer player, AccessoriesMenuVariant variant) {
        var result = ProjectileUtil.getHitResultOnViewVector(player, e -> e instanceof LivingEntity, player.entityInteractionRange());

        if(!(result instanceof EntityHitResult entityHitResult)) return false;

        Accessories.openAccessoriesMenu(player, variant, (LivingEntity) entityHitResult.getEntity());

        return true;
    }

    public static void openAccessoriesMenu(Player player, AccessoriesMenuVariant variant, @Nullable LivingEntity targetEntity) {
        openAccessoriesMenu(player, variant, targetEntity, null);
    }

    public static void openAccessoriesMenu(Player player, AccessoriesMenuVariant variant, @Nullable LivingEntity targetEntity, @Nullable ItemStack carriedStack) {
        if(targetEntity != null && !player.equals(targetEntity)) {
            var buffer = new ActionResponseBuffer(false);

            AllowEntityModificationCallback.EVENT.invoker().allowModifications(targetEntity, player, null, buffer);

            if(!buffer.canPerformAction().isValid(false) && !player.hasPermissions(Commands.LEVEL_ADMINS)) return;
        }

        AccessoriesInternals.INSTANCE.openAccessoriesMenu(player, variant, targetEntity, carriedStack);
    }

    //--

    public static AccessoryChangedCriterion ACCESSORY_EQUIPPED;
    public static AccessoryChangedCriterion ACCESSORY_UNEQUIPPED;

    public static void init() {
        Reflection.initialize(SlotTypeLoader.class, SlotGroupLoader.class, EntitySlotLoader.class, CustomRendererLoader.class);

        AccessoriesCommands.init();

        AllowEntityModificationCallback.EVENT.register((target, player, reference, buffer) -> {
            var type = target.getType();

            if(type.is(AccessoriesTags.MODIFIABLE_ENTITY_BLACKLIST)) {
                buffer.respondWith(ActionResponse.of(false, Component.literal("Given entity can not be manged by you!")));
                return;
            }

            var isOwnersPet = (target instanceof OwnableEntity ownableEntity && ownableEntity.getOwner() != null && ownableEntity.getOwner().equals(player));

            if(isOwnersPet || type.is(AccessoriesTags.MODIFIABLE_ENTITY_WHITELIST)) {
                buffer.respondWith(ActionResponse.of(true,
                    isOwnersPet
                        ? Component.literal("Your pet can be managed by you.")
                        : Component.literal("Given entity can be manged by you."))
                );
            }
        });

        ArmorSlotTypes.INSTANCE.init();

        VanillaItemPredicates.init();

        // TODO: remove when proper sync changes go into effect
        var config = config();
        var contentOptions = config().contentOptions;
        var keys = config().keys;
        SyncServerOverrideOption.hookUpdate(contentOptions::subscribeToValidBannerSlots, config, keys.contentOptions_validBannerSlots);
        SyncServerOverrideOption.hookUpdate(contentOptions::subscribeToValidGliderSlots, config, keys.contentOptions_validGliderSlots);
        SyncServerOverrideOption.hookUpdate(contentOptions::subscribeToValidTotemSlots, config, keys.contentOptions_validTotemSlots);
        SyncServerOverrideOption.hookUpdate(contentOptions::subscribeToAllowBannerEquip, config, keys.contentOptions_allowBannerEquip);
        SyncServerOverrideOption.hookUpdate(contentOptions::subscribeToAllowGliderEquip, config, keys.contentOptions_allowGliderEquip);
        SyncServerOverrideOption.hookUpdate(contentOptions::subscribeToAllowTotemEquip, config, keys.contentOptions_allowTotemEquip);
    }

    public static void registerCriteria(){
        ACCESSORY_EQUIPPED = CriteriaTriggersAccessor.accessories$callRegister("accessories:equip_accessory", new AccessoryChangedCriterion());
        ACCESSORY_UNEQUIPPED = CriteriaTriggersAccessor.accessories$callRegister("accessories:unequip_accessory", new AccessoryChangedCriterion());
    }

    //--

    public static <T> T handleIoError(String dataName, Function<ProblemReporter.ScopedCollector, T> function) {
        return handleIoError(() -> dataName, function);
    }

    public static <T> T handleIoError(ProblemReporter.PathElement pathElement, Function<ProblemReporter.ScopedCollector, T> function) {
        try (var scopedCollector = new ProblemReporter.ScopedCollector(pathElement, Accessories.LOGGER)) {
            return function.apply(scopedCollector);
        }
    }

    public static void handleIoError(String dataName, Consumer<ProblemReporter.ScopedCollector> function) {
        handleIoError(() -> dataName, function);
    }

    public static void handleIoError(ProblemReporter.PathElement pathElement, Consumer<ProblemReporter.ScopedCollector> function) {
        try (var scopedCollector = new ProblemReporter.ScopedCollector(pathElement, Accessories.LOGGER)) {
            function.accept(scopedCollector);
        }
    }
}