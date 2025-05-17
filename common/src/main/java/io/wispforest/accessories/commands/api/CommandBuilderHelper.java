package io.wispforest.accessories.commands.api;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Range;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.wispforest.accessories.Accessories;
import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.StructEndecBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.wispforest.accessories.commands.api.Argument.*;

public abstract class CommandBuilderHelper extends CommandExecuteBuilder {

    public Map<String, NodeTreeHelper> baseCommandPart = new LinkedHashMap<>();

    public BiMap<Key, NodeTreeHelper> commandParts = HashBiMap.create();

    public void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        generateTrees(context);

        // TODO: DEHARDCODE LIT ARG BUILDER CAST
        this.baseCommandPart.forEach((string, builder) -> dispatcher.register((LiteralArgumentBuilder<CommandSourceStack>) builder.addToNode()));

        this.baseCommandPart.clear();
        this.commandParts.clear();
    }

    protected abstract void generateTrees(CommandBuildContext context);

    public void registerArgumentTypes(ArgumentRegistration registration) {}

    //--

    public static <T> Argument<T> arg(String name, ArgumentType<?> type, NamedArgumentGetter<T> getter) {
        return Argument.arg(name, type, getter);
    }

    public static <T> Argument<T> defaultedArg(String name, ArgumentType<?> type, NamedArgumentGetter<T> getter, T defaultValue) {
        return Argument.defaultedArg(name, type, getter, defaultValue);
    }

    public static Argument<String> branches(String ...branches) {
        return branches(List.of(branches));
    }

    public static Argument<String> branches(List<String> branches) {
        return new Argument.LiteralBranches(branches);
    }

    protected void addToNode(Key key, SequencedMap<Key, ArgumentBuilderConstructor<?>> unpackedArgs, CommandAddition addition) {
        if (commandParts.containsKey(key)) {
            commandParts.get(key).andWith(addition);

            return;
        }

        NodeTreeHelper helper = new NodeTreeHelper(unpackedArgs.get(key).createNodeBuilder());

        var parentKey = key.parent();

        if (parentKey == null) {
            this.baseCommandPart.put(key.topPath(), helper);
        } else {
            addToNode(parentKey, unpackedArgs, builder -> builder.then(helper.addToNode()));

            helper.andWith(addition);
        }

        this.commandParts.put(key, helper);
    }

    //--

    protected void addWithArgsAndKey(Key key, List<Argument<?>> commandArgs, CommandAddition commandAddition) {
        this.addToNode(key, builder -> builder);

        var rootNodeHelper = this.commandParts.get(key);

        // If no args then we can just execute the command addition since nothing is required
        if (commandArgs.isEmpty()) {
            rootNodeHelper.andWith(commandAddition);

            return;
        } else {
            commandArgs = new ArrayList<>(commandArgs);
        }

        SequencedMap<Key, NodeTreeHelper> baseBranchEnds = new LinkedHashMap<>(Map.of(key, rootNodeHelper));

        // The section handles the Literal Arguments to build the nodeTreeHelper for the final set of arguments or just uses the root node
        //--
        var branchArgs = commandArgs.stream()
                .filter(argument -> argument instanceof ArgumentBuilderConstructorList)
                .map(constructor -> ((ArgumentBuilderConstructorList) constructor))
                .toList();

        if(!branchArgs.isEmpty()) {
            var allOptionalArgs = commandArgs.stream()
                    .filter(argumentGetter -> {
                        if (argumentGetter instanceof ArgumentBuilderConstructor<?> constructor) {
                            return constructor.defaulted();
                        }

                        return false;
                    })
                    .map(constructor -> ((ArgumentBuilderConstructor<?>) constructor))
                    .collect(Collectors.toList());

            var branchToArgRange = new LinkedHashMap<ArgumentBuilderConstructorList, Range<Integer>>();

            int lastBranchIndex = 0;

            for (var branchArg : branchArgs) {
                var branchIndex = commandArgs.indexOf(branchArg);

                if (branchIndex == -1) throw new IllegalArgumentException("Unable to get branch arg in the command args list: " + branchArg);

                branchToArgRange.put(branchArg, Range.closedOpen(lastBranchIndex, branchIndex));

                lastBranchIndex = branchIndex + 1;
            }

            Range<Integer> currentRange = null;

            for (var entry : branchToArgRange.entrySet()) {
                var range = entry.getValue();

                List<ArgumentBuilderConstructor<?>> requiredArgs;

                if (range.lowerEndpoint().equals(range.upperEndpoint())) {
                    requiredArgs = List.of();
                } else {
                    requiredArgs = (List<ArgumentBuilderConstructor<?>>) (Object) commandArgs.subList(range.lowerEndpoint(), range.upperEndpoint())
                            .stream()
                            .map(argumentGetter -> ((ArgumentBuilderConstructor<?>) argumentGetter))
                            .toList();
                }

                currentRange = range;

                baseBranchEnds = buildBranchEnds(baseBranchEnds, entry.getKey(), requiredArgs);
            }

            try {
                commandArgs = commandArgs.subList(currentRange.upperEndpoint() + 1, commandArgs.size());
            } catch (IndexOutOfBoundsException e) {
                commandArgs = List.of();
            }

            // Handles adding any of the optional args nested between the given literals as it's not valid
            //--
            var remainingOptionalArgs = commandArgs.stream()
                    .filter(argumentGetter -> {
                        if (argumentGetter instanceof ArgumentBuilderConstructor<?> constructor) {
                            return constructor.defaulted();
                        }

                        return false;
                    })
                    .map(constructor -> ((ArgumentBuilderConstructor<?>) constructor))
                    .toList();

            allOptionalArgs.removeAll(remainingOptionalArgs);

            commandArgs.addAll(0, allOptionalArgs);
            //--
        }
        //--


        // Deal with the last bits of the command args finally executing
        //--
        if (commandArgs.isEmpty()) {
            baseBranchEnds.forEach((key1, baseBranchEnd) -> baseBranchEnd.andWith(commandAddition));
        } else {
            var singleArgs = commandArgs.stream()
                    .filter(argumentGetter -> argumentGetter instanceof ArgumentBuilderConstructor<?>)
                    .map(constructor -> ((ArgumentBuilderConstructor<?>) constructor))
                    .toList();

            var requiredArgs = singleArgs.stream()
                    .filter(argument -> !argument.defaulted())
                    .toList();

            var optionalArgs = singleArgs.stream()
                    .filter(ArgumentBuilderConstructor::defaulted)
                    .toList();

            baseBranchEnds.forEach((key1, baseBranchEnd) -> {
                NodeTreeHelper baseHelper = baseBranchEnd;

                for (var requiredArg : requiredArgs) {
                    var newNode = requiredArg.createNodeBuilder();

                    var newKey = key1.child(requiredArg.name());

                    if (this.commandParts.containsKey(newKey)) {
                        baseHelper = this.commandParts.get(newKey);
                    } else {
                        var newHelper = new NodeTreeHelper(newNode);

                        baseHelper.andWith(builder -> builder.then(newHelper.addToNode()));

                        this.commandParts.put(newKey, newHelper);

                        baseHelper = newHelper;
                    }
                }

                if (optionalArgs.isEmpty()) {
                    baseHelper.andWith(commandAddition);
                } else {
                    for (var optionalArg : optionalArgs) {
                        var newNode = optionalArg.createNodeBuilder();

                        var newKey = key1.child(optionalArg.name());

                        if (this.commandParts.containsKey(newKey)) {
                            baseHelper = this.commandParts.get(newKey);
                        } else {
                            var newHelper = new NodeTreeHelper(newNode);

                            baseHelper
                                    .andWith(builder -> builder.then(newHelper.addToNode()))
                                    .andWith(commandAddition);

                            this.commandParts.put(newKey, newHelper);

                            baseHelper = newHelper;
                        }
                    }

                    baseHelper.andWith(commandAddition);
                }
            });
        }
    }

    private SequencedMap<Key, NodeTreeHelper> buildBranchEnds(SequencedMap<Key, NodeTreeHelper> currentBranchEnds, ArgumentBuilderConstructorList argBuilderList, List<ArgumentBuilderConstructor<?>> args) {
        SequencedMap<Key, NodeTreeHelper> baseBranchEnds = new LinkedHashMap<>();

        currentBranchEnds.forEach((key, baseBranchEnd) -> {
            for (var arg : args) {
                if (arg.defaulted()) continue;

                key = key.child(arg.name());

                var newNode = arg.createNodeBuilder();

                NodeTreeHelper newHelper;

                if (this.commandParts.containsKey(key)) {
                    baseBranchEnd = this.commandParts.get(key);
                } else {
                    newHelper = new NodeTreeHelper(newNode);

                    this.commandParts.put(key, newHelper);

                    baseBranchEnd.andWith(builder -> builder.then(newHelper.addToNode()));

                    baseBranchEnd = newHelper;
                }
            }

            var helper = this.commandParts.get(key);

            if (helper != null) {
                baseBranchEnd = helper;
            }

            baseBranchEnds.put(key, baseBranchEnd);
        });

        SequencedMap<Key, NodeTreeHelper> finalBranchEnds = new LinkedHashMap<>();

        if (argBuilderList.builders().isEmpty()) {
            finalBranchEnds.putAll(baseBranchEnds);
        } else {
            baseBranchEnds.forEach((key, branchNode) -> {
                for (var argBuilderConstructor : argBuilderList.builders()) {
                    var newNode = argBuilderConstructor.createNodeBuilder();

                    var branchKey = key.child(argBuilderConstructor.name());

                    NodeTreeHelper newHelper;

                    if (this.commandParts.containsKey(branchKey)) {
                        newHelper = this.commandParts.get(branchNode);
                    } else {
                        newHelper = new NodeTreeHelper(newNode);

                        branchNode.andWith(builder -> builder.then(newHelper.addToNode()));
                    }

                    finalBranchEnds.put(branchKey, newHelper);
                }
            });
        }
        
        return finalBranchEnds;
    }

    private static final class NodeTreeHelper {
        private final ArgumentBuilder<CommandSourceStack, ?> baseNode;
        private CommandAddition commandAddition = builder -> builder;

        public NodeTreeHelper(ArgumentBuilder<CommandSourceStack, ?> baseNode) {
            Objects.requireNonNull(baseNode, () -> "NodeTreeHelper was attempted to be constructed with a null base node");

            this.baseNode = baseNode;
        }

        public ArgumentBuilder<CommandSourceStack, ?> addToNode() {
            return currentFunc().addToBuilder(baseNode);
        }

        public CommandAddition currentFunc() {
            return commandAddition;
        }

        public NodeTreeHelper andWith(CommandAddition func) {
            commandAddition = commandAddition.andWith(func);

            return this;
        }
    }

    //--

    public interface NamedArgumentGetter<T> {
        T getArgument(CommandContext<CommandSourceStack> ctx, String name) throws CommandSyntaxException;
    }

    //--

    public interface ArgumentRegistration {
        <A extends ArgumentType<?>, T> RecordArgumentTypeInfo<A, T> register(ResourceLocation location, Class<A> clazz, RecordArgumentTypeInfo<A, T> info);
    }
}
