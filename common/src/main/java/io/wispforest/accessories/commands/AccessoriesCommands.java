package io.wispforest.accessories.commands;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.*;
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
import io.wispforest.accessories.api.data.AccessoriesBaseData;
import io.wispforest.accessories.commands.api.CommandGenerators;
import io.wispforest.accessories.commands.api.CommandTreeGenerator;
import io.wispforest.accessories.commands.api.base.BranchedCommandGenerator;
import io.wispforest.accessories.commands.api.core.NamedArgumentGetter;
import io.wispforest.accessories.commands.api.core.RecordArgumentTypeInfo;
import io.wispforest.accessories.data.CustomRendererLoader;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.data.SlotGroupLoader;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.mixin.CommandSelectionAccessor;
import io.wispforest.accessories.mixin.EnchantCommandAccessor;
import io.wispforest.accessories.mixin.ResourceArgumentAccessor;
import io.wispforest.endec.Endec;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.*;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.commands.EnchantCommand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;

public class AccessoriesCommands implements CommandTreeGenerator.Branched {

    public static final AccessoriesCommands INSTANCE = new AccessoriesCommands();

    private AccessoriesCommands(){}

    public static final SimpleCommandExceptionType NON_LIVING_ENTITY_TARGET = new SimpleCommandExceptionType(Component.translatable("accessories.argument.livingEntities.nonLiving"));

    public static final SimpleCommandExceptionType INVALID_SLOT_TYPE = new SimpleCommandExceptionType(new LiteralMessage("Invalid Slot Type"));

    public static final SimpleCommandExceptionType ERROR_CAPABILITY_MISSING = new SimpleCommandExceptionType(Component.literal("Unable to get the needed capability from the given target!"));

    public static final DynamicCommandExceptionType ERROR_CONTAINER_MISSING = new DynamicCommandExceptionType((obj) -> Component.literal("Unable to get the needed Container from the given target! [Container: " + obj + "]"));

    private static final Logger LOGGER = LogUtils.getLogger();

    public static void init() {
        CommandGenerators.create(
                "accessories",
                AccessoriesCommands.INSTANCE,
                registration -> {
                    registration.register(Accessories.of("slot_type"), SlotArgumentType.class, RecordArgumentTypeInfo.of(ctx -> SlotArgumentType.INSTANCE));
                    registration.register(Accessories.of("resource"), ResourceExtendedArgument.class, RecordArgumentTypeInfo.of(ResourceExtendedArgument::attributes));
                    registration.register(Accessories.of("slot_path"), AccessoriesMixedSlotArgument.class, RecordArgumentTypeInfo.of(Endec.STRING, "entity_argument_name", AccessoriesMixedSlotArgument::entityArgumentName, AccessoriesMixedSlotArgument::new));
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

    @Override
    public <T> NamedArgumentGetter<CommandSourceStack, T> getArgumentGetter(ArgumentType<T> type) {
        var getter = getArgumentGetterErased(type);

        return getter != null ? (NamedArgumentGetter<CommandSourceStack, T>) getter : Branched.super.getArgumentGetter(type);
    }

    @Nullable
    public static <T> NamedArgumentGetter<CommandSourceStack, ?> getArgumentGetterErased(ArgumentType<T> type) {
        if (type instanceof ResourceLocationArgument) return ResourceLocationArgument::getId;
        if (type instanceof ComponentArgument) return ComponentArgument::getResolvedComponent;
        if (type instanceof BoolArgumentType) return BoolArgumentType::getBool;
        if (type instanceof SlotArgumentType) return SlotArgumentType::getSlot;
        if (type instanceof DoubleArgumentType) return DoubleArgumentType::getDouble;
        if (type instanceof IntegerArgumentType) return IntegerArgumentType::getInteger;
        if (type instanceof ResourceExtendedArgument<?>) return ResourceExtendedArgument::getResource;
        if (type instanceof ResourceArgument<?> resourceArgument) {
            var key = (ResourceKey<Registry<Object>>) ((ResourceArgumentAccessor<?>) resourceArgument).accessories$registryKey();
            return (ctx, name) -> ResourceArgument.getResource(ctx, name, key);
        }
        if (type instanceof AccessoriesMixedSlotArgument) return AccessoriesMixedSlotArgument::getSlot;
        if (type instanceof SlotArgument) return SlotArgument::getSlot;

        return null;
    }

    public void generateTrees(BranchedCommandGenerator root, CommandBuildContext context, Commands.CommandSelection environment) {
        root.modifyRootNode(builder -> builder.requires(stack -> stack.hasPermission(Commands.LEVEL_GAMEMASTERS)));

        if (((CommandSelectionAccessor) (Object) environment).accessories$includeIntegrated()) {
            root.branch("rendering", renderingBranch -> {
                renderingBranch.leaves(
                    "create-renderer-stack",
                    required("renderer_id", ResourceLocationArgument.id()),
                    required("item_model_id", ResourceLocationArgument.id()),
                    required("custom_name", ComponentArgument.textComponent(context)),
                    defaulted("is_bundle", BoolArgumentType.bool(), false),
                    (ctx, rendererId, itemModelId, component, isBundle) -> {
                        AccessoriesCommands.createRenderStack(ctx, rendererId, itemModelId, component, isBundle);
                        return 0;
                    }
                ).leaves(
                    "listen-to-renderer",
                    defaulted("item_model_id", ResourceLocationArgument.id(), null),
                    (ctx, id) -> {
                        CustomRendererLoader.constantFileResolving(ctx.getSource().getServer(), id);

                        return 1;
                    }
                );
            });
        }

        var toggleableFeatures = List.of("banner", "glider", "totem");

        var featureToggleFailure = new DynamicCommandExceptionType(branch -> Component.translatable("accessories.commands.feature.toggle.failure", branch, toggleableFeatures.toString()));

        root.leaves(
            "feature",
            branches("enable", "disable"),
            branches(toggleableFeatures),
            //required("value", BoolArgumentType.bool()),
            (ctx, state, branch) -> {
                var bl = Objects.equals(state, "enable");

                switch (branch) {
                    case "banner" -> Accessories.config().contentOptions.allowBannerEquip(bl);
                    case "glider" -> Accessories.config().contentOptions.allowGliderEquip(bl);
                    case "totem" -> Accessories.config().contentOptions.allowTotemEquip(bl);
                    case null, default -> throw featureToggleFailure.create(branch);
                }

                ctx.getSource().sendSystemMessage(Component.translatable("accessories.commands.feature.toggle." + (bl ? "on" : "off"), branch));

                return 1;
            }
        );

        root.leaves(
            "edit",
            defaulted("entity", EntityArgument.entity(), AccessoriesCommands::getOrThrowLivingEntity, null),
            (ctx, livingEntity) -> {
                Accessories.askPlayerForVariant(ctx.getSource().getPlayerOrException(), livingEntity);

                return 1;
            });

        var validLoggingBranches = List.of("slots", "groups", "entity_bindings");

        var logFailureType = new DynamicCommandExceptionType(branch -> Component.translatable("accessories.commands.dump.failure", branch, validLoggingBranches.toString()));

        root.leaves(
            "dump",
            branches(validLoggingBranches),
            (ctx, branch) -> {
                switch (branch) {
                    case "slots" -> {
                        LOGGER.info("All given Slots registered:");

                        for (var slotType : SlotTypeLoader.INSTANCE.getEntries(ctx.getSource().getLevel()).values()) {
                            LOGGER.info(slotType.dumpData());
                        }
                    }
                    case "groups" -> {
                        LOGGER.info("All given Slot Groups registered:");

                        for (var group : SlotGroupLoader.getGroups(ctx.getSource().getLevel())) {
                            LOGGER.info(group.dumpData());
                        }
                    }
                    case "entity_bindings" ->{
                        LOGGER.info("All given Entity Bindings registered:");

                        EntitySlotLoader.INSTANCE.getEntitySlotData(false).forEach((type, slots) -> {
                            LOGGER.info("[EntityType: {}] <-> [Slots: {}]", type, slots.keySet());
                        });
                    }
                    default -> throw logFailureType.create(branch);
                }

                ctx.getSource().sendSystemMessage(Component.translatable("accessories.commands.dump.success", branch));

                return 1;
            }
        );

        AccessoriesItemCommands.INSTANCE.generateTrees(root, context, environment);

        root.branch("slot", slotBranch -> {
            slotBranch
                .leaves(
                    "get",
                    required("entity", EntityArgument.entity(), EntityArgument::getEntity),
                    required("slot", SlotArgumentType.INSTANCE),
                    defaulted("scale", DoubleArgumentType.doubleArg(), 1.0),
                    (ctx, entity, slot, scale) -> {
                        var container = getContainer(entity, slot);

                        var size = container.getSize();

                        ctx.getSource().sendSuccess(
                                () -> Component.translatable("accessories.commands.slot.value.get.success", Component.translatable(Accessories.translationKey("slot." + slot.replace(":", "."))), entity.getName(), size),
                                false
                        );

                        return (int)(size * scale);
                    }
                )
                .branch("modifier", modiferBranch -> {
                    modiferBranch
                        .leaves(
                            "clear",
                            required("entity", EntityArgument.entity(), EntityArgument::getEntity),
                            defaulted("slot", SlotArgumentType.INSTANCE, ""),
                            (ctx, entity, s) -> {
                                if (s.isBlank()) {
                                    var capability = getCapability(entity);

                                    capability.clearSlotModifiers();

                                    ctx.getSource().sendSuccess(
                                        () -> Component.translatable(
                                            "accessories.commands.slot.modifier.clear.all.success", entity.getName()
                                        ),
                                        false
                                    );
                                } else {
                                    var container = getContainer(entity, s);

                                    container.clearModifiers();

                                    ctx.getSource().sendSuccess(
                                        () -> Component.translatable(
                                            "accessories.commands.slot.modifier.clear.container.success", s, entity.getName()
                                        ),
                                        false
                                    );
                                }

                                return 1;
                            }
                        )
                        .branch(
                            required("entity", EntityArgument.entity(), EntityArgument::getEntity),
                            required("slot", SlotArgumentType.INSTANCE),
                            required("id", ResourceLocationArgument.id()),
                            branchBuilder -> {
                                branchBuilder.leaves(
                                    "add",
                                    required("amount", DoubleArgumentType.doubleArg()),
                                    branches(List.of("add_value", "add_multiplied_base", "add_multiplied_total"), operationTypeStr -> {
                                        return Arrays.stream(AttributeModifier.Operation.values())
                                            .filter(value -> value.getSerializedName().equals(operationTypeStr))
                                            .findFirst()
                                            .orElse(null);
                                    }),
                                    defaulted("is_persistent", BoolArgumentType.bool(), true),
                                    (ctx, entity, slot, id, amount, operation, isPersistent) -> {
                                        var container = getContainer(entity, slot);

                                        var modifier = new AttributeModifier(id, amount, operation);

                                        if (isPersistent) {
                                            container.addPersistentModifier(modifier);
                                        } else {
                                            container.addTransientModifier(modifier);
                                        }

                                        ctx.getSource().sendSystemMessage(Component.translatable("accessories.commands.slot.modifier.addition", id, slot, entity.getDisplayName()));

                                        return 1;
                                    }
                                ).leaves(
                                    "remove",
                                    (ctx, entity, slot, id) -> {
                                        var container = getContainer(entity, slot);

                                        var doseExist = container.hasModifier(id);

                                        if(doseExist) container.removeModifier(id);

                                        var messageType = (doseExist
                                            ? "accessories.commands.slot.modifier.removed.success"
                                            : "accessories.commands.slot.modifier.removed.failure");

                                        ctx.getSource().sendSystemMessage(Component.translatable(messageType, id, slot, entity.getDisplayName()));

                                        return 1;
                                    }
                                ).leaves(
                                    "get",
                                    defaulted("scale", DoubleArgumentType.doubleArg(), 1.0),
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
        });

        root.branch("enchant", enchantBranch -> {
            enchantBranch.leaves(
                required("targets", EntityArgument.entities(), EntityArgument::getEntities),
                required("enchantment", ResourceArgument.resource(context, Registries.ENCHANTMENT), ResourceArgument::getEnchantment),
                defaulted("applyDelay", IntegerArgumentType.integer(0), 1),
                (ctx, targets, enchantmentRef, level) -> {
                    var source = ctx.getSource();
                    var enchantment = enchantmentRef.value();

                    if (level > enchantment.getMaxLevel()) throw EnchantCommandAccessor.accessories$ERROR_LEVEL_TOO_HIGH().create(level, enchantment.getMaxLevel());

                    int i = 0;

                    var isSingleTarget = targets.size() == 1;

                    for (var entity : targets) {
                        if (entity instanceof LivingEntity livingEntity) {
                            var itemStack = livingEntity.getMainHandItem();
                            if (!itemStack.isEmpty()) {
                                if (EnchantmentHelper.isEnchantmentCompatible(EnchantmentHelper.getEnchantmentsForCrafting(itemStack).keySet(), enchantmentRef)) {
                                    itemStack.enchant(enchantmentRef, level);
                                    i++;
                                } else if (isSingleTarget) {
                                    throw EnchantCommandAccessor.accessories$ERROR_INCOMPATIBLE().create(itemStack.getHoverName().getString());
                                }
                            } else if (isSingleTarget) {
                                throw EnchantCommandAccessor.accessories$ERROR_NO_ITEM().create(livingEntity.getName().getString());
                            }
                        } else if (isSingleTarget) {
                            throw EnchantCommandAccessor.accessories$ERROR_NOT_LIVING_ENTITY().create(entity.getName().getString());
                        }
                    }

                    if (i == 0) throw EnchantCommandAccessor.accessories$ERROR_NOTHING_HAPPENED().create();

                    var fullname = Enchantment.getFullname(enchantmentRef, level);

                    source.sendSuccess(() -> isSingleTarget
                        ? Component.translatable("commands.enchant.success.single", fullname, targets.iterator().next().getDisplayName())
                        : Component.translatable("commands.enchant.success.multiple", fullname, targets.size()), true);

                    return i;
                }
            );
        });

        root.branch("components", itemComponentBranch -> {
            itemComponentBranch.leaves(
                "effect/add",
                required("effect", ResourceArgument.resource(context, Registries.MOB_EFFECT)),
                defaulted("applyDelay", IntegerArgumentType.integer(1, 1000000), null),
                defaulted("seconds", IntegerArgumentType.integer(1, 1000000), -1),
                defaulted("amplifier", IntegerArgumentType.integer(0, 255), 1),
                defaulted("hideParticles", BoolArgumentType.bool(), null),
                defaulted("hideIcon", BoolArgumentType.bool(), null),
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

                    ctx.getSource().sendSystemMessage(Component.translatable("accessories.commands.nest.addition"));

                    return 1;
                });

            //--

            itemComponentBranch.leaves(
                "slot",
                branches("add", "remove"),
                branches("valid", "invalid"),
                required("slot", SlotArgumentType.INSTANCE),
                (ctx, operation, condition, slot) -> adjustSlotValidationOnStack(condition, Objects.equals(operation, "add"), slot, ctx)
            );

            //--

            itemComponentBranch.branch("stack-sizing", branchBuilder -> {
                branchBuilder.leaves(
                    "useStackSize",
                    required("value", BoolArgumentType.bool()),
                    (ctx, bl) -> {
                        var player = ctx.getSource().getPlayerOrException();

                        player.getMainHandItem().update(AccessoriesDataComponents.STACK_SETTINGS,
                                AccessoryStackSettings.DEFAULT,
                                component -> component.useStackSize(bl));

                        return 1;
                        }
                ).leaves(
                    required("size", IntegerArgumentType.integer()),
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
                required("attribute", ResourceExtendedArgument.attributes(context)),
                required("id", ResourceLocationArgument.id()),
                branchBuilder -> {
                    branchBuilder.leaves(
                            "add",
                            required("amount", DoubleArgumentType.doubleArg()),
                            branches(List.of("add_value", "add_multiplied_base", "add_multiplied_total"), operationTypeStr -> {
                                return Arrays.stream(AttributeModifier.Operation.values())
                                        .filter(value -> value.getSerializedName().equals(operationTypeStr))
                                        .findFirst()
                                        .orElse(null);
                            }),
                            required("slot", SlotArgumentType.INSTANCE),
                            required("isStackable", BoolArgumentType.bool()),
                            defaulted("usedInSlotValidation", BoolArgumentType.bool(), false),
                            AccessoriesCommands::addModifier
                    ).leaves(
                            "remove",
                            AccessoriesCommands::removeModifier
                    ).leaves(
                            "get",
                            defaulted("scale", DoubleArgumentType.doubleArg(), 1.0),
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

    private static int addModifier(CommandContext<CommandSourceStack> ctx, Holder<Attribute> holder, ResourceLocation resourceLocation, double d, AttributeModifier.Operation operation, String slotName, boolean isStackable, boolean usedInSlotValidation) throws CommandSyntaxException {
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

        stack.set(AccessoriesDataComponents.ATTRIBUTES, component.withModifierAdded(holder, new AttributeModifier(resourceLocation, d, operation), slotName, isStackable, usedInSlotValidation));

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

        ctx.getSource().sendSystemMessage(Component.translatable("accessories.commands.slot.validation." + (addSlot ? "added" : "removed") + "." + (branch), slotName));

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
                new AccessorySlotValidationComponent(Set.of(AccessoriesBaseData.ANY_SLOT), Set.of())
        );

        ctx.getSource().getPlayerOrException()
                .addItem(itemStack);

        return 1;
    }
}
