package io.wispforest.accessories.commands;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.logging.LogUtils;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.api.attributes.SlotAttribute;
import io.wispforest.accessories.api.client.rendering.RenderingFunction;
import io.wispforest.accessories.api.components.*;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.commands.api.CommandGenerators;
import io.wispforest.accessories.commands.api.core.RecordArgumentTypeInfo;
import io.wispforest.accessories.commands.api.base.BranchedCommandGenerator;
import io.wispforest.accessories.data.CustomRendererLoader;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.data.SlotGroupLoader;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.endec.Endec;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.Set;

import static io.wispforest.accessories.commands.api.Arguments.*;

public class AccessoriesCommands {

    public static final SimpleCommandExceptionType NON_LIVING_ENTITY_TARGET = new SimpleCommandExceptionType(Component.translatable("accessories.argument.livingEntities.nonLiving"));

    public static final SimpleCommandExceptionType INVALID_SLOT_TYPE = new SimpleCommandExceptionType(new LiteralMessage("Invalid Slot Type"));

    public static final SimpleCommandExceptionType ERROR_CAPABILITY_MISSING = new SimpleCommandExceptionType(Component.literal("Unable to get the needed capability from the given target!"));

    public static final DynamicCommandExceptionType ERROR_CONTAINER_MISSING = new DynamicCommandExceptionType((obj) -> Component.literal("Unable to get the needed Container from the given target! [Container: " + obj + "]"));

    private static final Logger LOGGER = LogUtils.getLogger();

    public static void init() {
        CommandGenerators.create(
                "accessories",
                AccessoriesCommands::generateTrees,
                registration -> {
                    registration.register(Accessories.of("slot_type"), SlotArgumentType.class, RecordArgumentTypeInfo.of(ctx -> SlotArgumentType.INSTANCE));
                    registration.register(Accessories.of("resource"), ResourceExtendedArgument.class, RecordArgumentTypeInfo.of(ResourceExtendedArgument::attributes));
                    registration.register(Accessories.of("slot_path"), AccessoriesSlotArgument.class, RecordArgumentTypeInfo.of(Endec.STRING, "entity_argument_name", AccessoriesSlotArgument::entityArgumentName, AccessoriesSlotArgument::new));
                });
    }

    public static LivingEntity getOrThrowLivingEntity(CommandContext<CommandSourceStack> ctx, String name) throws CommandSyntaxException {
        var entity = EntityArgument.getEntity(ctx, name);

        if(!(entity instanceof LivingEntity livingEntity)) {
            throw NON_LIVING_ENTITY_TARGET.create();
        }

        return livingEntity;
    }

    public static AccessoriesCapability getCapability(Entity entity) throws CommandSyntaxException {
        if(!(entity instanceof LivingEntity livingEntity)) throw AccessoriesCommands.NON_LIVING_ENTITY_TARGET.create();

        var capability = livingEntity.accessoriesCapability();

        if (capability == null) throw AccessoriesCommands.ERROR_CAPABILITY_MISSING.create();

        return capability;
    }

    public static AccessoriesContainer getContainer(Entity entity, String slot) throws CommandSyntaxException {
        var capability = getCapability(entity);

        var container = capability.getContainers().get(slot);

        if (container == null) throw AccessoriesCommands.ERROR_CONTAINER_MISSING.create(slot);

        return container;
    }

    protected static void generateTrees(BranchedCommandGenerator generator, CommandBuildContext context, Commands.CommandSelection environment) {
        generator.modifyRootNode(builder -> builder.requires(stack -> stack.hasPermission(Commands.LEVEL_GAMEMASTERS)));

        if (environment.includeIntegrated) {
            generator.branch("rendering", renderingBranch -> {
                renderingBranch.leaves(
                        "create-renderer-stack",
                        required("renderer_id", ResourceLocationArgument.id(), (ctx, name) -> ctx.getArgument(name, ResourceLocation.class)),
                        required("item_model_id", ResourceLocationArgument.id(), (ctx, name) -> ctx.getArgument(name, ResourceLocation.class)),
                        required("custom_name", ComponentArgument.textComponent(context), ComponentArgument::getResolvedComponent),
                        defaulted("is_bundle", BoolArgumentType.bool(), (ctx, name) -> ctx.getArgument(name, Boolean.class), false),
                        (ctx, rendererId, itemModelId, component, isBundle) -> {
                            AccessoriesCommands.createRenderStack(ctx, rendererId, itemModelId, component, isBundle);
                            return 0;
                        }
                ).leaves(
                        "listen-to-renderer",
                        defaulted("item_model_id", ResourceLocationArgument.id(), (ctx, name) -> ctx.getArgument(name, ResourceLocation.class), null),
                        (ctx, id) -> {
                            CustomRendererLoader.constantFileResolving(ctx.getSource().getServer(), id);

                            return 1;
                        }
                );
            });
        }

        generator.leaves(
                "edit",
                defaulted("entity", EntityArgument.entity(), AccessoriesCommands::getOrThrowLivingEntity, null),
                (ctx, livingEntity) -> {
                    Accessories.askPlayerForVariant(ctx.getSource().getPlayerOrException(), livingEntity);

                    return 1;
                });

        var logFailureType = new DynamicCommandExceptionType(branch -> Component.literal("Unable to locate the given logging for the following command branch: " + branch));

        generator.leaves(
                "dump",
                branches("slots", "groups", "entity_bindings"),
                (ctx, branch) -> {
                    switch (branch) {
                        case "slots" -> {
                            LOGGER.info("All given Slots registered:");

                            for (var slotType : SlotTypeLoader.INSTANCE.getEntries(ctx.getSource().getLevel()).values()) {
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

        AccessoriesItemCommands.generateTrees(generator, context);

        generator.branch("slot", slotBranch -> {
            slotBranch
                    .leaves(
                            "get",
                            required("entity", EntityArgument.entity(), EntityArgument::getEntity),
                            required("slot", SlotArgumentType.INSTANCE, SlotArgumentType::getSlot),
                            defaulted("scale", DoubleArgumentType.doubleArg(), DoubleArgumentType::getDouble, 1.0),
                            (ctx, entity, slot, scale) -> {
                                var container = getContainer(entity, slot);

                                var size = container.getSize();

                                ctx.getSource().sendSuccess(
                                        () -> Component.translatable("accessories.commands.slot.value.get.success", Component.translatable(SlotType.translation(slot)), entity.getName(), size),
                                        false
                                );

                                return (int)(size * scale);
                            }
                    )
                    .branch(
                            "modifier",
                            required("entity", EntityArgument.entity(), EntityArgument::getEntity),
                            required("slot", SlotArgumentType.INSTANCE, SlotArgumentType::getSlot),
                            required("id", ResourceLocationArgument.id(), ResourceLocationArgument::getId),
                            branchBuilder -> {
                                branchBuilder.leaves(
                                        "add",
                                        required("amount", DoubleArgumentType.doubleArg(), DoubleArgumentType::getDouble),
                                        branches(List.of("add_value", "add_multiplied_base", "add_multiplied_total"), operationTypeStr -> {
                                            return Arrays.stream(AttributeModifier.Operation.values())
                                                    .filter(value -> value.getSerializedName().equals(operationTypeStr))
                                                    .findFirst()
                                                    .orElse(null);
                                        }),
                                        defaulted("is_persistent", BoolArgumentType.bool(), BoolArgumentType::getBool, true),
                                        (ctx, entity, slot, id, amount, operation, isPersistent) -> {
                                            var container = getContainer(entity, slot);

                                            var modifier = new AttributeModifier(id, amount, operation);

                                            if (isPersistent) {
                                                container.addPersistentModifier(modifier);
                                            } else {
                                                container.addTransientModifier(modifier);
                                            }

                                            return 1;
                                        }
                                ).leaves(
                                        "remove",
                                        (ctx, entity, slot, id) -> {
                                            var container = getContainer(entity, slot);

                                            container.removeModifier(id);

                                            return 1;
                                        }
                                ).leaves(
                                        "get",
                                        defaulted("scale", DoubleArgumentType.doubleArg(), DoubleArgumentType::getDouble, 1.0),
                                        (ctx, entity, slot, id, scale) -> {
                                            var container = getContainer(entity, slot);
                                            var modifiers = container.getModifiers();
                                            var attribute = SlotAttribute.getAttributeHolder(container.slotType());

                                            if (!modifiers.containsKey(id)) {
                                                throw ERROR_NO_SUCH_MODIFIER.create(entity.getName(), getAttributeDescription(attribute), id);
                                            }

                                            double d = modifiers.get(id).amount();
                                            ctx.getSource().sendSuccess(
                                                    () -> Component.translatable(
                                                            "commands.attribute.modifier.value.get.success", Component.translationArg(id), getAttributeDescription(attribute), entity.getName(), d
                                                    ),
                                                    false
                                            );
                                            return (int)(d * scale);
                                        }
                                );
                            });
        });

        generator.branch("components", itemComponentBranch -> {
            itemComponentBranch.leaves(
                    "effect/add",
                    required("effect", ResourceArgument.resource(context, Registries.MOB_EFFECT), ResourceArgument::getMobEffect),
                    defaulted("applyDelay", IntegerArgumentType.integer(1, 1000000), IntegerArgumentType::getInteger, null),
                    defaulted("seconds", IntegerArgumentType.integer(1, 1000000), IntegerArgumentType::getInteger, -1),
                    defaulted("amplifier", IntegerArgumentType.integer(0, 255), IntegerArgumentType::getInteger, 1),
                    defaulted("hideParticles", BoolArgumentType.bool(), BoolArgumentType::getBool, null),
                    defaulted("hideIcon", BoolArgumentType.bool(), BoolArgumentType::getBool, null),
                    (ctx, effect, applyDelay, seconds, amplifier, hideParticles, hideIcon) -> {
                        if (seconds == -1) {
                            if (hideParticles == null) hideParticles = true;
                        }

                        if (hideIcon == null) hideIcon = false;

                        var effectInstance = new MobEffectInstance(effect, seconds, amplifier, false, !hideParticles, !hideIcon);

                        var player = ctx.getSource().getPlayerOrException();

                        player.getMainHandItem().update(
                                AccessoriesDataComponents.MOB_EFFECTS,
                                AccessoryMobEffectsComponent.EMPTY,
                                data -> applyDelay != null
                                        ? data.addEffect(effectInstance, applyDelay)
                                        : data.addEffect(effectInstance));

                        return 1;
                    }
            );

            //--

            itemComponentBranch.leaves(
                    "nest",
                    required("item", ItemArgument.item(context), (ctx, name) -> ItemArgument.getItem(ctx, name).createItemStack(1, false)),
                    (ctx, innerStack) -> {
                        var player = ctx.getSource().getPlayerOrException();

                        player.getMainHandItem().update(
                                AccessoriesDataComponents.NESTED_ACCESSORIES,
                                AccessoryNestContainerContents.EMPTY,
                                data -> data.addStack(innerStack));

                        return 1;
                    });

            //--

            itemComponentBranch.leaves(
                    "slot",
                    branches("add", "remove"),
                    branches("valid", "invalid"),
                    required("slot", SlotArgumentType.INSTANCE, SlotArgumentType::getSlot),
                    (ctx, operation, condition, slot) -> adjustSlotValidationOnStack(condition, Objects.equals(operation, "add"), slot, ctx)
            );

            //--

            itemComponentBranch.branch("stack-sizing", branchBuilder -> {
                branchBuilder.leaves(
                        "useStackSize",
                        required("value", BoolArgumentType.bool(), (ctx, name) -> ctx.getArgument(name, Boolean.class)),
                        (ctx, bl) -> {
                            var player = ctx.getSource().getPlayerOrException();

                            player.getMainHandItem().update(AccessoriesDataComponents.STACK_SETTINGS,
                                    AccessoryStackSettings.DEFAULT,
                                    component -> component.useStackSize(bl));

                            return 1;
                        }
                ).leaves(
                        required("size", IntegerArgumentType.integer(), (ctx, name) -> ctx.getArgument(name, Integer.class)),
                        (ctx, size) -> {
                            var player = ctx.getSource().getPlayerOrException();

                            player.getMainHandItem().update(AccessoriesDataComponents.STACK_SETTINGS,
                                    AccessoryStackSettings.DEFAULT,
                                    component -> component.sizeOverride(size));

                            return 1;
                        }
                );
            });

            //--

            itemComponentBranch.branch(
                    "attribute",
                    required("attribute", ResourceExtendedArgument.attributes(context), ResourceExtendedArgument::getAttribute),
                    required("id", ResourceLocationArgument.id(), ResourceLocationArgument::getId),
                    branchBuilder -> {
                        branchBuilder.leaves(
                                "add",
                                required("amount", DoubleArgumentType.doubleArg(), DoubleArgumentType::getDouble),
                                branches(List.of("add_value", "add_multiplied_base", "add_multiplied_total"), operationTypeStr -> {
                                    return Arrays.stream(AttributeModifier.Operation.values())
                                            .filter(value -> value.getSerializedName().equals(operationTypeStr))
                                            .findFirst()
                                            .orElse(null);
                                }),
                                required("slot", SlotArgumentType.INSTANCE, SlotArgumentType::getSlot),
                                required("isStackable", BoolArgumentType.bool(), BoolArgumentType::getBool),
                                AccessoriesCommands::addModifier
                        ).leaves(
                                "remove",
                                AccessoriesCommands::removeModifier
                        ).leaves(
                                "get",
                                defaulted("scale", DoubleArgumentType.doubleArg(), DoubleArgumentType::getDouble, 1.0),
                                AccessoriesCommands::getAttributeModifier
                        );
                    });
        });
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
                        "accessories.commands.attribute.modifier.value.get.success_itemstack", Component.translationArg(resourceLocation), getAttributeDescription(holder), stack.getDisplayName(), e
                ),
                false
        );

        return (int)(e * d);
    }

    private static final Dynamic3CommandExceptionType ERROR_MODIFIER_ALREADY_PRESENT = new Dynamic3CommandExceptionType(
            (var1, var2, var3) -> Component.translatableEscape("accessories.commands.attribute.failed.modifier_already_present_itemstack", var1, var2, var3)
    );

    private static int addModifier(CommandContext<CommandSourceStack> ctx, Holder<Attribute> holder, ResourceLocation resourceLocation, double d, AttributeModifier.Operation operation, String slotName, boolean isStackable) throws CommandSyntaxException {
        var commandSourceStack = ctx.getSource();

        if (operation == null) {
            commandSourceStack.sendFailure(Component.literal("Unable to locate AttributeModifier Operation type passed to the command!"));

            return -1;
        }

        var livingEntity = ctx.getSource().getPlayerOrException();
        var stack = livingEntity.getMainHandItem();

        var component = stack.getOrDefault(AccessoriesDataComponents.ATTRIBUTES, AccessoryItemAttributeModifiers.EMPTY);

        if (component.hasModifier(holder, resourceLocation)) {
            throw ERROR_MODIFIER_ALREADY_PRESENT.create(resourceLocation, getAttributeDescription(holder), stack.getDisplayName());
        }

        stack.set(AccessoriesDataComponents.ATTRIBUTES, component.withModifierAdded(holder, new AttributeModifier(resourceLocation, d, operation), slotName, isStackable));

        commandSourceStack.sendSuccess(
                () -> Component.translatable(
                        "accessories.commands.attribute.modifier.add.success_itemstack", Component.translationArg(resourceLocation), getAttributeDescription(holder), stack.getDisplayName()
                ),
                false
        );

        return 1;
    }

    private static final Dynamic3CommandExceptionType ERROR_NO_SUCH_MODIFIER = new Dynamic3CommandExceptionType(
            (var1, var2, var3) -> Component.translatableEscape("accessories.commands.attribute.failed.no_modifier_itemstack", var1, var2, var3)
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
            throw ERROR_NO_SUCH_MODIFIER.create(location, stack.getDisplayName(), getAttributeDescription(holder));
        }

        commandSourceStack.sendSuccess(
                () -> Component.translatable(
                        "accessories.commands.attribute.modifier.remove.success_itemstack", Component.translationArg(location), getAttributeDescription(holder), stack.getDisplayName()
                ),
                false
        );

        return 1;
    }

    private static Component getAttributeDescription(Holder<Attribute> attribute) {
        return Component.translatable(attribute.value().getDescriptionId());
    }

    private static int adjustSlotValidationOnStack(String branch, boolean addSlot, String slotName, CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        LivingEntity targetEntity = ctx.getSource().getPlayerOrException();

        targetEntity.getMainHandItem().update(AccessoriesDataComponents.SLOT_VALIDATION, AccessorySlotValidationComponent.EMPTY, component -> {
            return (Objects.equals(branch, "valid"))
                    ? (addSlot ? component.addValidSlot(slotName) : component.removeValidSlot(slotName))
                    : (addSlot ? component.addInvalidSlot(slotName) : component.removeInvalidSlot(slotName));
        });

        return 1;
    }

    private static int createRenderStack(CommandContext<CommandSourceStack> ctx, ResourceLocation rendererId, ResourceLocation modelId, Component component, boolean isBundle) throws CommandSyntaxException {
        Item item = Items.STICK;

        try {
            if (ctx.getArgument("is_bundle", Boolean.class)) item = Items.BUNDLE;
        } catch (Throwable ignored) {}

        var itemStack = item.getDefaultInstance();

        itemStack.set(DataComponents.ITEM_NAME, component);

        itemStack.set(
                AccessoriesDataComponents.CUSTOM_RENDERER,
                new AccessoryCustomRendererComponent(
                        List.of(new RenderingFunction.DeferredRenderer(rendererId, Map.of(), RenderingFunction.ArmTarget.BOTH)), null, false)
        );

        itemStack.set(DataComponents.ITEM_MODEL, modelId);

        itemStack.set(
                AccessoriesDataComponents.SLOT_VALIDATION,
                new AccessorySlotValidationComponent(Set.of("any"), Set.of())
        );

        ctx.getSource().getPlayerOrException()
                .addItem(itemStack);

        return 1;
    }
}
