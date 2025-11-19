/*
 * Copyright (C) 2021-2022 DarkKronicle
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.darkkronicle.advancedchatcore.mixin;

import io.github.darkkronicle.advancedchatcore.chat.AdvancedChatScreen;
import io.github.darkkronicle.advancedchatcore.chat.AdvancedSleepingChatScreen;
import io.github.darkkronicle.advancedchatcore.chat.ChatHistory;
import io.github.darkkronicle.advancedchatcore.config.ConfigStorage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {

    @Inject(method = "disconnect", at = @At("RETURN"))
    private void onDisconnect(Text reason, CallbackInfo ci) {
        // Clear data on disconnect
        if (ConfigStorage.General.CLEAR_ON_DISCONNECT.config.getBooleanValue()) {
            ChatHistory.getInstance().clearAll();
        }
    }

    @Inject(method = "openChatScreen", at = @At("HEAD"), cancellable = true)
    private void openChatScreen(ChatHud.ChatMethod method, CallbackInfo ci) {
        MinecraftClient.getInstance().setScreen(new AdvancedChatScreen(""));
        ci.cancel();
    }


    @ModifyArg(method = "tick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/ChatHud;setClientScreen(Lnet/minecraft/client/gui/hud/ChatHud$ChatMethod;Lnet/minecraft/client/gui/screen/ChatScreen$Factory;)V"))
    public ChatHud.ChatMethod openSleepingChatScreen(ChatHud.ChatMethod method) {
        MinecraftClient.getInstance().setScreen(new AdvancedSleepingChatScreen());
        return method;
    }
}
