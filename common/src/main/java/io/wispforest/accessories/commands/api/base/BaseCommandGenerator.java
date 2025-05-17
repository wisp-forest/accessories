package io.wispforest.accessories.commands.api.base;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Range;
import com.mojang.brigadier.builder.ArgumentBuilder;
import io.wispforest.accessories.commands.api.core.CommandAddition;
import io.wispforest.accessories.commands.api.core.Key;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import io.wispforest.accessories.commands.api.base.Argument.*;

public abstract sealed class BaseCommandGenerator<S, B extends CommandTreeBuilder<S, B>> implements CommandTreeBuilder<S, B>, CommandNodeHandler<S> permits CommandGenerator, BranchedCommandGenerator {

    private final Map<String, NodeTreeHelper<S>> baseCommandPart = new LinkedHashMap<>();

    private final BiMap<Key, NodeTreeHelper<S>> commandParts = HashBiMap.create();

    public final void addToCommandsAndClear(BiConsumer<String, NodeTreeHelper<S>> addCallback) {
        this.baseCommandPart.forEach(addCallback);

        this.baseCommandPart.clear();
        this.commandParts.clear();
    }

    //--

    @Override
    public void modifyNode(Key key, List<? extends Argument<?>> args, CommandAddition<S> addition) {
        args = new ArrayList<>(args);

        SequencedMap<Key, ArgumentBuilderConstructor<?>> unpackedArgs = new LinkedHashMap<>();

        var branchArgs = key.path().stream()
                .map(Argument.LiteralBranch::asKeyPath)
                .toList();

        args.addAll(0, (List) branchArgs);

        var runningKey = new Key();

        for (var arg : args) {
            if (arg instanceof Argument.ArgumentBuilderConstructorList) {
                throw new IllegalStateException("Unable to handle Branch arguments here!");
            }

            var castedArg = (Argument.ArgumentBuilderConstructor<?>) arg;

            runningKey = runningKey.child(castedArg.name());

            unpackedArgs.put(runningKey, castedArg);
        }

        modifyNode(unpackedArgs.lastEntry().getKey(), unpackedArgs, addition);
    }

    private void modifyNode(Key key, SequencedMap<Key, ArgumentBuilderConstructor<?>> unpackedArgs, CommandAddition<S> addition) {
        if (commandParts.containsKey(key)) {
            commandParts.get(key).andWith(addition);

            return;
        }

        NodeTreeHelper<S> helper = new NodeTreeHelper<>(unpackedArgs.get(key).<S>createNodeBuilder());

        var parentKey = key.parent();

        if (parentKey == null) {
            this.baseCommandPart.put(key.topPath(), helper);
        } else {
            modifyNode(parentKey, unpackedArgs, builder -> builder.then(helper.addToNode()));

            helper.andWith(addition);
        }

        this.commandParts.put(key, helper);
    }

    //--


    @Override
    public B createLeaves(Key key, List<Argument<?>> commandArgs, CommandAddition<S> commandAddition) {
        createLeavesWithKeyAndArgs(key, commandArgs, commandAddition);

        return getThis();
    }

    public void createLeavesWithKeyAndArgs(Key key, List<Argument<?>> commandArgs, CommandAddition<S> commandAddition) {
        this.modifyNode(key, builder -> builder);

        var rootNodeHelper = this.commandParts.get(key);

        // If no args then we can just execute the command addition since nothing is required
        if (commandArgs.isEmpty()) {
            rootNodeHelper.andWith(commandAddition);

            return;
        } else {
            commandArgs = new ArrayList<>(commandArgs);
        }

        SequencedMap<Key, NodeTreeHelper<S>> baseBranchEnds = new LinkedHashMap<>(Map.of(key, rootNodeHelper));

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
                NodeTreeHelper<S> baseHelper = baseBranchEnd;

                for (var requiredArg : requiredArgs) {
                    var newNode = requiredArg.<S>createNodeBuilder();

                    var newKey = key1.child(requiredArg.name());

                    if (this.commandParts.containsKey(newKey)) {
                        baseHelper = this.commandParts.get(newKey);
                    } else {
                        var newHelper = new NodeTreeHelper<S>(newNode);

                        baseHelper.andWith(builder -> builder.then(newHelper.addToNode()));

                        this.commandParts.put(newKey, newHelper);

                        baseHelper = newHelper;
                    }
                }

                if (optionalArgs.isEmpty()) {
                    baseHelper.andWith(commandAddition);
                } else {
                    for (var optionalArg : optionalArgs) {
                        var newNode = optionalArg.<S>createNodeBuilder();

                        var newKey = key1.child(optionalArg.name());

                        if (this.commandParts.containsKey(newKey)) {
                            baseHelper = this.commandParts.get(newKey);
                        } else {
                            var newHelper = new NodeTreeHelper<S>(newNode);

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

    private SequencedMap<Key, NodeTreeHelper<S>> buildBranchEnds(SequencedMap<Key, NodeTreeHelper<S>> currentBranchEnds, ArgumentBuilderConstructorList argBuilderList, List<ArgumentBuilderConstructor<?>> args) {
        SequencedMap<Key, NodeTreeHelper<S>> baseBranchEnds = new LinkedHashMap<>();

        currentBranchEnds.forEach((key, baseBranchEnd) -> {
            for (var arg : args) {
                if (arg.defaulted()) continue;

                key = key.child(arg.name());

                var newNode = arg.<S>createNodeBuilder();

                NodeTreeHelper<S> newHelper;

                if (this.commandParts.containsKey(key)) {
                    baseBranchEnd = this.commandParts.get(key);
                } else {
                    newHelper = new NodeTreeHelper<>(newNode);

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

        SequencedMap<Key, NodeTreeHelper<S>> finalBranchEnds = new LinkedHashMap<>();

        if (argBuilderList.builders().isEmpty()) {
            finalBranchEnds.putAll(baseBranchEnds);
        } else {
            baseBranchEnds.forEach((key, branchNode) -> {
                for (var argBuilderConstructor : argBuilderList.builders()) {
                    var newNode = argBuilderConstructor.<S>createNodeBuilder();

                    var branchKey = key.child(argBuilderConstructor.name());

                    NodeTreeHelper<S> newHelper;

                    if (this.commandParts.containsKey(branchKey)) {
                        newHelper = this.commandParts.get(branchKey);
                    } else {
                        newHelper = new NodeTreeHelper<S>(newNode);

                        branchNode.andWith(builder -> builder.then(newHelper.addToNode()));
                    }

                    finalBranchEnds.put(branchKey, newHelper);
                }
            });
        }
        
        return finalBranchEnds;
    }

    public static final class NodeTreeHelper<S> {
        private final ArgumentBuilder<S, ?> baseNode;
        private CommandAddition<S> commandAddition = builder -> builder;

        public NodeTreeHelper(ArgumentBuilder<S, ?> baseNode) {
            Objects.requireNonNull(baseNode, () -> "NodeTreeHelper was attempted to be constructed with a null base node");

            this.baseNode = baseNode;
        }

        public ArgumentBuilder<S, ?> addToNode() {
            return currentFunc().addToBuilder(baseNode);
        }

        public CommandAddition<S> currentFunc() {
            return commandAddition;
        }

        public NodeTreeHelper<S> andWith(CommandAddition<S> func) {
            commandAddition = commandAddition.andWith(func);

            return this;
        }
    }

    //--

    //--

}
