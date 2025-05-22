package io.wispforest.accessories.commands.api.base;

import io.wispforest.accessories.commands.api.core.Branch;
import io.wispforest.accessories.commands.api.core.CommandAddition;
import io.wispforest.accessories.commands.api.core.Key;

import java.util.ArrayList;
import java.util.List;

import static io.wispforest.accessories.commands.api.base.CommandTreeBuilder.*;

public interface CommandTreeBuilderWithArgs<S, B extends CommandTreeBuilderWithArgs<S, B, T>, T extends Branch & CommandTreeBuilderWithArgs<S, ?, T>> extends CommandLeafBuilder<S, B, T> {

    CommandLeafBuilder<S, ?, ?> commandLeafBuilder();

    //--

    default B leaves(String key, List<Argument<?>> commandArgs, CommandAddition<S> commandAddition) {
        leaves(new Key(key), commandArgs, commandAddition);

        return getThis();
    }

    default B leaves(List<Argument<?>> commandArgs, CommandAddition<S> commandAddition) {
        leaves(new Key(), commandArgs, commandAddition);

        return getThis();
    }

    default B leaves(Key key, List<Argument<?>> commandArgs, CommandAddition<S> commandAddition) {
        this.leaves(key.asArgumentList(), commandArgs, commandAddition);

        return getThis();
    }

    default B leaves(List<Argument<?>> startingArgs, List<Argument<?>> commandArgs, CommandAddition<S> commandAddition) {
        this.commandLeafBuilder().leaves(addArgsToStart(startingArgs), commandArgs, commandAddition);

        return getThis();
    }

    //--

    static <S, T1> Argument1TreeBuilder<S, T1> createArgBranch(CommandLeafBuilder<S, ?, ?> commandLeafBuilder, Argument<T1> arg1) {
        return new Argument1TreeBuilderImpl<>(commandLeafBuilder, arg1);
    }

    static <S, T1, T2> Argument2TreeBuilder<S, T1, T2> createArgBranch(CommandLeafBuilder<S, ?, ?> commandLeafBuilder, Argument<T1> arg1, Argument<T2> arg2) {
        return new Argument2TreeBuilderImpl<>(commandLeafBuilder, arg1, arg2);
    }

    static <S, T1, T2, T3> Argument3TreeBuilder<S, T1, T2, T3> createArgBranch(CommandLeafBuilder<S, ?, ?> commandLeafBuilder, Argument<T1> arg1, Argument<T2> arg2, Argument<T3> arg3) {
        return new Argument3TreeBuilderImpl<>(commandLeafBuilder, arg1, arg2, arg3);
    }

    static <S, T1> void createArgBranch(CommandLeafBuilder<S, ?, ?> commandLeafBuilder, Argument<T1> arg1, LeafBuilder<S, Argument1TreeBuilder<S, T1>> builder) {
        builder.addLeaves(createArgBranch(commandLeafBuilder, arg1));
    }

    static <S, T1, T2> void createArgBranch(CommandLeafBuilder<S, ?, ?> commandLeafBuilder, Argument<T1> arg1, Argument<T2> arg2, LeafBuilder<S, Argument2TreeBuilder<S, T1, T2>> builder) {
        builder.addLeaves(createArgBranch(commandLeafBuilder, arg1, arg2));
    }

    static <S, T1, T2, T3> void createArgBranch(CommandLeafBuilder<S, ?, ?> commandLeafBuilder, Argument<T1> arg1, Argument<T2> arg2, Argument<T3> arg3, LeafBuilder<S, Argument3TreeBuilder<S, T1, T2, T3>> builder) {
        builder.addLeaves(createArgBranch(commandLeafBuilder, arg1, arg2, arg3));
    }

    //--

    @Override
    T branch(Key baseKey);

    //--

    B getThis();

    default List<Argument<?>> getArgs() {
        return List.of();
    }

    default List<Argument<?>> addArgsToStart(List<Argument<?>> startingArgs) {
        var list = new ArrayList<>(startingArgs);

        list.addAll(0, getArgs());

        return list;
    }

    //--

    interface Argument1TreeBuilder<S, T1> extends CommandTreeBuilderWithArgs<S, Argument1TreeBuilder<S, T1>, BranchedArgument1TreeBuilder<S, T1>> {
        Argument<T1> arg1();

        //--

        @Override
        default Argument1TreeBuilder<S, T1> getThis() {
            return this;
        }

        @Override
        default BranchedArgument1TreeBuilder<S, T1> branch(Key baseKey) {
            return new BranchedArgument1TreeBuilder<>(baseKey, this, arg1());
        }

        //--

        default <T2> CommandTreeBuilderWithArgs.Argument2TreeBuilder<S, T1, T2> createArgBranch(Argument<T2> argument2) {
            return new Argument2TreeBuilderImpl<>(this.commandLeafBuilder(), arg1(), argument2);
        }

        default <T2, T3> CommandTreeBuilderWithArgs.Argument3TreeBuilder<S, T1, T2, T3> createArgBranch(Argument<T2> argument2, Argument<T3> argument3) {
            return new Argument3TreeBuilderImpl<>(this.commandLeafBuilder(), arg1(), argument2, argument3);
        }

        //--

        default Argument1TreeBuilder<S, T1> leaves(String key, CommandFunction1<S, T1> commandExecution) {
            return leaves(key, List.of(), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx)));
            });
        }

        default <T2> Argument1TreeBuilder<S, T1> leaves(String key, Argument<T2> arg2, CommandFunction2<S, T1, T2> commandExecution) {
            return leaves(key, List.of(arg2), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2.getArgument(ctx)));
            });
        }

        default <T2, T3> Argument1TreeBuilder<S, T1> leaves(String key, Argument<T2> arg2, Argument<T3> arg3, CommandFunction3<S, T1, T2, T3> commandExecution) {
            return leaves(key, List.of(arg2, arg3), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx)));
            });
        }

        default <T2, T3, T4> Argument1TreeBuilder<S, T1> leaves(String key, Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, CommandFunction4<S, T1, T2, T3, T4> commandExecution) {
            return leaves(key, List.of(arg2, arg3, arg4), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx)));
            });
        }

        default <T2, T3, T4, T5> Argument1TreeBuilder<S, T1> leaves(String key, Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, CommandFunction5<S, T1, T2, T3, T4, T5> commandExecution) {
            return leaves(key, List.of(arg2, arg3, arg4, arg5), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx)));
            });
        }

        default <T2, T3, T4, T5, T6> Argument1TreeBuilder<S, T1> leaves(String key, Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, CommandFunction6<S, T1, T2, T3, T4, T5, T6> commandExecution) {
            return leaves(key, List.of(arg2, arg3, arg4, arg5, arg6), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx)));
            });
        }

        default <T2, T3, T4, T5, T6, T7> Argument1TreeBuilder<S, T1> leaves(String key, Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, Argument<T7> arg7, CommandFunction7<S, T1, T2, T3, T4, T5, T6, T7> commandExecution) {
            return leaves(key, List.of(arg2, arg3, arg4, arg5, arg6, arg7), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx), arg7.getArgument(ctx)));
            });
        }

        default <T2, T3, T4, T5, T6, T7, T8> Argument1TreeBuilder<S, T1> leaves(String key, Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, Argument<T7> arg7, Argument<T8> arg8, CommandFunction8<S, T1, T2, T3, T4, T5, T6, T7, T8> commandExecution) {
            return leaves(key, List.of(arg2, arg3, arg4, arg5, arg6, arg7, arg8), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx), arg7.getArgument(ctx), arg8.getArgument(ctx)));
            });
        }

        default <T2, T3, T4, T5, T6, T7, T8, T9> Argument1TreeBuilder<S, T1> leaves(String key, Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, Argument<T7> arg7, Argument<T8> arg8, Argument<T9> arg9, CommandFunction9<S, T1, T2, T3, T4, T5, T6, T7, T8, T9> commandExecution) {
            return leaves(key, List.of(arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx), arg7.getArgument(ctx), arg8.getArgument(ctx), arg9.getArgument(ctx)));
            });
        }

        //--

        default Argument1TreeBuilder<S, T1> leaves(CommandFunction1<S, T1> commandExecution) {
            return leaves(List.of(), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx)));
            });
        }

        default <T2> Argument1TreeBuilder<S, T1> leaves(Argument<T2> arg2, CommandFunction2<S, T1, T2> commandExecution) {
            return leaves(List.of(arg2), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2.getArgument(ctx)));
            });
        }

        default <T2, T3> Argument1TreeBuilder<S, T1> leaves(Argument<T2> arg2, Argument<T3> arg3, CommandFunction3<S, T1, T2, T3> commandExecution) {
            return leaves(List.of(arg2, arg3), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx)));
            });
        }

        default <T2, T3, T4> Argument1TreeBuilder<S, T1> leaves(Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, CommandFunction4<S, T1, T2, T3, T4> commandExecution) {
            return leaves(List.of(arg2, arg3, arg4), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx)));
            });
        }

        default <T2, T3, T4, T5> Argument1TreeBuilder<S, T1> leaves(Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, CommandFunction5<S, T1, T2, T3, T4, T5> commandExecution) {
            return leaves(List.of(arg2, arg3, arg4, arg5), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx)));
            });
        }

        default <T2, T3, T4, T5, T6> Argument1TreeBuilder<S, T1> leaves(Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, CommandFunction6<S, T1, T2, T3, T4, T5, T6> commandExecution) {
            return leaves(List.of(arg2, arg3, arg4, arg5, arg6), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx)));
            });
        }

        default <T2, T3, T4, T5, T6, T7> Argument1TreeBuilder<S, T1> leaves(Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, Argument<T7> arg7, CommandFunction7<S, T1, T2, T3, T4, T5, T6, T7> commandExecution) {
            return leaves(List.of(arg2, arg3, arg4, arg5, arg6, arg7), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx), arg7.getArgument(ctx)));
            });
        }

        default <T2, T3, T4, T5, T6, T7, T8> Argument1TreeBuilder<S, T1> leaves(Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, Argument<T7> arg7, Argument<T8> arg8, CommandFunction8<S, T1, T2, T3, T4, T5, T6, T7, T8> commandExecution) {
            return leaves(List.of(arg2, arg3, arg4, arg5, arg6, arg7, arg8), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx), arg7.getArgument(ctx), arg8.getArgument(ctx)));
            });
        }

        default <T2, T3, T4, T5, T6, T7, T8, T9> Argument1TreeBuilder<S, T1> leaves(Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, Argument<T7> arg7, Argument<T8> arg8, Argument<T9> arg9, CommandFunction9<S, T1, T2, T3, T4, T5, T6, T7, T8, T9> commandExecution) {
            return leaves(List.of(arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx), arg7.getArgument(ctx), arg8.getArgument(ctx), arg9.getArgument(ctx)));
            });
        }
    }

    record Argument1TreeBuilderImpl<S, T1>(CommandLeafBuilder<S, ?, ?> commandLeafBuilder, Argument<T1> arg1) implements Argument1TreeBuilder<S, T1> {
        @Override public List<Argument<?>> getArgs() { return List.of(arg1()); }
    }

    record BranchedArgument1TreeBuilder<S, T1>(Key branchKey, CommandLeafBuilder<S, ?, ?> commandLeafBuilder, Argument<T1> arg1) implements Argument1TreeBuilder<S, T1>, Branch {
        @Override public List<Argument<?>> getArgs() { return branchKey.asArgumentList(); }
    }

    //--

    interface Argument2TreeBuilder<S, T1, T2> extends CommandTreeBuilderWithArgs<S, Argument2TreeBuilder<S, T1, T2>, BranchedArgument2TreeBuilder<S, T1, T2>> {

        Argument<T1> arg1();

        Argument<T2> arg2();

        //--

        @Override
        default Argument2TreeBuilder<S, T1, T2> getThis() {
            return this;
        }

        @Override
        default BranchedArgument2TreeBuilder<S, T1, T2> branch(Key baseKey) {
            return new BranchedArgument2TreeBuilder<>(baseKey, this, arg1(), arg2());
        }

        //--

        default <T3> CommandTreeBuilderWithArgs.Argument3TreeBuilder<S, T1, T2, T3> createArgBranch(Argument<T3> argument3) {
            return new Argument3TreeBuilderImpl<>(this.commandLeafBuilder(), arg1(), arg2(), argument3);
        }

        //--

        default Argument2TreeBuilder<S, T1, T2> leaves(String key, CommandFunction2<S, T1, T2> commandExecution) {
            return leaves(key, List.of(), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx)));
            });
        }

        default <T3> Argument2TreeBuilder<S, T1, T2> leaves(String key, Argument<T3> arg3, CommandFunction3<S, T1, T2, T3> commandExecution) {
            return leaves(key, List.of(arg3), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3.getArgument(ctx)));
            });
        }

        default <T3, T4> Argument2TreeBuilder<S, T1, T2> leaves(String key, Argument<T3> arg3, Argument<T4> arg4, CommandFunction4<S, T1, T2, T3, T4> commandExecution) {
            return leaves(key, List.of(arg3, arg4), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx)));
            });
        }

        default <T3, T4, T5> Argument2TreeBuilder<S, T1, T2> leaves(String key, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, CommandFunction5<S, T1, T2, T3, T4, T5> commandExecution) {
            return leaves(key, List.of(arg3, arg4, arg5), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx)));
            });
        }

        default <T3, T4, T5, T6> Argument2TreeBuilder<S, T1, T2> leaves(String key, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, CommandFunction6<S, T1, T2, T3, T4, T5, T6> commandExecution) {
            return leaves(key, List.of(arg3, arg4, arg5, arg6), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx)));
            });
        }

        default <T3, T4, T5, T6, T7> Argument2TreeBuilder<S, T1, T2> leaves(String key, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, Argument<T7> arg7, CommandFunction7<S, T1, T2, T3, T4, T5, T6, T7> commandExecution) {
            return leaves(key, List.of(arg3, arg4, arg5, arg6, arg7), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx), arg7.getArgument(ctx)));
            });
        }

        default <T3, T4, T5, T6, T7, T8> Argument2TreeBuilder<S, T1, T2> leaves(String key, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, Argument<T7> arg7, Argument<T8> arg8, CommandFunction8<S, T1, T2, T3, T4, T5, T6, T7, T8> commandExecution) {
            return leaves(key, List.of(arg3, arg4, arg5, arg6, arg7, arg8), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx), arg7.getArgument(ctx), arg8.getArgument(ctx)));
            });
        }

        default <T3, T4, T5, T6, T7, T8, T9> Argument2TreeBuilder<S, T1, T2> leaves(String key, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, Argument<T7> arg7, Argument<T8> arg8, Argument<T9> arg9, CommandFunction9<S, T1, T2, T3, T4, T5, T6, T7, T8, T9> commandExecution) {
            return leaves(key, List.of(arg3, arg4, arg5, arg6, arg7, arg8, arg9), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx), arg7.getArgument(ctx), arg8.getArgument(ctx), arg9.getArgument(ctx)));
            });
        }

        //--

        default Argument2TreeBuilder<S, T1, T2> leaves(CommandFunction2<S, T1, T2> commandExecution) {
            return leaves(List.of(), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx)));
            });
        }

        default <T3> Argument2TreeBuilder<S, T1, T2> leaves(Argument<T3> arg3, CommandFunction3<S, T1, T2, T3> commandExecution) {
            return leaves(List.of(arg3), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3.getArgument(ctx)));
            });
        }

        default <T3, T4> Argument2TreeBuilder<S, T1, T2> leaves(Argument<T3> arg3, Argument<T4> arg4, CommandFunction4<S, T1, T2, T3, T4> commandExecution) {
            return leaves(List.of(arg3, arg4), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx)));
            });
        }

        default <T3, T4, T5> Argument2TreeBuilder<S, T1, T2> leaves(Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, CommandFunction5<S, T1, T2, T3, T4, T5> commandExecution) {
            return leaves(List.of(arg3, arg4, arg5), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx)));
            });
        }

        default <T3, T4, T5, T6> Argument2TreeBuilder<S, T1, T2> leaves(Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, CommandFunction6<S, T1, T2, T3, T4, T5, T6> commandExecution) {
            return leaves(List.of(arg3, arg4, arg5, arg6), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx)));
            });
        }

        default <T3, T4, T5, T6, T7> Argument2TreeBuilder<S, T1, T2> leaves(Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, Argument<T7> arg7, CommandFunction7<S, T1, T2, T3, T4, T5, T6, T7> commandExecution) {
            return leaves(List.of(arg3, arg4, arg5, arg6, arg7), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx), arg7.getArgument(ctx)));
            });
        }

        default <T3, T4, T5, T6, T7, T8> Argument2TreeBuilder<S, T1, T2> leaves(Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, Argument<T7> arg7, Argument<T8> arg8, CommandFunction8<S, T1, T2, T3, T4, T5, T6, T7, T8> commandExecution) {
            return leaves(List.of(arg3, arg4, arg5, arg6, arg7, arg8), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx), arg7.getArgument(ctx), arg8.getArgument(ctx)));
            });
        }

        default <T3, T4, T5, T6, T7, T8, T9> Argument2TreeBuilder<S, T1, T2> leaves(Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, Argument<T7> arg7, Argument<T8> arg8, Argument<T9> arg9, CommandFunction9<S, T1, T2, T3, T4, T5, T6, T7, T8, T9> commandExecution) {
            return leaves(List.of(arg3, arg4, arg5, arg6, arg7, arg8, arg9), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx), arg7.getArgument(ctx), arg8.getArgument(ctx), arg9.getArgument(ctx)));
            });
        }
    }

    record Argument2TreeBuilderImpl<S, T1, T2>(CommandLeafBuilder<S, ?, ?> commandLeafBuilder, Argument<T1> arg1, Argument<T2> arg2) implements Argument2TreeBuilder<S, T1, T2> {
        @Override public List<Argument<?>> getArgs() { return List.of(arg1(), arg2()); }
    }

    record BranchedArgument2TreeBuilder<S, T1, T2>(Key branchKey, CommandLeafBuilder<S, ?, ?> commandLeafBuilder, Argument<T1> arg1, Argument<T2> arg2) implements Argument2TreeBuilder<S, T1, T2>, Branch {
        @Override public List<Argument<?>> getArgs() { return branchKey.asArgumentList(); }
    }

    //--

    interface Argument3TreeBuilder<S, T1, T2, T3> extends CommandTreeBuilderWithArgs<S, Argument3TreeBuilder<S, T1, T2, T3>, BranchedArgument3TreeBuilder<S, T1, T2, T3>> {

        //--

        Argument<T1> arg1();

        Argument<T2> arg2();

        Argument<T3> arg3();

        //--

        @Override
        default Argument3TreeBuilder<S, T1, T2, T3> getThis() {
            return this;
        }

        @Override
        default BranchedArgument3TreeBuilder<S, T1, T2, T3> branch(Key baseKey) {
            return new BranchedArgument3TreeBuilder<>(baseKey, this, arg1(), arg2(), arg3());
        }

        //--

        default Argument3TreeBuilder<S, T1, T2, T3> leaves(String key, CommandFunction3<S, T1, T2, T3> commandExecution) {
            return leaves(key, List.of(), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3().getArgument(ctx)));
            });
        }

        default <T4> Argument3TreeBuilder<S, T1, T2, T3> leaves(String key, Argument<T4> arg4, CommandFunction4<S, T1, T2, T3, T4> commandExecution) {
            return leaves(key, List.of(arg4), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3().getArgument(ctx), arg4.getArgument(ctx)));
            });
        }

        default <T4, T5> Argument3TreeBuilder<S, T1, T2, T3> leaves(String key, Argument<T4> arg4, Argument<T5> arg5, CommandFunction5<S, T1, T2, T3, T4, T5> commandExecution) {
            return leaves(key, List.of(arg4, arg5), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3().getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx)));
            });
        }

        default <T4, T5, T6> Argument3TreeBuilder<S, T1, T2, T3> leaves(String key, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, CommandFunction6<S, T1, T2, T3, T4, T5, T6> commandExecution) {
            return leaves(key, List.of(arg4, arg5, arg6), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3().getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx)));
            });
        }

        default <T4, T5, T6, T7> Argument3TreeBuilder<S, T1, T2, T3> leaves(String key, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, Argument<T7> arg7, CommandFunction7<S, T1, T2, T3, T4, T5, T6, T7> commandExecution) {
            return leaves(key, List.of(arg4, arg5, arg6, arg7), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3().getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx), arg7.getArgument(ctx)));
            });
        }

        default <T4, T5, T6, T7, T8> Argument3TreeBuilder<S, T1, T2, T3> leaves(String key, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, Argument<T7> arg7, Argument<T8> arg8, CommandFunction8<S, T1, T2, T3, T4, T5, T6, T7, T8> commandExecution) {
            return leaves(key, List.of(arg4, arg5, arg6, arg7, arg8), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3().getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx), arg7.getArgument(ctx), arg8.getArgument(ctx)));
            });
        }

        default <T4, T5, T6, T7, T8, T9> Argument3TreeBuilder<S, T1, T2, T3> leaves(String key, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, Argument<T7> arg7, Argument<T8> arg8, Argument<T9> arg9, CommandFunction9<S, T1, T2, T3, T4, T5, T6, T7, T8, T9> commandExecution) {
            return leaves(key, List.of(arg4, arg5, arg6, arg7, arg8, arg9), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3().getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx), arg7.getArgument(ctx), arg8.getArgument(ctx), arg9.getArgument(ctx)));
            });
        }

        //--

        default Argument3TreeBuilder<S, T1, T2, T3> leaves(CommandFunction3<S, T1, T2, T3> commandExecution) {
            return leaves(List.of(), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3().getArgument(ctx)));
            });
        }

        default <T4> Argument3TreeBuilder<S, T1, T2, T3> leaves(Argument<T4> arg4, CommandFunction4<S, T1, T2, T3, T4> commandExecution) {
            return leaves(List.of(arg4), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3().getArgument(ctx), arg4.getArgument(ctx)));
            });
        }

        default <T4, T5> Argument3TreeBuilder<S, T1, T2, T3> leaves(Argument<T4> arg4, Argument<T5> arg5, CommandFunction5<S, T1, T2, T3, T4, T5> commandExecution) {
            return leaves(List.of(arg4, arg5), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3().getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx)));
            });
        }

        default <T4, T5, T6> Argument3TreeBuilder<S, T1, T2, T3> leaves(Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, CommandFunction6<S, T1, T2, T3, T4, T5, T6> commandExecution) {
            return leaves(List.of(arg4, arg5, arg6), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3().getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx)));
            });
        }

        default <T4, T5, T6, T7> Argument3TreeBuilder<S, T1, T2, T3> leaves(Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, Argument<T7> arg7, CommandFunction7<S, T1, T2, T3, T4, T5, T6, T7> commandExecution) {
            return leaves(List.of(arg4, arg5, arg6, arg7), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3().getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx), arg7.getArgument(ctx)));
            });
        }

        default <T4, T5, T6, T7, T8> Argument3TreeBuilder<S, T1, T2, T3> leaves(Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, Argument<T7> arg7, Argument<T8> arg8, CommandFunction8<S, T1, T2, T3, T4, T5, T6, T7, T8> commandExecution) {
            return leaves(List.of(arg4, arg5, arg6, arg7, arg8), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3().getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx), arg7.getArgument(ctx), arg8.getArgument(ctx)));
            });
        }

        default <T4, T5, T6, T7, T8, T9> Argument3TreeBuilder<S, T1, T2, T3> leaves(Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, Argument<T7> arg7, Argument<T8> arg8, Argument<T9> arg9, CommandFunction9<S, T1, T2, T3, T4, T5, T6, T7, T8, T9> commandExecution) {
            return leaves(List.of(arg4, arg5, arg6, arg7, arg8, arg9), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3().getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx), arg7.getArgument(ctx), arg8.getArgument(ctx), arg9.getArgument(ctx)));
            });
        }
    }

    record BranchedArgument3TreeBuilder<S, T1, T2, T3>(Key branchKey, CommandLeafBuilder<S, ?, ?> commandLeafBuilder, Argument<T1> arg1, Argument<T2> arg2, Argument<T3> arg3) implements Argument3TreeBuilder<S, T1, T2, T3>, Branch {
        @Override public List<Argument<?>> getArgs() { return branchKey.asArgumentList(); }
    }

    record Argument3TreeBuilderImpl<S, T1, T2, T3>(CommandLeafBuilder<S, ?, ?> commandLeafBuilder, Argument<T1> arg1, Argument<T2> arg2, Argument<T3> arg3) implements Argument3TreeBuilder<S, T1, T2, T3> {
        @Override public List<Argument<?>> getArgs() { return List.of(arg1(), arg2(), arg3()); }
    }
}
