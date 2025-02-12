package ca.rttv.ae2extensions.actions;

import ca.rttv.ae2extensions.AE2Extensions;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import static ca.rttv.ae2extensions.InteractionHelper.*;

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
    public void execute() {
        final ScreenHandler handler = AE2Extensions.getTerminalScreenHandler();

        for (Pair<Integer, Integer> entry : reductionMap) {
            OptionalInt slotOpt = inventorySlotIdToTerminalSlotId(entry.getLeft());
            if (slotOpt.isEmpty()) continue;
            int slot = slotOpt.getAsInt();
            int reduction = entry.getRight();
            ItemStack stack = handler.getSlot(slot).getStack();
            if (reduction == stack.getCount()) {
                inventorySlotIntoTerminal(slot);
            } else {
                inventorySlotOntoCursorStack(slot, reduction);
                cursorStackIntoTerminal();
            }
        }
    }
}
