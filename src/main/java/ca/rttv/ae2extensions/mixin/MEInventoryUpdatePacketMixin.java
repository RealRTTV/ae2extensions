package ca.rttv.ae2extensions.mixin;

import appeng.core.sync.packets.MEInventoryUpdatePacket;
import ca.rttv.ae2extensions.AE2Extensions;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MEInventoryUpdatePacket.class)
public class MEInventoryUpdatePacketMixin {
    @Inject(method = "clientPacketData", at = @At("HEAD"), remap = false)
    private void onClientPacketDataHead(PlayerEntity player, CallbackInfo ci) {
        if (AE2Extensions.getExtensionsState() == AE2Extensions.ExtensionsState.AWAITING_TERMINAL_CONTENTS) {
            AE2Extensions.toggleTerminalScreen();
        }
    }

    @Inject(method = "clientPacketData", at = @At("RETURN"), remap = false)
    private void onClientPacketDataTail(PlayerEntity player, CallbackInfo ci) {
        if (AE2Extensions.getExtensionsState() == AE2Extensions.ExtensionsState.AWAITING_TERMINAL_CONTENTS) {
            AE2Extensions.toggleTerminalScreen();
            AE2Extensions.seenMEInventoryUpdatePackets++;
            AE2Extensions.onSeenTerminalDataPacket();
        }
    }
}
