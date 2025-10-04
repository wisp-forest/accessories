package io.wispforest.accessories.api.slot;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.data.SlotGroupLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

///
/// As Holder object, [SlotGroup] is a group of other [SlotType] by their name and used to cosmetically group
/// them together for any UI.
///
/// Due to the nature that these objects are reloadable from [SlotGroupLoader],
/// it is recommended not to hold onto such values as there inner properties might change.
///
public interface SlotGroup extends Comparable<SlotGroup> {

    ResourceLocation UNKNOWN = Accessories.of("gui/group/unknown");

    ///
    /// @return The group name which may or may not contain a namespace
    ///
    String name();

    ///
    /// @return A parsed group name as either `accessories:{group_name_here}` or `{group_namespace_here}:{group_name_here}`
    ///
    default ResourceLocation getId() {
        return Accessories.parseLocationOrDefault(this.name());
    }

    ///
    /// @return A {@link Component} Translation key for the given group
    ///
    default String translation(){
        return Accessories.translationKey("slot_group." + name().replace(":", "."));
    }

    ///
    /// @return The location for the given icon within the GUI Atlas for the given slot group
    ///
    ResourceLocation icon();

    ///
    /// Used with sorting all registered groups when creating list of slot groups
    ///
    /// @return The priority order for the given group
    ///
    int order();

    ///
    /// @return All slot names found within this group
    ///
    Set<String> slots();

    @Override
    default int compareTo(@NotNull SlotGroup o) {
        var value = Integer.compare(this.order(), o.order());

        if (value != 0) return value;

        return this.name().compareTo(o.name());
    }

    default String dumpData() {
        return "SlotGroup[" +
            "name:'" + name() + '\'' +
            ", order:" + order() +
            ", slots:" + slots() +
            ", icon:" + (icon() == UNKNOWN ? "none" : icon()) +
            ", translation:" + translation() +
            ']';
    }
}
