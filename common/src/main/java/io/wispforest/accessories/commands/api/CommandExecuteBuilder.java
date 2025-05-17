package io.wispforest.accessories.commands.api;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.functions.CommandFunction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;
import java.util.function.Consumer;

public abstract class CommandExecuteBuilder {

    protected abstract Key baseKey();

    //--

    public void addToNode(String key, CommandAddition addition) {
        addToNode(new Key(key), addition);
    }

    public void addToNode(List<String> keyParts, CommandAddition addition) {
        addToNode(new Key(keyParts), addition);
    }

    public void addToNode(Key key, CommandAddition addition) {
        addToNodeFromArgs(key.path().stream().map(Argument.LiteralBranch::of).toList(), addition);
    }

    public void addToNodeFromArgs(String key, List<? extends Argument<?>> args, CommandAddition addition) {
        addToNodeFromArgs(new Key(key), args, addition);
    }

    public void addToNodeFromArgs(Key key, List<? extends Argument<?>> args, CommandAddition addition) {
        List<Argument<?>> argList = new ArrayList<>(args);

        argList.addAll(0, key.path().stream().map(Argument.LiteralBranch::of).toList());

        addToNodeFromArgs(argList, addition);
    }

    public void addToNodeFromArgs(List<? extends Argument<?>> args, CommandAddition addition) {
        SequencedMap<Key, Argument.ArgumentBuilderConstructor<?>> unpackedArgs = new LinkedHashMap<>();

        var key = new Key();

        for (var arg : args) {
            if (arg instanceof Argument.ArgumentBuilderConstructorList) {
                throw new IllegalStateException("Unable to handle Branch arguments here!");
            }

            var castedArg = (Argument.ArgumentBuilderConstructor<?>) arg;

            key = key.child(castedArg.name());

            unpackedArgs.put(key, castedArg);
        }

        addToNode(unpackedArgs.lastEntry().getKey(), unpackedArgs, addition);
    }

    protected abstract void addToNode(Key key, SequencedMap<Key, Argument.ArgumentBuilderConstructor<?>> unpackedArgs, CommandAddition addition);

    //--

    public CommandExecuteBuilder execute(String key, CommandFunction commandExecution) {
        return executeWithArgs(key, List.of(), (node) -> node.executes(commandExecution::execute));
    }

    public <T1> CommandExecuteBuilder executeWithArgs(String key, Argument<T1> arg1, CommandFunction1<T1> commandExecution) {
        return executeWithArgs(key, List.of(arg1), (node) -> {
            return node.executes(ctx -> commandExecution.execute(ctx, arg1.getArgument(ctx)));
        });
    }

    public <T1, T2> CommandExecuteBuilder executeWithArgs(String key, Argument<T1> arg1, Argument<T2> arg2, CommandFunction2<T1, T2> commandExecution) {
        return executeWithArgs(key, List.of(arg1, arg2), (node) -> {
            return node.executes(ctx -> commandExecution.execute(ctx, arg1.getArgument(ctx), arg2.getArgument(ctx)));
        });
    }

    public <T1, T2, T3> CommandExecuteBuilder executeWithArgs(String key, Argument<T1> arg1, Argument<T2> arg2, Argument<T3> arg3, CommandFunction3<T1, T2, T3> commandExecution) {
        return executeWithArgs(key, List.of(arg1, arg2, arg3), (node) -> {
            return node.executes(ctx -> commandExecution.execute(ctx, arg1.getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx)));
        });
    }

    public <T1, T2, T3, T4> CommandExecuteBuilder executeWithArgs(String key, Argument<T1> arg1, Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, CommandFunction4<T1, T2, T3, T4> commandExecution) {
        return executeWithArgs(key, List.of(arg1, arg2, arg3, arg4), (node) -> {
            return node.executes(ctx -> commandExecution.execute(ctx, arg1.getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx)));
        });
    }

    public <T1, T2, T3, T4, T5> CommandExecuteBuilder executeWithArgs(String key, Argument<T1> arg1, Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, CommandFunction5<T1, T2, T3, T4, T5> commandExecution) {
        return executeWithArgs(key, List.of(arg1, arg2, arg3, arg4, arg5), (node) -> {
            return node.executes(ctx -> commandExecution.execute(ctx, arg1.getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx)));
        });
    }

    public <T1, T2, T3, T4, T5, T6> CommandExecuteBuilder executeWithArgs(String key, Argument<T1> arg1, Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, CommandFunction6<T1, T2, T3, T4, T5, T6> commandExecution) {
        return executeWithArgs(key, List.of(arg1, arg2, arg3, arg4, arg5, arg6), (node) -> {
            return node.executes(ctx -> commandExecution.execute(ctx, arg1.getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx)));
        });
    }

    //--

    public void executeUnder(List<String> keyParts, Consumer<CommandExecuteBuilder> consumer) {
        executeUnder(new Key(keyParts), consumer);
    }

    public void executeUnder(String key, Consumer<CommandExecuteBuilder> consumer) {
        executeUnder(new Key(key), consumer);
    }

    public void executeUnder(Key baseKey, Consumer<CommandExecuteBuilder> consumer) {
        consumer.accept(builderUnder(baseKey));
    }

    //--

    public CommandExecuteBuilder builderUnder(List<String> keyParts) {
        return builderUnder(new Key(keyParts));
    }

    public CommandExecuteBuilder builderUnder(String key) {
        return builderUnder(new Key(key));
    }

    public CommandExecuteBuilder builderUnder(Key baseKey) {
        return new CommandExecuteBuilder() {
            @Override
            protected Key baseKey() {
                return CommandExecuteBuilder.this.baseKey().child(baseKey);
            }

            @Override
            protected void addToNode(Key key, SequencedMap<Key, Argument.ArgumentBuilderConstructor<?>> unpackedArgs, CommandAddition addition) {
                CommandExecuteBuilder.this.addToNode(key, unpackedArgs, addition);
            }

            @Override
            protected void addWithArgsAndKey(Key key, List<Argument<?>> commandArgs, CommandAddition commandAddition) {
                CommandExecuteBuilder.this.addWithArgsAndKey(key, commandArgs, commandAddition);
            }
        };
    }

    //--

    public CommandExecuteBuilder executeWithArgs(String key, List<Argument<?>> commandArgs, CommandAddition commandAddition) {
        return executeWithArgs(new Key(key), commandArgs, commandAddition);
    }

    public CommandExecuteBuilder executeWithArgs(Key key, List<Argument<?>> commandArgs, CommandAddition commandAddition) {
        var trueKey = (!baseKey().equals(key)) ? baseKey().child(key) : key;

        addWithArgsAndKey(trueKey, commandArgs, commandAddition);

        return this;
    }

    protected abstract void addWithArgsAndKey(Key key, List<Argument<?>> commandArgs, CommandAddition commandAddition);

    //--

    public interface CommandFunction {
        int execute(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException;
    }

    public interface CommandFunction1<T1> {
        int execute(CommandContext<CommandSourceStack> ctx, T1 t1) throws CommandSyntaxException;
    }

    public interface CommandFunction2<T1, T2> {
        int execute(CommandContext<CommandSourceStack> ctx, T1 t1, T2 t2) throws CommandSyntaxException;
    }

    public interface CommandFunction3<T1, T2, T3> {
        int execute(CommandContext<CommandSourceStack> ctx, T1 t1, T2 t2, T3 t3) throws CommandSyntaxException;
    }

    public interface CommandFunction4<T1, T2, T3, T4> {
        int execute(CommandContext<CommandSourceStack> ctx, T1 t1, T2 t2, T3 t3, T4 t4) throws CommandSyntaxException;
    }

    public interface CommandFunction5<T1, T2, T3, T4, T5> {
        int execute(CommandContext<CommandSourceStack> ctx, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) throws CommandSyntaxException;
    }

    public interface CommandFunction6<T1, T2, T3, T4, T5, T6> {
        int execute(CommandContext<CommandSourceStack> ctx, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6) throws CommandSyntaxException;
    }

}
