package ca.rttv.ae2extensions;

import appeng.api.features.HotkeyAction;
import appeng.core.sync.BasePacketHandler;
import appeng.menu.me.common.GridInventoryEntry;
import ca.rttv.ae2extensions.actions.RestockTerminalAction;
import ca.rttv.ae2extensions.actions.ShelveTerminalAction;
import ca.rttv.ae2extensions.actions.TerminalAction;
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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

public class AE2Extensions {
    public static final long FAILED_TERMINAL_REQUEST_COOLDOWN = 30_000;
    public static final long RESTOCK_COOLDOWN = 30_000;
    public static final long NO_ACTIONS_IDLE_COOLDOWN = 15_000;

    public static boolean isHotkeyEnabled = false;
    public static boolean isDevNullActive = false;
    public static boolean isRestockActive = false;
    @Nullable
    public static HandledScreen<?> terminalScreen = null;
    public static long terminalScreenPacketTimestamp = 0;
    public static long lastFailedTerminalAttempt = 0;
    public static boolean requestingTerminal = false;
    public static Set<Item> devNullItems = new HashSet<>();
    public static Set<Item> restockItems = new HashSet<>();
    public static long lastRestockAttempt = 0;
    public static final List<GridInventoryEntry> terminalEntries = new ArrayList<>();
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
        if (isTerminalOpen() || requestingTerminal || client.currentScreen != null || now - lastFailedTerminalAttempt < FAILED_TERMINAL_REQUEST_COOLDOWN) return;
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        DefaultedList<ItemStack> inventory = client.player.getInventory().main;
        a: {
            for (ItemStack stack : inventory) {
                Identifier id = Registries.ITEM.getId(stack.getItem());
                if (id.equals(new Identifier("ae2:wireless_terminal")) || id.equals(new Identifier("ae2:wireless_crafting_terminal"))) {
                    break a;
                }
            }
            lastFailedTerminalAttempt = now;
            return;
        }
        buf.writeInt(BasePacketHandler.PacketTypes.HOTKEY.getPacketId());
        buf.writeVarInt(HotkeyAction.WIRELESS_TERMINAL.length());
        buf.writeBytes(HotkeyAction.WIRELESS_TERMINAL.getBytes(StandardCharsets.UTF_8));
        client.getNetworkHandler().sendPacket(new CustomPayloadC2SPacket(new Identifier("ae2:m"), buf));
        requestingTerminal = true;
    }

    public static boolean isTerminalOpen() {
        return terminalScreen != null;
    }

    public static void addTerminalAction(TerminalAction action) {
        lastFailedTerminalAttempt = 0;
        AE2Extensions.actions.add(action);
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
            addTerminalAction(new ShelveTerminalAction(client.player.getInventory().selectedSlot));
        }
    }

    public static void onRestockKeyPressed() {
        final MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            isRestockActive = !isRestockActive;
            client.inGameHud.setOverlayMessage(Text.literal("AE2 Restock: ").append(Text.literal(isRestockActive ? "On" : "Off").styled(style -> style.withColor(isRestockActive ? Formatting.GREEN : Formatting.RED))), false);
            if (isRestockActive) {
                addTerminalAction(new RestockTerminalAction(restockItems.toArray(Item[]::new)));
                lastRestockAttempt = System.currentTimeMillis();
            }
        }
    }

    private static void writeDefaultConfig() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            writer.write("""
                {
                    "/dev/null/": [
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
        } catch (IOException e) {
            LOGGER.error("Error writing config file: ", e);
        }
    }

    public static void refreshConfig() {
        File dir = new File(".", "config");

        while (true) {
            try {
                if ((dir.exists() && dir.isDirectory() || dir.mkdirs()) && (!CONFIG_FILE.exists() && CONFIG_FILE.createNewFile())) {
                    writeDefaultConfig();
                }

                FileReader reader = new FileReader(CONFIG_FILE);
                JsonElement json = JsonParser.parseReader(new JsonReader(reader));
                reader.close();
                devNullItems = json.getAsJsonObject()
                    .getAsJsonArray("/dev/null")
                    .asList()
                    .stream()
                    .map(element -> Registries.ITEM.get(new Identifier(element.getAsJsonPrimitive().getAsString())))
                    .collect(Collectors.toSet());
                restockItems = json.getAsJsonObject().getAsJsonArray("restock")
                    .asList()
                    .stream()
                    .map(element -> Registries.ITEM.get(new Identifier(element.getAsJsonPrimitive().getAsString())))
                    .collect(Collectors.toSet());

                break;
            } catch (Exception e) {
                LOGGER.error("Error parsing config file: ", e);
                writeDefaultConfig();
            }
        }
    }

    public static void onKeyPressed() {
        final MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            isHotkeyEnabled = !isHotkeyEnabled;
            client.inGameHud.setOverlayMessage(Text.literal("AE2 Extensions: ").append(Text.literal(isHotkeyEnabled ? "On" : "Off").styled(style -> style.withColor(isHotkeyEnabled ? Formatting.GREEN : Formatting.RED))), false);
            if (isHotkeyEnabled) {
                refreshConfig();
            } else {
                if (isTerminalOpen()) {
                    getTerminalScreen().removed();
                    closeTerminalScreen();
                }

                isDevNullActive = false;
                isRestockActive = false;
                terminalScreenPacketTimestamp = 0;
                lastFailedTerminalAttempt = 0;
                lastRestockAttempt = 0;
                actions.clear();
            }
        }
    }

    @Nullable
    public static ScreenHandler getTerminalScreenHandler() {
        if (!isTerminalOpen()) {
            return null;
        }

        return terminalScreen.getScreenHandler();
    }

    @Nullable
    public static HandledScreen<?> getTerminalScreen() {
        return terminalScreen;
    }

    public static void closeTerminalScreen() {
        if (isTerminalOpen()) {
            MinecraftClient.getInstance().getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(terminalScreen.getScreenHandler().syncId));
            terminalScreen = null;
        }
    }

    public static boolean isTerminalActive() {
        long now = System.currentTimeMillis();

        if (isHotkeyEnabled && isTerminalOpen() && now - terminalScreenPacketTimestamp > getScreenPacketCooldown()) {
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
            return isTerminalOpen() ? ExtensionsState.IDLE_OPEN_SCREEN : ExtensionsState.IDLE;
        } else if (requestingTerminal) {
            return ExtensionsState.AWAITING_SCREEN;
        } else if (now - terminalScreenPacketTimestamp <= getScreenPacketCooldown()) {
            return ExtensionsState.AWAITING_OPENING_COOLDOWN;
        } else if (now - lastFailedTerminalAttempt <= FAILED_TERMINAL_REQUEST_COOLDOWN) {
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
