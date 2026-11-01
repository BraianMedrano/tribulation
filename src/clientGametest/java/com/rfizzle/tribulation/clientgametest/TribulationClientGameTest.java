package com.rfizzle.tribulation.clientgametest;

import com.rfizzle.tribulation.Tribulation;
import com.rfizzle.tribulation.client.ClientConfigState;
import com.rfizzle.tribulation.client.ClientTribulationState;
import com.rfizzle.tribulation.client.TribulationClient;
import com.rfizzle.tribulation.client.TribulationHudOverlay;
import com.rfizzle.tribulation.config.TribulationConfig;
import com.rfizzle.tribulation.data.PlayerDifficultyState;
import com.rfizzle.tribulation.event.HardcoreHeartsHandler;
import com.rfizzle.tribulation.item.TribulationItems;
import com.rfizzle.tribulation.scaling.TierManager;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.server.level.ServerPlayer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.UUID;

/**
 * End-to-end client check for the 26.3 port: a real client joins a real dedicated server, so
 * every payload crosses the network codec, and the HUD, the peek panel and item use are
 * observed on the rendered frame rather than inferred from state.
 */
public class TribulationClientGameTest implements FabricClientGameTest {
    private static final int TIER_FIVE_LEVEL = 250;
    private static final int HEARTS_LOST = 4;
    private static final int SETTLE_TICKS = 60;

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestDedicatedServerContext server = context.worldBuilder().createServer();
             TestDedicatedServerConnection connection = server.connect()) {
            connection.waitForChunksRender();
            String name = context.computeOnClient(mc -> mc.getUser().getName());
            UUID uuid = server.computeOnServer(s -> connection.getServerPlayer().getUUID());

            checkPayloads(context, server, connection, name);
            checkHudBadge(context);
            checkPeekPanel(context);
            checkHeartFragment(context, server, connection, name, uuid);
        }
    }

    /** Config sync, level sync and Blood Moon sync all arrive through their payloads. */
    private static void checkPayloads(ClientGameTestContext context, TestDedicatedServerContext server,
                                      TestDedicatedServerConnection connection, String name) {
        // The client and the in-process server share the local config object, so a synced config is
        // recognisable as a different instance parsed from the ConfigSyncPayload JSON.
        context.waitFor(mc -> ClientConfigState.effective() != Tribulation.getConfig());
        context.waitFor(mc -> ClientTribulationState.getLevel() == 0);

        server.runCommand("tribulation set " + name + " " + TIER_FIVE_LEVEL);
        context.waitFor(mc -> ClientTribulationState.getLevel() == TIER_FIVE_LEVEL);
        int tier = TierManager.getTier(ClientTribulationState.getLevel(), ClientConfigState.effective().tiers);
        check(tier == 5, "level " + TIER_FIVE_LEVEL + " should be tier 5, was tier " + tier);

        server.runCommand("time set night");
        server.runCommand("tribulation bloodmoon start");
        context.waitFor(mc -> ClientTribulationState.isBloodMoonActive());
        server.runCommand("tribulation bloodmoon stop");
        context.waitFor(mc -> !ClientTribulationState.isBloodMoonActive());
        server.runCommand("time set day");
        // Let the day time and the level-up flash (2 s) settle so later frames differ only by
        // what each check toggles.
        connection.waitForClientboundPackets();
        context.waitTicks(SETTLE_TICKS);
    }

    /** The tier badge is drawn at its anchor, and disappears when the HUD is turned off. */
    private static void checkHudBadge(ClientGameTestContext context) {
        check(context.computeOnClient(mc -> TribulationHudOverlay.isHudVisible()), "HUD should be visible in game");
        TribulationConfig config = Tribulation.getConfig();
        BufferedImage shown = screenshot(context, "tribulation_hud_tier5");
        boolean saved = config.enableTierHud;
        BufferedImage hidden;
        try {
            config.enableTierHud = false;
            context.waitTick();
            hidden = screenshot(context, "tribulation_hud_off");
        } finally {
            config.enableTierHud = saved;
        }

        int scale = context.computeOnClient(mc -> mc.getWindow().getGuiScale());
        int badgeX = config.hudOffsetX * scale;
        int badgeY = config.hudOffsetY * scale;
        int size = 16 * scale;
        double badge = changedFraction(shown, hidden, badgeX, badgeY, size, size);
        double control = changedFraction(shown, hidden, badgeX + 4 * size, badgeY, size, size);
        check(badge > 0.25, "badge area should change when the HUD is hidden, changed " + badge);
        check(control < 0.05, "area beside the badge should not change, changed " + control);
    }

    /** Holding the peek key (Left Alt by default) draws the centred tier detail panel. */
    private static void checkPeekPanel(ClientGameTestContext context) {
        BufferedImage before = screenshot(context, "tribulation_peek_released");
        context.getInput().holdKey(TribulationClient.KEY_PEEK_DETAIL);
        context.waitTicks(2);
        check(context.computeOnClient(mc -> TribulationClient.KEY_PEEK_DETAIL.isDown()), "peek key should be down");
        BufferedImage held = screenshot(context, "tribulation_peek_held");
        context.getInput().releaseKey(TribulationClient.KEY_PEEK_DETAIL);

        int w = before.getWidth();
        int h = before.getHeight();
        // A band left of the crosshair, inside the centred panel whatever its final size.
        double panel = changedFraction(before, held, w / 2 - w / 8, h / 2 - h / 16, w / 10, h / 8);
        check(panel > 0.5, "peek panel should cover the centre of the screen, changed " + panel);
    }

    /**
     * The Heart Fragment renders with its model (no missing-texture magenta in the hotbar slot) and a
     * right click on the client restores hearts on the server.
     */
    private static void checkHeartFragment(ClientGameTestContext context, TestDedicatedServerContext server,
                                           TestDedicatedServerConnection connection, String name, UUID uuid) {
        TribulationConfig config = Tribulation.getConfig();
        boolean saved = config.hardcoreHearts.enabled;
        try {
            config.hardcoreHearts.enabled = true;
            server.runOnServer(s -> {
                ServerPlayer player = s.getPlayerList().getPlayer(uuid);
                PlayerDifficultyState.getOrCreate(s).addHeartsLost(uuid, HEARTS_LOST, 2);
                HardcoreHeartsHandler.applyModifier(player);
            });
            server.runCommand("give " + name + " " + TribulationItems.HEART_FRAGMENT_ID + " 2");
            context.runOnClient(mc -> mc.player.getInventory().setSelectedSlot(0));
            context.waitFor(mc -> mc.player.getMainHandItem().getCount() == 2);
            context.waitTicks(2);

            BufferedImage frame = screenshot(context, "tribulation_heart_fragment_hotbar");
            int scale = context.computeOnClient(mc -> mc.getWindow().getGuiScale());
            int guiW = context.computeOnClient(mc -> mc.getWindow().getGuiScaledWidth());
            int guiH = context.computeOnClient(mc -> mc.getWindow().getGuiScaledHeight());
            int slotX = (guiW / 2 - 91 + 3) * scale;
            int slotY = (guiH - 19) * scale;
            int magenta = countMagenta(frame, slotX, slotY, 16 * scale, 16 * scale);
            // The checker fills half the slot; the crystal's own shading matches the hue on a few pixels.
            int slotPixels = 16 * scale * 16 * scale;
            check(magenta < slotPixels / 8,
                    "hotbar slot shows the missing-texture checker (" + magenta + " magenta pixels)");

            context.getInput().pressKey(options -> options.keyUse);
            server.waitFor(s -> PlayerDifficultyState.getOrCreate(s).getHeartsLost(uuid) < HEARTS_LOST);
            context.waitFor(mc -> mc.player.getMainHandItem().getCount() == 1);
            connection.waitForClientboundPackets();
        } finally {
            config.hardcoreHearts.enabled = saved;
        }
    }

    private static BufferedImage screenshot(ClientGameTestContext context, String name) {
        Path path = context.takeScreenshot(name);
        try {
            return ImageIO.read(path.toFile());
        } catch (IOException e) {
            throw new UncheckedIOException("could not read screenshot " + path, e);
        }
    }

    private static double changedFraction(BufferedImage a, BufferedImage b, int x0, int y0, int w, int h) {
        int changed = 0;
        int total = 0;
        for (int y = y0; y < y0 + h; y++) {
            for (int x = x0; x < x0 + w; x++) {
                total++;
                if (channelDistance(a.getRGB(x, y), b.getRGB(x, y)) > 48) changed++;
            }
        }
        return (double) changed / total;
    }

    private static int countMagenta(BufferedImage image, int x0, int y0, int w, int h) {
        int count = 0;
        for (int y = y0; y < y0 + h; y++) {
            for (int x = x0; x < x0 + w; x++) {
                // The missing-texture checker reaches the hotbar as dimmed magenta (0x7F007F on
                // 26.3), so match the hue rather than the 0xF800F8 source colour.
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                if (r >= 0x60 && Math.abs(r - b) < 24 && g < 24) count++;
            }
        }
        return count;
    }

    private static int channelDistance(int a, int b) {
        return Math.abs(((a >> 16) & 0xFF) - ((b >> 16) & 0xFF))
                + Math.abs(((a >> 8) & 0xFF) - ((b >> 8) & 0xFF))
                + Math.abs((a & 0xFF) - (b & 0xFF));
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
