package io.wispforest.accessories.commands;


import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.datafixers.util.Either;
import io.wispforest.accessories.api.slot.SlotPath;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.commands.api.CommandTreeGenerator;
import io.wispforest.accessories.commands.api.base.BranchedCommandGenerator;
import io.wispforest.accessories.commands.api.core.NamedArgumentGetter;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceOrIdArgument;
import net.minecraft.commands.arguments.SlotArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.Optional;

public class AccessoriesItemCommands implements CommandTreeGenerator.Branched {

    public static final AccessoriesItemCommands INSTANCE = new AccessoriesItemCommands();

    private AccessoriesItemCommands() {}

	private static final Dynamic3CommandExceptionType ERROR_TARGET_NOT_A_CONTAINER = new Dynamic3CommandExceptionType(
		(object, object2, object3) -> Component.translatableEscape("commands.item.target.not_a_container", object, object2, object3)
	);

	private static final Dynamic3CommandExceptionType ERROR_SOURCE_NOT_A_CONTAINER = new Dynamic3CommandExceptionType(
		(object, object2, object3) -> Component.translatableEscape("commands.item.source.not_a_container", object, object2, object3)
	);

	private static final DynamicCommandExceptionType ERROR_TARGET_INAPPLICABLE_SLOT = new DynamicCommandExceptionType(
		object -> Component.translatableEscape("commands.item.target.no_such_slot", object)
	);

	private static final DynamicCommandExceptionType ERROR_SOURCE_INAPPLICABLE_SLOT = new DynamicCommandExceptionType(
		object -> Component.translatableEscape("commands.item.source.no_such_slot", object)
	);

	private static final SuggestionProvider<CommandSourceStack> SUGGEST_MODIFIER = (commandContext, suggestionsBuilder) -> {
		return SharedSuggestionProvider.listSuggestions(commandContext, suggestionsBuilder, Registries.ITEM_MODIFIER, SharedSuggestionProvider.ElementSuggestionType.ELEMENTS);
	};

    @Override
    public <T> NamedArgumentGetter<CommandSourceStack, T> getArgumentGetter(ArgumentType<T> type) {
        var getter = AccessoriesCommands.getArgumentGetterErased(type);

        return getter != null ? (NamedArgumentGetter<CommandSourceStack, T>) getter : Branched.super.getArgumentGetter(type);
    }

    @Override
    public void generateTrees(BranchedCommandGenerator generator, CommandBuildContext context, Commands.CommandSelection environment) {
		var slotArg = required("slot", SlotArgument.slot());

		var blockArg = required("pos", BlockPosArgument.blockPos(), BlockPosArgument::getLoadedBlockPos);

		var modifierArg = defaulted("modifier", ResourceOrIdArgument.lootModifier(context), ResourceOrIdArgument::getLootModifier, null, SUGGEST_MODIFIER);

		var sourceSlotArg = required("sourceSlot", SlotArgument.slot());

		generator
				.branch("item")
				.branch("replace", replaceBranch -> {
					replaceBranch
							.branch(
									"block",
									blockArg,
									slotArg,
									blockBranch -> {
										blockBranch.branch("from", fromBranch -> {
											fromBranch.leaves(
													"entity",
													required("source_entity", EntityArgument.entity(), EntityArgument::getEntity),
													required("source_path", AccessoriesMixedSlotArgument.slot("source_entity")),
													modifierArg,
													(ctx, targetPos, targetSlot, sourceEntity, sourceSlot, modifier) -> {
														return (modifier == null)
																? entityToBlock(ctx.getSource(), sourceEntity, sourceSlot, targetPos, targetSlot)
																: entityToBlock(ctx.getSource(), sourceEntity, sourceSlot, targetPos, targetSlot, modifier);
													}
											);
										});
									}
							)
							.branch(
									"entity",
									required("entity", EntityArgument.entity(), EntityArgument::getEntity),
									required("path", AccessoriesMixedSlotArgument.slot("entity")),
									entityBranch -> {
										entityBranch
												.leaves(
														"with",
														required("item", ItemArgument.item(context), (ctx, name) -> ItemArgument.getItem(ctx, name).createItemStack(1, false)),
														defaulted("count", IntegerArgumentType.integer(1, 99), 1),
														(ctx, entity, slot, stack, count) -> {
															stack.setCount(count);

															return setEntityItem(ctx.getSource(), entity, slot, stack);
														}
												)
												.branch("from", fromBranch -> {
													fromBranch.leaves(
															"block",
															required("source", BlockPosArgument.blockPos(), BlockPosArgument::getLoadedBlockPos),
															sourceSlotArg,
															modifierArg,
															(ctx, targetPos, targetSlot, sourcePos, sourceSlot, modifier) -> {
																return (modifier == null)
																		? blockToEntity(ctx.getSource(), sourcePos, sourceSlot, targetPos, targetSlot)
																		: blockToEntity(ctx.getSource(), sourcePos, sourceSlot, targetPos, targetSlot, modifier);
															}
													).leaves(
															"entity",
															required("source_entity", EntityArgument.entity(), EntityArgument::getEntity),
															required("source_path", AccessoriesMixedSlotArgument.slot("source_entity")),
															modifierArg,
															(ctx, targetEntity, targetPath, sourceEntity, sourcePath, modifier) -> {
																return (modifier == null)
																		? entityToEntity(ctx.getSource(), sourceEntity, sourcePath, targetEntity, targetPath)
																		: entityToEntity(ctx.getSource(), sourceEntity, sourcePath, targetEntity, targetPath, modifier);
															}
													);
												});
							}
					);
				})
				.branch("modify", modifyBranch -> {
					modifyBranch.leaves(
							"entity",
							required("entity", EntityArgument.entity(), EntityArgument::getEntity),
							required("path", AccessoriesMixedSlotArgument.slot("entity")),
							modifierArg,
							(ctx, entity, path, modifier) -> modifyEntityItem(ctx.getSource(), entity, path, modifier)
					);
				});
	}

	private static int blockToEntity(CommandSourceStack source, BlockPos pos, int sourceSlot, Entity target, Either<SlotPath, Integer> slot) throws CommandSyntaxException {
		return setEntityItem(source, target, slot, getBlockItem(source, pos, sourceSlot));
	}

	private static int blockToEntity(CommandSourceStack source, BlockPos pos, int sourceSlot, Entity target, Either<SlotPath, Integer> slot, Holder<LootItemFunction> modifier) throws CommandSyntaxException {
		return setEntityItem(source, target, slot, applyModifier(source, modifier, getBlockItem(source, pos, sourceSlot)));
	}

	private static int entityToBlock(CommandSourceStack source, Entity sourceEntity, Either<SlotPath, Integer> sourceSlot, BlockPos pos, int slot) throws CommandSyntaxException {
		return setBlockItem(source, pos, slot, getEntityItem(sourceEntity, sourceSlot));
	}

	private static int entityToBlock(CommandSourceStack source, Entity sourceEntity, Either<SlotPath, Integer> sourceSlot, BlockPos pos, int slot, Holder<LootItemFunction> modifier) throws CommandSyntaxException {
		return setBlockItem(source, pos, slot, applyModifier(source, modifier, getEntityItem(sourceEntity, sourceSlot)));
	}

	private static int entityToEntity(CommandSourceStack source, Entity sourceEntity, Either<SlotPath, Integer> sourceSlot, Entity targetEntity, Either<SlotPath, Integer> targetSlot) throws CommandSyntaxException {
		return setEntityItem(source, targetEntity, targetSlot, getEntityItem(sourceEntity, sourceSlot));
	}

	private static int entityToEntity(CommandSourceStack source, Entity sourceEntity, Either<SlotPath, Integer> sourceSlot, Entity targetEntity, Either<SlotPath, Integer> targetSlot, Holder<LootItemFunction> modifier) throws CommandSyntaxException {
		return setEntityItem(source, targetEntity, targetSlot, applyModifier(source, modifier, getEntityItem(sourceEntity, sourceSlot)));
	}

	private static ItemStack applyModifier(CommandSourceStack source, Holder<LootItemFunction> modifier, ItemStack originalStack) {
		var lootParams = new LootParams.Builder(source.getLevel())
			.withParameter(LootContextParams.ORIGIN, source.getPosition())
			.withOptionalParameter(LootContextParams.THIS_ENTITY, source.getEntity())
			.create(LootContextParamSets.COMMAND);

		var lootContext = new LootContext.Builder(lootParams).create(Optional.empty());

		lootContext.pushVisitedElement(LootContext.createVisitedEntry(modifier.value()));

		var modifiedStack = modifier.value().apply(originalStack, lootContext);

		modifiedStack.limitSize(modifiedStack.getMaxStackSize());

		return modifiedStack;
	}

	public static final Dynamic2CommandExceptionType ERROR_INVALID_SLOT_INDEX = new Dynamic2CommandExceptionType((ob1, ob2) -> Component.literal("The given path for [" + ob1 + "] container is invalid: [Path: " + ob2 +  "]"));

	//--

	private static ItemStack getEntityItem(Entity entity, Either<SlotPath, Integer> slot) throws CommandSyntaxException {
		if (slot.right().isPresent()) {
			var index = slot.right().get();

			SlotAccess slotAccess = entity.getSlot(index);

			if (slotAccess == SlotAccess.NULL) throw ERROR_SOURCE_INAPPLICABLE_SLOT.create(slot);

			return slotAccess.get().copy();
		} else {
			if(!(entity instanceof LivingEntity livingEntity)) throw AccessoriesCommands.NON_LIVING_ENTITY_TARGET.create();
			if (livingEntity.accessoriesCapability() == null) throw AccessoriesCommands.ERROR_CAPABILITY_MISSING.create();

			var slotPath = slot.left().get();
			var reference = SlotReference.of(livingEntity, slotPath);

			var container = reference.slotContainer();

			if (container == null) throw AccessoriesCommands.ERROR_CONTAINER_MISSING.create(reference.slotName());

			var stack = reference.getStack();

			if (stack == null) throw ERROR_INVALID_SLOT_INDEX.create(slotPath.slotName(), slotPath);

			return stack.copy();
		}
	}

	private static int setEntityItem(CommandSourceStack source, Entity entity, Either<SlotPath, Integer> slot, ItemStack stack) throws CommandSyntaxException {
		if (slot.right().isPresent()) {
			var index = slot.right().get();

			SlotAccess slotAccess = entity.getSlot(index);

			if (slotAccess == SlotAccess.NULL) throw ERROR_SOURCE_INAPPLICABLE_SLOT.create(slot);

			slotAccess.set(stack);
		} else {
			if(!(entity instanceof LivingEntity livingEntity)) throw AccessoriesCommands.NON_LIVING_ENTITY_TARGET.create();
			var slotPath = slot.left().get();
			var reference = SlotReference.of(livingEntity, slotPath);

			var container = reference.slotContainer();

			if (container == null) throw AccessoriesCommands.ERROR_CONTAINER_MISSING.create(reference.slotName());

			if (reference.setStack(stack)) throw ERROR_INVALID_SLOT_INDEX.create(slotPath.slotName(), slotPath);
		}

		return 1;
	}

	//--

	private static ItemStack getBlockItem(CommandSourceStack source, BlockPos pos, int slot) throws CommandSyntaxException {
		var container = getContainer(source, pos, ERROR_SOURCE_NOT_A_CONTAINER);

		if (slot >= 0 && slot < container.getContainerSize()) return container.getItem(slot).copy();

		throw ERROR_SOURCE_INAPPLICABLE_SLOT.create(slot);
	}

	private static int setBlockItem(CommandSourceStack source, BlockPos pos, int slot, ItemStack item) throws CommandSyntaxException {
		var container = getContainer(source, pos, ERROR_TARGET_NOT_A_CONTAINER);

		if (slot >= 0 && slot < container.getContainerSize()) {
			container.setItem(slot, item);
			source.sendSuccess(() -> Component.translatable("commands.item.block.set.success", pos.getX(), pos.getY(), pos.getZ(), item.getDisplayName()), true);
			return 1;
		}

		throw ERROR_TARGET_INAPPLICABLE_SLOT.create(slot);
	}

	private static Container getContainer(CommandSourceStack source, BlockPos pos, Dynamic3CommandExceptionType exception) throws CommandSyntaxException {
		if (source.getLevel().getBlockEntity(pos) instanceof Container container) return container;

		throw exception.create(pos.getX(), pos.getY(), pos.getZ());
	}

	//--

	private static int modifyEntityItem(CommandSourceStack source, Entity target, Either<SlotPath, Integer> slot, Holder<LootItemFunction> modifer) throws CommandSyntaxException {
		ItemStack modifiedStack = applyModifier(source, modifer, getEntityItem(target, slot).copy());

		setEntityItem(source, target, slot, modifiedStack);

		source.sendSuccess(
				() -> Component.translatable("commands.item.entity.set.success.single", target.getDisplayName(), modifiedStack.getDisplayName()),
				true
		);

		return 1;
	}
}
