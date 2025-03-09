package ca.rttv.ae2extensions.actions;

import appeng.menu.me.common.GridInventoryEntry;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Item;
import net.minecraft.screen.ScreenHandler;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static ca.rttv.ae2extensions.InteractionHelper.quickMoveFromTerminal;

public class RestockTerminalAction implements TerminalAction {
    private final Set<Item> items;

    public RestockTerminalAction(Item... items) {
        this.items = Arrays.stream(items).collect(Collectors.toSet());
    }

    public boolean contains(Item item) {
        return items.contains(item);
    }

    @Override
    public void execute(HandledScreen<?> screen, ScreenHandler handler, Supplier<List<GridInventoryEntry>> entries) {
        quickMoveFromTerminal(stack -> items.contains(stack.getItem()), handler, entries);
    }
}
