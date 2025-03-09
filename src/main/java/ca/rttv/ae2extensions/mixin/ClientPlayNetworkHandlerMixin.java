package ca.rttv.ae2extensions.mixin;

import ca.rttv.ae2extensions.AE2Extensions;
import ca.rttv.ae2extensions.actions.DevNullTerminalAction;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
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
    private void onScreenHandlerSlotUpdateHead(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo ci) {
        AE2Extensions.toggleTerminalScreen();
    }

    @Inject(method = "onScreenHandlerSlotUpdate", at = @At("RETURN"))
    private void onScreenHandlerSlotUpdateTail(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo ci) {
        AE2Extensions.toggleTerminalScreen();
    }

    @Inject(method = "onInventory", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/thread/ThreadExecutor;)V", shift = At.Shift.AFTER))
    private void onInventoryHead(InventoryS2CPacket packet, CallbackInfo ci) {
        AE2Extensions.toggleTerminalScreen();
    }

    @Inject(method = "onInventory", at = @At("RETURN"))
    private void onInventoryTail(InventoryS2CPacket packet, CallbackInfo ci) {
        AE2Extensions.toggleTerminalScreen();
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
            AE2Extensions.onTerminalFailed(packet.content());
        }
    }
}
