package ca.rttv.ae2extensions.mixin;

import ca.rttv.ae2extensions.AE2Extensions;
import ca.rttv.ae2extensions.duck.GameOptionsDuck;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardMixin {
    @Shadow @Final private MinecraftClient client;

    @Inject(method = "onKey", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;getNarratorManager()Lnet/minecraft/client/util/NarratorManager;"))
    private void onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (client.currentScreen == null) {
            if (action != 0 && GameOptionsDuck.getExtensionsKey(client.options).matchesKey(key, scancode)) {
                AE2Extensions.onKeyPressed();
            }

            if (action != 0 && GameOptionsDuck.getDevNullKey(client.options).matchesKey(key, scancode)) {
                AE2Extensions.onDevNullKeyPressed();
            }

            if (action != 0 && GameOptionsDuck.getShelveKey(client.options).matchesKey(key, scancode)) {
                AE2Extensions.onShelveKeyPressed();
            }

            if (action != 0 && GameOptionsDuck.getRestockKey(client.options).matchesKey(key, scancode)) {
                AE2Extensions.onRestockKeyPressed();
            }
        }
    }
}
