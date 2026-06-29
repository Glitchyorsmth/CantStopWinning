package com.cantstopwinning;

import dev.anvil.api.config.ConfigHandle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleConsumer;

/**
 * CantStopWinning settings screen — dark SkyFisher style, three tabs (General / Triggers / Corpse).
 * 1.21.11 only (uses {@code GuiGraphics}, removed in 26.x); the build excludes this file on 26.x,
 * where {@code /csw} falls back to Anvil's generated screen.
 *
 * <p>The whole UI is drawn over a full-screen backdrop but scaled down by {@link #UI_SCALE} and
 * centered. Because the content is scaled, mouse input is mapped back into the unscaled layout
 * space, and the {@link EditBox} widgets are rendered by hand inside the scaled transform (added as
 * focus-only children, not auto-rendered). Tooltips render after the scaled pass, in screen space.
 */
public final class CswConfigScreen extends Screen {

    // ── Palette (ARGB) ──
    private static final int FULLSCREEN_BG = 0xF00a0a12;
    private static final int PANEL_BG      = 0xFF141422;
    private static final int HEADER_BG     = 0xFF0c0c18;
    private static final int BORDER        = 0xFF1a2744;
    private static final int CYAN          = 0xFF00d2ff;
    private static final int GREEN         = 0xFF00e676;
    private static final int RED           = 0xFFff1744;
    private static final int TEXT_WHITE    = 0xFFe8e8e8;
    private static final int TEXT_DIM      = 0xFF8899aa;
    private static final int TEXT_DIMMER   = 0xFF556677;
    private static final int TOGGLE_ON_BG  = 0xFF1a3a2a;
    private static final int TOGGLE_OFF_BG = 0xFF3a1a1a;
    private static final int FIELD_BORDER  = 0xFF2a3a5a;
    private static final int CHIP_BG       = 0xFF1b2540;
    private static final int CHIP_HOVER    = 0xFF26345a;
    private static final int TAB_ACTIVE    = 0xFF1a3a4a;
    private static final int ROW_BORDER    = 0xFF2a3a5a;
    private static final int TRACK_BG      = 0xFF0a0a16;
    private static final int TRACK_FILL    = 0xFF1a5a6a;
    private static final int KNOB          = 0xFF00d2ff;
    private static final int SECTION_LINE  = 0xFF1a2440;
    private static final int TIP_BG        = 0xF0101826;
    private static final int TIP_BORDER    = 0xFF2a3a5a;

    /** Fixed logical panel size; the panel is scaled to fit the window and centered. */
    private static final int DESIGN_W = 380;
    private static final int DESIGN_H = 300;
    /** Fraction of the window the panel is allowed to fill, and a cap so it stays readable. */
    private static final float FIT_MARGIN = 0.92f;
    private static final float MAX_SCALE = 3.0f;

    private static final int ROW = 24;
    private static final int TRIGGERS_PER_PAGE = 6;

    private enum Tab { GENERAL, TRIGGERS, CORPSE, TESTING }

    private final ConfigHandle<CswConfig> handle;
    private final CswConfig cfg;

    private Tab tab = Tab.TRIGGERS;
    private int panelX, panelY, panelW, panelH;
    private int contentTop;
    private int triggerPage;
    private int triggerBoxStart;     // absolute index of the first trigger box on this page
    private int draggingSlider = -1;

    private float scale = 1f;
    private int offX, offY;

    // Text widgets (focus-only children; rendered by hand).
    private EditBox addField;
    private EditBox presetField;
    private EditBox umberBox, tungBox, vanBox, simpleBox;
    private final List<EditBox> boxes = new ArrayList<>();         // every box, for render + focus
    private final List<EditBox> triggerBoxes = new ArrayList<>();  // editable trigger rows (this page)

    private final List<Zone> zones = new ArrayList<>();
    private final List<Zone> overlayZones = new ArrayList<>();   // dropdown items — checked first
    private final List<SliderZone> sliders = new ArrayList<>();
    private final List<Tip> tips = new ArrayList<>();
    private String hoverTooltip;

    // Preset dropdown state + geometry (layout space).
    private boolean presetDropdownOpen;
    private int ddX, ddY, ddW;

    private record Zone(int x1, int y1, int x2, int y2, Runnable action) {
        boolean hit(double mx, double my) {
            return mx >= x1 && mx < x2 && my >= y1 && my < y2;
        }
    }

    private record SliderZone(int x, int y, int w, int h, DoubleConsumer setter) {
        boolean hit(double mx, double my) {
            return mx >= x && mx < x + w && my >= y - 4 && my < y + h + 4;
        }
        double valueAt(double mx) {
            return Math.max(0.0, Math.min(1.0, (mx - x) / w));
        }
    }

    private record Tip(int x1, int y1, int x2, int y2, String text) {
        boolean hit(double mx, double my) {
            return mx >= x1 && mx < x2 && my >= y1 && my < y2;
        }
    }

    public CswConfigScreen(ConfigHandle<CswConfig> handle) {
        super(Component.literal("CantStopWinning"));
        this.handle = handle;
        this.cfg = handle.get();
    }

    @Override
    protected void init() {
        // Fit the fixed-size panel to the window (preserve aspect), centered, capped for readability.
        scale = Math.min(width * FIT_MARGIN / DESIGN_W, height * FIT_MARGIN / DESIGN_H);
        scale = Math.min(scale, MAX_SCALE);
        offX = Math.round((width - DESIGN_W * scale) / 2f);
        offY = Math.round((height - DESIGN_H * scale) / 2f);
        panelX = 0;
        panelY = 0;
        panelW = DESIGN_W;
        panelH = DESIGN_H;
        contentTop = panelY + 56;

        boxes.clear();
        triggerBoxes.clear();
        addField = null;
        presetField = null;
        umberBox = tungBox = vanBox = simpleBox = null;

        int innerX = panelX + 12;
        int innerW = panelW - 24;

        if (tab == Tab.TRIGGERS) {
            presetField = makeBox(150, 16, 48);
            presetField.setValue(cfg.activePreset);
            addField = makeBox(innerW - 52, 16, 256);
            addField.setHint(Component.literal("Type a trigger phrase…"));

            // One editable box per trigger on the current page.
            List<String> triggers = cfg.activeTriggers();
            int totalPages = Math.max(1, (triggers.size() + TRIGGERS_PER_PAGE - 1) / TRIGGERS_PER_PAGE);
            triggerPage = Math.max(0, Math.min(totalPages - 1, triggerPage));
            triggerBoxStart = triggerPage * TRIGGERS_PER_PAGE;
            int end = Math.min(triggerBoxStart + TRIGGERS_PER_PAGE, triggers.size());
            for (int i = triggerBoxStart; i < end; i++) {
                final int idx = i;
                EditBox tb = makeBox(innerW - 24, 16, 256);
                tb.setValue(triggers.get(i));
                tb.setResponder(text -> {
                    List<String> live = cfg.activeTriggers();
                    if (idx < live.size()) {
                        live.set(idx, text);
                        save();
                    }
                });
                triggerBoxes.add(tb);
            }
        } else if (tab == Tab.CORPSE) {
            simpleBox = costBox(cfg.simpleAmountMillions, v -> { cfg.simpleAmountMillions = v; save(); });
            umberBox = costBox(cfg.umberKeyCost, v -> { cfg.umberKeyCost = v; save(); });
            tungBox  = costBox(cfg.tungstenKeyCost, v -> { cfg.tungstenKeyCost = v; save(); });
            vanBox   = costBox(cfg.vanguardKeyCost, v -> { cfg.vanguardKeyCost = v; save(); });
        }
    }

    private EditBox makeBox(int w, int h, int maxLen) {
        EditBox box = new EditBox(font, 0, 0, w, h, Component.literal(""));
        box.setMaxLength(maxLen);
        addWidget(box);
        boxes.add(box);
        return box;
    }

    private EditBox costBox(double value, DoubleConsumer setter) {
        EditBox box = makeBox(64, 16, 12);
        box.setValue(trimNum(value));
        box.setResponder(text -> {
            try {
                setter.accept(Math.max(0.0, Double.parseDouble(text.trim())));
            } catch (NumberFormatException ignored) {
                // keep last good value
            }
        });
        return box;
    }

    // ── Rendering ──────────────────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        zones.clear();
        overlayZones.clear();
        sliders.clear();
        tips.clear();
        hoverTooltip = null;

        ctx.fill(0, 0, width, height, FULLSCREEN_BG);

        int mvx = toLayoutX(mouseX);
        int mvy = toLayoutY(mouseY);

        ctx.pose().pushMatrix();
        ctx.pose().translate((float) offX, (float) offY);
        ctx.pose().scale(scale, scale);

        ctx.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, BORDER);
        ctx.fill(panelX, panelY, panelX + panelW, panelY + panelH, PANEL_BG);
        ctx.fill(panelX, panelY, panelX + panelW, panelY + 22, HEADER_BG);
        ctx.fill(panelX, panelY + 22, panelX + panelW, panelY + 23, BORDER);
        ctx.drawString(font, "CantStopWinning", panelX + 10, panelY + 7, CYAN, true);

        int doneW = 50;
        drawChip(ctx, "Done", panelX + panelW - 10 - doneW, panelY + 4, doneW, 14, mvx, mvy, "Save & close", this::onClose);

        drawTabs(ctx, mvx, mvy);

        switch (tab) {
            case GENERAL -> drawGeneral(ctx, mvx, mvy);
            case TRIGGERS -> drawTriggers(ctx, mvx, mvy);
            case CORPSE -> drawCorpse(ctx, mvx, mvy);
            case TESTING -> drawTesting(ctx, mvx, mvy);
        }

        for (EditBox box : boxes) {
            box.render(ctx, mvx, mvy, delta);
        }

        // Preset dropdown list draws on top of everything else (still inside the scaled pose).
        if (tab == Tab.TRIGGERS && presetDropdownOpen) {
            drawPresetList(ctx, mvx, mvy);
        }

        // Resolve the hovered tooltip (layout space).
        for (Tip t : tips) {
            if (t.hit(mvx, mvy)) {
                hoverTooltip = t.text();
            }
        }

        ctx.pose().popMatrix();

        if (hoverTooltip != null) {
            drawTooltip(ctx, hoverTooltip, mouseX, mouseY);
        }
    }

    private void drawTooltip(GuiGraphics ctx, String text, int mouseX, int mouseY) {
        int w = font.width(text);
        int tx = Math.min(mouseX + 10, width - w - 8);
        int ty = Math.max(mouseY - 14, 4);
        ctx.fill(tx - 4, ty - 3, tx + w + 4, ty + 11, TIP_BG);
        ctx.fill(tx - 4, ty - 3, tx + w + 4, ty - 2, TIP_BORDER);
        ctx.fill(tx - 4, ty + 10, tx + w + 4, ty + 11, TIP_BORDER);
        ctx.fill(tx - 4, ty - 3, tx - 3, ty + 11, TIP_BORDER);
        ctx.fill(tx + w + 3, ty - 3, tx + w + 4, ty + 11, TIP_BORDER);
        ctx.drawString(font, text, tx, ty, TEXT_WHITE, true);
    }

    private int toLayoutX(double screenX) {
        return (int) ((screenX - offX) / scale);
    }

    private int toLayoutY(double screenY) {
        return (int) ((screenY - offY) / scale);
    }

    private void drawTabs(GuiGraphics ctx, int mvx, int mvy) {
        String[] names = {"General", "Triggers", "Corpse", "Testing"};
        String[] tipText = {
            "Master toggle, volume & overlay opacity",
            "Trigger phrases & switchable presets",
            "Corpse-loss detection options",
            "Fire overlays & test chat detection"
        };
        Tab[] tabs = {Tab.GENERAL, Tab.TRIGGERS, Tab.CORPSE, Tab.TESTING};
        int x = panelX + 10;
        int y = panelY + 30;
        for (int i = 0; i < names.length; i++) {
            int w = font.width(names[i]) + 16;
            boolean active = tab == tabs[i];
            boolean hover = inRect(mvx, mvy, x, y, w, 16);
            ctx.fill(x, y, x + w, y + 16, active ? TAB_ACTIVE : (hover ? CHIP_HOVER : CHIP_BG));
            ctx.fill(x, y + 15, x + w, y + 16, active ? CYAN : BORDER);
            ctx.drawString(font, names[i], x + 8, y + 4, active ? CYAN : TEXT_DIM, true);
            final Tab target = tabs[i];
            zones.add(new Zone(x, y, x + w, y + 16, () -> switchTab(target)));
            tips.add(new Tip(x, y, x + w, y + 16, tipText[i]));
            x += w + 4;
        }
        ctx.fill(panelX + 10, panelY + 49, panelX + panelW - 10, panelY + 50, SECTION_LINE);
    }

    private void drawGeneral(GuiGraphics ctx, int mvx, int mvy) {
        int x = panelX + 12;
        int rightX = panelX + panelW - 12;
        int y = contentTop;

        rowLabel(ctx, "Enabled", x, y);
        drawToggle(ctx, rightX - 38, y, 38, 14, cfg.enabled, mvx, mvy, "Turn the whole mod on or off", () -> {
            cfg.enabled = !cfg.enabled;
            save();
        });
        y += ROW + 4;

        drawSliderRow(ctx, "Volume", x, rightX, y, cfg.volume,
                v -> { cfg.volume = v; save(); }, (int) Math.round(cfg.volume * 100) + "%");
        y += ROW + 4;

        drawSliderRow(ctx, "Overlay opacity", x, rightX, y, cfg.overlayOpacity,
                v -> { cfg.overlayOpacity = v; save(); }, (int) Math.round(cfg.overlayOpacity * 100) + "%");
    }

    private void drawTriggers(GuiGraphics ctx, int mvx, int mvy) {
        int x = panelX + 12;
        int rightX = panelX + panelW - 12;

        ctx.drawString(font, "PRESET", x, contentTop, TEXT_DIMMER, true);
        // Row 1: dropdown selector + New + Del.
        int rowY = contentTop + 11;
        int ddWidth = rightX - x - 68;
        drawPresetDropdownChip(ctx, x, rowY, ddWidth, mvx, mvy);
        int bx = x + ddWidth + 4;
        bx += drawChip(ctx, "New", bx, rowY, 32, 16, mvx, mvy, "Create a new preset", this::newPreset) + 4;
        drawChip(ctx, "Del", bx, rowY, 28, 16, mvx, mvy, "Delete the current preset", this::deletePreset);

        // Row 2: name box + Rename (the box names new/renamed presets).
        int row2 = rowY + 22;
        if (presetField != null) {
            presetField.setWidth(150);
            presetField.setPosition(x, row2);
        }
        drawChip(ctx, "Rename", x + 156, row2, 56, 16, mvx, mvy, "Rename current preset to the name box", this::renamePreset);

        int addLabelY = row2 + 26;
        ctx.drawString(font, "TRIGGERS", x, addLabelY, TEXT_DIMMER, true);
        int addY = addLabelY + 11;
        if (addField != null) {
            addField.setWidth(rightX - x - 48);
            addField.setPosition(x, addY);
        }
        drawChip(ctx, "Add", rightX - 44, addY, 44, 16, mvx, mvy, "Add the typed phrase as a trigger", this::addTrigger);

        int listTop = addY + 24;
        ctx.fill(x, listTop - 2, rightX, listTop - 1, SECTION_LINE);
        List<String> triggers = cfg.activeTriggers();
        if (triggers.isEmpty()) {
            ctx.drawString(font, "No triggers — type one above and press Add.", x, listTop + 4, TEXT_DIMMER, true);
            return;
        }

        int totalPages = (triggers.size() + TRIGGERS_PER_PAGE - 1) / TRIGGERS_PER_PAGE;
        int rowH = 22;
        int ry = listTop;
        for (int k = 0; k < triggerBoxes.size(); k++) {
            EditBox tb = triggerBoxes.get(k);
            tb.setWidth(rightX - x - 22);
            tb.setPosition(x, ry + 2);
            int xs = rightX - 16;
            boolean hov = inRect(mvx, mvy, xs, ry, 16, rowH - 2);
            ctx.drawString(font, "✕", xs + 4, ry + 7, hov ? RED : TEXT_DIM, true);
            final int idx = triggerBoxStart + k;
            zones.add(new Zone(xs, ry, rightX, ry + rowH - 2, () -> removeTrigger(idx)));
            tips.add(new Tip(xs, ry, rightX, ry + rowH - 2, "Remove this trigger"));
            ry += rowH;
        }

        if (totalPages > 1) {
            ry += 4;
            String pageLabel = "Page " + (triggerPage + 1) + " / " + totalPages;
            int pageStr = font.width(pageLabel);
            int navW = 16 + 6 + pageStr + 6 + 16;
            int navX = x + (rightX - x - navW) / 2;
            drawChip(ctx, "◀", navX, ry, 16, 16, mvx, mvy, "Previous page", this::triggerPagePrev);
            ctx.drawString(font, pageLabel, navX + 22, ry + 4, TEXT_DIM, true);
            drawChip(ctx, "▶", navX + 22 + pageStr + 6, ry, 16, 16, mvx, mvy, "Next page", this::triggerPageNext);
        }
    }

    private void triggerPagePrev() {
        if (triggerPage > 0) {
            triggerPage--;
            rebuildWidgets();
        }
    }

    private void triggerPageNext() {
        int totalPages = (cfg.activeTriggers().size() + TRIGGERS_PER_PAGE - 1) / TRIGGERS_PER_PAGE;
        if (triggerPage < totalPages - 1) {
            triggerPage++;
            rebuildWidgets();
        }
    }

    private void drawPresetDropdownChip(GuiGraphics ctx, int x, int y, int w, int mvx, int mvy) {
        ddX = x;
        ddY = y;
        ddW = w;
        boolean hover = inRect(mvx, mvy, x, y, w, 16);
        ctx.fill(x, y, x + w, y + 16, hover ? CHIP_HOVER : CHIP_BG);
        outline(ctx, x, y, w, 16, FIELD_BORDER);
        ctx.drawString(font, trim(cfg.activePreset, w - 22), x + 6, y + 4, TEXT_WHITE, true);
        ctx.drawString(font, presetDropdownOpen ? "▲" : "▼", x + w - 12, y + 4, CYAN, true);
        zones.add(new Zone(x, y, x + w, y + 16, () -> presetDropdownOpen = !presetDropdownOpen));
        tips.add(new Tip(x, y, x + w, y + 16, "Select a preset"));
    }

    private void drawPresetList(GuiGraphics ctx, int mvx, int mvy) {
        List<String> names = new ArrayList<>(cfg.presets.keySet());
        int x = ddX;
        int w = ddW;
        int y = ddY + 17;
        int h = names.size() * 16 + 2;
        ctx.fill(x - 1, y - 1, x + w + 1, y + h + 1, FIELD_BORDER);
        ctx.fill(x, y, x + w, y + h, 0xFF0c1220);
        int ly = y + 1;
        for (String name : names) {
            boolean active = name.equals(cfg.activePreset);
            boolean hover = inRect(mvx, mvy, x, ly, w, 16);
            if (hover) {
                ctx.fill(x, ly, x + w, ly + 16, CHIP_HOVER);
            }
            ctx.drawString(font, trim(name, w - 12), x + 6, ly + 4, active ? CYAN : (hover ? TEXT_WHITE : TEXT_DIM), true);
            final String sel = name;
            overlayZones.add(new Zone(x, ly, x + w, ly + 16, () -> selectPreset(sel)));
            ly += 16;
        }
    }

    private void selectPreset(String name) {
        cfg.activePreset = name;
        save();
        triggerPage = 0;
        presetDropdownOpen = false;
        rebuildWidgets();
    }

    private void drawCorpse(GuiGraphics ctx, int mvx, int mvy) {
        int x = panelX + 12;
        int rightX = panelX + panelW - 12;
        int y = contentTop;

        rowLabel(ctx, "Corpse detection", x, y);
        drawToggle(ctx, rightX - 38, y, 38, 14, cfg.corpseDetectionEnabled, mvx, mvy,
                "Toggle SkyHanni corpse-loss alerts", () -> {
                    cfg.corpseDetectionEnabled = !cfg.corpseDetectionEnabled;
                    save();
                });
        y += ROW;

        boolean simple = cfg.corpseMode == CorpseMode.SIMPLE;
        rowLabel(ctx, "Mode", x, y);
        String modeLabel = simple ? "Set amount" : "Percentage (%)";
        String modeTip = simple
                ? "Set amount: fires when the loss is at least the amount below."
                : "Percentage: fires when the loss exceeds the threshold % of the key cost.";
        int modeW = font.width(modeLabel) + 14;
        drawChip(ctx, modeLabel, rightX - modeW, y, modeW, 16, mvx, mvy, modeTip, () -> {
            cfg.corpseMode = cfg.corpseMode == CorpseMode.SIMPLE ? CorpseMode.PERCENTAGE : CorpseMode.SIMPLE;
            save();
        });
        y += ROW;

        if (simple) {
            positionCostRow(ctx, "Min loss (M)", simpleBox, x, rightX, y);
            if (simpleBox != null) {
                simpleBox.visible = true;
            }
            y += ROW;
        } else {
            if (simpleBox != null) {
                simpleBox.visible = false;
            }
            drawSliderRow(ctx, "Loss threshold", x, rightX, y, cfg.corpseThreshold / 100.0,
                    v -> { cfg.corpseThreshold = v * 100; save(); }, (int) Math.round(cfg.corpseThreshold) + "%");
            y += ROW;
        }

        y += 6;
        ctx.fill(x, y, rightX, y + 1, SECTION_LINE);
        y += 6;
        ctx.drawString(font, "KEY PRICES (millions)", x, y, TEXT_DIMMER, true);
        y += 14;

        positionCostRow(ctx, "Umber", umberBox, x, rightX, y);
        y += ROW;
        positionCostRow(ctx, "Tungsten", tungBox, x, rightX, y);
        y += ROW;
        positionCostRow(ctx, "Skeleton", vanBox, x, rightX, y);
        y += ROW;
        ctx.drawString(font, "Auto-fills from Bazaar buys / tooltips.", x, y + 2, TEXT_DIMMER, true);
    }

    private void positionCostRow(GuiGraphics ctx, String label, EditBox box, int x, int rightX, int y) {
        rowLabel(ctx, label, x, y);
        if (box != null) {
            box.setPosition(rightX - 64, y - 1);
        }
    }

    private void drawTesting(GuiGraphics ctx, int mvx, int mvy) {
        int x = panelX + 12;
        int w = panelX + panelW - 12 - x;
        int y = contentTop;
        ctx.drawString(font, "Each button closes the menu so the overlay shows.", x, y, TEXT_DIMMER, true);
        y += 16;
        y = testButton(ctx, "Celebration overlay (win)", x, y, w, mvx, mvy,
                Celebrations::win, "§b[CSW]§r §aCelebration overlay fired.");
        y = testButton(ctx, "Loss overlay", x, y, w, mvx, mvy,
                Celebrations::loss, "§b[CSW]§r §cLoss overlay fired.");
        y = testButton(ctx, "Sim: default trigger → win", x, y, w, mvx, mvy,
                () -> CswChat.onChat(Component.literal("Wow! You dug out 1,000,000 coins!")),
                "§b[CSW]§r Simulated default trigger §7→§r §acelebration.");
        y = testButton(ctx, "Sim: Bazaar key buy (filtered + sets price)", x, y, w, mvx, mvy,
                () -> CswChat.onChat(Component.literal("[Bazaar] Bought 1x Skeleton Key for 30M coins!")),
                "§b[CSW]§r Simulated Bazaar buy §7(filtered, Skeleton price → 30M)§r.");
        y = testButton(ctx, "Sim: corpse loss → loss", x, y, w, mvx, mvy,
                () -> CswChat.onChat(Component.literal("[SkyHanni] Profit for Vanguard Corpse: -20M")),
                "§b[CSW]§r Simulated corpse loss §7→§r §closs overlay.");
        testButton(ctx, "Sim: no match (does nothing)", x, y, w, mvx, mvy,
                () -> CswChat.onChat(Component.literal("hello there")),
                "§b[CSW]§r Simulated unmatched message §7(nothing fired)§r.");
    }

    private int testButton(GuiGraphics ctx, String label, int x, int y, int w, int mvx, int mvy,
                           Runnable action, String feedback) {
        drawChip(ctx, label, x, y, w, 18, mvx, mvy, null, () -> runTest(action, feedback));
        return y + 24;
    }

    /** Closes the menu (so the HUD overlay is visible), runs the test, then prints chat feedback. */
    private void runTest(Runnable action, String feedback) {
        onClose();
        action.run();
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.literal(feedback), false);
        }
    }

    // ── Draw helpers ─────────────────────────────────────────────────────────────────────────

    private void rowLabel(GuiGraphics ctx, String text, int x, int y) {
        ctx.drawString(font, text, x, y + 4, TEXT_WHITE, true);
    }

    private int drawChip(GuiGraphics ctx, String label, int x, int y, int w, int h, int mvx, int mvy,
                         String tip, Runnable action) {
        boolean hover = inRect(mvx, mvy, x, y, w, h);
        ctx.fill(x, y, x + w, y + h, hover ? CHIP_HOVER : CHIP_BG);
        outline(ctx, x, y, w, h, FIELD_BORDER);
        ctx.drawString(font, label, x + (w - font.width(label)) / 2, y + (h - 8) / 2, hover ? CYAN : TEXT_WHITE, true);
        zones.add(new Zone(x, y, x + w, y + h, action));
        if (tip != null) {
            tips.add(new Tip(x, y, x + w, y + h, tip));
        }
        return w;
    }

    private void drawToggle(GuiGraphics ctx, int x, int y, int w, int h, boolean on, int mvx, int mvy,
                            String tip, Runnable action) {
        int accent = on ? GREEN : RED;
        ctx.fill(x, y, x + w, y + h, on ? TOGGLE_ON_BG : TOGGLE_OFF_BG);
        outline(ctx, x, y, w, h, accent);
        String label = on ? "ON" : "OFF";
        ctx.drawString(font, label, x + (w - font.width(label)) / 2, y + (h - 8) / 2, accent, true);
        zones.add(new Zone(x, y, x + w, y + h, action));
        if (tip != null) {
            tips.add(new Tip(x, y, x + w, y + h, tip));
        }
    }

    private void drawSliderRow(GuiGraphics ctx, String label, int x, int rightX, int y, double value,
                               DoubleConsumer setter, String valueText) {
        rowLabel(ctx, label, x, y);
        int trackW = 120;
        int trackX = rightX - trackW;
        int trackY = y + 7;
        int trackH = 4;
        ctx.fill(trackX, trackY, trackX + trackW, trackY + trackH, TRACK_BG);
        int fillW = (int) (trackW * Math.max(0.0, Math.min(1.0, value)));
        ctx.fill(trackX, trackY, trackX + fillW, trackY + trackH, TRACK_FILL);
        int knobX = trackX + fillW;
        ctx.fill(knobX - 2, trackY - 4, knobX + 2, trackY + trackH + 4, KNOB);
        ctx.drawString(font, valueText, trackX - font.width(valueText) - 8, y + 3, CYAN, true);
        sliders.add(new SliderZone(trackX, trackY, trackW, trackH, setter));
        tips.add(new Tip(trackX, trackY - 4, trackX + trackW, trackY + trackH + 4, "Drag to adjust"));
    }

    private void outline(GuiGraphics ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    private static boolean inRect(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ── Interaction (mouse coords mapped into layout space) ──────────────────────────────────

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        int mx = toLayoutX(click.x());
        int my = toLayoutY(click.y());

        // Open dropdown captures clicks first.
        if (presetDropdownOpen) {
            for (Zone z : overlayZones) {
                if (z.hit(mx, my)) {
                    z.action().run();
                    return true;
                }
            }
            presetDropdownOpen = false;
            return true;
        }

        for (int i = 0; i < sliders.size(); i++) {
            SliderZone s = sliders.get(i);
            if (s.hit(mx, my)) {
                draggingSlider = i;
                s.setter().accept(s.valueAt(mx));
                clearBoxFocus();
                return true;
            }
        }
        for (Zone z : zones) {
            if (z.hit(mx, my)) {
                z.action().run();
                clearBoxFocus();
                return true;
            }
        }
        for (EditBox box : boxes) {
            if (inRect(mx, my, box.getX(), box.getY(), box.getWidth(), box.getHeight())) {
                clearBoxFocus();
                box.setFocused(true);
                setFocused(box);
                return true;
            }
        }
        clearBoxFocus();
        setFocused(null);
        return false;
    }

    private void clearBoxFocus() {
        for (EditBox box : boxes) {
            box.setFocused(false);
        }
        setFocused(null);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double dragX, double dragY) {
        if (draggingSlider >= 0 && draggingSlider < sliders.size()) {
            SliderZone s = sliders.get(draggingSlider);
            s.setter().accept(s.valueAt(toLayoutX(click.x())));
            return true;
        }
        return super.mouseDragged(click, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        draggingSlider = -1;
        return super.mouseReleased(click);
    }

    // ── Actions ──────────────────────────────────────────────────────────────────────────────

    private void switchTab(Tab target) {
        if (tab != target) {
            tab = target;
            triggerPage = 0;
            presetDropdownOpen = false;
            rebuildWidgets();
        }
    }

    private void addTrigger() {
        if (addField == null) {
            return;
        }
        String text = addField.getValue().trim();
        List<String> triggers = cfg.activeTriggers();
        if (!text.isEmpty() && !triggers.contains(text)) {
            triggers.add(text);
            save();
            // Jump to the last page so the new trigger is visible, then rebuild rows.
            triggerPage = (triggers.size() - 1) / TRIGGERS_PER_PAGE;
            rebuildWidgets();
        }
    }

    private void removeTrigger(int index) {
        List<String> triggers = cfg.activeTriggers();
        if (index >= 0 && index < triggers.size()) {
            triggers.remove(index);
            save();
            rebuildWidgets();
        }
    }

    private void newPreset() {
        String name = presetField != null ? presetField.getValue().trim() : "";
        if (name.isEmpty()) {
            name = "Preset " + (cfg.presets.size() + 1);
        }
        if (!cfg.presets.containsKey(name)) {
            cfg.presets.put(name, new ArrayList<>());
        }
        cfg.activePreset = name;
        save();
        triggerPage = 0;
        rebuildWidgets();
    }

    private void renamePreset() {
        if (presetField == null) {
            return;
        }
        String name = presetField.getValue().trim();
        if (name.isEmpty() || name.equals(cfg.activePreset) || cfg.presets.containsKey(name)) {
            return;
        }
        Map<String, List<String>> rebuilt = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> e : cfg.presets.entrySet()) {
            if (e.getKey().equals(cfg.activePreset)) {
                rebuilt.put(name, e.getValue());
            } else {
                rebuilt.put(e.getKey(), e.getValue());
            }
        }
        cfg.presets = rebuilt;
        cfg.activePreset = name;
        save();
        rebuildWidgets();
    }

    private void deletePreset() {
        if (cfg.presets.size() <= 1) {
            return;
        }
        cfg.presets.remove(cfg.activePreset);
        cfg.activePreset = cfg.presets.keySet().iterator().next();
        save();
        triggerPage = 0;
        rebuildWidgets();
    }

    private void save() {
        handle.save();
    }

    @Override
    public void onClose() {
        handle.save();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ── small utils ──
    private static String trimNum(double v) {
        if (v == (long) v) {
            return String.valueOf((long) v);
        }
        return String.valueOf(v);
    }

    private String trim(String s, int maxW) {
        if (font.width(s) <= maxW) {
            return s;
        }
        while (s.length() > 1 && font.width(s + "...") > maxW) {
            s = s.substring(0, s.length() - 1);
        }
        return s + "...";
    }
}
