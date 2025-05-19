package io.wispforest.accessories.commands.api.base;

import io.wispforest.accessories.commands.api.core.CommandAddition;
import io.wispforest.accessories.commands.api.core.Key;

import java.util.ArrayList;
import java.util.List;

import static io.wispforest.accessories.commands.api.base.CommandTreeBuilder.*;

public abstract class CommandTreeBuilderWithArgs<S, B extends CommandTreeBuilderWithArgs<S, B>> {

    protected final CommandTreeBuilder<S, ?> commandTreeBuilder;

    CommandTreeBuilderWithArgs(CommandTreeBuilder<S, ?> commandTreeBuilder){
        this.commandTreeBuilder = commandTreeBuilder;
    }

    //--

    public B createLeaves(String key, List<Argument<?>> commandArgs, CommandAddition<S> commandAddition) {
        createLeaves(new Key(key), commandArgs, commandAddition);

        return getThis();
    }

    public B createLeaves(List<Argument<?>> commandArgs, CommandAddition<S> commandAddition) {
        createLeaves(new Key(), commandArgs, commandAddition);

        return getThis();
    }

    protected B createLeaves(Key key, List<Argument<?>> commandArgs, CommandAddition<S> commandAddition) {
        var startingArgs = new ArrayList<>(this.getArgs());

        startingArgs.addAll(key.asArgumentList());

        this.commandTreeBuilder.createLeaves(startingArgs, commandArgs, commandAddition);

        return getThis();
    }

    //--

    public static <S, T1> Argument1TreeBuilder<S, T1> createArgBranch(CommandTreeBuilder<S, ?> commandTreeBuilder, Argument<T1> arg1) {
        return new Argument1TreeBuilder<>(commandTreeBuilder, arg1);
    }

    public static <S, T1, T2> Argument2TreeBuilder<S, T1, T2> createArgBranch(CommandTreeBuilder<S, ?> commandTreeBuilder, Argument<T1> arg1, Argument<T2> arg2) {
        return new Argument2TreeBuilder<>(commandTreeBuilder, arg1, arg2);
    }

    public static <S, T1, T2, T3> Argument3TreeBuilder<S, T1, T2, T3> createArgBranch(CommandTreeBuilder<S, ?> commandTreeBuilder, Argument<T1> arg1, Argument<T2> arg2, Argument<T3> arg3) {
        return new Argument3TreeBuilder<>(commandTreeBuilder, arg1, arg2, arg3);
    }

    public static <S, T1> void createArgBranch(CommandTreeBuilder<S, ?> commandTreeBuilder, Argument<T1> arg1, ArgBranchBuilder<S, Argument1TreeBuilder<S, T1>> builder) {
        builder.addLeaves(createArgBranch(commandTreeBuilder, arg1));
    }

    public static <S, T1, T2> void createArgBranch(CommandTreeBuilder<S, ?> commandTreeBuilder, Argument<T1> arg1, Argument<T2> arg2, ArgBranchBuilder<S, Argument2TreeBuilder<S, T1, T2>> builder) {
        builder.addLeaves(createArgBranch(commandTreeBuilder, arg1, arg2));
    }

    public static <S, T1, T2, T3> void createArgBranch(CommandTreeBuilder<S, ?> commandTreeBuilder, Argument<T1> arg1, Argument<T2> arg2, Argument<T3> arg3, ArgBranchBuilder<S, Argument3TreeBuilder<S, T1, T2, T3>> builder) {
        builder.addLeaves(createArgBranch(commandTreeBuilder, arg1, arg2, arg3));
    }

    //--

    public void branch(List<String> keyParts, ArgBranchBuilder<S, B> builder) {
        branch(new Key(keyParts), builder);
    }

    public void branch(String key, ArgBranchBuilder<S, B> builder) {
        branch(new Key(key), builder);
    }

    public void branch(Key baseKey, ArgBranchBuilder<S, B> builder) {
        builder.addLeaves(branch(baseKey));
    }

    public interface ArgBranchBuilder<S, B>  {
        void addLeaves(B builder);
    }

    //--

    public B branch(List<String> keyParts) {
        return branch(new Key(keyParts));
    }

    public B branch(String key) {
        return branch(new Key(key));
    }

    public B branch(Key baseKey) {
        return cloneWithNewBuilder(commandTreeBuilder.branch(baseKey));
    }

    //--

    protected abstract B cloneWithNewBuilder(CommandTreeBuilder<S, ?> commandTreeBuilder);

    protected abstract B getThis();

    protected abstract List<Argument<?>> getArgs();

    public final static class Argument1TreeBuilder<S, T1> extends CommandTreeBuilderWithArgs<S, Argument1TreeBuilder<S, T1>> {

        private final Argument<T1> arg1;

        Argument1TreeBuilder(CommandTreeBuilder<S, ?> commandTreeBuilder, Argument<T1> arg1) {
            super(commandTreeBuilder);

            this.arg1 = arg1;
        }

        //--

        @Override
        protected Argument1TreeBuilder<S, T1> cloneWithNewBuilder(CommandTreeBuilder<S, ?> commandTreeBuilder) {
            return new Argument1TreeBuilder<>(commandTreeBuilder, arg1());
        }

        @Override
        protected Argument1TreeBuilder<S, T1> getThis() {
            return this;
        }

        @Override
        protected List<Argument<?>> getArgs() {
            return List.of(arg1());
        }

        //--

        public Argument<T1> arg1() {
            return this.arg1;
        }

        //--

        public <T2> CommandTreeBuilderWithArgs.Argument2TreeBuilder<S, T1, T2> createArgBranch(Argument<T2> argument2) {
            return new CommandTreeBuilderWithArgs.Argument2TreeBuilder<>(this.commandTreeBuilder, arg1(), argument2);
        }

        public <T2, T3> CommandTreeBuilderWithArgs.Argument3TreeBuilder<S, T1, T2, T3> createArgBranch(Argument<T2> argument2, Argument<T3> argument3) {
            return new CommandTreeBuilderWithArgs.Argument3TreeBuilder<>(this.commandTreeBuilder, arg1(), argument2, argument3);
        }

        //--

        public Argument1TreeBuilder<S, T1> createLeaves(String key, CommandFunction1<S, T1> commandExecution) {
            return createLeaves(key, List.of(), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx)));
            });
        }

        public <T2> Argument1TreeBuilder<S, T1> createLeaves(String key, Argument<T2> arg2, CommandFunction2<S, T1, T2> commandExecution) {
            return createLeaves(key, List.of(arg2), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2.getArgument(ctx)));
            });
        }

        public <T2, T3> Argument1TreeBuilder<S, T1> createLeaves(String key, Argument<T2> arg2, Argument<T3> arg3, CommandFunction3<S, T1, T2, T3> commandExecution) {
            return createLeaves(key, List.of(arg2, arg3), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx)));
            });
        }

        public <T2, T3, T4> Argument1TreeBuilder<S, T1> createLeaves(String key, Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, CommandFunction4<S, T1, T2, T3, T4> commandExecution) {
            return createLeaves(key, List.of(arg2, arg3, arg4), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx)));
            });
        }

        public <T2, T3, T4, T5> Argument1TreeBuilder<S, T1> createLeaves(String key, Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, CommandFunction5<S, T1, T2, T3, T4, T5> commandExecution) {
            return createLeaves(key, List.of(arg2, arg3, arg4, arg5), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx)));
            });
        }

        public <T2, T3, T4, T5, T6> Argument1TreeBuilder<S, T1> createLeaves(String key, Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, CommandFunction6<S, T1, T2, T3, T4, T5, T6> commandExecution) {
            return createLeaves(key, List.of(arg2, arg3, arg4, arg5, arg6), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx)));
            });
        }

        public <T2, T3, T4, T5, T6, T7> Argument1TreeBuilder<S, T1> createLeaves(String key, Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, Argument<T7> arg7, CommandFunction7<S, T1, T2, T3, T4, T5, T6, T7> commandExecution) {
            return createLeaves(key, List.of(arg2, arg3, arg4, arg5, arg6, arg7), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx), arg7.getArgument(ctx)));
            });
        }

        public <T2, T3, T4, T5, T6, T7, T8> Argument1TreeBuilder<S, T1> createLeaves(String key, Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, Argument<T7> arg7, Argument<T8> arg8, CommandFunction8<S, T1, T2, T3, T4, T5, T6, T7, T8> commandExecution) {
            return createLeaves(key, List.of(arg2, arg3, arg4, arg5, arg6, arg7, arg8), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx), arg7.getArgument(ctx), arg8.getArgument(ctx)));
            });
        }

        public <T2, T3, T4, T5, T6, T7, T8, T9> Argument1TreeBuilder<S, T1> createLeaves(String key, Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, Argument<T7> arg7, Argument<T8> arg8, Argument<T9> arg9, CommandFunction9<S, T1, T2, T3, T4, T5, T6, T7, T8, T9> commandExecution) {
            return createLeaves(key, List.of(arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx), arg7.getArgument(ctx), arg8.getArgument(ctx), arg9.getArgument(ctx)));
            });
        }

        //--

        public Argument1TreeBuilder<S, T1> createLeaves(CommandFunction1<S, T1> commandExecution) {
            return createLeaves(List.of(), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx)));
            });
        }

        public <T2> Argument1TreeBuilder<S, T1> createLeaves(Argument<T2> arg2, CommandFunction2<S, T1, T2> commandExecution) {
            return createLeaves(List.of(arg2), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2.getArgument(ctx)));
            });
        }

        public <T2, T3> Argument1TreeBuilder<S, T1> createLeaves(Argument<T2> arg2, Argument<T3> arg3, CommandFunction3<S, T1, T2, T3> commandExecution) {
            return createLeaves(List.of(arg2, arg3), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx)));
            });
        }

        public <T2, T3, T4> Argument1TreeBuilder<S, T1> createLeaves(Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, CommandFunction4<S, T1, T2, T3, T4> commandExecution) {
            return createLeaves(List.of(arg2, arg3, arg4), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx)));
            });
        }

        public <T2, T3, T4, T5> Argument1TreeBuilder<S, T1> createLeaves(Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, CommandFunction5<S, T1, T2, T3, T4, T5> commandExecution) {
            return createLeaves(List.of(arg2, arg3, arg4, arg5), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx)));
            });
        }

        public <T2, T3, T4, T5, T6> Argument1TreeBuilder<S, T1> createLeaves(Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, CommandFunction6<S, T1, T2, T3, T4, T5, T6> commandExecution) {
            return createLeaves(List.of(arg2, arg3, arg4, arg5, arg6), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx)));
            });
        }

        public <T2, T3, T4, T5, T6, T7> Argument1TreeBuilder<S, T1> createLeaves(Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, Argument<T7> arg7, CommandFunction7<S, T1, T2, T3, T4, T5, T6, T7> commandExecution) {
            return createLeaves(List.of(arg2, arg3, arg4, arg5, arg6, arg7), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx), arg7.getArgument(ctx)));
            });
        }

        public <T2, T3, T4, T5, T6, T7, T8> Argument1TreeBuilder<S, T1> createLeaves(Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, Argument<T7> arg7, Argument<T8> arg8, CommandFunction8<S, T1, T2, T3, T4, T5, T6, T7, T8> commandExecution) {
            return createLeaves(List.of(arg2, arg3, arg4, arg5, arg6, arg7, arg8), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx), arg7.getArgument(ctx), arg8.getArgument(ctx)));
            });
        }

        public <T2, T3, T4, T5, T6, T7, T8, T9> Argument1TreeBuilder<S, T1> createLeaves(Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, Argument<T7> arg7, Argument<T8> arg8, Argument<T9> arg9, CommandFunction9<S, T1, T2, T3, T4, T5, T6, T7, T8, T9> commandExecution) {
            return createLeaves(List.of(arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx), arg7.getArgument(ctx), arg8.getArgument(ctx), arg9.getArgument(ctx)));
            });
        }
    }

    public final static class Argument2TreeBuilder<S, T1, T2> extends CommandTreeBuilderWithArgs<S, Argument2TreeBuilder<S, T1, T2>> {

        private final Argument<T1> arg1;
        private final Argument<T2> arg2;

        Argument2TreeBuilder(CommandTreeBuilder<S, ?> commandTreeBuilder, Argument<T1> arg1, Argument<T2> arg2) {
            super(commandTreeBuilder);

            this.arg1 = arg1;
            this.arg2 = arg2;
        }

        //--

        @Override
        protected Argument2TreeBuilder<S, T1, T2> cloneWithNewBuilder(CommandTreeBuilder<S, ?> commandTreeBuilder) {
            return new Argument2TreeBuilder<>(commandTreeBuilder, arg1(), arg2());
        }

        @Override
        protected Argument2TreeBuilder<S, T1, T2> getThis() {
            return this;
        }

        @Override
        protected List<Argument<?>> getArgs() {
            return List.of(arg1(), arg2());
        }

        //--

        public Argument<T1> arg1() {
            return this.arg1;
        }

        public Argument<T2> arg2() {
            return this.arg2;
        }

        //--

        public <T3> CommandTreeBuilderWithArgs.Argument3TreeBuilder<S, T1, T2, T3> createArgBranch(Argument<T3> argument3) {
            return new CommandTreeBuilderWithArgs.Argument3TreeBuilder<>(this.commandTreeBuilder, arg1(), arg2(), argument3);
        }

        //--

        public Argument2TreeBuilder<S, T1, T2> leaves(String key, CommandFunction2<S, T1, T2> commandExecution) {
            return createLeaves(key, List.of(), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx)));
            });
        }

        public <T3> Argument2TreeBuilder<S, T1, T2> leaves(String key, Argument<T3> arg3, CommandFunction3<S, T1, T2, T3> commandExecution) {
            return createLeaves(key, List.of(arg3), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3.getArgument(ctx)));
            });
        }

        public <T3, T4> Argument2TreeBuilder<S, T1, T2> leaves(String key, Argument<T3> arg3, Argument<T4> arg4, CommandFunction4<S, T1, T2, T3, T4> commandExecution) {
            return createLeaves(key, List.of(arg3, arg4), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx)));
            });
        }

        public <T3, T4, T5> Argument2TreeBuilder<S, T1, T2> leaves(String key, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, CommandFunction5<S, T1, T2, T3, T4, T5> commandExecution) {
            return createLeaves(key, List.of(arg3, arg4, arg5), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx)));
            });
        }

        public <T3, T4, T5, T6> Argument2TreeBuilder<S, T1, T2> leaves(String key, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, CommandFunction6<S, T1, T2, T3, T4, T5, T6> commandExecution) {
            return createLeaves(key, List.of(arg3, arg4, arg5, arg6), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx)));
            });
        }

        public <T3, T4, T5, T6, T7> Argument2TreeBuilder<S, T1, T2> leaves(String key, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, Argument<T7> arg7, CommandFunction7<S, T1, T2, T3, T4, T5, T6, T7> commandExecution) {
            return createLeaves(key, List.of(arg3, arg4, arg5, arg6, arg7), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx), arg7.getArgument(ctx)));
            });
        }

        public <T3, T4, T5, T6, T7, T8> Argument2TreeBuilder<S, T1, T2> leaves(String key, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, Argument<T7> arg7, Argument<T8> arg8, CommandFunction8<S, T1, T2, T3, T4, T5, T6, T7, T8> commandExecution) {
            return createLeaves(key, List.of(arg3, arg4, arg5, arg6, arg7, arg8), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx), arg7.getArgument(ctx), arg8.getArgument(ctx)));
            });
        }

        public <T3, T4, T5, T6, T7, T8, T9> Argument2TreeBuilder<S, T1, T2> leaves(String key, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, Argument<T7> arg7, Argument<T8> arg8, Argument<T9> arg9, CommandFunction9<S, T1, T2, T3, T4, T5, T6, T7, T8, T9> commandExecution) {
            return createLeaves(key, List.of(arg3, arg4, arg5, arg6, arg7, arg8, arg9), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx), arg7.getArgument(ctx), arg8.getArgument(ctx), arg9.getArgument(ctx)));
            });
        }

        //--

        public Argument2TreeBuilder<S, T1, T2> leaves(CommandFunction2<S, T1, T2> commandExecution) {
            return createLeaves(List.of(), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx)));
            });
        }

        public <T3> Argument2TreeBuilder<S, T1, T2> leaves(Argument<T3> arg3, CommandFunction3<S, T1, T2, T3> commandExecution) {
            return createLeaves(List.of(arg3), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3.getArgument(ctx)));
            });
        }

        public <T3, T4> Argument2TreeBuilder<S, T1, T2> leaves(Argument<T3> arg3, Argument<T4> arg4, CommandFunction4<S, T1, T2, T3, T4> commandExecution) {
            return createLeaves(List.of(arg3, arg4), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx)));
            });
        }

        public <T3, T4, T5> Argument2TreeBuilder<S, T1, T2> leaves(Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, CommandFunction5<S, T1, T2, T3, T4, T5> commandExecution) {
            return createLeaves(List.of(arg3, arg4, arg5), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx)));
            });
        }

        public <T3, T4, T5, T6> Argument2TreeBuilder<S, T1, T2> leaves(Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, CommandFunction6<S, T1, T2, T3, T4, T5, T6> commandExecution) {
            return createLeaves(List.of(arg3, arg4, arg5, arg6), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx)));
            });
        }

        public <T3, T4, T5, T6, T7> Argument2TreeBuilder<S, T1, T2> leaves(Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, Argument<T7> arg7, CommandFunction7<S, T1, T2, T3, T4, T5, T6, T7> commandExecution) {
            return createLeaves(List.of(arg3, arg4, arg5, arg6, arg7), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx), arg7.getArgument(ctx)));
            });
        }

        public <T3, T4, T5, T6, T7, T8> Argument2TreeBuilder<S, T1, T2> leaves(Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, Argument<T7> arg7, Argument<T8> arg8, CommandFunction8<S, T1, T2, T3, T4, T5, T6, T7, T8> commandExecution) {
            return createLeaves(List.of(arg3, arg4, arg5, arg6, arg7, arg8), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx), arg7.getArgument(ctx), arg8.getArgument(ctx)));
            });
        }

        public <T3, T4, T5, T6, T7, T8, T9> Argument2TreeBuilder<S, T1, T2> leaves(Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, Argument<T7> arg7, Argument<T8> arg8, Argument<T9> arg9, CommandFunction9<S, T1, T2, T3, T4, T5, T6, T7, T8, T9> commandExecution) {
            return createLeaves(List.of(arg3, arg4, arg5, arg6, arg7, arg8, arg9), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx), arg7.getArgument(ctx), arg8.getArgument(ctx), arg9.getArgument(ctx)));
            });
        }
    }

    public final static class Argument3TreeBuilder<S, T1, T2, T3> extends CommandTreeBuilderWithArgs<S, Argument3TreeBuilder<S, T1, T2, T3>> {

        private final Argument<T1> arg1;
        private final Argument<T2> arg2;
        private final Argument<T3> arg3;

        Argument3TreeBuilder(CommandTreeBuilder<S, ?> commandTreeBuilder, Argument<T1> arg1, Argument<T2> arg2, Argument<T3> arg3) {
            super(commandTreeBuilder);

            this.arg1 = arg1;
            this.arg2 = arg2;
            this.arg3 = arg3;
        }

        //--

        @Override
        protected Argument3TreeBuilder<S, T1, T2, T3> cloneWithNewBuilder(CommandTreeBuilder<S, ?> commandTreeBuilder) {
            return new Argument3TreeBuilder<>(commandTreeBuilder, arg1(), arg2(), arg3());
        }

        @Override
        protected Argument3TreeBuilder<S, T1, T2, T3> getThis() {
            return this;
        }

        @Override
        protected List<Argument<?>> getArgs() {
            return List.of(arg1(), arg2(), arg3());
        }

        //--

        public Argument<T1> arg1() {
            return this.arg1;
        }

        public Argument<T2> arg2() {
            return this.arg2;
        }

        public Argument<T3> arg3() {
            return this.arg3;
        }

        //--

        public Argument3TreeBuilder<S, T1, T2, T3> createLeaves(Key key, CommandFunction3<S, T1, T2, T3> commandExecution) {
            return createLeaves(key, List.of(), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3().getArgument(ctx)));
            });
        }

        public <T4> Argument3TreeBuilder<S, T1, T2, T3> createLeaves(Key key, Argument<T4> arg4, CommandFunction4<S, T1, T2, T3, T4> commandExecution) {
            return createLeaves(key, List.of(arg4), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3().getArgument(ctx), arg4.getArgument(ctx)));
            });
        }

        public <T4, T5> Argument3TreeBuilder<S, T1, T2, T3> createLeaves(Key key, Argument<T4> arg4, Argument<T5> arg5, CommandFunction5<S, T1, T2, T3, T4, T5> commandExecution) {
            return createLeaves(key, List.of(arg4, arg5), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3().getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx)));
            });
        }

        public <T4, T5, T6> Argument3TreeBuilder<S, T1, T2, T3> createLeaves(Key key, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, CommandFunction6<S, T1, T2, T3, T4, T5, T6> commandExecution) {
            return createLeaves(key, List.of(arg4, arg5, arg6), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3().getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx)));
            });
        }

        public <T4, T5, T6, T7> Argument3TreeBuilder<S, T1, T2, T3> createLeaves(String key, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, Argument<T7> arg7, CommandFunction7<S, T1, T2, T3, T4, T5, T6, T7> commandExecution) {
            return createLeaves(key, List.of(arg4, arg5, arg6, arg7), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3().getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx), arg7.getArgument(ctx)));
            });
        }

        public <T4, T5, T6, T7, T8> Argument3TreeBuilder<S, T1, T2, T3> createLeaves(String key, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, Argument<T7> arg7, Argument<T8> arg8, CommandFunction8<S, T1, T2, T3, T4, T5, T6, T7, T8> commandExecution) {
            return createLeaves(key, List.of(arg4, arg5, arg6, arg7, arg8), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3().getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx), arg7.getArgument(ctx), arg8.getArgument(ctx)));
            });
        }

        public <T4, T5, T6, T7, T8, T9> Argument3TreeBuilder<S, T1, T2, T3> createLeaves(String key, Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, Argument<T7> arg7, Argument<T8> arg8, Argument<T9> arg9, CommandFunction9<S, T1, T2, T3, T4, T5, T6, T7, T8, T9> commandExecution) {
            return createLeaves(key, List.of(arg4, arg5, arg6, arg7, arg8, arg9), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3().getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx), arg7.getArgument(ctx), arg8.getArgument(ctx), arg9.getArgument(ctx)));
            });
        }

        //--

        public Argument3TreeBuilder<S, T1, T2, T3> createLeaves(CommandFunction3<S, T1, T2, T3> commandExecution) {
            return createLeaves(List.of(), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3().getArgument(ctx)));
            });
        }

        public <T4> Argument3TreeBuilder<S, T1, T2, T3> createLeaves(Argument<T4> arg4, CommandFunction4<S, T1, T2, T3, T4> commandExecution) {
            return createLeaves(List.of(arg4), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3().getArgument(ctx), arg4.getArgument(ctx)));
            });
        }

        public <T4, T5> Argument3TreeBuilder<S, T1, T2, T3> createLeaves(Argument<T4> arg4, Argument<T5> arg5, CommandFunction5<S, T1, T2, T3, T4, T5> commandExecution) {
            return createLeaves(List.of(arg4, arg5), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3().getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx)));
            });
        }

        public <T4, T5, T6> Argument3TreeBuilder<S, T1, T2, T3> createLeaves(Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, CommandFunction6<S, T1, T2, T3, T4, T5, T6> commandExecution) {
            return createLeaves(List.of(arg4, arg5, arg6), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3().getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx)));
            });
        }

        public <T4, T5, T6, T7> Argument3TreeBuilder<S, T1, T2, T3> createLeaves(Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, Argument<T7> arg7, CommandFunction7<S, T1, T2, T3, T4, T5, T6, T7> commandExecution) {
            return createLeaves(List.of(arg4, arg5, arg6, arg7), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3().getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx), arg7.getArgument(ctx)));
            });
        }

        public <T4, T5, T6, T7, T8> Argument3TreeBuilder<S, T1, T2, T3> createLeaves(Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, Argument<T7> arg7, Argument<T8> arg8, CommandFunction8<S, T1, T2, T3, T4, T5, T6, T7, T8> commandExecution) {
            return createLeaves(List.of(arg4, arg5, arg6, arg7, arg8), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3().getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx), arg7.getArgument(ctx), arg8.getArgument(ctx)));
            });
        }

        public <T4, T5, T6, T7, T8, T9> Argument3TreeBuilder<S, T1, T2, T3> createLeaves(Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6, Argument<T7> arg7, Argument<T8> arg8, Argument<T9> arg9, CommandFunction9<S, T1, T2, T3, T4, T5, T6, T7, T8, T9> commandExecution) {
            return createLeaves(List.of(arg4, arg5, arg6, arg7, arg8, arg9), (node) -> {
                return node.executes(ctx -> commandExecution.execute(ctx, arg1().getArgument(ctx), arg2().getArgument(ctx), arg3().getArgument(ctx), arg4.getArgument(ctx), arg5.getArgument(ctx), arg6.getArgument(ctx), arg7.getArgument(ctx), arg8.getArgument(ctx), arg9.getArgument(ctx)));
            });
        }
    }
}
