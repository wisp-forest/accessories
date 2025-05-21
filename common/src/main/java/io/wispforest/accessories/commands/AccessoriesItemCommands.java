package io.wispforest.accessories.commands;


import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.*;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.wispforest.accessories.commands.api.base.BranchedCommandGenerator;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
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
import net.minecraft.server.ReloadableServerRegistries;
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
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static io.wispforest.accessories.commands.api.Arguments.defaulted;
import static io.wispforest.accessories.commands.api.Arguments.required;

public class AccessoriesItemCommands {

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
		ReloadableServerRegistries.Holder holder = commandContext.getSource().getServer().reloadableRegistries();
		return SharedSuggestionProvider.suggestResource(holder.getKeys(Registries.ITEM_MODIFIER), suggestionsBuilder);
	};

	protected static void generateTrees(BranchedCommandGenerator generator, CommandBuildContext context) {
		var slotArg = required("slot", SlotArgument.slot(), SlotArgument::getSlot);

		var blockArg = required("pos", BlockPosArgument.blockPos(), BlockPosArgument::getLoadedBlockPos);

		var modifierArg = defaulted("modifier", ResourceOrIdArgument.lootModifier(context), ResourceOrIdArgument::getLootModifier, null, SUGGEST_MODIFIER);

		var sourceSlotArg = required("sourceSlot", SlotArgument.slot(), SlotArgument::getSlot);

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
													required("source_path", AccessoriesSlotArgument.slot("source_entity"), AccessoriesSlotArgument::getSlot),
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
									required("path", AccessoriesSlotArgument.slot("entity"), AccessoriesSlotArgument::getSlot),
									entityBranch -> {
										entityBranch
												.leaves(
														"with",
														required("item", ItemArgument.item(context), (ctx, name) -> ItemArgument.getItem(ctx, name).createItemStack(1, false)),
														defaulted("count", IntegerArgumentType.integer(1, 99), IntegerArgumentType::getInteger, 1),
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
															required("source_path", AccessoriesSlotArgument.slot("source_entity"), AccessoriesSlotArgument::getSlot),
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
							required("path", AccessoriesSlotArgument.slot("entity"), AccessoriesSlotArgument::getSlot),
							modifierArg,
							(ctx, entity, path, modifier) -> modifyEntityItem(ctx.getSource(), entity, path, modifier)
					);
				});
	}

	private static int blockToEntity(CommandSourceStack source, BlockPos pos, int sourceSlot, Entity target, Pair<@Nullable String, Integer> slot) throws CommandSyntaxException {
		return setEntityItem(source, target, slot, getBlockItem(source, pos, sourceSlot));
	}

	private static int blockToEntity(CommandSourceStack source, BlockPos pos, int sourceSlot, Entity target, Pair<@Nullable String, Integer> slot, Holder<LootItemFunction> modifier) throws CommandSyntaxException {
		return setEntityItem(source, target, slot, applyModifier(source, modifier, getBlockItem(source, pos, sourceSlot)));
	}

	private static int entityToBlock(CommandSourceStack source, Entity sourceEntity, Pair<String, Integer> sourceSlot, BlockPos pos, int slot) throws CommandSyntaxException {
		return setBlockItem(source, pos, slot, getEntityItem(sourceEntity, sourceSlot));
	}

	private static int entityToBlock(CommandSourceStack source, Entity sourceEntity, Pair<String, Integer> sourceSlot, BlockPos pos, int slot, Holder<LootItemFunction> modifier) throws CommandSyntaxException {
		return setBlockItem(source, pos, slot, applyModifier(source, modifier, getEntityItem(sourceEntity, sourceSlot)));
	}

	private static int entityToEntity(CommandSourceStack source, Entity sourceEntity, Pair<@Nullable String, Integer> sourceSlot, Entity targetEntity, Pair<@Nullable String, Integer> targetSlot) throws CommandSyntaxException {
		return setEntityItem(source, targetEntity, targetSlot, getEntityItem(sourceEntity, sourceSlot));
	}

	private static int entityToEntity(CommandSourceStack source, Entity sourceEntity, Pair<@Nullable String, Integer> sourceSlot, Entity targetEntity, Pair<@Nullable String, Integer> targetSlot, Holder<LootItemFunction> modifier) throws CommandSyntaxException {
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

	public static final Dynamic2CommandExceptionType ERROR_INVALID_SLOT_INDEX = new Dynamic2CommandExceptionType((ob1, ob2) -> Component.literal("The given index for [" + ob1 + "] container is invalid: [Index: " + ob2 +  "]"));

	//--

	private static ItemStack getEntityItem(Entity entity, Pair<@Nullable String, Integer> slot) throws CommandSyntaxException {
		var path = slot.first();
		var index = slot.second();

		if (path == null) {
			SlotAccess slotAccess = entity.getSlot(index);

			if (slotAccess == SlotAccess.NULL) throw ERROR_SOURCE_INAPPLICABLE_SLOT.create(slot);

			return slotAccess.get().copy();
		}

		if(!(entity instanceof LivingEntity livingEntity)) throw AccessoriesCommands.NON_LIVING_ENTITY_TARGET.create();

		var capability = livingEntity.accessoriesCapability();

		if (capability == null) throw AccessoriesCommands.ERROR_CAPABILITY_MISSING.create();

		var container = capability.getContainers().get(path);

		if (container == null) throw AccessoriesCommands.ERROR_CONTAINER_MISSING.create(path);
		if (!container.getAccessories().validIndex(index)) throw ERROR_INVALID_SLOT_INDEX.create(path, index);

		return container.getAccessories().getItem(index).copy();
	}

	private static int setEntityItem(CommandSourceStack source, Entity entity, Pair<@Nullable String, Integer> slot, ItemStack item) throws CommandSyntaxException {
		var path = slot.first();
		var index = slot.second();

		if (path == null) {
			SlotAccess slotAccess = entity.getSlot(index);

			if (slotAccess == SlotAccess.NULL) throw ERROR_SOURCE_INAPPLICABLE_SLOT.create(slot);

			slotAccess.set(item);
		}

		var capability = AccessoriesCommands.getCapability(entity);

		var container = capability.getContainers().get(path);

		if (container == null) throw AccessoriesCommands.ERROR_CONTAINER_MISSING.create(path);
		if (!container.getAccessories().validIndex(index)) throw ERROR_INVALID_SLOT_INDEX.create(path, index);

		container.getAccessories().setItem(index, item);

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

	private static int modifyEntityItem(CommandSourceStack source, Entity target, Pair<@Nullable String, Integer> slot, Holder<LootItemFunction> modifer) throws CommandSyntaxException {
		ItemStack modifiedStack = applyModifier(source, modifer, getEntityItem(target, slot).copy());

		setEntityItem(source, target, slot, modifiedStack);

		source.sendSuccess(
				() -> Component.translatable("commands.item.entity.set.success.single", target.getDisplayName(), modifiedStack.getDisplayName()),
				true
		);

		return 1;
	}
}
