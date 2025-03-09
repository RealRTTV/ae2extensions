package ca.rttv.ae2extensions.mixin;

import appeng.items.storage.SpatialStorageCellItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SpatialStorageCellItem.class)
public class SpatialStorageCellItemMixin {
    @Redirect(method = "appendTooltip", at = @At(value = "INVOKE", target = "Lappeng/items/storage/SpatialStorageCellItem;getAllocatedPlotId(Lnet/minecraft/item/ItemStack;)I"))
    private int getAllocatedPlotId(SpatialStorageCellItem instance, ItemStack stack) {
        return stack.getNbt() != null && stack.getNbt().contains("plot_id", NbtElement.INT_TYPE) ? stack.getNbt().getInt("plot_id") : -1;
    }
}
