package io.github.darkkronicle.advancedchatcore.mixin;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.render.GuiContext;
import io.github.darkkronicle.advancedchatcore.util.CursorUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(ButtonGeneric.class)
public class MixinButtonGeneric {

    @Inject(method = "render", at = @At("TAIL"))
    private void handCursor(GuiContext context, int mouseX, int mouseY, boolean selected, CallbackInfo ci) {
        if (((ButtonBase) (Object) this).isMouseOver(mouseX, mouseY)) {
            CursorUtil.requestCursor(context, CursorTypes.POINTING_HAND);
        }
    }
}
