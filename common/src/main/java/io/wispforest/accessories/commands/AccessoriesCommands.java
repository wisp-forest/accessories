package io.wispforest.accessories.commands;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.logging.LogUtils;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.components.*;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.data.SlotGroupLoader;
import io.wispforest.accessories.data.SlotTypeLoader;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class AccessoriesCommands extends CommandBuilderHelper {

    public static final AccessoriesCommands INSTANCE = new AccessoriesCommands();

    private static final SimpleCommandExceptionType NON_LIVING_ENTITY_TARGET = new SimpleCommandExceptionType(Component.translatable("argument.livingEntities.nonLiving"));

    public static final SimpleCommandExceptionType INVALID_SLOT_TYPE = new SimpleCommandExceptionType(new LiteralMessage("Invalid Slot Type"));

    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void registerArgumentTypes(ArgumentRegistration registration) {
        registration.register(Accessories.of("slot_type"), SlotArgumentType.class, RecordArgumentTypeInfo.of(ctx -> SlotArgumentType.INSTANCE));
        registration.register(Accessories.of("resource"), ResourceExtendedArgument.class, RecordArgumentTypeInfo.of(ResourceExtendedArgument::attributes));
    }

    public static LivingEntity getOrThrowLivingEntity(CommandContext<CommandSourceStack> ctx, String name) throws CommandSyntaxException {
        var entity = EntityArgument.getEntity(ctx, name);

        if(!(entity instanceof LivingEntity livingEntity)) {
            throw NON_LIVING_ENTITY_TARGET.create();
        }

        return livingEntity;
    }

    @Override
    protected void generateTrees(CommandBuildContext context) {
        getOrCreateNode("accessories").requires(stack -> stack.hasPermission(Commands.LEVEL_GAMEMASTERS));

        if (Accessories.DEBUG) {
            base.then(
                    Commands.literal("create-render-stack")
                            .executes(ctx -> {
                                TempUtilCommands.createRenderStack(ctx.getSource().getPlayerOrException());

                                return 1;
                            })
            );
        }

        optionalArgExectution(
                "accessories/edit",
                argumentHolder("entity", EntityArgument.entity(), AccessoriesCommands::getOrThrowLivingEntity),
                (ctx, livingEntity) -> {
                    Accessories.askPlayerForVariant(ctx.getSource().getPlayerOrException(), livingEntity);

                    return 1;
                });

        //--

        requiredArgExectution(
                "accessories/nest",
                argumentHolder("item", ItemArgument.item(context), (ctx, name) -> ItemArgument.getItem(ctx, name).createItemStack(1, false)),
                (ctx, innerStack) -> {
                    var player = ctx.getSource().getPlayerOrException();

                    player.getMainHandItem().update(
                            AccessoriesDataComponents.NESTED_ACCESSORIES,
                            AccessoryNestContainerContents.EMPTY,
                            data -> data.addStack(innerStack));

                    return 1;
                });

        //--

        var slotArgument = argumentHolder("slot", SlotArgumentType.INSTANCE, SlotArgumentType::getSlot);
        var slotGroupings = List.of("valid", "invalid");

        requiredArgExectutionBranched(
                "accessories/slot/add",
                slotGroupings,
                slotArgument,
                (ctx, branch, slot) -> adjustSlotValidationOnStack(Objects.equals(branch, "valid"), true, slot, ctx)
        );

        requiredArgExectutionBranched(
                "accessories/slot/remove",
                slotGroupings,
                slotArgument,
                (ctx, branch, slot) -> adjustSlotValidationOnStack(Objects.equals(branch, "valid"), false, slot, ctx)
        );

        //--

        requiredArgExectution(
                "accessories/stack-sizing/useStackSize",
                argumentHolder("value", BoolArgumentType.bool(), (ctx, name) -> ctx.getArgument(name, Boolean.class)),
                (ctx, bl) -> {
                    var player = ctx.getSource().getPlayerOrException();

                    player.getMainHandItem().update(AccessoriesDataComponents.STACK_SIZE,
                            AccessoryStackSizeComponent.DEFAULT,
                            component -> component.useStackSize(bl));

                    return 1;
                }
        );

        requiredArgExectution(
                "accessories/stack-sizing",
                argumentHolder("size", IntegerArgumentType.integer(), (ctx, name) -> ctx.getArgument(name, Integer.class)),
                (ctx, size) -> {
                    var player = ctx.getSource().getPlayerOrException();

                    player.getMainHandItem().update(AccessoriesDataComponents.STACK_SIZE,
                            AccessoryStackSizeComponent.DEFAULT,
                            component -> component.sizeOverride(size));

                    return 1;
                }
        );

        //--

        var attributeArg = argumentHolder("attribute", ResourceExtendedArgument.attributes(context), ResourceExtendedArgument::getAttribute);
        var idArg = argumentHolder("id", ResourceLocationArgument.id(), ResourceLocationArgument::getId);

        var modifierAdd = getOrCreateNode(
                "accessories/attribute/modifier/add",
                attributeArg,
                idArg,
                argumentHolder("amount", DoubleArgumentType.doubleArg(), DoubleArgumentType::getDouble)
        );

        modifierAdd
                .then(createAddLiteral("add_value"))
                .then(createAddLiteral("add_multiplied_base"))
                .then(createAddLiteral("add_multiplied_total"));

        updateParent(modifierAdd);

        requiredArgExectution(
                "accessories/attribute/modifier/remove",
                attributeArg,
                idArg,
                AccessoriesCommands::removeModifier
        );

        requiredArgExectution(
                "accessories/attribute/modifier/get",
                attributeArg,
                idArg,
                (ctx, attributeHolder, location) -> getAttributeModifier(ctx, attributeArg.getArgument(ctx), idArg.getArgument(ctx), 1.0)
        );

        requiredArgExectution(
                "accessories/attribute/modifier/get",
                attributeArg,
                idArg,
                argumentHolder("scale", DoubleArgumentType.doubleArg(), DoubleArgumentType::getDouble),
                (ctx, attributeHolder, location, scale) -> getAttributeModifier(ctx, attributeArg.getArgument(ctx), idArg.getArgument(ctx), scale)
        );

        //--

        var logFailureType = new DynamicCommandExceptionType(branch -> Component.literal("Unable to locate the given logging for the following command branch: " + branch));

        requiredExectutionBranched(
                "accessories/log",
                List.of("slots", "groups", "entity_bindings"),
                (ctx, branch) -> {
                    switch (branch) {
                        case "slots" -> {
                            LOGGER.info("All given Slots registered:");

                            for (var slotType : SlotTypeLoader.getSlotTypes(ctx.getSource().getLevel()).values()) {
                                LOGGER.info(slotType.toString());
                            }
                        }
                        case "groups" -> {
                            LOGGER.info("All given Slot Groups registered:");

                            for (var group : SlotGroupLoader.getGroups(ctx.getSource().getLevel())) {
                                LOGGER.info(group.toString());
                            }
                        }
                        case "entity_bindings" ->{
                            LOGGER.info("All given Entity Bindings registered:");

                            EntitySlotLoader.INSTANCE.getEntitySlotData(false).forEach((type, slots) -> {
                                LOGGER.info("[{}]: {}", type, slots.keySet());
                            });

                        }
                        default -> throw logFailureType.create(branch);
                    }

                    return 1;
                }
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createAddLiteral(String literal) {
        var selectedValue = Arrays.stream(AttributeModifier.Operation.values())
                .filter(value -> value.getSerializedName().equals(literal))
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
                                                                DoubleArgumentType.getDouble(ctx, "amount"),
                                                                selectedValue,
                                                                SlotArgumentType.getSlot(ctx, "slot"),
                                                                BoolArgumentType.getBool(ctx, "isStackable")
                                                        )
                                                )
                                )
                );
    }

    private static int getAttributeModifier(CommandContext<CommandSourceStack> ctx, Holder<Attribute> holder, ResourceLocation resourceLocation, double d) throws CommandSyntaxException {
        var commandSourceStack = ctx.getSource();
        var livingEntity = ctx.getSource().getPlayerOrException();


        var stack = livingEntity.getMainHandItem();

        var component = stack.getOrDefault(AccessoriesDataComponents.ATTRIBUTES, AccessoryItemAttributeModifiers.EMPTY);

        var modifier = component.getModifier(holder, resourceLocation);

        if (modifier == null) {
            throw ERROR_NO_SUCH_MODIFIER.create(stack.getDisplayName(), getAttributeDescription(holder), resourceLocation);
        }

        double e = modifier.amount();

        commandSourceStack.sendSuccess(
                () -> Component.translatable(
                        "commands.attribute.modifier.value.get.success_itemstack", Component.translationArg(resourceLocation), getAttributeDescription(holder), stack.getDisplayName(), e
                ),
                false
        );

        return (int)(e * d);
    }

    private static final Dynamic3CommandExceptionType ERROR_MODIFIER_ALREADY_PRESENT = new Dynamic3CommandExceptionType(
            (var1, var2, var3) -> Component.translatableEscape("commands.attribute.failed.modifier_already_present_itemstack", var1, var2, var3)
    );

    private static int addModifier(CommandSourceStack commandSourceStack, LivingEntity livingEntity, Holder<Attribute> holder, ResourceLocation resourceLocation, double d, AttributeModifier.Operation operation, String slotName, boolean isStackable) throws CommandSyntaxException {
        var stack = livingEntity.getMainHandItem();

        var component = stack.getOrDefault(AccessoriesDataComponents.ATTRIBUTES, AccessoryItemAttributeModifiers.EMPTY);

        if (component.hasModifier(holder, resourceLocation)) {
            throw ERROR_MODIFIER_ALREADY_PRESENT.create(resourceLocation, getAttributeDescription(holder), stack.getDisplayName());
        }

        stack.set(AccessoriesDataComponents.ATTRIBUTES, component.withModifierAdded(holder, new AttributeModifier(resourceLocation, d, operation), slotName, isStackable));

        commandSourceStack.sendSuccess(
                () -> Component.translatable(
                        "commands.attribute.modifier.add.success_itemstack", Component.translationArg(resourceLocation), getAttributeDescription(holder), stack.getDisplayName()
                ),
                false
        );

        return 1;
    }

    private static final Dynamic3CommandExceptionType ERROR_NO_SUCH_MODIFIER = new Dynamic3CommandExceptionType(
            (var1, var2, var3) -> Component.translatableEscape("commands.attribute.failed.no_modifier_itemstack", var1, var2, var3)
    );

    private static int removeModifier(CommandContext<CommandSourceStack> ctx, Holder<Attribute> holder, ResourceLocation location) throws CommandSyntaxException {
        var commandSourceStack = ctx.getSource();
        var livingEntity = ctx.getSource().getPlayerOrException();

        MutableBoolean removedModifier = new MutableBoolean(false);

        var stack = livingEntity.getMainHandItem();

        stack.update(AccessoriesDataComponents.ATTRIBUTES, AccessoryItemAttributeModifiers.EMPTY, component -> {
            var size = component.modifiers().size();

            component = component.withoutModifier(holder, location);

            if(size != component.modifiers().size()) removedModifier.setTrue();

            return component;
        });

        if(!removedModifier.getValue()) {
            throw ERROR_NO_SUCH_MODIFIER.create(location, getAttributeDescription(holder), stack.getDisplayName());
        }

        commandSourceStack.sendSuccess(
                () -> Component.translatable(
                        "commands.attribute.modifier.remove.success_itemstack", Component.translationArg(location), getAttributeDescription(holder), stack.getDisplayName()
                ),
                false
        );

        return 1;
    }

    private static Component getAttributeDescription(Holder<Attribute> attribute) {
        return Component.translatable(attribute.value().getDescriptionId());
    }

    private static int adjustSlotValidationOnStack(boolean validSlot, boolean addSlot, String slotName, CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        LivingEntity targetEntity = ctx.getSource().getPlayerOrException();

        targetEntity.getMainHandItem().update(AccessoriesDataComponents.SLOT_VALIDATION, AccessorySlotValidationComponent.EMPTY, component -> {
            return (validSlot)
                    ? (addSlot ? component.addValidSlot(slotName) : component.removeValidSlot(slotName))
                    : (addSlot ? component.addInvalidSlot(slotName) : component.removeInvalidSlot(slotName));
        });

        return 1;
    }
}
