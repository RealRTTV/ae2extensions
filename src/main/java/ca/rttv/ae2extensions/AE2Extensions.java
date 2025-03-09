package ca.rttv.ae2extensions;

import appeng.api.features.HotkeyAction;
import appeng.client.gui.me.common.MEStorageScreen;
import appeng.client.gui.me.common.Repo;
import appeng.core.sync.BasePacketHandler;
import appeng.menu.me.common.GridInventoryEntry;
import appeng.menu.me.common.MEStorageMenu;
import ca.rttv.ae2extensions.actions.RestockTerminalAction;
import ca.rttv.ae2extensions.actions.ShelveTerminalAction;
import ca.rttv.ae2extensions.actions.TerminalAction;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.mojang.logging.LogUtils;
import io.netty.buffer.Unpooled;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
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
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.Supplier;
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
    @Nullable
    private static ScreenHandler realScreenHandler = null;
    @Nullable
    private static Screen realScreen = null;
    public static boolean extensionsTerminalVisible = false;
    public static long lastFailedTerminalAttempt = 0;
    public static boolean requestingTerminal = false;
    public static int seenGuiDataSyncPackets = 0;
    public static int seenMEInventoryUpdatePackets = 0;
    public static Set<Item> devNullItems = new HashSet<>();
    public static Set<Item> restockItems = new HashSet<>();
    public static long lastRestockAttempt = 0;
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
        requestingTerminal = false;
        seenGuiDataSyncPackets = 0;
        seenMEInventoryUpdatePackets = 0;
    }

    public static void openTerminal() {
        final MinecraftClient client = MinecraftClient.getInstance();
        long now = System.currentTimeMillis();
        if (getExtensionsState() != ExtensionsState.OFF) return;
        if (client.currentScreen instanceof HandledScreen<?> handledScreen && handledScreen.getScreenHandler() instanceof MEStorageMenu) {
            seenGuiDataSyncPackets = 2;
            seenMEInventoryUpdatePackets = 1;
            terminalScreen = handledScreen;
        } else {
            if (client.currentScreen != null) {
                client.player.closeScreen();
            }
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            DefaultedList<ItemStack> inventory = client.player.getInventory().main;
            a:
            {
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
    }

    public static boolean isTerminalOpen() {
        return terminalScreen != null;
    }

    public static void toggleTerminalScreen() {
        if (!isTerminalOpen() || !extensionsTerminalVisible && isTerminalVisible()) {
            return;
        }

        final MinecraftClient client = MinecraftClient.getInstance();
        final ClientPlayerEntity player = client.player;

        if (extensionsTerminalVisible) {
            client.currentScreen = realScreen;
            player.currentScreenHandler = realScreenHandler;
        } else {
            realScreen = client.currentScreen;
            realScreenHandler = player.currentScreenHandler;
            client.currentScreen = terminalScreen;
            player.currentScreenHandler = terminalScreen.getScreenHandler();
        }

        extensionsTerminalVisible = !extensionsTerminalVisible;
    }

    public static boolean isTerminalVisible() {
        return MinecraftClient.getInstance().currentScreen instanceof MEStorageScreen<?>;
    }

    public static void addTerminalAction(TerminalAction action) {
        lastFailedTerminalAttempt = 0;
        AE2Extensions.actions.add(action);
    }

    public static void onDevNullKeyPressed() {
        final MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            isDevNullActive = !isDevNullActive;
            client.inGameHud.setOverlayMessage(Text.literal("Terminal /dev/null: ").append(Text.literal(isDevNullActive ? "On" : "Off").styled(style -> style.withColor(isDevNullActive ? Formatting.GREEN : Formatting.RED))), false);
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
            client.inGameHud.setOverlayMessage(Text.literal("Terminal Restock: ").append(Text.literal(isRestockActive ? "On" : "Off").styled(style -> style.withColor(isRestockActive ? Formatting.GREEN : Formatting.RED))), false);
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
                    "/dev/null": [
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
            client.inGameHud.setOverlayMessage(Text.literal("Terminal Extensions: ").append(Text.literal(isHotkeyEnabled ? "On" : "Off").styled(style -> style.withColor(isHotkeyEnabled ? Formatting.GREEN : Formatting.RED))), false);
            if (isHotkeyEnabled) {
                refreshConfig();
            } else {
                if (isTerminalOpen()) {
                    getTerminalScreen().removed();
                    closeTerminalScreen();
                }

                isDevNullActive = false;
                isRestockActive = false;
                lastFailedTerminalAttempt = 0;
                lastRestockAttempt = 0;
                seenGuiDataSyncPackets = 0;
                seenMEInventoryUpdatePackets = 0;
                actions.clear();
            }
        }
    }

    @Nullable
    public static ScreenHandler getTerminalScreenHandler() {
        return terminalScreen == null ? null : terminalScreen.getScreenHandler();
    }

    @Nullable
    public static HandledScreen<?> getTerminalScreen() {
        return terminalScreen;
    }

    public static void closeTerminalScreen() {
        if (isTerminalOpen()) {
            MinecraftClient.getInstance().getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(terminalScreen.getScreenHandler().syncId));
            terminalScreen = null;
            extensionsTerminalVisible = false;
            seenGuiDataSyncPackets = 0;
            seenMEInventoryUpdatePackets = 0;
        }
    }

    public static boolean isTerminalActive() {
        return getExtensionsState() == ExtensionsState.ACTIVE;
    }

    public static boolean isDevNullStack(ItemStack stack) {
        return devNullItems.contains(stack.getItem());
    }

    public static void onSeenTerminalDataPacket() {
        requestingTerminal = false;

        if (isTerminalActive() && !((MEStorageMenu) getTerminalScreenHandler()).hasPower) {
            onTerminalFailed(Text.literal("Network is down"));
        }
    }

    public static void onTerminalFailed(Text reason) {
        final MinecraftClient client = MinecraftClient.getInstance();
        final ClientPlayerEntity player = client.player;

        lastFailedTerminalAttempt = System.currentTimeMillis();
        closeTerminalScreen();
        requestingTerminal = false;

        Text message = Text.literal("Failed to open the extensions terminal, reason: ").append(reason);
        player.sendMessage(message, true);
    }

    public static List<GridInventoryEntry> getTerminalEntries() {
        Repo repo = ((Repo) ((MEStorageMenu) AE2Extensions.getTerminalScreenHandler()).getClientRepo());
        try {
            Field viewField = Repo.class.getDeclaredField("view");
            viewField.setAccessible(true);
            return (List<GridInventoryEntry>) viewField.get(repo);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public static ExtensionsState getExtensionsState() {
        long now = System.currentTimeMillis();
        if (!isHotkeyEnabled) {
            return ExtensionsState.DISABLED;
        } else if (!isTerminalOpen() && !requestingTerminal) {
            return ExtensionsState.OFF;
        } else if (requestingTerminal) {
            return ExtensionsState.AWAITING_SCREEN;
        } else if (!(seenGuiDataSyncPackets >= 2 && seenMEInventoryUpdatePackets >= 1)) {
            return ExtensionsState.AWAITING_TERMINAL_CONTENTS;
        } else if (now - lastFailedTerminalAttempt <= FAILED_TERMINAL_REQUEST_COOLDOWN) {
            return ExtensionsState.AWAITING_FAILED_COOLDOWN;
        } else {
            return ExtensionsState.ACTIVE;
        }
    }

    public enum ExtensionsState {
        DISABLED("Disabled"),
        OFF("Off"),
        AWAITING_SCREEN("Awaiting Screen Packet"),
        AWAITING_TERMINAL_CONTENTS(() -> String.format("Awaiting Terminal Contents Packets (%d/2 GUI Data, %d/1 ME Inventory)", seenGuiDataSyncPackets, seenMEInventoryUpdatePackets)),
        AWAITING_FAILED_COOLDOWN(() -> String.format("Awaiting Failed Terminal Cooldown to be over (%ds left)", (FAILED_TERMINAL_REQUEST_COOLDOWN - (System.currentTimeMillis() - lastFailedTerminalAttempt)) / 1_000)),
        ACTIVE("Extensions Terminal is active");

        private final Supplier<String> displayNameSupplier;

        ExtensionsState(String displayName) {
            this.displayNameSupplier = () -> displayName;
        }

        ExtensionsState(Supplier<String> displayNameSupplier) {
            this.displayNameSupplier = displayNameSupplier;
        }

        @Override
        public String toString() {
            return displayNameSupplier.get();
        }
    }
}
