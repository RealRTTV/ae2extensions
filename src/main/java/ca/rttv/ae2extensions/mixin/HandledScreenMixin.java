package ca.rttv.ae2extensions.mixin;

import appeng.client.gui.me.common.MEStorageScreen;
import appeng.client.gui.me.common.RepoSlot;
import ca.rttv.ae2extensions.InteractionHelper;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
abstract class HandledScreenMixin<T extends ScreenHandler> extends Screen {
    @Shadow @Final protected T handler;

    @Shadow private boolean doubleClicking;

    @Shadow private ItemStack quickMovingStack;

    @Shadow private long lastButtonClickTime;

    @Shadow protected native void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType);

    @Shadow @Nullable protected native Slot getSlotAt(double x, double y);

    protected HandledScreenMixin(Text title) {
        super(title);
    }

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

    @Inject(method = "mouseReleased", at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/ScreenHandler;getCursorStack()Lnet/minecraft/item/ItemStack;", ordinal = 2))
    private void moveAllRepoSlot(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        // @Local didn't work
        Slot slot = getSlotAt(mouseX, mouseY);

        if (doubleClicking && button == 0 && slot instanceof RepoSlot repo && hasShiftDown() && !quickMovingStack.isEmpty()) {
            for (int slotIndex, prevSlotIndex = -1; (slotIndex = InteractionHelper.getSlotToInsertIntoClientMain(repo.getStack())) != -1 || slotIndex != prevSlotIndex; prevSlotIndex = slotIndex) {
                onMouseClick(slot, slot.id, button, SlotActionType.QUICK_MOVE);
            }

            doubleClicking = false;
            lastButtonClickTime = 0L;
        }
    }
}
