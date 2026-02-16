package io.wispforest.accessories.mixin;

import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import org.spongepowered.asm.mixin.gen.Accessor;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.server.commands.EnchantCommand.class)
public interface EnchantCommandAccessor {
    @Accessor("ERROR_NOT_LIVING_ENTITY") static DynamicCommandExceptionType accessories$ERROR_NOT_LIVING_ENTITY() { throw new UnsupportedOperationException(); }
    @Accessor("ERROR_NO_ITEM") static DynamicCommandExceptionType accessories$ERROR_NO_ITEM() { throw new UnsupportedOperationException(); }
    @Accessor("ERROR_INCOMPATIBLE") static DynamicCommandExceptionType accessories$ERROR_INCOMPATIBLE() { throw new UnsupportedOperationException(); }
    @Accessor("ERROR_LEVEL_TOO_HIGH") static Dynamic2CommandExceptionType accessories$ERROR_LEVEL_TOO_HIGH() { throw new UnsupportedOperationException(); }
    @Accessor("ERROR_NOTHING_HAPPENED") static SimpleCommandExceptionType accessories$ERROR_NOTHING_HAPPENED() { throw new UnsupportedOperationException(); }
}
