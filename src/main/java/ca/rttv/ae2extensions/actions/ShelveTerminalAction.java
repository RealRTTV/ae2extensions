package ca.rttv.ae2extensions.actions;

import static ca.rttv.ae2extensions.InteractionHelper.inventoryMainIdToTerminalId;
import static ca.rttv.ae2extensions.InteractionHelper.inventorySlotIntoTerminal;

public class ShelveTerminalAction implements TerminalAction {
    private final int slot;

    public ShelveTerminalAction(int slot) {
        this.slot = slot;
    }

    @Override
    public void execute() {
        inventorySlotIntoTerminal(inventoryMainIdToTerminalId(slot));
    }
}
