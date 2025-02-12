package ca.rttv.ae2extensions.actions;

import appeng.api.stacks.AEItemKey;
import appeng.menu.me.common.GridInventoryEntry;
import ca.rttv.ae2extensions.AE2Extensions;
import ca.rttv.ae2extensions.InteractionHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import static ca.rttv.ae2extensions.InteractionHelper.clickInventorySlot;
import static ca.rttv.ae2extensions.InteractionHelper.inventoryMainIdToTerminalId;
import static ca.rttv.ae2extensions.InteractionHelper.inventorySlotIntoTerminal;
import static ca.rttv.ae2extensions.InteractionHelper.selectHotbarSlot;
import static ca.rttv.ae2extensions.InteractionHelper.swapInventoryAndHotbarSlots;

public class PickBlockTerminalAction implements TerminalAction {
    private final ItemStack targetStack;

    public PickBlockTerminalAction(ItemStack targetStack) {
        this.targetStack = targetStack;
    }

    @Override
    public void execute() {
        final MinecraftClient client = MinecraftClient.getInstance();
        final PlayerInventory inventory = client.player.getInventory();

        for (GridInventoryEntry entry : AE2Extensions.terminalEntries) {
            if (entry.getWhat() instanceof AEItemKey key) {
                Item item = key.getItem();
                if (targetStack.isOf(item)) {
                    InteractionHelper.terminalSlotOntoCursor(entry);

                    int slotId = inventory.getSwappableHotbarSlot();
                    int terminalSlotId = inventoryMainIdToTerminalId(slotId);
                    // empty slot in hotbar
                    if (PlayerInventory.isValidHotbarIndex(slotId) && inventory.main.get(slotId).isEmpty()) {
                        clickInventorySlot(terminalSlotId);
                        selectHotbarSlot(terminalSlotId);
                    } else {
                        slotId = inventory.getEmptySlot();
                        // empty slot in inventory
                        if (slotId != -1 && PlayerInventory.isValidHotbarIndex(inventory.selectedSlot)) {
                            clickInventorySlot(terminalSlotId);
                            swapInventoryAndHotbarSlots(terminalSlotId, inventory.selectedSlot);
                        }
                        // no empty slot, swap from selected slot
                        else if (PlayerInventory.isValidHotbarIndex(inventory.selectedSlot)) {
                            inventorySlotIntoTerminal(terminalSlotId);
                            clickInventorySlot(terminalSlotId);
                        } else {
                            AE2Extensions.LOGGER.warn("Could not AE2 pick-block");
                        }
                    }
                }
            }
        }
    }
}
