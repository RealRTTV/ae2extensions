package ca.rttv.ae2extensions.actions;

import static ca.rttv.ae2extensions.InteractionHelper.mainToTerminal;
import static ca.rttv.ae2extensions.InteractionHelper.slotToTerminal;

public class ShelveTerminalAction implements TerminalAction {
    private final int slot;

    public ShelveTerminalAction(int slot) {
        this.slot = slot;
    }

    @Override
    public void execute() {
        slotToTerminal(mainToTerminal(slot));
    }
}
