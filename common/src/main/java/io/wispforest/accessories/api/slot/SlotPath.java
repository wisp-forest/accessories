package io.wispforest.accessories.api.slot;

import io.wispforest.accessories.utils.EndecUtils;
import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public sealed interface SlotPath permits SlotPathImpl, DelegatingSlotPath {

    StructEndec<SlotPath> ENDEC = StructEndecBuilder.of(
            Endec.STRING.fieldOf("slot_name", SlotPath::slotName),
            Endec.VAR_INT.fieldOf("index", SlotPath::index),
            EndecUtils.optionalFieldOf(Endec.VAR_INT.listOf(),"inner_indices", SlotPath::innerIndices, List::of, List::isEmpty),
            SlotPath::of
    );

    /**
     * @return the referenced slot name
     */
    String slotName();

    /**
     * @return the referenced slot index
     */
    int index();

    List<Integer> innerIndices();

    boolean isNested();

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
        var parts = path.split("/");

        if (parts.length <= 1) return null;

        var baseSlotName = parts[0].replace("-", ":");
        var index = Integer.parseInt(parts[1]);

        return of(baseSlotName, index);
    }

    default String createString() {
        var baseString = slotName().replace(":", "-") + "/" + index();

        var innerSlotIndices = this.innerIndices();

        if (innerSlotIndices.isEmpty()) return baseString;

        var fullString = new StringBuilder(baseString);

        for (int i = 0; i < innerSlotIndices.size(); i++) {
            fullString.append("/nest_")
                    .append(i)
                    .append("_")
                    .append(innerSlotIndices.get(i));
        }

        return fullString.toString();
    }

    //--

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
}

sealed interface DelegatingSlotPath extends SlotPath permits SlotReference {
    SlotPath slotPath();

    @Override
    default String slotName() {
        return slotPath().slotName();
    }

    @Override
    default int index() {
        return slotPath().index();
    }

    @Override
    default List<Integer> innerIndices() {
        return slotPath().innerIndices();
    }

    @Override
    default boolean isNested() {
        return slotPath().isNested();
    }

    @Override
    default String createString() {
        return slotPath().createString();
    }
}