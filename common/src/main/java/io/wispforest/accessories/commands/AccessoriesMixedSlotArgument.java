package io.wispforest.accessories.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.datafixers.util.Either;
import io.wispforest.accessories.api.slot.SlotPath;
import io.wispforest.accessories.data.EntitySlotLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.ParserUtils;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.SlotRange;
import net.minecraft.world.inventory.SlotRanges;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public record AccessoriesMixedSlotArgument(String entityArgumentName) implements ArgumentType<Either<SlotPath, Integer>> {

    private static final Collection<String> EXAMPLES = Arrays.asList("back/1", "charm/1", "feet.1", "weapon");

    private static final DynamicCommandExceptionType ERROR_UNKNOWN_SLOT = new DynamicCommandExceptionType(
            object -> Component.translatableEscape("slot.unknown", object)
    );

    private static final DynamicCommandExceptionType ERROR_ONLY_SINGLE_SLOT_ALLOWED = new DynamicCommandExceptionType(
            object -> Component.translatableEscape("slot.only_single_allowed", object)
    );

    public static AccessoriesMixedSlotArgument slot(String entityArgumentName) {
        return new AccessoriesMixedSlotArgument(entityArgumentName);
    }

    public static Either<SlotPath, Integer> getSlot(CommandContext<CommandSourceStack> context, String name) {
        return context.getArgument(name, Either.class);
    }

    @Override
    public Either<SlotPath, Integer> parse(StringReader reader) throws CommandSyntaxException {
        String string = ParserUtils.readWhile(reader, c -> c != ' ');
        var slotPath = SlotPath.fromString(string);

        if (slotPath != null) return Either.left(slotPath);

        var vanillaSlot = parseVanillaSlot(reader, string);

        if (vanillaSlot != null) return Either.right(vanillaSlot);

        throw ERROR_UNKNOWN_SLOT.createWithContext(reader, string);
    }

    @Nullable
    public Integer parseVanillaSlot(StringReader reader, String string) throws CommandSyntaxException {
        SlotRange slotRange = SlotRanges.nameToIds(string);

        if (slotRange == null) {
            return null;
        } else if (slotRange.size() != 1) {
            throw ERROR_ONLY_SINGLE_SLOT_ALLOWED.createWithContext(reader, string);
        } else {
            return slotRange.slots().getInt(0);
        }
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
        S object = commandContext.getSource();

        if (object instanceof CommandSourceStack) {
            List<String> suggestions = new ArrayList<>();

            try {
                // TODO: ISSUE THIS DOSE NOT WORK ON FABRIC
                var entityTarget = EntityArgument.getEntity((CommandContext<CommandSourceStack>) commandContext, entityArgumentName);

                if (entityTarget instanceof LivingEntity livingEntity) {
                    var capability = livingEntity.accessoriesCapability();

                    if (capability != null) {
                        suggestions.addAll(
                                EntitySlotLoader.getEntitySlots(livingEntity).values().stream().flatMap(slotType -> {
                                    var slotPaths = new ArrayList<String>();

                                    var container = capability.getContainer(slotType);

                                    if (container != null) {
                                        for (int i = 0; i < container.getSize(); i++) {
                                            slotPaths.add(SlotPath.createBaseSlotPath(slotType, i));
                                        }
                                    }

                                    return slotPaths.stream();
                                }).toList()
                        );
                    }
                }
            } catch (Exception e) {}

            suggestions.addAll(SlotRanges.singleSlotNames().toList());

            return SharedSuggestionProvider.suggest(suggestions, suggestionsBuilder);
        } else {
            return object instanceof SharedSuggestionProvider sharedSuggestionProvider
                    ? sharedSuggestionProvider.customSuggestion(commandContext)
                    : Suggestions.empty();
        }
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
