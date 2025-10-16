package io.wispforest.accessories.commands.api.core;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

public class ContextAwareLiteralArgumentBuilder<S> extends LiteralArgumentBuilder<S> {
    public static final String literalStackKey = "accessories:literal_stack";

    protected ContextAwareLiteralArgumentBuilder(String literal) {
        super(literal);
    }

    public static <S> ContextAwareLiteralArgumentBuilder<S> literal(final String name) {
        return new ContextAwareLiteralArgumentBuilder<>(name);
    }

    public static <S> String getBranch(CommandContext<S> ctx) throws CommandSyntaxException {
        try {
            var argument = ctx.getArgument(literalStackKey, ParsedArgument.class);

            if (!(argument instanceof ParsedArgument)) {
                throw INCORRECT_COMMAND_LITERAL_STACK.create(argument);
            }

            return (String) argument.getResult();
        } catch (Exception e) {
            throw INCORRECT_COMMAND_LITERAL_STACK.create(null);
        }
    }

    public static <S> Optional<LiteralArgumentBuilder<S>> builderFromNode(CommandNode<S> node) {
        if (node instanceof ContextAwareLiteralArgumentBuilder.ContextedLiteralCommandNode<S> node1) {
            return Optional.of(literal(node1.getLiteral()));
        }

        return Optional.empty();
    }

    //--

    @Override
    public LiteralCommandNode<S> build() {
        final LiteralCommandNode<S> result = new ContextedLiteralCommandNode<>(getLiteral(), getCommand(), getRequirement(), getRedirect(), getRedirectModifier(), isFork());

        for (var argument : getArguments()) result.addChild(argument);

        return result;
    }

    @Override
    public String getLiteral() {
        return super.getLiteral();
    }

    private static final DynamicCommandExceptionType INCORRECT_COMMAND_LITERAL_STACK = new DynamicCommandExceptionType(
            object -> () -> (object != null)
                    ? "Invalid command literal stack argument as its currently: " + ((ParsedArgument)object).getResult().toString()
                    : "Invalid command literal stack argument as its currently empty!"
    );

    static class ContextedLiteralCommandNode<S> extends LiteralCommandNode<S> {
        public ContextedLiteralCommandNode(String literal, Command<S> command, Predicate<S> requirement, CommandNode<S> redirect, RedirectModifier<S> modifier, boolean forks) {
            super(literal, command, requirement, redirect, modifier, forks);
        }

        @Override
        public String getLiteral() {
            return super.getLiteral();
        }

        @Override
        public void parse(StringReader reader, CommandContextBuilder<S> contextBuilder) throws CommandSyntaxException {
            super.parse(reader, contextBuilder);

            var range = contextBuilder.getNodes().getLast().getRange();

            var parsedArg = contextBuilder.getArguments().get(literalStackKey);

            if (parsedArg == null) {
                parsedArg = new ParsedArgumentStack<>();
            } else if (!(parsedArg instanceof ParsedArgumentStack)) {
                throw INCORRECT_COMMAND_LITERAL_STACK.create(parsedArg);
            }

            ((ParsedArgumentStack<S, String>) parsedArg).add(new ParsedArgument<>(range.getStart(), range.getEnd(), getLiteral()));

            contextBuilder.withArgument(literalStackKey, parsedArg);
        }

        @Override
        public LiteralArgumentBuilder<S> createBuilder() {
            final LiteralArgumentBuilder<S> builder = literal(this.getLiteral());
            builder.requires(getRequirement());
            builder.forward(getRedirect(), getRedirectModifier(), isFork());
            if (getCommand() != null) {
                builder.executes(getCommand());
            }
            return builder;
        }
    }

    static class ParsedArgumentStack<S, T> extends ParsedArgument<S, ParsedArgument<S, T>> {

        public final Deque<ParsedArgument<S, T>> argumentStack = new ArrayDeque<>();

        public ParsedArgumentStack() {
            super(0,0, null);
        }

        @Override
        public StringRange getRange() {
            if (argumentStack.isEmpty()) return new StringRange(0, 0);

            return argumentStack.peek().getRange();
        }

        @Override
        public ParsedArgument<S, T> getResult() {
            if (argumentStack.isEmpty()) return null;

            return this.pollFirst();
        }

        public void add(ParsedArgument<S, T> argument) {
            this.argumentStack.add(argument);
        }

        public ParsedArgument<S, T> pollFirst() {
            return this.argumentStack.pollFirst();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ContextAwareLiteralArgumentBuilder.ParsedArgumentStack)) return false;
            final ParsedArgumentStack<?, ?> that = (ParsedArgumentStack<?, ?>) o;
            return Objects.equals(argumentStack, that.argumentStack);
        }

        @Override
        public int hashCode() {
            return argumentStack.hashCode();
        }
    }
}
