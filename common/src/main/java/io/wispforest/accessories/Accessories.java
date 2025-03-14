package io.wispforest.accessories;

import io.wispforest.accessories.api.data.AccessoriesTags;
import io.wispforest.accessories.api.events.AllowEntityModificationCallback;
import io.wispforest.accessories.criteria.AccessoryChangedCriterion;
import io.wispforest.accessories.impl.VanillaItemPredicates;
import io.wispforest.accessories.menu.AccessoriesMenuVariant;
import io.wispforest.accessories.menu.ArmorSlotTypes;
import io.wispforest.accessories.mixin.CriteriaTriggersAccessor;
import io.wispforest.accessories.networking.AccessoriesNetworking;
import io.wispforest.accessories.networking.client.ScreenVariantPing;
import io.wispforest.accessories.utils.EndecUtils;
import net.fabricmc.fabric.api.util.TriState;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;

public class Accessories {

    public static final ResourceLocation SLOT_LOADER_LOCATION = Accessories.of("slot_loader");
    public static final ResourceLocation ENTITY_SLOT_LOADER_LOCATION = Accessories.of("entity_slot_loader");
    public static final ResourceLocation SLOT_GROUP_LOADER_LOCATION = Accessories.of("slot_group_loader");
    public static final ResourceLocation DATA_RELOAD_HOOK = Accessories.of("data_reload_hook");

    public static final boolean DEBUG;

    static {
        boolean debug = AccessoriesLoaderInternals.isDevelopmentEnv();

        if (System.getProperty("owo.debug") != null) {
            debug = Boolean.getBoolean("owo.debug");
        }

        DEBUG = debug;
    }

    @ApiStatus.Internal
    public static GameRules.Key<GameRules.BooleanValue> RULE_KEEP_ACCESSORY_INVENTORY = null;

    public static final String MODID = "accessories";

    public static ResourceLocation of(String path){
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    public static String translationKey(String path){
        return MODID + "." + path;
    }

    public static Component translation(String path) {
        return Component.translatable(translationKey(path));
    }

    //--

    private static final io.wispforest.accessories.compat.config.AccessoriesConfig CONFIG = io.wispforest.accessories.compat.config.AccessoriesConfig.createAndLoad(serializationBuilder -> {
        serializationBuilder.addEndec(Vector2i.class, EndecUtils.VECTOR_2_I_ENDEC);
    });

    public static io.wispforest.accessories.compat.config.AccessoriesConfig config(){
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
            var result = AllowEntityModificationCallback.EVENT.invoker().allowModifications(targetEntity, player, null);

            if(!result.orElse(false)) return;
        }

        AccessoriesInternals.openAccessoriesMenu(player, variant, targetEntity, carriedStack);
    }

    //--

    public static AccessoryChangedCriterion ACCESSORY_EQUIPPED;
    public static AccessoryChangedCriterion ACCESSORY_UNEQUIPPED;

    public static void init() {
        AllowEntityModificationCallback.EVENT.register((target, player, reference) -> {
            var type = target.getType();

            if(type.is(AccessoriesTags.MODIFIABLE_ENTITY_BLACKLIST)) return TriState.FALSE;

            var isOwnersPet = (target instanceof OwnableEntity ownableEntity && ownableEntity.getOwner() != null && ownableEntity.getOwner().equals(player));

            if(isOwnersPet || type.is(AccessoriesTags.MODIFIABLE_ENTITY_WHITELIST)) return TriState.TRUE;

            return TriState.DEFAULT;
        });

        ArmorSlotTypes.INSTANCE.init();

        VanillaItemPredicates.init();
    }

    public static void registerCriteria(){
        ACCESSORY_EQUIPPED = CriteriaTriggersAccessor.accessories$callRegister("accessories:equip_accessory", new AccessoryChangedCriterion());
        ACCESSORY_UNEQUIPPED = CriteriaTriggersAccessor.accessories$callRegister("accessories:unequip_accessory", new AccessoryChangedCriterion());
    }
}