package io.github.darkkronicle.advancedchatcore.mixin;

import io.github.darkkronicle.advancedchatcore.chat.AdvancedChatScreen;
import io.github.darkkronicle.advancedchatcore.chat.AdvancedSleepingChatScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(Gui.class)
public class MixinGui {

    @Inject(method = "openChatScreen", at = @At("HEAD"), cancellable = true)
    private void openChatScreen(ChatComponent.ChatMethod method, CallbackInfo ci) {
        Minecraft.getInstance().gui.setScreen(new AdvancedChatScreen(method.prefix()));
        ci.cancel();
    }

    @Redirect(method = "tick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/ChatComponent;openScreen(Lnet/minecraft/client/gui/components/ChatComponent$ChatMethod;Lnet/minecraft/client/gui/screens/ChatScreen$ChatConstructor;)V"))
    public void openSleepingChatScreen(ChatComponent chat, ChatComponent.ChatMethod method, ChatScreen.ChatConstructor<?> constructor) {
        Minecraft.getInstance().gui.setScreen(new AdvancedSleepingChatScreen());
    }
}
