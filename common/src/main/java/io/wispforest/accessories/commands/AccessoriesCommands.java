package io.wispforest.accessories.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.api.components.AccessoriesDataComponents;
import io.wispforest.accessories.api.components.AccessoryItemAttributeModifiers;
import io.wispforest.accessories.api.components.AccessorySlotValidationComponent;
import io.wispforest.accessories.api.components.AccessoryStackSizeComponent;
import io.wispforest.accessories.utils.AttributeUtils;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.Arrays;

public class AccessoriesCommands {

    public static final SimpleCommandExceptionType NON_LIVING_ENTITY_TARGET = new SimpleCommandExceptionType(Component.translatable("argument.livingEntities.nonLiving"));

    public static final SimpleCommandExceptionType INVALID_SLOT_TYPE = new SimpleCommandExceptionType(new LiteralMessage("Invalid Slot Type"));

    public static void registerCommandArgTypes() {
        AccessoriesInternals.registerCommandArgumentType(Accessories.of("slot_type"), SlotArgumentType.class, RecordArgumentTypeInfo.of(ctx -> SlotArgumentType.INSTANCE));
        AccessoriesInternals.registerCommandArgumentType(Accessories.of("resource"), ResourceExtendedArgument.class, RecordArgumentTypeInfo.of(ResourceExtendedArgument::attributes));
    }

    public static LivingEntity getOrThrowLivingEntity(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var entity = EntityArgument.getEntity(ctx, "entity");

        if(!(entity instanceof LivingEntity livingEntity)) {
            throw NON_LIVING_ENTITY_TARGET.create();
        }

        return livingEntity;
    }

    //accessories edit {}
    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register(
                Commands.literal("accessories")
                        .requires(commandSourceStack -> commandSourceStack.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(
                                Commands.literal("edit")
                                        .then(
                                                Commands.argument("entity", EntityArgument.entity())
                                                        .executes((ctx) -> {
                                                            var player = ctx.getSource().getPlayerOrException();

                                                            Accessories.openAccessoriesMenu(player, getOrThrowLivingEntity(ctx));

                                                            return 1;
                                                        })
                                        )
                                        .executes(ctx -> {
                                            return Accessories.attemptOpenScreenPlayer(ctx.getSource().getPlayerOrException())
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
                        .then(
                                Commands.literal("stack-sizing")
                                        .then(
                                                Commands.literal("useStackSize")
                                                        .then(
                                                                Commands.argument("value", BoolArgumentType.bool())
                                                                        .executes(ctx -> {
                                                                            var player = ctx.getSource().getPlayerOrException();

                                                                            var bl = ctx.getArgument("value", Boolean.class);

                                                                            AccessoriesDataComponents.update(
                                                                                    AccessoriesDataComponents.STACK_SIZE,
                                                                                    player.getMainHandItem(),
                                                                                    component -> component.useStackSize(bl));

                                                                            return 1;
                                                                        })
                                                        )
                                        ).then(
                                                Commands.argument("value", IntegerArgumentType.integer())
                                                        .executes(ctx -> {
                                                            var player = ctx.getSource().getPlayerOrException();

                                                            var size = ctx.getArgument("value", Integer.class);

                                                            AccessoriesDataComponents.update(
                                                                    AccessoriesDataComponents.STACK_SIZE,
                                                                    player.getMainHandItem(),
                                                                    component -> component.sizeOverride(size));

                                                            return 1;
                                                        })
                                        )
                        )
                        .then(
                                Commands.literal("attribute")
                                        .then(
                                                Commands.literal("modifier")
                                                        .then(
                                                                Commands.literal("add")
                                                                        .then(
                                                                                Commands.argument("attribute", ResourceExtendedArgument.attributes(context))
                                                                                        .then(
                                                                                                Commands.argument("id", ResourceLocationArgument.id())
                                                                                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                                                                                                        .then(createAddLiteral("addition"))
                                                                                                                        .then(createAddLiteral("multiply_base"))
                                                                                                                        .then(createAddLiteral("multiply_total"))
                                                                                                        )
                                                                                        )
                                                                        )
                                                        )
                                                        .then(
                                                                Commands.literal("remove")
                                                                        .then(
                                                                                Commands.argument("attribute", ResourceExtendedArgument.attributes(context))
                                                                                        .then(
                                                                                                Commands.argument("id", ResourceLocationArgument.id())
                                                                                                        .executes(
                                                                                                                ctx -> removeModifier(
                                                                                                                        ctx.getSource(),
                                                                                                                        ctx.getSource().getPlayerOrException(),
                                                                                                                        ResourceExtendedArgument.getAttribute(ctx, "attribute"),
                                                                                                                        ResourceLocationArgument.getId(ctx, "id")
                                                                                                                )
                                                                                                        )
                                                                                        )
                                                                        )
                                                        )
                                                        .then(
                                                                Commands.literal("get")
                                                                        .then(
                                                                                Commands.argument("attribute", ResourceExtendedArgument.attributes(context))
                                                                                        .then(
                                                                                                Commands.argument("id", ResourceLocationArgument.id())
                                                                                                        .executes(
                                                                                                                ctx -> getAttributeModifier(
                                                                                                                        ctx.getSource(),
                                                                                                                        ctx.getSource().getPlayerOrException(),
                                                                                                                        ResourceExtendedArgument.getAttribute(ctx, "attribute"),
                                                                                                                        ResourceLocationArgument.getId(ctx, "id"),
                                                                                                                        1.0
                                                                                                                )
                                                                                                        )
                                                                                                        .then(
                                                                                                                Commands.argument("scale", DoubleArgumentType.doubleArg())
                                                                                                                        .executes(
                                                                                                                                ctx -> getAttributeModifier(
                                                                                                                                        ctx.getSource(),
                                                                                                                                        ctx.getSource().getPlayerOrException(),
                                                                                                                                        ResourceExtendedArgument.getAttribute(ctx, "attribute"),
                                                                                                                                        ResourceLocationArgument.getId(ctx, "id"),
                                                                                                                                        DoubleArgumentType.getDouble(ctx, "scale")
                                                                                                                                )
                                                                                                                        )
                                                                                                        )
                                                                                        )
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createAddLiteral(String literal) {
        var selectedValue = Arrays.stream(AttributeModifier.Operation.values())
                .filter(value -> value.name().toLowerCase().equals(literal))
                .findFirst()
                .orElse(null);

        if(selectedValue == null) throw new IllegalStateException("Unable to handle the given literal as its not a valid AttributeModifier Operation! [Literal: " + literal + "]");

        return Commands.literal(literal)
                .then(
                        Commands.argument("slot", SlotArgumentType.INSTANCE)
                                .then(
                                        Commands.argument("isStackable", BoolArgumentType.bool())
                                                .executes(
                                                        ctx -> addModifier(
                                                                ctx.getSource(),
                                                                ctx.getSource().getPlayerOrException(),
                                                                ResourceExtendedArgument.getAttribute(ctx, "attribute"),
                                                                ResourceLocationArgument.getId(ctx, "id"),
                                                                DoubleArgumentType.getDouble(ctx, "value"),
                                                                selectedValue,
                                                                SlotArgumentType.getSlot(ctx, "slot"),
                                                                BoolArgumentType.getBool(ctx, "isStackable")
                                                        )
                                                )
                                )
                );
    }

    private static int getAttributeModifier(CommandSourceStack commandSourceStack, LivingEntity livingEntity, Holder<Attribute> holder, ResourceLocation resourceLocation, double d) throws CommandSyntaxException {
        var stack = livingEntity.getMainHandItem();

        var component = AccessoriesDataComponents.readOrDefault(AccessoriesDataComponents.ATTRIBUTES, stack);

        var modifier = component.getModifier(holder.value(), resourceLocation);

        if (modifier == null) {
            throw ERROR_NO_SUCH_MODIFIER.create(stack.getDisplayName(), getAttributeDescription(holder), resourceLocation);
        }

        double e = modifier.getAmount();

        commandSourceStack.sendSuccess(
                () -> Component.translatable(
                        "commands.attribute.modifier.value.get.success_itemstack", resourceLocation, getAttributeDescription(holder), stack.getDisplayName(), e
                ),
                false
        );

        return (int)(e * d);
    }

    private static final Dynamic3CommandExceptionType ERROR_MODIFIER_ALREADY_PRESENT = new Dynamic3CommandExceptionType(
            (var1, var2, var3) -> Component.translatable("commands.attribute.failed.modifier_already_present_itemstack", var1, var2, var3)
    );

    private static int addModifier(CommandSourceStack commandSourceStack, LivingEntity livingEntity, Holder<Attribute> holder, ResourceLocation resourceLocation, double d, AttributeModifier.Operation operation, String slotName, boolean isStackable) throws CommandSyntaxException {
        var stack = livingEntity.getMainHandItem();

        var component = AccessoriesDataComponents.readOrDefault(AccessoriesDataComponents.ATTRIBUTES, stack);

        if (component.hasModifier(holder.value(), resourceLocation)) {
            throw ERROR_MODIFIER_ALREADY_PRESENT.create(resourceLocation, getAttributeDescription(holder), stack.getDisplayName());
        }

        var data = AttributeUtils.getModifierData(resourceLocation);

        AccessoriesDataComponents.write(
                AccessoriesDataComponents.ATTRIBUTES,
                stack,
                component.withModifierAdded(holder.value(), new AttributeModifier(data.second(), data.first(), d, operation), slotName, isStackable));

        commandSourceStack.sendSuccess(
                () -> Component.translatable(
                        "commands.attribute.modifier.add.success_itemstack", resourceLocation, getAttributeDescription(holder), stack.getDisplayName()
                ),
                false
        );

        return 1;
    }

    private static final Dynamic3CommandExceptionType ERROR_NO_SUCH_MODIFIER = new Dynamic3CommandExceptionType(
            (var1, var2, var3) -> Component.translatable("commands.attribute.failed.no_modifier_itemstack", var1, var2, var3)
    );

    private static int removeModifier(CommandSourceStack commandSourceStack, LivingEntity livingEntity, Holder<Attribute> holder, ResourceLocation resourceLocation) throws CommandSyntaxException {
        MutableBoolean removedModifier = new MutableBoolean(false);

        var stack = livingEntity.getMainHandItem();

        AccessoriesDataComponents.update(AccessoriesDataComponents.ATTRIBUTES, stack, component -> {
            var size = component.modifiers().size();

            component = component.withoutModifier(holder, resourceLocation);

            if(size != component.modifiers().size()) removedModifier.setTrue();

            return component;
        });

        if(!removedModifier.getValue()) {
            throw ERROR_NO_SUCH_MODIFIER.create(resourceLocation, getAttributeDescription(holder), stack.getDisplayName());
        }

        commandSourceStack.sendSuccess(
                () -> Component.translatable(
                        "commands.attribute.modifier.remove.success_itemstack", resourceLocation, getAttributeDescription(holder), stack.getDisplayName()
                ),
                false
        );

        return 1;
    }

    private static Component getAttributeDescription(Holder<Attribute> attribute) {
        return Component.translatable(attribute.value().getDescriptionId());
    }

    private static int adjustSlotValidationOnStack(int operation, LivingEntity player, CommandContext<CommandSourceStack> ctx) {
        var slotName = SlotArgumentType.getSlot(ctx, "slot");

        AccessoriesDataComponents.update(
                AccessoriesDataComponents.SLOT_VALIDATION,
                player.getMainHandItem(),
                component -> {
                    return switch (operation) {
                        case 0 -> component.addValidSlot(slotName);
                        case 1 -> component.addInvalidSlot(slotName);
                        case 2 -> component.removeValidSlot(slotName);
                        case 3 -> component.removeInvalidSlot(slotName);
                        default -> throw new IllegalStateException("Unexpected value: " + operation);
                    };
                }
        );

        return 1;
    }

}
