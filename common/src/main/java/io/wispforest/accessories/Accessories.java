package io.wispforest.accessories;

import io.wispforest.accessories.api.data.AccessoriesTags;
import io.wispforest.accessories.api.events.AllowEntityModificationCallback;
import io.wispforest.accessories.compat.AccessoriesConfig;
import io.wispforest.accessories.criteria.AccessoryChangedCriterion;
import io.wispforest.accessories.impl.AccessoriesTags;
import io.wispforest.accessories.menu.AccessoriesMenuVariant;
import io.wispforest.accessories.menu.ArmorSlotTypes;
import io.wispforest.accessories.mixin.CriteriaTriggersAccessor;
import io.wispforest.accessories.networking.client.ScreenVariantPing;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.Nullable;

public class Accessories {

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

    public static AccessoriesConfig getConfig(){
        if(CONFIG_HOLDER == null) return null;

        return CONFIG_HOLDER.getConfig();
    }

    public static void askPlayerForVariant(ServerPlayer player) {
        askPlayerForVariant(player, null);
    }

    public static void askPlayerForVariant(ServerPlayer player, @Nullable LivingEntity targetEntity) {
        AccessoriesInternals.getNetworkHandler().sendToPlayer(player, ScreenVariantPing.of(targetEntity));
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
        if(targetEntity != null) {
            var result = AllowEntityModificationCallback.EVENT.invoker().allowModifications(targetEntity, player, null);

            if(!result.orElse(false)) return;
        }

        AccessoriesInternals.openAccessoriesMenu(player, variant, targetEntity, carriedStack);
    }

    //--

    @Nullable
    public static ConfigHolder<AccessoriesConfig> CONFIG_HOLDER = null;

    public static AccessoryChangedCriterion ACCESSORY_EQUIPPED;
    public static AccessoryChangedCriterion ACCESSORY_UNEQUIPPED;

    public static void init() {
        CONFIG_HOLDER = AutoConfig.register(AccessoriesConfig.class, JanksonConfigSerializer::new);

        AllowEntityModificationCallback.EVENT.register((target, player, reference) -> {
            var type = target.getType();

            if(type.is(AccessoriesTags.MODIFIABLE_ENTITY_BLACKLIST)) return TriState.FALSE;

            var isOwnersPet = (target instanceof OwnableEntity ownableEntity && ownableEntity.getOwner() != null && ownableEntity.getOwner().equals(player));

            if(isOwnersPet || type.is(AccessoriesTags.MODIFIABLE_ENTITY_WHITELIST)) return TriState.TRUE;

            return TriState.DEFAULT;
        });

        ArmorSlotTypes.INSTANCE.init();
    }

    public static void registerCriteria(){
        ACCESSORY_EQUIPPED = CriteriaTriggersAccessor.accessories$callRegister("accessories:equip_accessory", new AccessoryChangedCriterion());
        ACCESSORY_UNEQUIPPED = CriteriaTriggersAccessor.accessories$callRegister("accessories:unequip_accessory", new AccessoryChangedCriterion());
    }

}