package io.wispforest.accessories;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.wispforest.accessories.api.components.AccessoriesDataComponents;
import io.wispforest.accessories.api.components.AccessorySlotValidationComponent;
import io.wispforest.accessories.api.events.AllowEntityModificationCallback;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.client.AccessoriesMenu;
import io.wispforest.accessories.commands.RecordArgumentTypeInfo;
import io.wispforest.accessories.compat.AccessoriesConfig;
import io.wispforest.accessories.criteria.AccessoryChangedCriterion;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.impl.AccessoriesTags;
import io.wispforest.accessories.mixin.CriteriaTriggersAccessor;
import io.wispforest.accessories.utils.EndecUtils;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;

public class Accessories {

    public static final String MODID = "accessories";

    public static ResourceLocation of(String path){
        return new ResourceLocation(MODID, path);
    }

    public static String translation(String path){
        return MODID + "." + path;
    }

    public static AccessoriesConfig getConfig(){
        if(CONFIG_HOLDER == null) return null;

        return CONFIG_HOLDER.getConfig();
    }

    public static boolean attemptOpenScreenPlayer(ServerPlayer player) {
        var result = ProjectileUtil.getHitResultOnViewVector(player, e -> e instanceof LivingEntity, player.entityInteractionRange());

        if(!(result instanceof EntityHitResult entityHitResult)) return false;

        Accessories.openAccessoriesMenu(player, (LivingEntity) entityHitResult.getEntity());

        return true;
    }

    public static void openAccessoriesMenu(Player player, @Nullable LivingEntity targetEntity) {
        openAccessoriesMenu(player, targetEntity, null);
    }

    public static void openAccessoriesMenu(Player player, @Nullable LivingEntity targetEntity, @Nullable ItemStack carriedStack) {
        if(targetEntity != null) {
            var result = AllowEntityModificationCallback.EVENT.invoker().allowModifications(targetEntity, player, null);

            if(!result.orElse(false)) return;
        }

        AccessoriesInternals.openAccessoriesMenu(player, targetEntity, carriedStack);
    }

    //--

    @Nullable
    public static ConfigHolder<AccessoriesConfig> CONFIG_HOLDER = null;

    public static MenuType<AccessoriesMenu> ACCESSORIES_MENU_TYPE;

    public static AccessoryChangedCriterion ACCESSORY_EQUIPPED;
    public static AccessoryChangedCriterion ACCESSORY_UNEQUIPPED;

    public static void init() {
        CONFIG_HOLDER = AutoConfig.register(AccessoriesConfig.class, JanksonConfigSerializer::new);

        AllowEntityModificationCallback.EVENT.register((target, player, reference) -> {
            var type = target.getType();

            if(type.is(AccessoriesTags.MODIFIABLE_ENTITY_BLACKLIST)) return TriState.FALSE;
            if(target instanceof OwnableEntity || type.is(AccessoriesTags.MODIFIABLE_ENTITY_WHITELIST)) return TriState.TRUE;

            return TriState.DEFAULT;
        });
    }

    public static void registerCriteria(){
        ACCESSORY_EQUIPPED = CriteriaTriggersAccessor.accessories$callRegister("accessories:equip_accessory", new AccessoryChangedCriterion());
        ACCESSORY_UNEQUIPPED = CriteriaTriggersAccessor.accessories$callRegister("accessories:unequip_accessory", new AccessoryChangedCriterion());
    }

    public static void registerMenuType() {
        ACCESSORIES_MENU_TYPE = AccessoriesInternals.registerMenuType(of("accessories_menu"), (i, inv, menuData) -> AccessoriesMenu.of(i, inv, false, menuData));
    }

    public static void registerCommandArgTypes() {
        AccessoriesInternals.registerCommandArgumentType(Accessories.of("slot_type"), SlotArgumentType.class, SLOT_ARGUMENT_TYPE_INFO);
    }

    private static final SimpleCommandExceptionType NON_LIVING_ENTITY_TARGET = new SimpleCommandExceptionType(Component.translatable("argument.livingEntities.nonLiving"));

    private static final SimpleCommandExceptionType INVALID_SLOT_TYPE = new SimpleCommandExceptionType(new LiteralMessage("Invalid Slot Type"));

    //accessories edit {}
    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("accessories")
                        .requires(commandSourceStack -> commandSourceStack.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(
                                Commands.literal("edit")
                                        .then(
                                                Commands.argument("entity", EntityArgument.entity())
                                                        .executes((context) -> {
                                                            var player = context.getSource().getPlayerOrException();

                                                            var entity = EntityArgument.getEntity(context, "entity");

                                                            if(!(entity instanceof LivingEntity livingEntity)) {
                                                                throw NON_LIVING_ENTITY_TARGET.create();
                                                            }

                                                            Accessories.openAccessoriesMenu(player, livingEntity);

                                                            return 1;
                                                        })
                                        )
                                        .executes(context -> {
                                            return attemptOpenScreenPlayer(context.getSource().getPlayerOrException())
                                                    ? 1
                                                    : 0;
                                        })
                        )
                        .then(
                                Commands.literal("slot")
                                        .then(
                                                Commands.literal("add")
                                                        .then(
                                                                Commands.literal("valid")
                                                                        .then(Commands.argument("slot", SlotArgumentType.INSTANCE)
                                                                                .executes(ctx -> adjustSlotValidationOnStack(0, ctx.getSource().getPlayerOrException(), ctx))
                                                                        ))
                                                        .then(
                                                                Commands.literal("invalid")
                                                                        .then(Commands.argument("slot", SlotArgumentType.INSTANCE)
                                                                                .executes(ctx -> adjustSlotValidationOnStack(1, ctx.getSource().getPlayerOrException(), ctx))
                                                                        ))

                                        ).then(
                                                Commands.literal("remove")
                                                        .then(
                                                                Commands.literal("valid")
                                                                        .then(Commands.argument("slot", SlotArgumentType.INSTANCE)
                                                                                .executes(ctx -> adjustSlotValidationOnStack(2, ctx.getSource().getPlayerOrException(), ctx))
                                                                        ))
                                                        .then(
                                                                Commands.literal("invalid")
                                                                        .then(Commands.argument("slot", SlotArgumentType.INSTANCE)
                                                                                .executes(ctx -> adjustSlotValidationOnStack(3, ctx.getSource().getPlayerOrException(), ctx))
                                                                        ))
                                        )
                        )
        );
    }

    public static int adjustSlotValidationOnStack(int operation, ServerPlayer player, CommandContext<CommandSourceStack> ctx) {
        var slotName = ctx.getArgument("slot", String.class);

        player.getMainHandItem().update(AccessoriesDataComponents.SLOT_VALIDATION, AccessorySlotValidationComponent.EMPTY, component -> {
            return switch (operation) {
                case 0 -> component.addValidSlot(slotName);
                case 1 -> component.addInvalidSlot(slotName);
                case 2 -> component.removeValidSlot(slotName);
                case 3 -> component.removeInvalidSlot(slotName);
                default -> throw new IllegalStateException("Unexpected value: " + operation);
            };
        });

        return 1;
    }

    public static final RecordArgumentTypeInfo<SlotArgumentType, Void> SLOT_ARGUMENT_TYPE_INFO = RecordArgumentTypeInfo.of(ctx -> SlotArgumentType.INSTANCE);

    public static final class SlotArgumentType implements ArgumentType<String> {

        public static final SlotArgumentType INSTANCE = new SlotArgumentType();

        @Override
        public String parse(StringReader reader) throws CommandSyntaxException {
            var slot = reader.readUnquotedString();

            if(slot.equals("any")) return "any";

            var slotType = SlotTypeLoader.INSTANCE.getSlotTypes(false).getOrDefault(slot, null);

            if(slotType == null) throw INVALID_SLOT_TYPE.create();

            return slotType.name();
        }

        @Override
        public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
            if (context.getSource() instanceof SharedSuggestionProvider) {
                var stringReader = new StringReader(builder.getInput());

                stringReader.setCursor(builder.getStart());

                var validSlots = new ArrayList<>(SlotTypeLoader.INSTANCE.getSlotTypes(false).keySet());

                validSlots.addFirst("any");

                return SharedSuggestionProvider.suggest(validSlots, builder);
            }

            return Suggestions.empty();
        }
    }
}