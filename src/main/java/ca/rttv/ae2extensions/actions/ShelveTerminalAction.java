package ca.rttv.ae2extensions.actions;

import net.minecraft.item.Item;
import org.jetbrains.annotations.Nullable;

import static ca.rttv.ae2extensions.InteractionHelper.*;

public class ShelveTerminalAction implements TerminalAction {
    private static long lastShelve = -1;
    @Nullable
    private static Item lastShelveItem = null;
    private static int lastShelveSlot = -1;

    private final int slot;

    public ShelveTerminalAction(int slot) {
        this.slot = slot;
    }

    @Override
    public void execute() {
        long now = System.nanoTime();
        if (lastShelveItem != null && lastShelveSlot == slot && now - lastShelve < 500_000_000L) {
            moveAllIntoTerminal(lastShelveItem);
            lastShelveItem = null;
            lastShelveSlot = -1;
        } else {
            lastShelveItem = getStackFromInventorySlot(slot).getItem();
            lastShelveSlot = slot;
            inventorySlotIntoTerminal(inventoryMainIdToTerminalId(slot));
        }
        lastShelve = now;
    }
}
