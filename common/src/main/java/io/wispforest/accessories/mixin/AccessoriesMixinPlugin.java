package io.wispforest.accessories.mixin;

import io.wispforest.accessories.AccessoriesLoaderInternals;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class AccessoriesMixinPlugin implements IMixinConfigPlugin {

    private static final Logger LOGGER = LogManager.getLogger("Accessories");

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.contains("SodiumImpl")) {
            return AccessoriesLoaderInternals.INSTANCE.isModLoaded("sodium");
        }

        // Allow for the disabling of nbt fixer mixins to by checking if the given file is present
        if (mixinClassName.contains("temp_fixes")) {
            var pathToFile = AccessoriesLoaderInternals.INSTANCE.getConfigPath().resolve("accessories_temp_mixin_disable.txt");

            if (pathToFile.toFile().exists()) {
                LOGGER.warn("[Accessories] Temp Mixin [{}] fixing some NBT data stuff has been disabled just a FYI things may be broken with older world data!", mixinClassName);

                return false;
            }
        }

        return true;
    }

    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return ""; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return List.of(); }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
