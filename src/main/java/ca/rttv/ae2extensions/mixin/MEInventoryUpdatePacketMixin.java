package ca.rttv.ae2extensions.mixin;

import appeng.core.sync.packets.MEInventoryUpdatePacket;
import appeng.menu.me.common.GridInventoryEntry;
import ca.rttv.ae2extensions.AE2Extensions;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(MEInventoryUpdatePacket.class)
public class MEInventoryUpdatePacketMixin {
    @Shadow private boolean fullUpdate;

    @Shadow @Final private List<GridInventoryEntry> entries;

    @Inject(method = "clientPacketData", at = @At("HEAD"), remap = false)
    private void onClientPacketDataHead(PlayerEntity player, CallbackInfo ci) {
        if (fullUpdate) {
            AE2Extensions.terminalEntries.clear();
        }

        AE2Extensions.terminalEntries.addAll(entries);
    }
}
