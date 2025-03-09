package ca.rttv.ae2extensions.actions;

import appeng.menu.me.common.GridInventoryEntry;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.ScreenHandler;

import java.util.List;
import java.util.function.Supplier;

public interface TerminalAction {
    void execute(HandledScreen<?> screen, ScreenHandler handler, Supplier<List<GridInventoryEntry>> entries);
}
