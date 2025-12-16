package io.wispforest.accessories.data.api;

import io.wispforest.accessories.AccessoriesInternals;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;

import java.util.Set;

public interface IdentifiedResourceReloadListener extends PreparableReloadListener {

    Identifier getId();

    Set<Identifier> getDependencyIds();

    default void registerForType(PackType packType) {
        AccessoriesInternals.INSTANCE.registerLoader(packType, this);
    }
}
