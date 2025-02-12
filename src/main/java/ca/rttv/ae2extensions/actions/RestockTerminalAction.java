package ca.rttv.ae2extensions.actions;

import appeng.api.stacks.AEItemKey;
import appeng.helpers.InventoryAction;
import appeng.menu.me.common.GridInventoryEntry;
import ca.rttv.ae2extensions.AE2Extensions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

import java.util.Arrays;

import static ca.rttv.ae2extensions.InteractionHelper.*;

public class RestockTerminalAction implements TerminalAction {
    private final Item[] items;

    public RestockTerminalAction(Item... items) {
        this.items = items;
    }

    public boolean contains(Item item) {
        for (Item entry : items) {
            if (entry == item) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void execute() {
        final MinecraftClient client = MinecraftClient.getInstance();
        final PlayerInventory inventory = client.player.getInventory();

        for (DefaultedList<ItemStack> list : new DefaultedList[]{ inventory.main, inventory.offHand, inventory.armor }) {
            for (ItemStack stack : list) {
                if (Arrays.asList(items).contains(stack.getItem())) {
                    for (GridInventoryEntry entry : AE2Extensions.terminalEntries) {
                        if (entry.getWhat() instanceof AEItemKey key && ItemStack.canCombine(key.toStack(), stack) && stack.getCount() < stack.getMaxCount()) {
                            // it doesn't matter that we're setting this stack instead of the first applicable stack, since all stacks are covered, we'll be setting them all anyway, the order doesn't matter.
                            terminalSlotIntoMisc(entry.getSerial(), InventoryAction.SHIFT_CLICK, () -> stack.setCount((int) Math.min(stack.getMaxCount(), stack.getCount() + entry.getStoredAmount())));
                        }
                    }
                }
            }
        }
    }
}
