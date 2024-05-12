package top.theillusivec4.curios.compat;

import io.wispforest.accessories.api.slot.SlotType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.type.ISlotType;
import top.theillusivec4.curios.api.type.capability.ICurio;

import java.util.Set;

public class WrappedSlotType implements ISlotType {

    private final SlotType slotType;

    public WrappedSlotType(SlotType slotType){
        this.slotType = slotType;
    }

    //--

    @Override
    public String getIdentifier() {
        return this.slotType.name();
    }

    @Override
    public ResourceLocation getIcon() {
        return this.slotType.icon();
    }

    @Override
    public int getOrder() {
        return this.slotType.order();
    }

    @Override
    public int getSize() {
        return this.slotType.amount();
    }

    @Override
    public boolean useNativeGui() {
        return false;
    }

    @Override
    public boolean hasCosmetic() {
        return true;
    }

    @Override
    public boolean canToggleRendering() {
        return true;
    }

    @Override
    public ICurio.DropRule getDropRule() {
        return CuriosWrappingUtils.convert(this.slotType.dropRule());
    }

    @Override
    public Set<ResourceLocation> getValidators() {
        return this.slotType.validators();
    }

    @Override
    public int compareTo(@NotNull ISlotType otherType) {
        if (this.getOrder() == otherType.getOrder()) {
            return this.getIdentifier().compareTo(otherType.getIdentifier());
        } else if (this.getOrder() > otherType.getOrder()) {
            return 1;
        } else {
            return -1;
        }
    }
}
