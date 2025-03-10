package ca.rttv.ae2extensions.mixin;

import appeng.client.gui.me.common.MEStorageScreen;
import ca.rttv.ae2extensions.AE2Extensions;
import ca.rttv.ae2extensions.actions.PickBlockTerminalAction;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Cancellable;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.Window;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Shadow @Nullable public ClientPlayerEntity player;

    @Shadow @Final private Window window;

    @ModifyVariable(method = "setScreen", at = @At(value = "LOAD", ordinal = 0), index = 1, argsOnly = true)
    private Screen changeScreen(Screen screen) {
        if (screen instanceof MEStorageScreen<?>) {
            if (AE2Extensions.getExtensionsState() == AE2Extensions.ExtensionsState.AWAITING_SCREEN) {
                player.currentScreenHandler = player.playerScreenHandler;
                AE2Extensions.terminalScreen = (HandledScreen<?>) screen;
                AE2Extensions.requestingTerminal = false;
                screen.onDisplayed();
                screen.init((MinecraftClient) (Object) this, window.getScaledWidth(), window.getScaledHeight());
                return null;
            } else {
                AE2Extensions.terminalScreen = null;
                AE2Extensions.requestingTerminal = false;
                AE2Extensions.seenGuiDataSyncPackets = 0;
                AE2Extensions.seenMEInventoryUpdatePackets = 0;
                return screen;
            }
        } else if (screen != null) {
            AE2Extensions.closeTerminalScreen();
            return screen;
        } else {
            return null;
        }
    }

    @ModifyExpressionValue(method = "doItemPick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;getSlotWithStack(Lnet/minecraft/item/ItemStack;)I"))
    private int doItemPick(int slot, @Cancellable CallbackInfo ci, @Local ItemStack targetStack) {
        if (slot == -1 && !player.getAbilities().creativeMode && AE2Extensions.isHotkeyEnabled) {
            AE2Extensions.addTerminalAction(new PickBlockTerminalAction(targetStack));
            ci.cancel();
        }
        return slot;
    }
}
