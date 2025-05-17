package io.wispforest.accessories.commands.api.base;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.wispforest.accessories.commands.api.core.Branch;
import io.wispforest.accessories.commands.api.core.CommandAddition;
import io.wispforest.accessories.commands.api.core.Key;

import java.util.List;

public sealed interface CommandTreeBuilder<S, B extends CommandTreeBuilder<S, B>> permits BaseCommandGenerator, CommandTreeBuilder.BranchedCommandTreeBuilder, CommandTreeBuilder.CommandTreeBuilderImpl {

    default B createLeaf(String key, CommandFunction<S> commandExecution) {
        return createLeaves(key, List.of(), (node) -> node.executes(commandExecution::execute));
    }

    default <T1> B createLeaves(String key, Argument<T1> arg1, CommandFunction1<S, T1> commandExecution) {
        return createLeaves(key, List.of(arg1), (node) -> {
            return node.executes(ctx -> commandExecution.execute(ctx, arg1.getArgument(ctx)));
        });
    }

    default <T1, T2> B createLeaves(String key, Argument<T1> arg1, Argument<T2> arg2, CommandFunction2<S, T1, T2> commandExecution) {
        return createLeaves(key, List.of(arg1, arg2), (node) -> {
            return node.executes(ctx -> commandExecution.execute(ctx, arg1.getArgument(ctx), arg2.getArgument(ctx)));
        });
    }

    default <T1, T2, T3> B createLeaves(String key, Argument<T1> arg1, Argument<T2> arg2, Argument<T3> arg3, CommandFunction3<S, T1, T2, T3> commandExecution) {
        return createLeaves(key, List.of(arg1, arg2, arg3), (node) -> {
            return node.executes(ctx -> commandExecution.execute(ctx, arg1.getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx)));
        });
    }

    default <T1, T2, T3, T4> B createLeaves(String key, Argument<T1> arg1, Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, CommandFunction4<S, T1, T2, T3, T4> commandExecution) {
        return createLeaves(key, List.of(arg1, arg2, arg3, arg4), (node) -> {
            return node.executes(ctx -> commandExecution.execute(ctx, arg1.getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx)));
        });
    }

    default <T1, T2, T3, T4, T5> B createLeaves(String key, Argument<T1> arg1, Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, CommandFunction5<S, T1, T2, T3, T4, T5> commandExecution) {
        return createLeaves(key, List.of(arg1, arg2, arg3, arg4, arg5), (node) -> {
            return node.executes(ctx -> commandExecution.execute(ctx, arg1.getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx)));
        });
    }

    default <T1, T2, T3, T4, T5, T6> B createLeaves(String key, Argument<T1> arg1, Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, CommandFunction6<S, T1, T2, T3, T4, T5, T6> commandExecution) {
        return createLeaves(key, List.of(arg1, arg2, arg3, arg4, arg5, arg6), (node) -> {
            return node.executes(ctx -> commandExecution.execute(ctx, arg1.getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx)));
        });
    }

    //--

    default void createBranch(List<String> keyParts, BranchBuilder<S> builder) {
        createBranch(new Key(keyParts), builder);
    }

    default void createBranch(String key, BranchBuilder<S> builder) {
        createBranch(new Key(key), builder);
    }

    default void createBranch(Key baseKey, BranchBuilder<S> builder) {
        builder.addLeaves(createBranch(baseKey));
    }

    interface BranchBuilder<S>  {
        void addLeaves(BranchedCommandTreeBuilder<S, ?> branchBuilder);
    }

    //--

    default BranchedCommandTreeBuilder<S, ?> createBranch(List<String> keyParts) {
        return createBranch(new Key(keyParts));
    }

    default BranchedCommandTreeBuilder<S, ?> createBranch(String key) {
        return createBranch(new Key(key));
    }

    default BranchedCommandTreeBuilder<S, ?> createBranch(Key baseKey) {
        return new BranchedCommandTreeBuilderImpl<>(baseKey, this);
    }

    //--

    default B createLeaves(String key, List<Argument<?>> commandArgs, CommandAddition<S> commandAddition) {
        return createLeaves(new Key(key), commandArgs, commandAddition);
    }

    B createLeaves(Key key, List<Argument<?>> commandArgs, CommandAddition<S> commandAddition);

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

    non-sealed interface CommandTreeBuilderImpl<S> extends CommandTreeBuilder<S, CommandTreeBuilderImpl<S>> { }

    non-sealed interface BranchedCommandTreeBuilder<S, B extends BranchedCommandTreeBuilder<S, B>> extends CommandTreeBuilder<S, B>, Branch {

        default B createLeaf(CommandFunction commandExecution) {
            return createLeaves(List.of(), (node) -> node.executes(commandExecution::execute));
        }

        default <T1> B createLeaves(Argument<T1> arg1, CommandFunction1<S, T1> commandExecution) {
            return createLeaves(List.of(arg1), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1.getArgument(ctx)));
            });
        }

        default <T1, T2> B createLeaves(Argument<T1> arg1, Argument<T2> arg2, CommandFunction2<S, T1, T2> commandExecution) {
            return createLeaves(List.of(arg1, arg2), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1.getArgument(ctx), arg2.getArgument(ctx)));
            });
        }

        default <T1, T2, T3> B createLeaves(Argument<T1> arg1, Argument<T2> arg2, Argument<T3> arg3, CommandFunction3<S, T1, T2, T3> commandExecution) {
            return createLeaves(List.of(arg1, arg2, arg3), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1.getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx)));
            });
        }

        default <T1, T2, T3, T4> B createLeaves(Argument<T1> arg1, Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, CommandFunction4<S, T1, T2, T3, T4> commandExecution) {
            return createLeaves(List.of(arg1, arg2, arg3, arg4), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1.getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx)));
            });
        }

        default <T1, T2, T3, T4, T5> B createLeaves(Argument<T1> arg1, Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, CommandFunction5<S, T1, T2, T3, T4, T5> commandExecution) {
            return createLeaves(List.of(arg1, arg2, arg3, arg4, arg5), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1.getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx)));
            });
        }

        default <T1, T2, T3, T4, T5, T6> B createLeaves(Argument<T1> arg1, Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, CommandFunction6<S, T1, T2, T3, T4, T5, T6> commandExecution) {
            return createLeaves(List.of(arg1, arg2, arg3, arg4, arg5, arg6), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1.getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx)));
            });
        }

        default B createLeaves(List<Argument<?>> commandArgs, CommandAddition<S> commandAddition) {
            createLeaves(new Key(), commandArgs, commandAddition);

            return getThis();
        }

        @Override
        B getThis();
    }
}
