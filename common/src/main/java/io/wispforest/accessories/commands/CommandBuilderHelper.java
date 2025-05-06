package io.wispforest.accessories.commands;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class CommandBuilderHelper {

    public Map<String, LiteralArgumentBuilder<CommandSourceStack>> baseCommandPart = new LinkedHashMap<>();

    public BiMap<Key, ArgumentBuilder> commandParts = HashBiMap.create();

    public void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        generateTrees(context);

        this.baseCommandPart.forEach((string, builder) -> dispatcher.register(builder));

        this.baseCommandPart.clear();
        this.commandParts.clear();
    }

    protected abstract void generateTrees(CommandBuildContext context);

    public abstract void registerArgumentTypes(ArgumentRegistration registration);

    //--

    public <T> CommandArgumentHolder<T> argumentHolder(String name, ArgumentType<?> type, CommandArgumentGetter<T> getter) {
        return CommandArgumentHolder.of(name, type, getter);
    }

    public <T> CommandArgumentHolder<T> defaultedArgumentHolder(String name, ArgumentType<?> type, CommandArgumentGetter<T> getter, T defaultValue) {
        return CommandArgumentHolder.defaulted(name, type, getter, defaultValue);
    }

    public <T extends ArgumentBuilder<CommandSourceStack, T>> ArgumentBuilder<CommandSourceStack, T> getOrCreateNode(String key, CommandArgumentHolder<?> ...argumentsParts) {
        var baseBuilder = getOrCreateNode(key.split("/"));

        RequiredArgumentBuilder<CommandSourceStack, ?> outerArg = null;

        RequiredArgumentBuilder<CommandSourceStack, ?> currentArg = null;

        var args = new ArrayList<Pair<String, RequiredArgumentBuilder<CommandSourceStack, ?>>>();

        for (var arg : Lists.reverse(List.of(argumentsParts))) {
            if (currentArg == null) {
                currentArg = arg.builder();
                outerArg = currentArg;
            } else {
                currentArg = arg.builder().then(currentArg);
            }

            args.add(Pair.of(arg.name(), currentArg));
        }

        for (var pair : Lists.reverse(args)) {
            key = key + "/" + pair.first();

            var argKey = new Key(key);

            this.commandParts.put(argKey, pair.second());
        }

        baseBuilder.then(currentArg);

        return (ArgumentBuilder<CommandSourceStack, T>) outerArg;
    }

    public <T extends ArgumentBuilder<CommandSourceStack, T>> ArgumentBuilder<CommandSourceStack, T> getOrCreateNode(String key) {
        return getOrCreateNode(new Key(key));
    }

    public <T extends ArgumentBuilder<CommandSourceStack, T>> ArgumentBuilder<CommandSourceStack, T> getOrCreateNode(String ...keyParts) {
        return getOrCreateNode(new Key(keyParts));
    }

    public <T extends ArgumentBuilder<CommandSourceStack, T>> ArgumentBuilder<CommandSourceStack, T> getOrCreateNode(Key key) {
        if (commandParts.containsKey(key)) return commandParts.get(key);

        ArgumentBuilder<CommandSourceStack, T> builder;

        var parentKey = key.parent();

        if (parentKey == null) {
            var baseBuilder =  Commands.literal(key.topPath());

            this.baseCommandPart.put(key.topPath(), baseBuilder);

            builder = (ArgumentBuilder<CommandSourceStack, T>) baseBuilder;
        } else {
            var parentBuilder = getOrCreateNode(parentKey);

            builder = (ArgumentBuilder<CommandSourceStack, T>) Commands.literal(key.topPath());

            parentBuilder.then(builder);
        }

        this.commandParts.put(key, builder);

        return builder;
    }

    public <T extends ArgumentBuilder<CommandSourceStack, T>> void updateParent(ArgumentBuilder builder) {
        var key = this.commandParts.inverse().get(builder);

        var parentKey = key.parent();

        if (parentKey == null) return;

        var parentBuilder = this.commandParts.get(parentKey);

        parentBuilder.then(builder);

        updateParent(parentBuilder);
    }

    //--

    public <T1> void optionalArgExectution(String key, CommandArgumentHolder<T1> arg1, CommandFunction1<@Nullable T1> commandExecution) {
        optionalArgExectution(new Key(key), arg1, commandExecution);
    }

    public <T1> void optionalArgExectution(Key key, CommandArgumentHolder<T1> arg1, CommandFunction1<@Nullable T1> commandExecution) {
        updateParent(
                this.getOrCreateNode(key)
                        .then(arg1.builder().executes((ctx) -> commandExecution.execute(ctx, arg1.getArgument(ctx))))
                        .executes(ctx -> commandExecution.execute(ctx, null))
        );
    }

    public <T1> void requiredArgExectution(String key, CommandArgumentHolder<T1> arg1, CommandFunction1<T1> commandExecution) {
        requiredArgExectution(new Key(key), arg1, commandExecution);
    }

    public <T1> void requiredArgExectution(Key key, CommandArgumentHolder<T1> arg1, CommandFunction1<T1> commandExecution) {
        updateParent(
                this.getOrCreateNode(key).then(arg1.builder().executes((ctx) -> commandExecution.execute(ctx, arg1.getArgument(ctx))))
        );
    }

    public <T1, T2> void requiredArgExectution(String key, CommandArgumentHolder<T1> arg1, CommandArgumentHolder<T2> arg2, CommandFunction2<T1, T2> commandExecution) {
        requiredArgExectution(new Key(key), arg1, arg2, commandExecution);
    }

    public <T1, T2> void requiredArgExectution(Key key, CommandArgumentHolder<T1> arg1, CommandArgumentHolder<T2> arg2, CommandFunction2<T1, T2> commandExecution) {
        updateParent(
                this.getOrCreateNode(key).then(arg1.builder().then(arg2.builder().executes((ctx) -> commandExecution.execute(ctx, arg1.getArgument(ctx), arg2.getArgument(ctx)))))
        );
    }

    public <T1, T2, T3> void requiredArgExectution(String key, CommandArgumentHolder<T1> arg1, CommandArgumentHolder<T2> arg2, CommandArgumentHolder<T3> arg3, CommandFunction3<T1, T2, T3> commandExecution) {
        requiredArgExectution(new Key(key), arg1, arg2, arg3, commandExecution);
    }

    public <T1, T2, T3> void requiredArgExectution(Key key, CommandArgumentHolder<T1> arg1, CommandArgumentHolder<T2> arg2, CommandArgumentHolder<T3> arg3, CommandFunction3<T1, T2, T3> commandExecution) {
        updateParent(
                this.getOrCreateNode(key).then(arg1.builder().then(arg2.builder().then(arg3.builder().executes((ctx) -> commandExecution.execute(ctx, arg1.getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx))))))
        );
    }

    public <T1, T2, T3, T4> void requiredArgExectution(String key, CommandArgumentHolder<T1> arg1, CommandArgumentHolder<T2> arg2, CommandArgumentHolder<T3> arg3, CommandArgumentHolder<T4> arg4, CommandFunction4<T1, T2, T3, T4> commandExecution) {
        requiredArgExectution(new Key(key), arg1, arg2, arg3, arg4, commandExecution);
    }

    public <T1, T2, T3, T4> void requiredArgExectution(Key key, CommandArgumentHolder<T1> arg1, CommandArgumentHolder<T2> arg2, CommandArgumentHolder<T3> arg3, CommandArgumentHolder<T4> arg4, CommandFunction4<T1, T2, T3, T4> commandExecution) {
        updateParent(
                this.getOrCreateNode(key).then(arg1.builder().then(arg2.builder().then(arg3.builder().executes((ctx) -> commandExecution.execute(ctx, arg1.getArgument(ctx), arg2.getArgument(ctx), arg3.getArgument(ctx), arg4.getArgument(ctx))))))
        );
    }

    public void requiredExectutionBranched(String key, List<String> literalBranches, CommandFunction1<String> commandExecution) {
        requiredExectutionBranched(new Key(key), literalBranches, commandExecution);
    }

    public void requiredExectutionBranched(Key key, List<String> literalBranches, CommandFunction1<String> commandExecution) {
        var builder = this.getOrCreateNode(key);

        for (var branch : literalBranches) {
            builder.then(
                    Commands.literal(branch).executes((ctx) -> commandExecution.execute(ctx, branch))
            );
        }

        updateParent(builder);
    }

    public <T1> void requiredArgExectutionBranched(String key, List<String> literalBranches, CommandArgumentHolder<T1> arg1, CommandFunction2<String, T1> commandExecution) {
        requiredArgExectutionBranched(new Key(key), literalBranches, arg1, commandExecution);
    }

    public <T1> void requiredArgExectutionBranched(Key key, List<String> literalBranches, CommandArgumentHolder<T1> arg1, CommandFunction2<String, T1> commandExecution) {
        var builder = this.getOrCreateNode(key);

        for (var branch : literalBranches) {
            builder.then(
                    Commands.literal(branch)
                            .then(
                                    arg1.builder().executes((ctx) -> {
                                        return commandExecution.execute(ctx, branch, arg1.getArgument(ctx));
                                    })
                            )
            );
        }

        updateParent(builder);
    }

    //--

    public record CommandArgumentHolder<T>(String name, ArgumentType<?> type, CommandArgumentGetter<T> getter, boolean defaulted, @Nullable T defaultValue) {
        public static <T> CommandArgumentHolder<T> of(String name, ArgumentType<?> type, CommandArgumentGetter<T> getter) {
            return new CommandArgumentHolder<>(name, type, getter, false, null);
        }

        public static <T> CommandArgumentHolder<T> defaulted(String name, ArgumentType<?> type, CommandArgumentGetter<T> getter, T defaultValue) {
            return new CommandArgumentHolder<>(name, type, getter, true, defaultValue);
        }

        public RequiredArgumentBuilder<CommandSourceStack, ?> builder() {
            return Commands.argument(name, type);
        }

        public T getArgument(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
            try {
                return this.getter.get(ctx, name);
            } catch (IllegalArgumentException e) {
                if (defaulted) return defaultValue;

                throw e;
            }
        }
    }

    public interface CommandArgumentGetter<T> {
        T get(CommandContext<CommandSourceStack> ctx, String name) throws CommandSyntaxException;
    }

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

    //--

    public interface ArgumentRegistration {
        <A extends ArgumentType<?>, T> RecordArgumentTypeInfo<A, T> register(ResourceLocation location, Class<A> clazz, RecordArgumentTypeInfo<A, T> info);
    }
}
