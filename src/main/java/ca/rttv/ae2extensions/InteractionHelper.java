package ca.rttv.ae2extensions;

import appeng.api.stacks.AEItemKey;
import appeng.core.sync.BasePacketHandler;
import appeng.helpers.InventoryAction;
import appeng.menu.me.common.GridInventoryEntry;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import org.jetbrains.annotations.Nullable;

import java.util.OptionalInt;

public class InteractionHelper {
    /**
     * Simulates clicking a slot with the cursor in the inventory.
     * <p>
     * <u>Will update the client and server</u>
     * @param slot The slot to interact with
     * @return {@code true} if all the following are guaranteed:
     * <ul>
     *     <li>The AE2 Extensions terminal is active</li>
     *     <li>The cursor stack and the slot stack are not empty</li>
     * </ul>
     * <p>
     * {@code false} otherwise
     */
    public static boolean clickInventorySlot(int slot) {
        if (!AE2Extensions.isTerminalActive()) return onTerminalInactive();

        final MinecraftClient client = MinecraftClient.getInstance();
        final ScreenHandler handler = AE2Extensions.getTerminalScreenHandler();

        ItemStack cursorStack = handler.getCursorStack();
        ItemStack slotStack = handler.getSlot(slot).getStack();

        if (cursorStack.isEmpty() && slotStack.isEmpty()) return false;

        client.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(
            handler.syncId,
            handler.getRevision(),
            slot,
            0,
            SlotActionType.PICKUP,
            cursorStack,
            Int2ObjectMaps.emptyMap()
        ));

        if (cursorStack.isEmpty()) {
            handler.setCursorStack(slotStack);
            handler.getSlot(slot).setStack(ItemStack.EMPTY);
        } else {
            if (slotStack.isEmpty()) {
                handler.setCursorStack(ItemStack.EMPTY);
                handler.getSlot(slot).setStack(cursorStack);
            } else {
                if (ItemStack.canCombine(slotStack, cursorStack)) {
                    int slotStackCount = slotStack.getCount();
                    slotStack.setCount(Math.min(slotStack.getMaxCount(), slotStack.getCount() + cursorStack.getCount()));
                    cursorStack.setCount(slotStackCount + cursorStack.getCount() - slotStack.getCount());
                } else {
                    handler.setCursorStack(slotStack);
                    handler.getSlot(slot).setStack(cursorStack);
                }
            }
        }

        return true;
    }

    /**
     * Moves a specified quantity of items from a slot onto the cursor.
     * <p>
     * <u>Will update the client and server</u>
     * @param slot The slot to move from
     * @param quantity The amount to take
     * @return {@code true} if all the following are guaranteed:
     * <ul>
     *     <li>The cursor stack is originally empty</li>
     * </ul>
     * <p>
     * {@code false} otherwise
     */
    public static boolean inventorySlotOntoCursorStack(int slot, int quantity) {
        final MinecraftClient client = MinecraftClient.getInstance();
        final ScreenHandler handler = AE2Extensions.getTerminalScreenHandler();

        if (!handler.getCursorStack().isEmpty()) {
            AE2Extensions.LOGGER.warn("Tried to interact with a filled cursor. ({})", getCallerMethodName());
            return false;
        }

        ItemStack cursorStack = handler.getSlot(slot).getStack().copy();
        int originalCount = cursorStack.getCount();

        if ((cursorStack.getCount() + 1) / 2 >= quantity) {
            // right click to split stack
            client.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(
                handler.syncId,
                handler.getRevision(),
                slot,
                1,
                SlotActionType.PICKUP,
                ItemStack.EMPTY,
                Int2ObjectMaps.emptyMap()
            ));
        } else {
            // left click to take whole stack
            client.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(
                handler.syncId,
                handler.getRevision(),
                slot,
                0,
                SlotActionType.PICKUP,
                ItemStack.EMPTY,
                Int2ObjectMaps.emptyMap()
            ));
        }

        handler.setCursorStack(cursorStack);

        while (cursorStack.getCount() > quantity) {
            // right click to reduce the cursor amount by one
            client.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(
                handler.syncId,
                handler.getRevision(),
                slot,
                1,
                SlotActionType.PICKUP,
                cursorStack,
                Int2ObjectMaps.emptyMap()
            ));
            cursorStack.setCount(cursorStack.getCount() - 1);
        }

        // update the remaining quantity in the slot
        handler.getSlot(slot).getStack().setCount(originalCount - quantity);

        return true;
    }

    /**
     * Swaps a slot in the inventory with a slot in the hotbar
     * <p>
     * <u>Will update the client and server</u>
     * @param slot The slot to swap from
     * @param hotbarSlot The slot to swap to
     * @return {@code true} if all the following are guaranteed:
     * <ul>
     *     <li>The AE2 Extensions terminal is active</li>
     *     <li>{@code hotbarSlot} is a valid hotbar slot</li>
     * </ul>
     * <p>
     * {@code false} otherwise
     */
    public static boolean swapInventoryAndHotbarSlots(int slot, int hotbarSlot) {
        if (!PlayerInventory.isValidHotbarIndex(hotbarSlot)) {
            AE2Extensions.LOGGER.warn("Tried to interact with an invalid hotbar slot. ({})", getCallerMethodName());
            return false;
        }
        if (!AE2Extensions.isTerminalActive()) return onTerminalInactive();

        final MinecraftClient client = MinecraftClient.getInstance();
        final ScreenHandler handler = AE2Extensions.getTerminalScreenHandler();
        final PlayerInventory inventory = client.player.getInventory();

        client.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(handler.syncId, handler.getRevision(), slot + 21, hotbarSlot, SlotActionType.SWAP, handler.getCursorStack(), Int2ObjectMaps.emptyMap()));

        ItemStack stack = inventory.getStack(hotbarSlot);
        inventory.setStack(hotbarSlot, inventory.getStack(slot));
        inventory.setStack(slot, stack);

        return true;
    }

    /**
     * Selects a slot in the hotbar
     * <p>
     * <u>Will update the client and server</u>
     * @param slot The slot to select
     * @return {@code true} if all the following are guaranteed:
     * <ul>
     *     <li>The AE2 Extensions terminal is active</li>
     *     <li>The slot is a valid hotbar slot</li>
     * </ul>
     * <p>
     * {@code false} otherwise
     */
    public static boolean selectHotbarSlot(int slot) {
        final MinecraftClient client = MinecraftClient.getInstance();
        final PlayerInventory inventory = client.player.getInventory();

        if (!PlayerInventory.isValidHotbarIndex(slot)) {
            AE2Extensions.LOGGER.warn("Tried to interact with an invalid hotbar slot. ({})", getCallerMethodName());
            return false;
        }

        if (slot != inventory.selectedSlot) {
            client.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
            inventory.selectedSlot = slot;
        }

        return true;
    }

    /**
     * Moves the cursor stack into the terminal
     * <p>
     * <u>Will update the client and server</u>
     * @return {@code true} if all the following are guaranteed:
     * <ul>
     *     <li>The AE2 Extensions terminal is active</li>
     *     <li>The cursor stack is not empty</li>
     * </ul>
     * <p>
     * {@code false} otherwise
     */
    public static boolean cursorStackIntoTerminal() {
        if (!AE2Extensions.isTerminalActive()) return onTerminalInactive();

        final MinecraftClient client = MinecraftClient.getInstance();
        final ScreenHandler handler = AE2Extensions.getTerminalScreenHandler();

        if (handler.getCursorStack().isEmpty()) {
            AE2Extensions.LOGGER.warn("Tried to interact with an empty cursor. ({})", getCallerMethodName());
            return false;
        }

        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeInt(BasePacketHandler.PacketTypes.ME_INTERACTION.getPacketId());
        buf.writeInt(handler.syncId);
        buf.writeVarLong(-1);
        buf.writeEnumConstant(InventoryAction.PICKUP_OR_SET_DOWN);
        client.getNetworkHandler().sendPacket(new CustomPayloadC2SPacket(new Identifier("ae2:m"), buf));

        handler.setCursorStack(ItemStack.EMPTY);
        return true;
    }


    /**
     * Gets the stack at the specified slot
     * @param slot The slot to get from (inventory ordinal)
     * @return The stack at the slot
     */
    public static ItemStack getStackFromInventorySlot(int slot) {
        final DefaultedList<ItemStack> main = MinecraftClient.getInstance().player.getInventory().main;
        return main.get(slot);
    }

    /**
     * Shift-clicks the slot into the terminal.
     * <p>
     * <u>Will update the client and server</u>
     * @param slot The slot to move into the terminal
     * @return {@code true} if all the following are guaranteed:
     * <ul>
     *     <li>The AE2 Extensions terminal is active</li>
     *     <li>The slot is not empty</li>
     * </ul>
     * <p>
     * {@code false} otherwise
     */
    public static boolean inventorySlotIntoTerminal(int slot) {
        if (!AE2Extensions.isTerminalActive()) return onTerminalInactive();

        final MinecraftClient client = MinecraftClient.getInstance();
        final ScreenHandler handler = AE2Extensions.getTerminalScreenHandler();
        ItemStack stack = handler.getSlot(slot).getStack();

        if (stack.isEmpty()) {
            AE2Extensions.LOGGER.warn("Tried to interact with an empty slot. ({})", getCallerMethodName());
            return false;
        }

        client.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(
            handler.syncId,
            handler.getRevision(),
            slot,
            0,
            SlotActionType.QUICK_MOVE,
            handler.getCursorStack(),
            Int2ObjectMaps.emptyMap()
        ));

        stack.setCount(0);

        return true;
    }

    public static boolean moveAllIntoTerminal(Item item) {
        if (!AE2Extensions.isTerminalActive()) return onTerminalInactive();

        final MinecraftClient client = MinecraftClient.getInstance();
        final DefaultedList<ItemStack> inventoryHandler = client.player.getInventory().main;

        for (int i = 0; i < inventoryHandler.size(); i++) {
            if (inventoryHandler.get(i).isOf(item)) {
                if (!inventorySlotIntoTerminal(inventoryMainIdToTerminalId(i))) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Performs the {@link appeng.helpers.InventoryAction} on a specific serial in the inventory
     * <p>
     * Will <u>not</u> update the client about interaction, that responsibility is up to the caller
     * @param serial The serial of the {@link appeng.api.stacks.AEKey} to interact with
     * @param action The action to perform
     * @return {@code true} if the AE2 Extensions terminal is active; {@code false} otherwise
     */
    public static boolean terminalSlotIntoMisc(long serial, InventoryAction action, Runnable slotUpdateCallback) {
        if (!AE2Extensions.isTerminalActive()) return onTerminalInactive();

        final MinecraftClient client = MinecraftClient.getInstance();
        final ScreenHandler handler = AE2Extensions.getTerminalScreenHandler();

        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeInt(BasePacketHandler.PacketTypes.ME_INTERACTION.getPacketId());
        buf.writeInt(handler.syncId);
        buf.writeVarLong(serial);
        buf.writeEnumConstant(action);
        client.getNetworkHandler().sendPacket(new CustomPayloadC2SPacket(new Identifier("ae2:m"), buf));
        slotUpdateCallback.run();

        return true;
    }

    /**
     * Moves a {@link appeng.menu.me.common.GridInventoryEntry} from the terminal to your cursor stack
     * <p>
     * <u>Will update the client and server</u>
     * @param entry The entry to move
     * @return {@code true} if all the following are guaranteed:
     * <ul>
     *     <li>The AE2 Extensions terminal is active</li>
     *     <li>The cursor stack is empty</li>
     *     <li>The key is an instanceof an {@link appeng.api.stacks.AEItemKey}</li>
     * </ul>
     * <p>
     * {@code false} otherwise
     */
    public static boolean terminalSlotOntoCursor(GridInventoryEntry entry) {
        if (!AE2Extensions.isTerminalActive()) return onTerminalInactive();

        final MinecraftClient client = MinecraftClient.getInstance();
        final ScreenHandler handler = AE2Extensions.getTerminalScreenHandler();

        if (!handler.getCursorStack().isEmpty()) {
            AE2Extensions.LOGGER.warn("Tried to interact with a filled cursor. ({})", getCallerMethodName());
            return false;
        }
        if (!(entry.getWhat() instanceof AEItemKey key)) {
            AE2Extensions.LOGGER.warn("Tried to interact with a non-item AEKey. ({})", getCallerMethodName());
            return false;
        }

        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeInt(BasePacketHandler.PacketTypes.ME_INTERACTION.getPacketId());
        buf.writeInt(handler.syncId);
        buf.writeVarLong(entry.getSerial());
        buf.writeEnumConstant(InventoryAction.PICKUP_OR_SET_DOWN);
        client.getNetworkHandler().sendPacket(new CustomPayloadC2SPacket(new Identifier("ae2:m"), buf));

        handler.setCursorStack(key.toStack((int) Math.min(key.getMaxStackSize(), entry.getStoredAmount())));

        return true;
    }

    public static OptionalInt inventorySlotIdToTerminalSlotId(int slot) {
        if (slot < 5) {
            // crafting
            return OptionalInt.empty();
        }

        if (slot < 5 + 4) {
            // armor
            return OptionalInt.empty();
        }

        if (slot < 5 + 4 + 27) {
            // main inventory
            return OptionalInt.of(slot - (5 + 4) + 30);
        }

        if (slot < 5 + 4 + 27 + 9) {
            // hotbar
            return OptionalInt.of(slot - (5 + 4 + 27) + 21);
        }

        return OptionalInt.empty();
    }

    public static int inventoryMainIdToTerminalId(int slot) {
        return slot + 21;
    }
    
    private static boolean onTerminalInactive() {
        AE2Extensions.LOGGER.warn("Tried to interact with a closed terminal. ({})", getCallerMethodName());
        return false;
    }

    private static String getCallerMethodName() {
        return StackWalker.getInstance().walk(frames -> frames.skip(1).findFirst().map(StackWalker.StackFrame::getMethodName).orElse("null"));
    }
}
