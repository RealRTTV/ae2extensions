package ca.rttv.ae2extensions.mixin;

import ca.rttv.ae2extensions.AE2Extensions;
import ca.rttv.ae2extensions.actions.RestockTerminalAction;
import ca.rttv.ae2extensions.actions.TerminalAction;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalLongRef;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void tick(CallbackInfo ci, @Share("lastActionExecution") LocalLongRef lastActionExecution) {
        long now = System.currentTimeMillis();

        if (AE2Extensions.isHotkeyEnabled && AE2Extensions.isRestockActive && now - AE2Extensions.lastRestock > 60_000 && AE2Extensions.actions.stream().noneMatch(action -> action instanceof RestockTerminalAction)) {
            AE2Extensions.actions.add(new RestockTerminalAction(AE2Extensions.restockItems.toArray(Item[]::new)));
            AE2Extensions.lastRestock = now;
        }

        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            System.out.printf("AE2 Extensions State: %s%n", AE2Extensions.getExtensionsState());
        }

        if (!AE2Extensions.actions.isEmpty() && AE2Extensions.isHotkeyEnabled && AE2Extensions.terminalScreen == null) {
            AE2Extensions.openTerminal();
            lastActionExecution.set(now);
        } else if (AE2Extensions.isTerminalActive()) {
            if (!AE2Extensions.actions.isEmpty()) {
                lastActionExecution.set(now);
            }

            TerminalAction action;
            while ((action = AE2Extensions.actions.poll()) != null) {
                action.execute();
            }
        }

        if (AE2Extensions.isTerminalActive() && now - lastActionExecution.get() > 500) {
            AE2Extensions.closeTerminalScreen();
        }
    }
}
