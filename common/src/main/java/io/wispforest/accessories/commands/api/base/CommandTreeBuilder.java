package io.wispforest.accessories.commands.api.base;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.wispforest.accessories.commands.api.core.Branch;
import io.wispforest.accessories.commands.api.core.CommandAddition;
import io.wispforest.accessories.commands.api.core.Key;

import java.util.List;

public interface CommandTreeBuilder<S, B extends CommandTreeBuilder<S, B>> extends CommandLeafBuilder<S, B, CommandTreeBuilder.BranchedCommandTreeBuilder<S, ?>> {

    default B leaf(String key, CommandFunction<S> commandExecution) {
        return leaves(key, List.of(), (node) -> node.executes(commandExecution::execute));
    }

    default <T1> B leaves(String key, Argument<T1> arg1, CommandFunction1<S, T1> commandExecution) {
        return leaves(key, List.of(arg1), (node) -> {
            return node.executes(ctx -> commandExecution.execute(ctx, arg1.getArgument(ctx)));
        });
    }

    default <T1, T2> B leaves(String key, Argument<T1> arg1, Argument<T2> arg2, CommandFunction2<S, T1, T2> commandExecution) {
        return leaves(key, List.of(arg1, arg2), (node) -> {
            return node.executes(ctx -> commandExecution.execute(ctx, arg1.getArgument(ctx), arg2.getArgument(ctx)));
        });
    }

    default <T1, T2, T3> B leaves(String key, Argument<T1> arg1, Argument<T2> arg2, Argument<T3> arg3, CommandFunction3<S, T1, T2, T3> commandExecution) {
        return leaves(key, List.of(arg1, arg2, arg3), (node) -> {
            return node.executes(ctx -> commandExecution.execute(ctx, arg1.getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx)));
        });
    }

    default <T1, T2, T3, T4> B leaves(String key, Argument<T1> arg1, Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, CommandFunction4<S, T1, T2, T3, T4> commandExecution) {
        return leaves(key, List.of(arg1, arg2, arg3, arg4), (node) -> {
            return node.executes(ctx -> commandExecution.execute(ctx, arg1.getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx)));
        });
    }

    default <T1, T2, T3, T4, T5> B leaves(String key, Argument<T1> arg1, Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, CommandFunction5<S, T1, T2, T3, T4, T5> commandExecution) {
        return leaves(key, List.of(arg1, arg2, arg3, arg4, arg5), (node) -> {
            return node.executes(ctx -> commandExecution.execute(ctx, arg1.getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx)));
        });
    }

    default <T1, T2, T3, T4, T5, T6> B leaves(String key, Argument<T1> arg1, Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, CommandFunction6<S, T1, T2, T3, T4, T5, T6> commandExecution) {
        return leaves(key, List.of(arg1, arg2, arg3, arg4, arg5, arg6), (node) -> {
            return node.executes(ctx -> commandExecution.execute(ctx, arg1.getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx)));
        });
    }

    default <T1, T2, T3, T4, T5, T6, T7> B leaves(String key, Argument<T1> arg1, Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, Argument<T7> arg7, CommandFunction7<S, T1, T2, T3, T4, T5, T6, T7> commandExecution) {
        return leaves(key, List.of(arg1, arg2, arg3, arg4, arg5, arg6, arg7), (node) -> {
            return node.executes(ctx -> commandExecution.execute(ctx, arg1.getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx), arg7.getArgument(ctx)));
        });
    }

    default <T1, T2, T3, T4, T5, T6, T7, T8> B leaves(String key, Argument<T1> arg1, Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, Argument<T7> arg7, Argument<T8> arg8, CommandFunction8<S, T1, T2, T3, T4, T5, T6, T7, T8> commandExecution) {
        return leaves(key, List.of(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8), (node) -> {
            return node.executes(ctx -> commandExecution.execute(ctx, arg1.getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx), arg7.getArgument(ctx), arg8.getArgument(ctx)));
        });
    }

    default <T1, T2, T3, T4, T5, T6, T7, T8, T9> B leaves(String key, Argument<T1> arg1, Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, Argument<T7> arg7, Argument<T8> arg8, Argument<T9> arg9, CommandFunction9<S, T1, T2, T3, T4, T5, T6, T7, T8, T9> commandExecution) {
        return leaves(key, List.of(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9), (node) -> {
            return node.executes(ctx -> commandExecution.execute(ctx, arg1.getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx), arg7.getArgument(ctx), arg8.getArgument(ctx), arg9.getArgument(ctx)));
        });
    }

    //--

    default <T1> CommandTreeBuilderWithArgs.Argument1TreeBuilder<S, T1> branch(String key, Argument<T1> argument1) {
        return CommandTreeBuilderWithArgs.createArgBranch(CommandTreeBuilder.this.branch(key), argument1);
    }

    default <T1, T2> CommandTreeBuilderWithArgs.Argument2TreeBuilder<S, T1, T2> branch(String key, Argument<T1> argument1, Argument<T2> argument2) {
        return CommandTreeBuilderWithArgs.createArgBranch(CommandTreeBuilder.this.branch(key), argument1, argument2);
    }

    default <T1, T2, T3> CommandTreeBuilderWithArgs.Argument3TreeBuilder<S, T1, T2, T3> branch(String key, Argument<T1> argument1, Argument<T2> argument2, Argument<T3> argument3) {
        return CommandTreeBuilderWithArgs.createArgBranch(CommandTreeBuilder.this.branch(key), argument1, argument2, argument3);
    }

    default <T1> B branch(String key, Argument<T1> arg1, LeafBuilder<S, CommandTreeBuilderWithArgs.Argument1TreeBuilder<S, T1>> builder) {
        builder.addLeaves(branch(key, arg1));

        return getThis();
    }

    default <T1, T2> B branch(String key, Argument<T1> arg1, Argument<T2> arg2, LeafBuilder<S, CommandTreeBuilderWithArgs.Argument2TreeBuilder<S, T1, T2>> builder) {
        builder.addLeaves(branch(key, arg1, arg2));

        return getThis();
    }

    default <T1, T2, T3> B branch(String key, Argument<T1> arg1, Argument<T2> arg2, Argument<T3> arg3, LeafBuilder<S, CommandTreeBuilderWithArgs.Argument3TreeBuilder<S, T1, T2, T3>> builder) {
        builder.addLeaves(branch(key, arg1, arg2, arg3));

        return getThis();
    }

    //--

    default <T1> CommandTreeBuilderWithArgs.Argument1TreeBuilder<S, T1> branch(Argument<T1> argument1) {
        return CommandTreeBuilderWithArgs.createArgBranch(CommandTreeBuilder.this, argument1);
    }

    default <T1, T2> CommandTreeBuilderWithArgs.Argument2TreeBuilder<S, T1, T2> branch(Argument<T1> argument1, Argument<T2> argument2) {
        return CommandTreeBuilderWithArgs.createArgBranch(CommandTreeBuilder.this, argument1, argument2);
    }

    default <T1, T2, T3> CommandTreeBuilderWithArgs.Argument3TreeBuilder<S, T1, T2, T3> branch(Argument<T1> argument1, Argument<T2> argument2, Argument<T3> argument3) {
        return CommandTreeBuilderWithArgs.createArgBranch(CommandTreeBuilder.this, argument1, argument2, argument3);
    }

    default <T1> B branch(Argument<T1> arg1, LeafBuilder<S, CommandTreeBuilderWithArgs.Argument1TreeBuilder<S, T1>> builder) {
        builder.addLeaves(branch(arg1));

        return getThis();
    }

    default <T1, T2> B branch(Argument<T1> arg1, Argument<T2> arg2, LeafBuilder<S, CommandTreeBuilderWithArgs.Argument2TreeBuilder<S, T1, T2>> builder) {
        builder.addLeaves(branch(arg1, arg2));

        return getThis();
    }

    default <T1, T2, T3> B branch(Argument<T1> arg1, Argument<T2> arg2, Argument<T3> arg3, LeafBuilder<S, CommandTreeBuilderWithArgs.Argument3TreeBuilder<S, T1, T2, T3>> builder) {
        builder.addLeaves(branch(arg1, arg2, arg3));

        return getThis();
    }

    //--

    default CommandTreeBuilder.BranchedCommandTreeBuilder<S, ?> branch(Key baseKey) {
        return new BranchedCommandTreeBuilderImpl<>(this, baseKey);
    }

    //--

    default B leaves(String key, List<Argument<?>> commandArgs, CommandAddition<S> commandAddition) {
        return leaves(new Key(key), commandArgs, commandAddition);
    }

    default B leaves(Key key, List<Argument<?>> commandArgs, CommandAddition<S> commandAddition) {
        leaves(key.asArgumentList(), commandArgs, commandAddition);

        return getThis();
    }

    B leaves(List<Argument<?>> startingArgs, List<Argument<?>> commandArgs, CommandAddition<S> commandAddition);

    B getThis();

    //--

    interface CommandFunction<S> {
        int execute(CommandContext<S> ctx) throws CommandSyntaxException;
    }

    interface CommandFunction1<S, T1> {
        int execute(CommandContext<S> ctx, T1 t1) throws CommandSyntaxException;
    }

    interface CommandFunction2<S, T1, T2> {
        int execute(CommandContext<S> ctx, T1 t1, T2 t2) throws CommandSyntaxException;
    }

    interface CommandFunction3<S, T1, T2, T3> {
        int execute(CommandContext<S> ctx, T1 t1, T2 t2, T3 t3) throws CommandSyntaxException;
    }

    interface CommandFunction4<S, T1, T2, T3, T4> {
        int execute(CommandContext<S> ctx, T1 t1, T2 t2, T3 t3, T4 t4) throws CommandSyntaxException;
    }

    interface CommandFunction5<S, T1, T2, T3, T4, T5> {
        int execute(CommandContext<S> ctx, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) throws CommandSyntaxException;
    }

    interface CommandFunction6<S, T1, T2, T3, T4, T5, T6> {
        int execute(CommandContext<S> ctx, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6) throws CommandSyntaxException;
    }

    interface CommandFunction7<S, T1, T2, T3, T4, T5, T6, T7> {
        int execute(CommandContext<S> ctx, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7) throws CommandSyntaxException;
    }

    interface CommandFunction8<S, T1, T2, T3, T4, T5, T6, T7, T8> {
        int execute(CommandContext<S> ctx, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8) throws CommandSyntaxException;
    }

    interface CommandFunction9<S, T1, T2, T3, T4, T5, T6, T7, T8, T9> {
        int execute(CommandContext<S> ctx, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8, T9 t9) throws CommandSyntaxException;
    }

    interface CommandTreeBuilderImpl<S> extends CommandTreeBuilder<S, CommandTreeBuilderImpl<S>> { }

    record BranchedCommandTreeBuilderImpl<S>(CommandTreeBuilder<S, ?> parentBuilder, Key branchKey) implements BranchedCommandTreeBuilder<S, BranchedCommandTreeBuilderImpl<S>>{
        @Override
        public BranchedCommandTreeBuilderImpl<S> leaves(List<Argument<?>> startingArgs, List<Argument<?>> commandArgs, CommandAddition<S> commandAddition) {
            parentBuilder().leaves(addStartingToArgs(startingArgs), commandArgs, commandAddition);

            return getThis();
        }

        @Override
        public BranchedCommandTreeBuilderImpl<S> getThis() {
            return this;
        }
    }

    interface BranchedCommandTreeBuilder<S, B extends BranchedCommandTreeBuilder<S, B>> extends CommandTreeBuilder<S, B>, Branch {

        default B leaf(CommandTreeBuilder.CommandFunction<S> commandExecution){
            return leaves(List.of(), (node) -> node.executes(commandExecution::execute));
        }

        default <T1> B leaves(Argument<T1> arg1, CommandFunction1<S, T1> commandExecution) {
            return leaves(List.of(arg1), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1.getArgument(ctx)));
            });
        }

        default <T1, T2> B leaves(Argument<T1> arg1, Argument<T2> arg2, CommandFunction2<S, T1, T2> commandExecution) {
            return leaves(List.of(arg1, arg2), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1.getArgument(ctx), arg2.getArgument(ctx)));
            });
        }

        default <T1, T2, T3> B leaves(Argument<T1> arg1, Argument<T2> arg2, Argument<T3> arg3, CommandFunction3<S, T1, T2, T3> commandExecution) {
            return leaves(List.of(arg1, arg2, arg3), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1.getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx)));
            });
        }

        default <T1, T2, T3, T4> B leaves(Argument<T1> arg1, Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, CommandFunction4<S, T1, T2, T3, T4> commandExecution) {
            return leaves(List.of(arg1, arg2, arg3, arg4), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1.getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx)));
            });
        }

        default <T1, T2, T3, T4, T5> B leaves(Argument<T1> arg1, Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, CommandFunction5<S, T1, T2, T3, T4, T5> commandExecution) {
            return leaves(List.of(arg1, arg2, arg3, arg4, arg5), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1.getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx)));
            });
        }

        default <T1, T2, T3, T4, T5, T6> B leaves(Argument<T1> arg1, Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, CommandFunction6<S, T1, T2, T3, T4, T5, T6> commandExecution) {
            return leaves(List.of(arg1, arg2, arg3, arg4, arg5, arg6), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1.getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx)));
            });
        }

        default <T1, T2, T3, T4, T5, T6, T7> B leaves(Argument<T1> arg1, Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, Argument<T7> arg7, CommandFunction7<S, T1, T2, T3, T4, T5, T6, T7> commandExecution) {
            return leaves(List.of(arg1, arg2, arg3, arg4, arg5, arg6, arg7), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1.getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx), arg7.getArgument(ctx)));
            });
        }

        default <T1, T2, T3, T4, T5, T6, T7, T8> B leaves(Argument<T1> arg1, Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, Argument<T7> arg7, Argument<T8> arg8, CommandFunction8<S, T1, T2, T3, T4, T5, T6, T7, T8> commandExecution) {
            return leaves(List.of(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1.getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx), arg7.getArgument(ctx), arg8.getArgument(ctx)));
            });
        }

        default <T1, T2, T3, T4, T5, T6, T7, T8, T9> B leaves(Argument<T1> arg1, Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, Argument<T7> arg7, Argument<T8> arg8, Argument<T9> arg9, CommandFunction9<S, T1, T2, T3, T4, T5, T6, T7, T8, T9> commandExecution) {
            return leaves(List.of(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1.getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx), arg7.getArgument(ctx), arg8.getArgument(ctx), arg9.getArgument(ctx)));
            });
        }

        default B leaves(List<Argument<?>> commandArgs, CommandAddition<S> commandAddition) {
            return leaves(List.of(), commandArgs, commandAddition);
        }

        @Override
        B getThis();
    }
}
