package ca.rttv.ae2extensions.mixin;

import ca.rttv.ae2extensions.AE2Extensions;
import ca.rttv.ae2extensions.actions.DevNullTerminalAction;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.TranslatableTextContent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.List;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = "onCloseScreen", at = @At("TAIL"))
    private void onCloseScreen(CloseScreenS2CPacket packet, CallbackInfo ci) {
        AE2Extensions.onCloseScreen();
    }

    @Inject(method = "onScreenHandlerSlotUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/thread/ThreadExecutor;)V", shift = At.Shift.AFTER))
    private void onScreenHandlerSlotUpdateHead(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo ci, @Share("oldScreenHandler") LocalRef<ScreenHandler> oldScreenHandler, @Share("oldScreen") LocalRef<Screen> oldScreen) {
        if (AE2Extensions.isTerminalActive()) {
            oldScreenHandler.set(client.player.currentScreenHandler);
            oldScreen.set(client.currentScreen);
            client.player.currentScreenHandler = AE2Extensions.getTerminalScreenHandler();
            client.currentScreen = AE2Extensions.getTerminalScreen();
        }
    }

    @Inject(method = "onScreenHandlerSlotUpdate", at = @At("RETURN"))
    private void onScreenHandlerSlotUpdateTail(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo ci, @Share("oldScreenHandler") LocalRef<ScreenHandler> oldScreenHandler, @Share("oldScreen") LocalRef<Screen> oldScreen) {
        if (AE2Extensions.isTerminalActive()) {
            client.player.currentScreenHandler = oldScreenHandler.get();
            client.currentScreen = oldScreen.get();
        }
    }

    @Inject(method = "onScreenHandlerSlotUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/thread/ThreadExecutor;)V", shift = At.Shift.AFTER))
    private void setPreviousSlots(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo ci, @Share("previousSlots") LocalRef<List<ItemStack>> previousSlots) {
        if (AE2Extensions.isHotkeyEnabled && AE2Extensions.isDevNullActive && AE2Extensions.actions.stream().noneMatch(action -> action instanceof DevNullTerminalAction)) {
            previousSlots.set(client.player.playerScreenHandler.slots.stream().map(Slot::getStack).toList());
        }
    }

    @Inject(method = "onScreenHandlerSlotUpdate", at = @At("RETURN"))
    private void onScreenHandlerSlotUpdateTail(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo ci, @Share("previousSlots") LocalRef<List<ItemStack>> previousSlots) {
        if (previousSlots.get() != null) {
            AE2Extensions.addTerminalAction(new DevNullTerminalAction(previousSlots.get(), client.player.playerScreenHandler.slots.stream().map(Slot::getStack).toList()));
        }
    }

    @Inject(method = "onGameMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/thread/ThreadExecutor;)V", shift = At.Shift.AFTER))
    private void onGameMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
        if (packet.overlay() && packet.content().getContent() instanceof TranslatableTextContent translate && Arrays.asList(AE2Extensions.FAILED_CONNECTION_TRANSLATES).contains(translate.getKey())) {
            AE2Extensions.lastFailedTerminalAttempt = System.currentTimeMillis();
            AE2Extensions.closeTerminalScreen();
            AE2Extensions.requestingTerminal = false;
        }
    }
}
