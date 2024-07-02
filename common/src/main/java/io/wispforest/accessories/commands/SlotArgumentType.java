package io.wispforest.accessories.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.wispforest.accessories.data.SlotTypeLoader;
import net.minecraft.commands.SharedSuggestionProvider;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public final class SlotArgumentType implements ArgumentType<String> {

    public static final SlotArgumentType INSTANCE = new SlotArgumentType();

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        var slot = reader.readUnquotedString();

        if (slot.equals("any")) return "any";

        var slotType = SlotTypeLoader.INSTANCE.getSlotTypes(false).getOrDefault(slot, null);

        if (slotType == null) throw AccessoriesCommands.INVALID_SLOT_TYPE.create();

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
