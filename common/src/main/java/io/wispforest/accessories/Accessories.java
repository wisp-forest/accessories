package io.wispforest.accessories;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.wispforest.accessories.client.AccessoriesMenu;
import io.wispforest.accessories.compat.AccessoriesConfig;
import io.wispforest.accessories.criteria.AccessoryChangedCriterion;
import io.wispforest.accessories.mixin.CriteriaTriggersAccessor;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.Nullable;

public class Accessories {

    public static MenuType<AccessoriesMenu> ACCESSORIES_MENU_TYPE;

    public static AccessoryChangedCriterion ACCESSORY_EQUIPPED;
    public static AccessoryChangedCriterion ACCESSORY_UNEQUIPPED;

    public static final String MODID = "accessories";

    @Nullable
    private static ConfigHolder<AccessoriesConfig> CONFIG_HOLDER = null;

    public static void registerCriteria(){
        ACCESSORY_EQUIPPED = CriteriaTriggersAccessor.accessories$callRegister("accessories:equip_accessory", new AccessoryChangedCriterion());
        ACCESSORY_UNEQUIPPED = CriteriaTriggersAccessor.accessories$callRegister("accessories:unequip_accessory", new AccessoryChangedCriterion());
    }

    public static void registerMenuType() {
        ACCESSORIES_MENU_TYPE = AccessoriesInternals.registerMenuType(of("accessories_menu"), (i, inv, buf) -> AccessoriesMenu.of(i, inv, false, buf));
    }

    public static void setupConfig(){
        CONFIG_HOLDER = AutoConfig.register(AccessoriesConfig.class, JanksonConfigSerializer::new);
    }

    public static AccessoriesConfig getConfig(){
        if(CONFIG_HOLDER == null) return null;

        return CONFIG_HOLDER.getConfig();
    }

    private static final SimpleCommandExceptionType NON_LIVING_ENTITY_TARGET = new SimpleCommandExceptionType(Component.translatable("argument.livingEntities.nonLiving"));

    //accessories edit {}
    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("accessories")
                        .then(
                                Commands.literal("edit")
                                        .requires(commandSourceStack -> commandSourceStack.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                        .then(
                                                Commands.argument("entity", EntityArgument.entity())
                                                        .executes((context) -> {
                                                            var player = context.getSource().getPlayerOrException();

                                                            var entity = EntityArgument.getEntity(context, "entity");

                                                            if(!(entity instanceof LivingEntity livingEntity)) {
                                                                throw NON_LIVING_ENTITY_TARGET.create();
                                                            }

                                                            AccessoriesInternals.openAccessoriesMenu(player, livingEntity);

                                                            return 1;
                                                        })
                                        )
                                        .executes(context -> {
                                            var player = context.getSource().getPlayerOrException();

                                            var result = ProjectileUtil.getHitResultOnViewVector(player, e -> e instanceof LivingEntity, (double) Player.getPickRange(player.isCreative()));

                                            if(!(result instanceof EntityHitResult entityHitResult)) {
                                                return 0;
                                            }

                                            AccessoriesInternals.openAccessoriesMenu(player, (LivingEntity) entityHitResult.getEntity());

                                            return 1;
                                        })
                        )
        );
    }

    public static ResourceLocation of(String path){
        return new ResourceLocation(MODID, path);
    }

    public static String translation(String path){
        return MODID + "." + path;
    }
}