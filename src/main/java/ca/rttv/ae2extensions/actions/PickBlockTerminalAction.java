package ca.rttv.ae2extensions.actions;

import appeng.api.stacks.AEItemKey;
import appeng.menu.me.common.GridInventoryEntry;
import ca.rttv.ae2extensions.AE2Extensions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;

import java.util.List;
import java.util.function.Supplier;

import static ca.rttv.ae2extensions.InteractionHelper.clickInventorySlot;
import static ca.rttv.ae2extensions.InteractionHelper.inventoryMainIdToTerminalId;
import static ca.rttv.ae2extensions.InteractionHelper.quickMoveIntoTerminal;
import static ca.rttv.ae2extensions.InteractionHelper.selectHotbarSlot;
import static ca.rttv.ae2extensions.InteractionHelper.swapInventoryAndHotbarSlots;
import static ca.rttv.ae2extensions.InteractionHelper.terminalSlotOntoCursor;

public class PickBlockTerminalAction implements TerminalAction {
    private final ItemStack targetStack;

    public PickBlockTerminalAction(ItemStack targetStack) {
        this.targetStack = targetStack;
    }

    @Override
    public void execute(HandledScreen<?> screen, ScreenHandler handler, Supplier<List<GridInventoryEntry>> entries) {
        final MinecraftClient client = MinecraftClient.getInstance();
        final PlayerInventory inventory = client.player.getInventory();

        for (GridInventoryEntry entry : entries.get()) {
            if (entry.getWhat() instanceof AEItemKey key) {
                Item item = key.getItem();
                if (targetStack.isOf(item)) {
                    terminalSlotOntoCursor(entry, handler);

                    int slotId = inventory.getSwappableHotbarSlot();
                    int terminalSlotId = inventoryMainIdToTerminalId(slotId);
                    // empty slot in hotbar
                    if (PlayerInventory.isValidHotbarIndex(slotId) && inventory.main.get(slotId).isEmpty()) {
                        clickInventorySlot(terminalSlotId, handler);
                        selectHotbarSlot(terminalSlotId);
                    } else {
                        slotId = inventory.getEmptySlot();
                        // empty slot in inventory
                        if (slotId != -1 && PlayerInventory.isValidHotbarIndex(inventory.selectedSlot)) {
                            clickInventorySlot(terminalSlotId, handler);
                            swapInventoryAndHotbarSlots(terminalSlotId, inventory.selectedSlot, handler);
                        }
                        // no empty slot, swap from selected slot
                        else if (PlayerInventory.isValidHotbarIndex(inventory.selectedSlot)) {
                            quickMoveIntoTerminal(terminalSlotId, handler);
                            clickInventorySlot(terminalSlotId, handler);
                        } else {
                            AE2Extensions.LOGGER.warn("Could not AE2 pick-block");
                        }
                    }
                }
            }
        }
    }
}
