package top.theillusivec4.curios.platform;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.CuriosConstants;
import top.theillusivec4.curios.platform.services.ICuriosPlatform;

import java.util.ServiceLoader;

public class Services {

  private static final ICuriosPlatform INSTANCE = new ICuriosPlatform() {
    @Override
    public boolean makesPiglinsNeutral(ItemStack stack, LivingEntity livingEntity) {
      return false;
    }

    @Override
    public boolean canWalkOnPowderedSnow(ItemStack stack, LivingEntity livingEntity) {
      return false;
    }

    @Override
    public boolean isEnderMask(ItemStack stack, Player player, EnderMan enderMan) {
      return false;
    }
  };

  public static final ICuriosPlatform CURIOS = load(ICuriosPlatform.class);

  public static <T> T load(Class<T> clazz) {
    return (T) INSTANCE;
  }
}
