package ca.rttv.ae2extensions;

import appeng.menu.me.common.GridInventoryEntry;
import ca.rttv.ae2extensions.actions.RestockTerminalAction;
import ca.rttv.ae2extensions.actions.ShelveTerminalAction;
import ca.rttv.ae2extensions.actions.TerminalAction;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.mojang.logging.LogUtils;
import io.netty.buffer.Unpooled;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class AE2Extensions {
    public static boolean isHotkeyEnabled = false;
    public static boolean isDevNullActive = false;
    public static boolean isRestockActive = false;
    @Nullable
    public static HandledScreen<?> terminalScreen = null;
    public static List<GridInventoryEntry> terminalEntries = new ArrayList<>();
    public static long terminalScreenPacketTimestamp = 0;
    public static long lastFailedTerminalAttempt = 0;
    public static boolean requestingTerminal = false;
    public static Set<Item> devNullItems = new HashSet<>();
    public static Set<Item> restockItems = new HashSet<>();
    public static long lastRestock = 0;
    public static final Queue<TerminalAction> actions = new ArrayDeque<>();

    public static final File CONFIG_FILE = new File(".", "config/ae2extensions.json");
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String[] FAILED_CONNECTION_TRANSLATES = new String[]{
        "chat.ae2.CommunicationError",
        "chat.ae2.DeviceNotLinked",
        "chat.ae2.DeviceNotPowered",
        "chat.ae2.LinkedNetworkNotFound",
        "chat.ae2.OutOfRange"
    };

    public static void onCloseScreen() {
        terminalScreen = null;
    }

    public static void openTerminal() {
        final MinecraftClient client = MinecraftClient.getInstance();
        long now = System.currentTimeMillis();
        if (terminalScreen != null || requestingTerminal || client.currentScreen != null || now - lastFailedTerminalAttempt < 60_000) return;
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        int terminalIndex = -1;
        DefaultedList<ItemStack> inventory = client.player.getInventory().main;
        for (int i = 0; i < inventory.size(); i++) {
            Identifier id = Registries.ITEM.getId(inventory.get(i).getItem());
            if (id.equals(new Identifier("ae2:wireless_terminal")) || id.equals(new Identifier("ae2:wireless_crafting_terminal"))) {
                terminalIndex = i;
                break;
            }
        }
        if (terminalIndex == -1) return;
        buf.writeInt(terminalIndex + 10);
        buf.writeByte("wireless_terminal".length());
        buf.writeBytes("wireless_terminal".getBytes(StandardCharsets.UTF_8));
        client.getNetworkHandler().sendPacket(new CustomPayloadC2SPacket(new Identifier("ae2:m"), buf));
        requestingTerminal = true;
    }

    public static void onDevNullKeyPressed() {
        final MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            isDevNullActive = !isDevNullActive;
            client.inGameHud.setOverlayMessage(Text.literal("AE2 /dev/null: ").append(Text.literal(isDevNullActive ? "On" : "Off").styled(style -> style.withColor(isDevNullActive ? Formatting.GREEN : Formatting.RED))), false);
        }
    }

    public static void onShelveKeyPressed() {
        final MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && isHotkeyEnabled && PlayerInventory.isValidHotbarIndex(client.player.getInventory().selectedSlot)) {
            AE2Extensions.actions.add(new ShelveTerminalAction(client.player.getInventory().selectedSlot));
        }
    }

    public static void onRestockKeyPressed() {
        final MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            isRestockActive = !isRestockActive;
            client.inGameHud.setOverlayMessage(Text.literal("AE2 Restock: ").append(Text.literal(isRestockActive ? "On" : "Off").styled(style -> style.withColor(isRestockActive ? Formatting.GREEN : Formatting.RED))), false);
            if (isRestockActive) {
                AE2Extensions.actions.add(new RestockTerminalAction(restockItems.toArray(Item[]::new)));
                lastRestock = System.currentTimeMillis();
            }
        }
    }

    public static void onKeyPressed() {
        final MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            isHotkeyEnabled = !isHotkeyEnabled;
            client.inGameHud.setOverlayMessage(Text.literal("AE2 Extensions: ").append(Text.literal(isHotkeyEnabled ? "On" : "Off").styled(style -> style.withColor(isHotkeyEnabled ? Formatting.GREEN : Formatting.RED))), false);
            if (isHotkeyEnabled) {
                File dir = new File(".", "config");
                try {
                    if ((dir.exists() && dir.isDirectory()) || dir.mkdirs() && (!CONFIG_FILE.exists() && CONFIG_FILE.createNewFile())) {
                        FileWriter writer = new FileWriter(CONFIG_FILE);
                        writer.write("""
                            {
                                "valid_items": [
                                    "minecraft:cobblestone",
                                    "minecraft:stone",
                                    "minecraft:dirt",
                                    "minecraft:deepslate",
                                    "minecraft:cobbled_deepslate"
                                ],
                                "restock": [
                                    "minecraft:golden_carrot",
                                    "minecraft:firework_rocket"
                                ]
                            }
                            """);
                        writer.close();
                    }

                    if (CONFIG_FILE.exists() && CONFIG_FILE.isFile() && CONFIG_FILE.canRead()) {
                        FileReader reader = new FileReader(CONFIG_FILE);
                        JsonElement json = JsonParser.parseReader(new JsonReader(reader));
                        reader.close();
                        devNullItems = json.getAsJsonObject()
                            .getAsJsonArray("valid_items")
                            .asList()
                            .stream()
                            .map(element -> Registries.ITEM.get(new Identifier(element.getAsJsonPrimitive().getAsString())))
                            .collect(Collectors.toSet());
                        restockItems = Objects.requireNonNullElseGet(json.getAsJsonObject().getAsJsonArray("restock"), JsonArray::new)
                            .asList()
                            .stream()
                            .map(element -> Registries.ITEM.get(new Identifier(element.getAsJsonPrimitive().getAsString())))
                            .collect(Collectors.toSet());
                    }
                } catch (Exception e) {
                    LOGGER.error("Error parsing config file: ", e);
                }
            } else {
                if (terminalScreen != null) {
                    getTerminalScreen().removed();
                    closeTerminalScreen();
                }

                isDevNullActive = false;
                isRestockActive = false;
                terminalScreenPacketTimestamp = 0;
                lastFailedTerminalAttempt = 0;
                lastRestock = 0;
            }
        }
    }

    @Nullable
    public static ScreenHandler getTerminalScreenHandler() {
        if (terminalScreen == null) {
            return null;
        }

        return terminalScreen.getScreenHandler();
    }

    @Nullable
    public static HandledScreen<?> getTerminalScreen() {
        return terminalScreen;
    }

    public static void closeTerminalScreen() {
        if (terminalScreen != null) {
            MinecraftClient.getInstance().getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(terminalScreen.getScreenHandler().syncId));
            terminalScreen = null;
        }
    }

    public static boolean isTerminalActive() {
        long now = System.currentTimeMillis();

        if (isHotkeyEnabled && terminalScreen != null && now - terminalScreenPacketTimestamp > getScreenPacketCooldown()) {
            AE2Extensions.requestingTerminal = false;
            return true;
        } else {
            return false;
        }
    }

    public static boolean isDevNullStack(ItemStack stack) {
        return devNullItems.contains(stack.getItem());
    }

    public static int getScreenPacketCooldown() {
        final MinecraftClient client = MinecraftClient.getInstance();
        PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
        int ping = entry == null ? 0 : entry.getLatency();
        return 100 + ping;
    }

    public static ExtensionsState getExtensionsState() {
        long now = System.currentTimeMillis();
        if (!isHotkeyEnabled) {
            return ExtensionsState.DISABLED;
        } else if (actions.isEmpty()) {
            return terminalScreen == null ? ExtensionsState.IDLE : ExtensionsState.IDLE_OPEN_SCREEN;
        } else if (requestingTerminal) {
            return ExtensionsState.AWAITING_SCREEN;
        } else if (now - terminalScreenPacketTimestamp <= getScreenPacketCooldown()) {
            return ExtensionsState.AWAITING_OPENING_COOLDOWN;
        } else if (now - lastFailedTerminalAttempt <= 60_000) {
            return ExtensionsState.AWAITING_FAILED_COOLDOWN;
        } else {
            return ExtensionsState.INVALID;
        }
    }

    public enum ExtensionsState {
        INVALID("Invalid State"),
        DISABLED("Disabled"),
        IDLE("Idling"),
        IDLE_OPEN_SCREEN("Idling with an open screen"),
        AWAITING_SCREEN("Awaiting Screen Packet"),
        AWAITING_OPENING_COOLDOWN("Waiting for the Opening Cooldown to be over"),
        AWAITING_FAILED_COOLDOWN("Waiting for the Failed Cooldown to be over");

        private final String displayName;

        ExtensionsState(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}
