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
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Shadow @Nullable public ClientPlayerEntity player;

    @Shadow @Nullable public Screen currentScreen;

    @Shadow public native void setScreen(@Nullable Screen screen);

    @Inject(method = "setScreen", at = @At("TAIL"))
    private void setScreen(Screen screen, CallbackInfo ci) {
        long now = System.currentTimeMillis();
        if (screen instanceof MEStorageScreen<?> && AE2Extensions.isHotkeyEnabled && !AE2Extensions.isTerminalOpen() && AE2Extensions.requestingTerminal) {
            player.currentScreenHandler = player.playerScreenHandler;
            AE2Extensions.terminalScreen = (HandledScreen<?>) screen;
            AE2Extensions.terminalScreenPacketTimestamp = now;
            currentScreen = null;
            setScreen(null);
        } else if (screen != null) {
            AE2Extensions.closeTerminalScreen();
        }
    }

    @ModifyExpressionValue(method = "doItemPick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;getSlotWithStack(Lnet/minecraft/item/ItemStack;)I"))
    private int doItemPick(int slot, @Cancellable CallbackInfo ci, @Local ItemStack targetStack) {
        if (slot == -1 && !player.getAbilities().creativeMode && AE2Extensions.isHotkeyEnabled) {
            ci.cancel();
            AE2Extensions.addTerminalAction(new PickBlockTerminalAction(targetStack));
        }
        return slot;
    }
}
