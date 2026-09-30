package io.github.darkkronicle.advancedchatcore.util;

import com.mojang.blaze3d.platform.cursor.CursorType;
import fi.dy.masa.malilib.render.GuiContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;

@Environment(EnvType.CLIENT)
public class CursorUtil {

    private CursorUtil() {}

    public static void requestCursor(GuiGraphicsExtractor context, CursorType cursor) {
        GuiGraphicsExtractor graphics = context;
        while (graphics instanceof GuiContext guiContext && guiContext.getGuiGraphics() != graphics) {
            graphics = guiContext.getGuiGraphics();
        }
        graphics.requestCursor(cursor);
    }
}
