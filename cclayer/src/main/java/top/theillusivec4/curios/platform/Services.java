package top.theillusivec4.curios.platform;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.CuriosConstants;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.ISlotType;
import top.theillusivec4.curios.platform.services.ICuriosPlatform;

import java.util.Map;
import java.util.ServiceLoader;

public class Services {

  private static final ICuriosPlatform INSTANCE = new ICuriosPlatform() {
    @Override
    public boolean makesPiglinsNeutral(ItemStack stack, LivingEntity livingEntity) {
      return stack.makesPiglinsNeutral(livingEntity);
    }

    @Override
    public boolean canWalkOnPowderedSnow(ItemStack stack, LivingEntity livingEntity) {
      return stack.canWalkOnPowderedSnow(livingEntity);
    }

    @Override
    public boolean isEnderMask(ItemStack stack, Player player, EnderMan enderMan) {
      return stack.isEnderMask(player, enderMan);
    }

    @Override
    public Map<String, ISlotType> getItemStackSlots(ItemStack stack, @Nullable LivingEntity livingEntity) {
      return livingEntity != null
              ? CuriosApi.getItemStackSlots(stack, livingEntity)
              : CuriosApi.getItemStackSlots(stack, true);
    }
  };

  public static final ICuriosPlatform CURIOS = load(ICuriosPlatform.class);

  public static <T> T load(Class<T> clazz) {
    return (T) INSTANCE;
  }
}
