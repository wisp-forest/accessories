package io.wispforest.tclayer.mixin;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.logging.LogUtils;
import dev.emi.trinkets.compat.WrappingTrinketsUtils;
import io.wispforest.tclayer.TCLayer;
import io.wispforest.tclayer.compat.config.SlotIdRedirect;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagLoader;
import org.apache.commons.lang3.stream.Streams;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.util.BiConsumer;
import org.apache.logging.log4j.util.TriConsumer;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This mixin acts as a method to adjust tags for [Curios -> Trinkets <-> Accessories] specially
 * with another mixin added to handle [Trinkets -> Curios <-> Accessories] within the CCLayer.
 */
@Mixin(value = TagLoader.class, priority = 2000)
public abstract class TagGroupLoaderMixin {

    @Unique
    private static final Logger LOGGER = LogUtils.getLogger();

    @Final
    @Shadow
    private String directory;

    @Inject(method = "load", at = @At("TAIL"), order = 900)
    public void setupShares(ResourceManager resourceManager, CallbackInfoReturnable<Map<ResourceLocation, List<TagLoader.EntryWithSource>>> cir,
                            @Share(namespace = "accessories", value = "curiosToAccessoryCalls") LocalRef<Map<ResourceLocation, Triple<ResourceLocation, List<TagLoader.EntryWithSource>, BiConsumer<ResourceLocation, List<TagLoader.EntryWithSource>>>>> curiosToAccessoryCalls_share,
                            @Share(namespace = "accessories", value = "accessoryToCuriosCalls") LocalRef<Map<ResourceLocation, Triple<ResourceLocation, List<TagLoader.EntryWithSource>, BiConsumer<ResourceLocation, List<TagLoader.EntryWithSource>>>>> accessoryToCuriosCalls_share,
                            @Share(namespace = "accessories", value = "trinketToAccessoryCalls") LocalRef<Map<ResourceLocation, Triple<ResourceLocation, List<TagLoader.EntryWithSource>, BiConsumer<ResourceLocation, List<TagLoader.EntryWithSource>>>>> trinketToAccessoryCalls_share,
                            @Share(namespace = "accessories", value = "accessoryToTrinketCalls") LocalRef<Map<ResourceLocation, Triple<ResourceLocation, List<TagLoader.EntryWithSource>, BiConsumer<ResourceLocation, List<TagLoader.EntryWithSource>>>>> accessoryToTrinketCalls_share) {
        curiosToAccessoryCalls_share.set(new HashMap<>());
        accessoryToCuriosCalls_share.set(new HashMap<>());
        trinketToAccessoryCalls_share.set(new HashMap<>());
        accessoryToTrinketCalls_share.set(new HashMap<>());
    }

    @Inject(method = "load", at = @At("TAIL"))
    public void injectValues(ResourceManager resourceManager, CallbackInfoReturnable<Map<ResourceLocation, List<TagLoader.EntryWithSource>>> cir,
                             @Share(namespace = "accessories", value = "trinketToAccessoryCalls") LocalRef<Map<ResourceLocation, Triple<ResourceLocation, List<TagLoader.EntryWithSource>, BiConsumer<ResourceLocation, List<TagLoader.EntryWithSource>>>>> trinketToAccessoryCalls_share,
                             @Share(namespace = "accessories", value = "accessoryToTrinketCalls") LocalRef<Map<ResourceLocation, Triple<ResourceLocation, List<TagLoader.EntryWithSource>, BiConsumer<ResourceLocation, List<TagLoader.EntryWithSource>>>>> accessoryToTrinketCalls_share) {
        if (!TCLayer.CONFIG.useInjectionMethod() || !Registries.tagsDirPath(BuiltInRegistries.ITEM.key()).equals(directory)) return;

        var map = cir.getReturnValue();

        var redirects = SlotIdRedirect.getBiMap(TCLayer.CONFIG.slotIdRedirects());

        var trinketToAccessoryCalls = trinketToAccessoryCalls_share.get();
        var accessoryToTrinketCalls = accessoryToTrinketCalls_share.get();

        TriConsumer<ResourceLocation, ResourceLocation, List<TagLoader.EntryWithSource>> addCallback = (fromLocation, toLocation, tagEntries) -> {
            LOGGER.warn("Adding Entries from [{}] to [{}]: \n     {}", fromLocation, toLocation, tagEntries);
            map.computeIfAbsent(toLocation, location1 -> new ArrayList<>()).addAll(tagEntries);
        };

        Map.copyOf(map).forEach((location, entries) -> {
            var entriesCopy = new ArrayList<>(entries);

            if(location.getNamespace().equals("trinkets")) {
                var path = location.getPath();

                var parts = path.split("/");

                if (parts.length != 2) return;

                var group = parts[0];
                var slot = parts[1];

                WrappingTrinketsUtils.trinketsToAccessories_SlotEither(Optional.of(group), slot)
                        .ifRight(slotName -> {
                            var accessoryTag = ResourceLocation.fromNamespaceAndPath("accessories", slotName);

                            trinketToAccessoryCalls.put(accessoryTag, Triple.of(location, entriesCopy, (fromLocation, tagEntries) -> addCallback.accept(fromLocation, accessoryTag, tagEntries)));
                        }).ifLeft(slotName -> {
                            var accessoryTag = ResourceLocation.fromNamespaceAndPath("accessories", slotName);

                            trinketToAccessoryCalls.put(accessoryTag, Triple.of(location, entriesCopy, (fromLocation, tagEntries) -> addCallback.accept(fromLocation, accessoryTag, tagEntries)));

                            var redirect = redirects.get(location.getPath());

                            if (redirect != null) {
                                var accessoryRedirectTag = ResourceLocation.fromNamespaceAndPath("accessories", redirect);

                                trinketToAccessoryCalls.put(accessoryRedirectTag, Triple.of(location, entriesCopy, (fromLocation, tagEntries) -> addCallback.accept(fromLocation, accessoryTag, tagEntries)));
                            }
                        });
            } else if(location.getNamespace().equals("accessories")) {
                var possibleGroups = WrappingTrinketsUtils.getGroupFromDefaultSlot(location.getPath());

                if (!possibleGroups.isEmpty()) {
                    for (var group : possibleGroups) {
                        var trinketTag = ResourceLocation.fromNamespaceAndPath("trinkets", group + "/" + WrappingTrinketsUtils.accessoriesToTrinkets_Slot(location.getPath()));

                        accessoryToTrinketCalls.put(location, Triple.of(location, entriesCopy, (fromLocation, tagEntries) -> addCallback.accept(fromLocation, trinketTag, tagEntries)));
                    }
                } else {
                    var groupInfo = WrappingTrinketsUtils.getGroupInfo(location.getPath());

                    if (groupInfo != null) {
                        var trinketTag = ResourceLocation.fromNamespaceAndPath("trinkets", (groupInfo + "/" + WrappingTrinketsUtils.accessoriesToTrinkets_Slot(location.getPath())));

                        accessoryToTrinketCalls.put(location, Triple.of(location, entriesCopy, (fromLocation, tagEntries) -> addCallback.accept(fromLocation, trinketTag, tagEntries)));
                    }

                    var redirect = redirects.inverse().get(location.getPath());

                    if (redirect != null) {
                        var redirectTag = ResourceLocation.fromNamespaceAndPath("trinkets", redirect);

                        accessoryToTrinketCalls.put(location, Triple.of(location, entriesCopy, (fromLocation, tagEntries) -> addCallback.accept(fromLocation, redirectTag, tagEntries)));
                    }
                }
            }
        });
    }

    @Inject(method = "load", at = @At("TAIL"), order = 1100)
    public void handleShares(ResourceManager resourceManager, CallbackInfoReturnable<Map<ResourceLocation, List<TagLoader.EntryWithSource>>> cir,
                            @Share(namespace = "accessories", value = "curiosToAccessoryCalls") LocalRef<Map<ResourceLocation, Triple<ResourceLocation, List<TagLoader.EntryWithSource>, BiConsumer<ResourceLocation, List<TagLoader.EntryWithSource>>>>> curiosToAccessoryCalls_share,
                            @Share(namespace = "accessories", value = "accessoryToCuriosCalls") LocalRef<Map<ResourceLocation, Triple<ResourceLocation, List<TagLoader.EntryWithSource>, BiConsumer<ResourceLocation, List<TagLoader.EntryWithSource>>>>> accessoryToCuriosCalls_share,
                            @Share(namespace = "accessories", value = "trinketToAccessoryCalls") LocalRef<Map<ResourceLocation, Triple<ResourceLocation, List<TagLoader.EntryWithSource>, BiConsumer<ResourceLocation, List<TagLoader.EntryWithSource>>>>> trinketToAccessoryCalls_share,
                            @Share(namespace = "accessories", value = "accessoryToTrinketCalls") LocalRef<Map<ResourceLocation, Triple<ResourceLocation, List<TagLoader.EntryWithSource>, BiConsumer<ResourceLocation, List<TagLoader.EntryWithSource>>>>> accessoryToTrinketCalls_share) {
        if (!TCLayer.CONFIG.useInjectionMethod() || !Registries.tagsDirPath(BuiltInRegistries.ITEM.key()).equals(directory)) return;

        var curiosToAccessoryCalls = curiosToAccessoryCalls_share.get();
        var trinketToAccessoryCalls = trinketToAccessoryCalls_share.get();

        var accessoryToCuriosCalls = accessoryToCuriosCalls_share.get();
        var accessoryToTrinketCalls = accessoryToTrinketCalls_share.get();

        var allLocations = Streams.of(accessoryToCuriosCalls.keySet(), curiosToAccessoryCalls.keySet(), accessoryToTrinketCalls.keySet(), trinketToAccessoryCalls.keySet()).flatMap(Collection::stream).collect(Collectors.toSet());

        for (var accessoryLocation : allLocations) {
            var trinketEntries = trinketToAccessoryCalls.get(accessoryLocation);
            var curiosEntries = curiosToAccessoryCalls.get(accessoryLocation);

            if (trinketEntries != null) {
                accessoryToCuriosCalls.getOrDefault(accessoryLocation, EMPTY_TRIPLE).getRight().accept(trinketEntries.getLeft(), trinketEntries.getMiddle());
                curiosToAccessoryCalls.getOrDefault(accessoryLocation, trinketToAccessoryCalls.getOrDefault(accessoryLocation, EMPTY_TRIPLE)).getRight().accept(trinketEntries.getLeft(), trinketEntries.getMiddle());
            }

            if (curiosEntries != null) {
                accessoryToTrinketCalls.getOrDefault(accessoryLocation, EMPTY_TRIPLE).getRight().accept(curiosEntries.getLeft(), curiosEntries.getMiddle());
                curiosToAccessoryCalls.getOrDefault(accessoryLocation, trinketToAccessoryCalls.getOrDefault(accessoryLocation, EMPTY_TRIPLE)).getRight().accept(curiosEntries.getLeft(), curiosEntries.getMiddle());
            }

            Optional.ofNullable(accessoryToCuriosCalls.get(accessoryLocation)).ifPresent(triple -> triple.getRight().accept(triple.getLeft(), triple.getMiddle()));
            Optional.ofNullable(accessoryToTrinketCalls.get(accessoryLocation)).ifPresent(triple -> triple.getRight().accept(triple.getLeft(), triple.getMiddle()));
        }

        curiosToAccessoryCalls_share.set(new HashMap<>());
        accessoryToCuriosCalls_share.set(new HashMap<>());
        trinketToAccessoryCalls_share.set(new HashMap<>());
        accessoryToTrinketCalls_share.set(new HashMap<>());
    }

    @Unique
    private static final ResourceLocation INVALID_ID = ResourceLocation.fromNamespaceAndPath("invalid", "none");

    @Unique
    private static final Triple<ResourceLocation, List<TagLoader.EntryWithSource>, BiConsumer<ResourceLocation, List<TagLoader.EntryWithSource>>> EMPTY_TRIPLE = Triple.of(INVALID_ID, List.of(), (fromLocation, entryWithSources) -> {});
}
