package ca.rttv.ae2extensions.mixin;

import appeng.core.sync.packets.GuiDataSyncPacket;
import ca.rttv.ae2extensions.AE2Extensions;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiDataSyncPacket.class)
public class GuiDataSyncPacketMixin {
    @Inject(method = "clientPacketData", at = @At("HEAD"), remap = false)
    private void onClientPacketDataHead(PlayerEntity player, CallbackInfo ci, @Share("oldScreenHandler") LocalRef<ScreenHandler> oldScreenHandler, @Share("oldScreen") LocalRef<Screen> oldScreen) {
        final MinecraftClient client = MinecraftClient.getInstance();
        if (AE2Extensions.isTerminalActive()) {
            oldScreenHandler.set(client.player.currentScreenHandler);
            oldScreen.set(client.currentScreen);
            client.player.currentScreenHandler = AE2Extensions.getTerminalScreenHandler();
            client.currentScreen = AE2Extensions.getTerminalScreen();
        }
    }

    @Inject(method = "clientPacketData", at = @At("RETURN"), remap = false)
    private void onClientPacketDataTail(PlayerEntity player, CallbackInfo ci, @Share("oldScreenHandler") LocalRef<ScreenHandler> oldScreenHandler, @Share("oldScreen") LocalRef<Screen> oldScreen) {
        final MinecraftClient client = MinecraftClient.getInstance();
        if (AE2Extensions.isTerminalActive()) {
            client.player.currentScreenHandler = oldScreenHandler.get();
            client.currentScreen = oldScreen.get();
        }
    }
}
