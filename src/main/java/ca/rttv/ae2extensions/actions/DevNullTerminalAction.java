package ca.rttv.ae2extensions.actions;

import appeng.menu.me.common.GridInventoryEntry;
import ca.rttv.ae2extensions.AE2Extensions;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Supplier;

import static ca.rttv.ae2extensions.InteractionHelper.inventorySlotIdToTerminalSlotId;
import static ca.rttv.ae2extensions.InteractionHelper.moveCursorStackIntoTerminal;
import static ca.rttv.ae2extensions.InteractionHelper.pickupStack;
import static ca.rttv.ae2extensions.InteractionHelper.quickMoveIntoTerminal;

public class DevNullTerminalAction implements TerminalAction {
    private final List<Pair<Integer, Integer>> reductionMap;

    public DevNullTerminalAction(List<ItemStack> previousSlots, List<ItemStack> currentSlots) {
        reductionMap = new ArrayList<>();
        for (int i = 0; i < previousSlots.size(); i++) {
            ItemStack previousStack = previousSlots.get(i);
            ItemStack currentStack = currentSlots.get(i);
            if (AE2Extensions.isDevNullStack(currentStack) && (ItemStack.canCombine(currentStack, previousStack) || previousStack.isEmpty())) {
                int reduction = Math.max(0, currentStack.getCount() - previousStack.getCount());
                reductionMap.add(new Pair<>(i, reduction));
            }
        }
    }

    @Override
    public void execute(HandledScreen<?> screen, ScreenHandler handler, Supplier<List<GridInventoryEntry>> entries) {
        for (Pair<Integer, Integer> entry : reductionMap) {
            OptionalInt slotOpt = inventorySlotIdToTerminalSlotId(entry.getLeft());
            if (slotOpt.isEmpty()) continue;
            int slot = slotOpt.getAsInt();
            int reduction = entry.getRight();
            ItemStack stack = handler.getSlot(slot).getStack();
            if (reduction == stack.getCount()) {
                quickMoveIntoTerminal(slot, handler);
            } else {
                pickupStack(slot, reduction, handler);
                moveCursorStackIntoTerminal(handler);
            }
        }
    }
}
