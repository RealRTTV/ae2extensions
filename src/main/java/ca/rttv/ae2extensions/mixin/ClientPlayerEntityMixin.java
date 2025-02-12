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
    private void tick(CallbackInfo ci, @Share("lastActionExecutionMillis") LocalLongRef lastActionExecutionMillis) {
        long now = System.currentTimeMillis();

        if (AE2Extensions.isHotkeyEnabled && AE2Extensions.isRestockActive && now - AE2Extensions.lastRestockAttempt > AE2Extensions.RESTOCK_COOLDOWN && AE2Extensions.actions.stream().noneMatch(action -> action instanceof RestockTerminalAction)) {
            AE2Extensions.addTerminalAction(new RestockTerminalAction(AE2Extensions.restockItems.toArray(Item[]::new)));
            AE2Extensions.lastRestockAttempt = now;
        }

        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            System.out.printf("AE2 Extensions State: %s%n", AE2Extensions.getExtensionsState());
        }

        if (!AE2Extensions.actions.isEmpty() && AE2Extensions.isHotkeyEnabled && !AE2Extensions.isTerminalOpen()) {
            AE2Extensions.openTerminal();
            lastActionExecutionMillis.set(now);
        } else if (AE2Extensions.isTerminalActive()) {
            if (!AE2Extensions.actions.isEmpty()) {
                lastActionExecutionMillis.set(now);
            }

            TerminalAction action;
            while ((action = AE2Extensions.actions.poll()) != null) {
                action.execute();
            }
        }

        if (AE2Extensions.isTerminalActive() && now - lastActionExecutionMillis.get() > AE2Extensions.NO_ACTIONS_IDLE_COOLDOWN) {
            AE2Extensions.closeTerminalScreen();
        }
    }
}
