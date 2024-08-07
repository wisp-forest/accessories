package io.wispforest.accessories.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.wispforest.accessories.data.SlotTypeLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.AngleArgument;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public final class SlotArgumentType implements ArgumentType<String> {

    public static final SlotArgumentType INSTANCE = new SlotArgumentType();

    public static String getSlot(CommandContext<CommandSourceStack> context, String name) {
        return context.getArgument(name, String.class);

    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        ResourceLocation arg = ResourceLocation.read(reader);

        String slotName = "";

        if(arg.getNamespace().equals("minecraft")) {
            slotName = arg.getPath();
        } else {
            slotName = arg.toString();
        }

        if (slotName.equals("any")) return "any";

        var slotType = SlotTypeLoader.INSTANCE.getSlotTypes(false).getOrDefault(slotName, null);

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
