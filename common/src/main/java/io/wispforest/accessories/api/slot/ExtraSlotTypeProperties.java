package io.wispforest.accessories.api.slot;

import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.StructEndecBuilder;
import org.jetbrains.annotations.ApiStatus;

import java.util.HashMap;
import java.util.Map;

@ApiStatus.Internal
public record ExtraSlotTypeProperties(boolean allowResizing, boolean strictMode, boolean allowEquipFromUse) {

    private static final Map<String, ExtraSlotTypeProperties> EXTRA_PROPS_SERVER = new HashMap<>();
    private static final Map<String, ExtraSlotTypeProperties> EXTRA_PROPS_CLIENT = new HashMap<>();

    @ApiStatus.Internal
    public static ExtraSlotTypeProperties getProperty(String slotType, boolean isClient) {
        return getProperties(isClient).getOrDefault(slotType, ExtraSlotTypeProperties.DEFAULT);
    }

    @ApiStatus.Internal
    public static Map<String, ExtraSlotTypeProperties> getProperties(boolean isClient) {
        return isClient ? EXTRA_PROPS_CLIENT : EXTRA_PROPS_SERVER;
    }

    @ApiStatus.Internal
    public static void setClientPropertyMap(Map<String, ExtraSlotTypeProperties> map) {
        EXTRA_PROPS_CLIENT.clear();
        EXTRA_PROPS_CLIENT.putAll(map);
    }

    public static final ExtraSlotTypeProperties DEFAULT = new ExtraSlotTypeProperties(true, false, true);

    public static final Endec<ExtraSlotTypeProperties> ENDEC = StructEndecBuilder.of(
            Endec.BOOLEAN.fieldOf("allowResizing", ExtraSlotTypeProperties::allowResizing),
            Endec.BOOLEAN.fieldOf("strictMode", ExtraSlotTypeProperties::strictMode),
            Endec.BOOLEAN.fieldOf("allowEquipFromUse", ExtraSlotTypeProperties::allowEquipFromUse),
            ExtraSlotTypeProperties::new
    );
}
