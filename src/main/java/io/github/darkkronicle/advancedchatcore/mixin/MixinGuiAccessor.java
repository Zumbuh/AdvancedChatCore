package io.github.darkkronicle.advancedchatcore.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin(Gui.class)
public interface MixinGuiAccessor {
    @Accessor("screen")
    void setScreenDirectly(@Nullable Screen screen);
}
