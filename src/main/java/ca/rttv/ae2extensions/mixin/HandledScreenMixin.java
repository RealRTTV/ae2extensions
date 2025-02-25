package ca.rttv.ae2extensions.mixin;

import appeng.client.gui.me.common.MEStorageScreen;
import appeng.client.gui.me.common.RepoSlot;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(HandledScreen.class)
public class HandledScreenMixin<T extends ScreenHandler> {
    @Shadow @Final protected T handler;

    @WrapOperation(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V", ordinal = 1))
    private void dropRepoSlot(HandledScreen<T> instance, Slot slot, int slotId, int button, SlotActionType actionType, Operation<Void> original) {
        if ((Object) this instanceof MEStorageScreen<?>) {
            if (slot instanceof RepoSlot) {
                if (handler.getCursorStack().isEmpty()) {
                    original.call(instance, slot, slotId, 1 - button, button == 0 ? SlotActionType.QUICK_MOVE : SlotActionType.PICKUP);
                }

                original.call(instance, null, ScreenHandler.EMPTY_SPACE_SLOT_INDEX, 1 - button, SlotActionType.PICKUP);

                return;
            }
        }

        original.call(instance, slot, slotId, button, actionType);
    }
}
