package ca.rttv.ae2extensions.actions;

import appeng.menu.me.common.GridInventoryEntry;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Item;
import net.minecraft.screen.ScreenHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

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
    public void execute(HandledScreen<?> screen, ScreenHandler handler, Supplier<List<GridInventoryEntry>> entries) {
        long now = System.nanoTime();
        if (lastShelveItem != null && lastShelveSlot == slot && now - lastShelve < 500_000_000L) {
            moveAllIntoTerminal(stack -> stack.isOf(lastShelveItem), handler);
            lastShelveItem = null;
            lastShelveSlot = -1;
        } else {
            lastShelveItem = getPlayerInventoryMain().get(slot).getItem();
            lastShelveSlot = slot;
            quickMoveIntoTerminal(inventoryMainIdToTerminalId(slot), handler);
        }
        lastShelve = now;
    }
}
