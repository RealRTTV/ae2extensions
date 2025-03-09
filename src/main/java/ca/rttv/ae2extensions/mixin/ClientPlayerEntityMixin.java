package ca.rttv.ae2extensions.mixin;

import appeng.menu.me.common.GridInventoryEntry;
import ca.rttv.ae2extensions.AE2Extensions;
import ca.rttv.ae2extensions.actions.RestockTerminalAction;
import ca.rttv.ae2extensions.actions.TerminalAction;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {
    @Unique
    private long lastActionExecutionMillis = 0;

    @Inject(method = "tick", at = @At("HEAD"))
    private void tick(CallbackInfo ci) {
        long now = System.currentTimeMillis();

        if (AE2Extensions.isHotkeyEnabled && AE2Extensions.isRestockActive && now - AE2Extensions.lastRestockAttempt > AE2Extensions.RESTOCK_COOLDOWN && AE2Extensions.actions.stream().noneMatch(action -> action instanceof RestockTerminalAction)) {
            AE2Extensions.addTerminalAction(new RestockTerminalAction(AE2Extensions.restockItems.toArray(Item[]::new)));
            AE2Extensions.lastRestockAttempt = now;
        }

        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            System.out.printf("AE2 Extensions State: %s%n", AE2Extensions.getExtensionsState());
        }

        if (!AE2Extensions.actions.isEmpty() && AE2Extensions.getExtensionsState() == AE2Extensions.ExtensionsState.OFF) {
            lastActionExecutionMillis = now;
            AE2Extensions.openTerminal();
        } else if (AE2Extensions.isTerminalActive()) {
            if (!AE2Extensions.actions.isEmpty()) {
                AE2Extensions.toggleTerminalScreen();

                Supplier<List<GridInventoryEntry>> entries = Suppliers.memoize(AE2Extensions::getTerminalEntries);
                TerminalAction action;
                while ((action = AE2Extensions.actions.poll()) != null) {
                    action.execute(AE2Extensions.getTerminalScreen(), AE2Extensions.getTerminalScreenHandler(), entries);
                }

                AE2Extensions.toggleTerminalScreen();

                lastActionExecutionMillis = now;
            }
        }

        if (AE2Extensions.isTerminalActive() && now - lastActionExecutionMillis > AE2Extensions.NO_ACTIONS_IDLE_COOLDOWN) {
            AE2Extensions.closeTerminalScreen();
        }
    }
}
