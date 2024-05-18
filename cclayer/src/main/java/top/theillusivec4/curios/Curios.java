package top.theillusivec4.curios;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.InterModProcessEvent;
import top.theillusivec4.curios.api.SlotTypeMessage;
import top.theillusivec4.curios.common.slottype.LegacySlotManager;

@Mod(Curios.MODID)
public class Curios {
    public static final String MODID = CuriosConstants.MOD_ID;

    public Curios(IEventBus eventBus) {
        eventBus.addListener(this::process);
    }

    private void process(InterModProcessEvent evt) {
        LegacySlotManager.buildImcSlotTypes(
                evt.getIMCStream(SlotTypeMessage.REGISTER_TYPE::equals),
                evt.getIMCStream(SlotTypeMessage.MODIFY_TYPE::equals));
    }
}
