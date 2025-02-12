package ca.rttv.ae2extensions.actions;

import appeng.api.stacks.AEItemKey;
import appeng.menu.me.common.GridInventoryEntry;
import ca.rttv.ae2extensions.AE2Extensions;
import ca.rttv.ae2extensions.InteractionHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import static ca.rttv.ae2extensions.InteractionHelper.*;

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
                Item item = (Item) (Object) key.getItem();
                if (targetStack.isOf(item)) {
                    InteractionHelper.terminalToCursor(entry);

                    int slot = inventory.getSwappableHotbarSlot();
                    // empty slot in hotbar
                    if (PlayerInventory.isValidHotbarIndex(slot) && inventory.main.get(slot).isEmpty()) {
                        clickSlot(mainToTerminal(slot));
                        selectSlot(mainToTerminal(slot));
                    } else {
                        slot = inventory.getEmptySlot();
                        // empty slot in inventory
                        if (slot != -1 && PlayerInventory.isValidHotbarIndex(inventory.selectedSlot)) {
                            clickSlot(mainToTerminal(slot));
                            swapSlot(mainToTerminal(slot), inventory.selectedSlot);
                        }
                        // no empty slot, swap from selected slot
                        else if (PlayerInventory.isValidHotbarIndex(inventory.selectedSlot)) {
                            slotToTerminal(mainToTerminal(slot));
                            clickSlot(mainToTerminal(slot));
                        } else {
                            AE2Extensions.LOGGER.warn("Could not AE2 pick-block");
                        }
                    }
                }
            }
        }
    }
}
