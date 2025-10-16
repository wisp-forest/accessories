package io.wispforest.accessories.commands.api.base;

import com.google.common.collect.Range;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import io.wispforest.accessories.commands.api.base.Argument.ArgumentBuilderConstructor;
import io.wispforest.accessories.commands.api.base.Argument.ArgumentBuilderConstructorList;
import io.wispforest.accessories.commands.api.base.Argument.ArgumentWithType;
import io.wispforest.accessories.commands.api.core.CommandAddition;
import io.wispforest.accessories.commands.api.core.Key;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public abstract sealed class BaseCommandGenerator<S, B extends CommandTreeBuilder<S, B>> implements CommandTreeBuilder<S, B>, CommandNodeHandler<S> permits CommandGenerator, BranchedCommandGenerator {

    private final Map<String, ArgumentBuilderHolder<S>> baseCommandPart = new LinkedHashMap<>();

    private final Map<Key, ArgumentBuilderHolder<S>> commandParts = new LinkedHashMap<>();

    public final void addToCommandsAndClear(BiConsumer<String, ArgumentBuilder<S, ?>> addCallback) {
        this.baseCommandPart.forEach((string, holder) -> addCallback.accept(string, holder.addToNode()));

        this.baseCommandPart.clear();
        this.commandParts.clear();
    }

    //--

    @Override
    public ArgumentBuilderHolder<S> getOrCreateHolder(List<Argument<?>> args) {
        SequencedMap<Key, ArgumentBuilderConstructor<?>> unpackedArgs = new LinkedHashMap<>();

        var runningKey = new Key();

        for (var arg : args) {
            if (arg instanceof Argument.ArgumentBuilderConstructorList) {
                throw new IllegalStateException("Unable to handle Branch arguments here!");
            }

            var castedArg = (Argument.ArgumentBuilderConstructor<?>) arg;

            runningKey = runningKey.child(castedArg.name());

            unpackedArgs.put(runningKey, castedArg);
        }

        return getNode(unpackedArgs.lastEntry().getKey(), unpackedArgs);
    }

    private ArgumentBuilderHolder<S> getNode(Key key, SequencedMap<Key, ArgumentBuilderConstructor<?>> unpackedArgs) {
        if (this.commandParts.containsKey(key)) return this.commandParts.get(key);

        var arg = unpackedArgs.get(key);

        ArgumentBuilderHolder<S> helper = new ArgumentBuilderHolder<>(this, key, arg.<S>createNodeBuilder());

        if (arg instanceof ArgumentWithType argumentWithType && argumentWithType.suggestions() != null) {
            helper.andWith(builder -> {
                return ((RequiredArgumentBuilder) (builder)).suggests(argumentWithType.suggestions());
            });
        }

        var parentKey = key.parent();

        if (parentKey == null) {
            this.baseCommandPart.put(key.topPath(), helper);
        } else {
            getNode(parentKey, unpackedArgs).andWith(builder -> builder.then(helper.addToNode()));
        }

        this.commandParts.put(key, helper);

        return helper;
    }

    //--

    public B leaves(List<Argument<?>> startingArgs, List<Argument<?>> commandArgs, CommandAddition<S> commandAddition) {
        var rootNodeHelper = getOrCreateHolder(startingArgs);

        // If no args then we can just execute the command addition since nothing is required
        if (commandArgs.isEmpty()) {
            rootNodeHelper.andWith(commandAddition);

            return getThis();
        } else {
            commandArgs = new ArrayList<>(commandArgs);
        }

        SequencedMap<Key, ArgumentBuilderHolder<S>> baseBranchEnds = new LinkedHashMap<>(Map.of(rootNodeHelper.key(), rootNodeHelper));

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
                ArgumentBuilderHolder<S> baseHelper = getOrCreateHolder(key1, (List<Argument<?>>) (Object) requiredArgs);

                if (!optionalArgs.isEmpty()) {
                    var optionalArgStack = new ArrayList<Argument<?>>();

                    for (var optionalArg : optionalArgs) {
                        optionalArgStack.add(optionalArg);

                        baseHelper.getOrCreateChild(optionalArgStack)
                                .andWith(commandAddition);
                    }
                }

                baseHelper.andWith(commandAddition);
            });
        }

        return getThis();
    }

    private SequencedMap<Key, ArgumentBuilderHolder<S>> buildBranchEnds(SequencedMap<Key, ArgumentBuilderHolder<S>> currentBranchEnds, ArgumentBuilderConstructorList argBuilderList, List<ArgumentBuilderConstructor<?>> args) {
        var baseBranchEnds = new LinkedHashMap<Key, ArgumentBuilderHolder<S>>();

        for (var baseBranchEnd : currentBranchEnds.values()) {
            var newBaseBranchEnd = baseBranchEnd.getOrCreateChild((List<Argument<?>>) (Object) args.stream().filter(arg -> !arg.defaulted()).toList());

            baseBranchEnds.put(newBaseBranchEnd.key(), newBaseBranchEnd);
        }

        var finalBranchEnds = new LinkedHashMap<Key, ArgumentBuilderHolder<S>>();

        if (argBuilderList.builders().isEmpty()) {
            finalBranchEnds.putAll(baseBranchEnds);
        } else {
            for (var baseBranchEnd : baseBranchEnds.values()) {
                for (var arg : argBuilderList.builders()) {
                    var newBranchEnd = baseBranchEnd.getOrCreateChild(List.of(arg));

                    finalBranchEnds.put(newBranchEnd.key(), newBranchEnd);
                }
            }
        }
        
        return finalBranchEnds;
    }

}
