package ca.rttv.ae2extensions.mixin;

import ca.rttv.ae2extensions.AE2Extensions;
import ca.rttv.ae2extensions.actions.RestockTerminalAction;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Slot.class)
public class SlotMixin {
    @Shadow @Final public Inventory inventory;

    @Shadow public native ItemStack getStack();

    @Shadow public native boolean isEnabled();

    @Inject(method = "markDirty", at = @At("HEAD"))
    private void onMarkDirty(CallbackInfo ci) {
        if (inventory instanceof PlayerInventory && isEnabled()) {
            int countBefore = getStack().getCount();
            getStack().setCount(1);
            Item item = getStack().getItem();
            getStack().setCount(countBefore);

            if (AE2Extensions.isRestockActive && AE2Extensions.restockItems.contains(item)) {
                AE2Extensions.addTerminalAction(new RestockTerminalAction(item));
            }
        }
    }
}
