package io.client.discord;

import club.minnced.discord.rpc.DiscordEventHandlers;
import club.minnced.discord.rpc.DiscordRPC;
import club.minnced.discord.rpc.DiscordRichPresence;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

public final class DiscordRpcManager {
    private static final String APPLICATION_ID = System.getProperty("ioClientDiscordAppId", "1473119387337883763");
    private static final long CALLBACK_INTERVAL_MS = 2000L;
    private static volatile boolean running;
    private static Thread callbackThread;
    private static volatile long sessionStartTimestamp;
    private static volatile String lastDetails;
    private static volatile String lastState;

    private DiscordRpcManager() {
    }

    public static void init() {
        if (running)
            return;
        running = true;
        sessionStartTimestamp = System.currentTimeMillis() / 1000L;

        try {
            DiscordRPC rpc = DiscordRPC.INSTANCE;
            DiscordEventHandlers handlers = new DiscordEventHandlers();
            rpc.Discord_Initialize(APPLICATION_ID, handlers, true, null);

            updatePresence(rpc, true);

            callbackThread = new Thread(() -> {
                while (running) {
                    try {
                        rpc.Discord_RunCallbacks();
                        updatePresence(rpc, false);
                        Thread.sleep(CALLBACK_INTERVAL_MS);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Throwable ignored) {
                    }
                }
            }, "io-client-discord-rpc");
            callbackThread.setDaemon(true);
            callbackThread.start();
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Discord RPC native library not available for this platform: " + e.getMessage());
            running = false;
        } catch (Throwable e) {
            System.err.println("Failed to initialize Discord RPC: " + e.getMessage());
            running = false;
        }
    }

    public static void shutdown() {
        if (!running)
            return;
        running = false;
        if (callbackThread != null) {
            callbackThread.interrupt();
            callbackThread = null;
        }
        try {
            sessionStartTimestamp = 0L;
            lastDetails = null;
            lastState = null;
            DiscordRPC.INSTANCE.Discord_Shutdown();
        } catch (Throwable ignored) {
        }
    }

    public static boolean isRunning() {
        return running;
    }

    private static void updatePresence(DiscordRPC rpc, boolean force) {
        String details = "Playing IO Client";
        String state = getCurrentState();

        if (!force && details.equals(lastDetails) && state.equals(lastState)) {
            return;
        }

        DiscordRichPresence presence = new DiscordRichPresence();
        presence.startTimestamp = sessionStartTimestamp;
        presence.details = details;
        presence.state = state;
        presence.largeImageKey = "io";
        presence.largeImageText = "IO Client " + getCurrentVersion();
        rpc.Discord_UpdatePresence(presence);

        lastDetails = details;
        lastState = state;
    }

    private static String getCurrentState() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return "Idling";
        }

        ServerInfo server = client.getCurrentServerEntry();
        if (client.getNetworkHandler() != null && server != null && server.address != null && !server.address.isBlank()) {
            return server.address;
        }

        if (client.world != null) {
            return "In Singleplayer";
        }

        return "Idling";
    }

    private static String getCurrentVersion() {
        try {
            return FabricLoader.getInstance()
                    .getModContainer("io_client")
                    .map(container -> container.getMetadata().getVersion().getFriendlyString())
                    .orElse("unknown");
        } catch (Throwable ignored) {
            return "unknown";
        }
    }
}


