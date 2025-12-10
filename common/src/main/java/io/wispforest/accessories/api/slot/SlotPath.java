package io.wispforest.accessories.api.slot;

import com.mojang.serialization.Codec;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.AccessoriesStorage;
import io.wispforest.accessories.api.core.AccessoryNest;
import io.wispforest.accessories.utils.EndecUtils;
import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.serialization.CodecUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.RegExUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.include.com.google.common.base.Objects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

///
/// A Path Representation for where a given [ItemStack] is equipped within the Accessories API, which may
/// point to a specific spot within a [AccessoriesStorage] and possible [AccessoryNest]s.
///
/// Typically, it is safe to assume that such is valid for any methods that take such as a parameter.
///
/// It is **not recommend** to hold onto such objects due to the fact of [SlotType]'s being reloadable
/// and resizable meaning that such may not be present depending on the amount of time that has elapsed.
///
/// When using this as a key within a [Map] it is recommended that you unpack all [DelegatingSlotPath]'s used
/// and change to a map that allows for custom checks for [equals][#equals] and [hashcode][#hashCode].
///
public sealed interface SlotPath permits SlotPathImpl, DelegatingSlotPath {

    StructEndec<SlotPath> ENDEC = StructEndecBuilder.of(
            Endec.STRING.fieldOf("slot_name", SlotPath::slotName),
            Endec.VAR_INT.fieldOf("index", SlotPath::index),
            EndecUtils.optionalFieldOf(Endec.VAR_INT.listOf(),"inner_indices", SlotPath::innerIndices, List::of, List::isEmpty),
            SlotPath::of
    );

    Codec<SlotPath> CODEC = CodecUtils.toCodec(ENDEC);

    StreamCodec<? extends FriendlyByteBuf, SlotPath> STREAM_CODEC = CodecUtils.toPacketCodec(ENDEC);

    ///
    /// @return the referenced slot name referring to a [SlotType]
    ///
    String slotName();

    ///
    /// @return the referenced slot index for a given [AccessoriesStorage]
    ///
    int index();

    ///
    /// @return the referenced inner nesting indexes for [AccessoryNest] levels
    ///
    List<Integer> innerIndices();

    ///
    /// @return if the given path traverses though any amount of [AccessoryNest]s
    ///
    boolean isNested();

    default SlotPath unpack() {
        return this;
    }

    //--

    static SlotPath of(SlotType slotType, int index) {
        return of(slotType.name(), index);
    }

    static SlotPath of(String name, int index) {
        return new SlotPathImpl(name, index, List.of());
    }

    static SlotPath of(String name, int index, Integer... innerIndices) {
        return of(name, index, List.of(innerIndices));
    }

    static SlotPath of(String name, int index, List<Integer> innerIndices) {
        var finalInnerIndices = Collections.unmodifiableList(innerIndices);

        if (innerIndices.isEmpty()) {
            return of(name, index);
        } else {
            return new SlotPathImpl(name, index, finalInnerIndices);
        }
    }

    static SlotPath withInnerIndex(SlotPath basePath, int innerIndex) {
        var innerSlotIndices = new ArrayList<Integer>();

        if (basePath.isNested()) {
            innerSlotIndices.addAll(basePath.innerIndices());
        }

        innerSlotIndices.add(innerIndex);

        return of(basePath.slotName(), basePath.index(), innerSlotIndices);
    }

    static <S extends SlotPath> S clone(S path) {
        if (path instanceof SlotReference slotReference) {
            return (S) SlotReference.of(slotReference.entity(), slotReference.slotPath());
        }

        return path;
    }

    static <S extends SlotPath> S cloneWithInnerIndex(S basePath, int innerIndex) {
        var newPath = withInnerIndex(basePath, innerIndex);

        if (basePath instanceof SlotReference slotReference) {
            return (S) SlotReference.of(slotReference.entity(), slotReference.slotPath());
        }

        return (S) newPath;
    }

    @Nullable
    static SlotPath fromString(String path) {
        var nameAndParts = path.split("/(?=\\d)");

        if (nameAndParts.length <= 1) return null;

        var baseSlotName = nameAndParts[0].replaceFirst("-(?=([^-]+$))", ":");
        var pathParts = nameAndParts[1].split("/");

        var index = Integer.parseInt(pathParts[0]);

        if (pathParts.length == 1) return of(baseSlotName, index);

        var innerIndices = new ArrayList<Integer>();

        for (int i = 1; i < pathParts.length; i++) {
            var nestPath = pathParts[i];

            innerIndices.add(Integer.parseInt(nestPath.split("_(?=\\d+$)")[1]));
        }

        return of(baseSlotName, index, innerIndices);
    }

    default String createString() {
        var location = toLocation();

        return location.getNamespace().equals(Accessories.MODID)
            ? location.getPath()
            : location.toString().replace(":", "-");
    }

    default ResourceLocation toLocation() {
        var parts = slotName().split(":");

        ResourceLocation location;

        if (parts.length == 1) {
            location = Accessories.of(parts[0]);
        } else {
            location = ResourceLocation.fromNamespaceAndPath(parts[0], slotName().replace(parts[0] + ":", ""));
        }

        location = location.withSuffix("/" + index());

        var innerSlotIndices = this.innerIndices();

        if (!innerSlotIndices.isEmpty()) {
            for (int i = 0; i < innerSlotIndices.size(); i++) {
                location = location.withSuffix("/nest_" + i + "_" + innerSlotIndices.get(i));
            }
        }

        return location;
    }

    //--

    static String createBaseSlotPath(SlotType slotType, int index) {
        return createBaseSlotPath(slotType.name(), index);
    }

    static String createBaseSlotPath(String name, int index) {
        return SlotPath.of(name, index).createString();
    }

    // TODO: FIGURE OUT IF WE ALSO NEED TO HANDLE HASHCODE DIFFERENTLY AS HOLDING A SLOT PATH VS A SLOT REFERENCE ARE DIFFERENT HASHES BUT COULD BE EQUAL
    static boolean areEqual(SlotPath path, SlotPath otherPath) {
        if (path instanceof SlotReference ref && otherPath instanceof SlotReference otherRef) {
            if (ref.entity() != otherRef.entity()) return false;
        }

        return path.slotName().equals(otherPath.slotName())
            && path.index() == otherPath.index()
            && path.innerIndices().equals(otherPath.innerIndices())
            && path.isNested() == otherPath.isNested();
    }

    static int createHashCode(SlotPath path) {
        return Objects.hashCode(path.slotName(), path.index(), path.innerIndices(), path.isNested());
    }
}

@ApiStatus.Internal
record SlotPathImpl(String slotName, int index, List<Integer> innerIndices, boolean isNested) implements SlotPath {
    public SlotPathImpl(String slotName, int index, List<Integer> innerIndices) {
        this(slotName, index, innerIndices, !innerIndices.isEmpty());
    }

    @Override
    @NotNull
    public String toString() {
        return createString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SlotPath otherPath)) return false;

        return SlotPath.areEqual(this, otherPath);
    }
}