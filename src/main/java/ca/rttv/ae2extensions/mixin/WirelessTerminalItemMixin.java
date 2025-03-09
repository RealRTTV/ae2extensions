package ca.rttv.ae2extensions.mixin;

import appeng.items.tools.powered.WirelessTerminalItem;
import ca.rttv.ae2extensions.AE2Extensions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WirelessTerminalItem.class)
public class WirelessTerminalItemMixin {
    @Inject(method = "openFromInventory(Lnet/minecraft/entity/player/PlayerEntity;IZ)Z", at = @At(value = "INVOKE", target = "Lappeng/menu/MenuOpener;open(Lnet/minecraft/screen/ScreenHandlerType;Lnet/minecraft/entity/player/PlayerEntity;Lappeng/menu/locator/MenuLocator;Z)Z"))
    private void onManualOpen(PlayerEntity player, int inventorySlot, boolean returningFromSubmenu, CallbackInfoReturnable<Boolean> cir) {
        AE2Extensions.closeTerminalScreen();
    }

    @Inject(method = "use", at = @At(value = "INVOKE", target = "Lappeng/menu/MenuOpener;open(Lnet/minecraft/screen/ScreenHandlerType;Lnet/minecraft/entity/player/PlayerEntity;Lappeng/menu/locator/MenuLocator;)Z"))
    private void onManualOpen(World level, PlayerEntity player, Hand hand, CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        AE2Extensions.closeTerminalScreen();
    }
}
