package io.wispforest.accessories.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.api.components.AccessoriesDataComponents;
import io.wispforest.accessories.api.components.AccessorySlotValidationComponent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public class AccessoriesCommands {

    public static final SimpleCommandExceptionType NON_LIVING_ENTITY_TARGET = new SimpleCommandExceptionType(Component.translatable("argument.livingEntities.nonLiving"));

    public static final SimpleCommandExceptionType INVALID_SLOT_TYPE = new SimpleCommandExceptionType(new LiteralMessage("Invalid Slot Type"));

    public static void registerCommandArgTypes() {
        AccessoriesInternals.registerCommandArgumentType(Accessories.of("slot_type"), SlotArgumentType.class, RecordArgumentTypeInfo.of(ctx -> SlotArgumentType.INSTANCE));
    }

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
                                            return Accessories.attemptOpenScreenPlayer(context.getSource().getPlayerOrException())
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

    private static int adjustSlotValidationOnStack(int operation, ServerPlayer player, CommandContext<CommandSourceStack> ctx) {
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

}
