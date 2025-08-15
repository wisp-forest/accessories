package io.wispforest.accessories.impl.option;

import io.wispforest.accessories.impl.PlayerEquipControl;
import io.wispforest.endec.Endec;

import java.util.Set;

public class PlayerOptions {

    public static PlayerOption<PlayerEquipControl> EQUIP_CONTROL = new PlayerOption<>("equip_control", Endec.forEnum(PlayerEquipControl.class), PlayerEquipControl.MUST_NOT_CROUCH);
    public static PlayerOption<Boolean> SHOW_UNUSED_SLOTS = new PlayerOption<>("unused_slots", Endec.BOOLEAN, false);
    public static PlayerOption<Boolean> SHOW_COSMETIC_SLOTS = new PlayerOption<>("cosmetic", Endec.BOOLEAN, false);

    public static PlayerOption<Boolean> SIDE_BY_SIDE_SLOTS = new PlayerOption<>("side_by_side_slots", Endec.BOOLEAN, false);
    public static PlayerOption<Integer> COLUMN_AMOUNT = new PlayerOption<>("column_amount", Endec.VAR_INT, 2);
    public static PlayerOption<Integer> WIDGET_TYPE = new PlayerOption<>("widget_type", Endec.VAR_INT, 2);

    public static PlayerOption<Boolean> SHOW_GROUP_FILTER = new PlayerOption<>("group_filter", Endec.BOOLEAN, true);
    public static PlayerOption<Set<String>> FILTERED_GROUPS = new PlayerOption<>("filtered_groups", Endec.STRING.setOf(), Set.of());

    public static PlayerOption<Boolean> ENTITY_CENTERED = new PlayerOption<>("entity_centered", Endec.BOOLEAN, true);
    public static PlayerOption<Boolean> SIDE_BY_SIDE_ENTITY = new PlayerOption<>("side_by_side_entity", Endec.BOOLEAN, false);
    public static PlayerOption<Boolean> MAIN_WIDGET_POSITION = new PlayerOption<>("main_widget_position", Endec.BOOLEAN, true);
    public static PlayerOption<Boolean> SIDE_WIDGET_POSITION = new PlayerOption<>("side_widget_position", Endec.BOOLEAN, false);

    public static PlayerOption<Boolean> SHOW_CRAFTING_GRID = new PlayerOption<>("show_crafting_grid", Endec.BOOLEAN, false);
    public static PlayerOption<Boolean> ADVANCED_SETTINGS = new PlayerOption<>("advanced_settings", Endec.BOOLEAN, false);
}
