package com.cantstopwinning.gui;

import com.cantstopwinning.CantStopWinningClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class ConfigScreen extends Screen {

    private static final int TRIGGERS_PER_PAGE = 12;

    private final Screen parent;
    private String activeTab = "triggers";

    // Trigger tab widgets
    private TextFieldWidget inputField;
    private final List<ButtonWidget> removeButtons = new ArrayList<>();
    private int page = 0;
    private ButtonWidget prevBtn;
    private ButtonWidget nextBtn;

    // Corpse tab widgets
    private final List<ButtonWidget> corpseButtons = new ArrayList<>();

    // Tab buttons
    private ButtonWidget triggersTab;
    private ButtonWidget corpseTab;

    public ConfigScreen(Screen parent) {
        super(Text.literal("CantStopWinning Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearChildren();
        removeButtons.clear();
        corpseButtons.clear();

        // --- Tab buttons ---
        triggersTab = ButtonWidget.builder(Text.literal("Triggers"), btn -> {
            activeTab = "triggers";
            init();
        }).dimensions(width / 2 - 102, 28, 100, 20).build();
        addDrawableChild(triggersTab);

        corpseTab = ButtonWidget.builder(Text.literal("Corpse"), btn -> {
            activeTab = "corpse";
            init();
        }).dimensions(width / 2 + 2, 28, 100, 20).build();
        addDrawableChild(corpseTab);

        // Highlight active tab
        triggersTab.active = !activeTab.equals("triggers");
        corpseTab.active = !activeTab.equals("corpse");

        if (activeTab.equals("triggers")) {
            initTriggersTab();
        } else {
            initCorpseTab();
        }

        // --- Done ---
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), btn -> close())
            .dimensions(width / 2 - 50, height - 30, 100, 20).build());
    }

    private void initTriggersTab() {
        // --- Volume slider ---
        double vol = CantStopWinningClient.CONFIG.volume;
        addDrawableChild(new SliderWidget(width / 2 - 150, 55, 300, 20,
                Text.literal("Volume: " + (int)(vol * 100) + "%"), vol) {
            @Override
            protected void updateMessage() {
                setMessage(Text.literal("Volume: " + (int)(value * 100) + "%"));
            }
            @Override
            protected void applyValue() {
                CantStopWinningClient.CONFIG.volume = value;
                CantStopWinningClient.CONFIG.save();
            }
        });

        // --- Trigger input field ---
        inputField = new TextFieldWidget(textRenderer, width / 2 - 150, 85, 270, 20, Text.literal("Trigger message"));
        inputField.setMaxLength(256);
        addDrawableChild(inputField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Add"), btn -> {
            String text = inputField.getText().trim();
            if (!text.isEmpty() && !CantStopWinningClient.CONFIG.triggerMessages.contains(text)) {
                CantStopWinningClient.CONFIG.triggerMessages.add(text);
                CantStopWinningClient.CONFIG.save();
                inputField.setText("");
                rebuildTriggerList();
            }
        }).dimensions(width / 2 + 125, 85, 40, 20).build());

        // --- Page buttons ---
        prevBtn = ButtonWidget.builder(Text.literal("<"), btn -> {
            if (page > 0) { page--; rebuildTriggerList(); }
        }).dimensions(width / 2 - 80, height - 55, 20, 20).build();
        addDrawableChild(prevBtn);

        nextBtn = ButtonWidget.builder(Text.literal(">"), btn -> {
            if ((page + 1) * TRIGGERS_PER_PAGE < CantStopWinningClient.CONFIG.triggerMessages.size()) {
                page++; rebuildTriggerList();
            }
        }).dimensions(width / 2 + 60, height - 55, 20, 20).build();
        addDrawableChild(nextBtn);

        rebuildTriggerList();
    }

    private void rebuildTriggerList() {
        removeButtons.forEach(this::remove);
        removeButtons.clear();

        List<String> msgs = CantStopWinningClient.CONFIG.triggerMessages;
        int totalPages = Math.max(1, (msgs.size() + TRIGGERS_PER_PAGE - 1) / TRIGGERS_PER_PAGE);
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;

        prevBtn.active = page > 0;
        nextBtn.active = (page + 1) * TRIGGERS_PER_PAGE < msgs.size();

        int startIdx = page * TRIGGERS_PER_PAGE;
        int endIdx = Math.min(startIdx + TRIGGERS_PER_PAGE, msgs.size());
        int y = 120;
        for (int i = startIdx; i < endIdx; i++) {
            final int idx = i;
            ButtonWidget btn = ButtonWidget.builder(Text.literal("X"), b -> {
                if (idx < CantStopWinningClient.CONFIG.triggerMessages.size()) {
                    CantStopWinningClient.CONFIG.triggerMessages.remove(idx);
                    CantStopWinningClient.CONFIG.save();
                    rebuildTriggerList();
                }
            }).dimensions(width / 2 + 120, y, 20, 18).build();
            addDrawableChild(btn);
            removeButtons.add(btn);
            y += 20;
        }
    }

    private void initCorpseTab() {
        int y = 58;

        // --- Enable toggle ---
        boolean corpseEnabled = CantStopWinningClient.CONFIG.corpseDetectionEnabled;
        ButtonWidget enableBtn = ButtonWidget.builder(
                Text.literal("Corpse Detection: " + (corpseEnabled ? "ON" : "OFF")), btn -> {
            CantStopWinningClient.CONFIG.corpseDetectionEnabled = !CantStopWinningClient.CONFIG.corpseDetectionEnabled;
            CantStopWinningClient.CONFIG.save();
            init();
        }).dimensions(width / 2 - 100, y, 200, 20).build();
        addDrawableChild(enableBtn);
        corpseButtons.add(enableBtn);
        y += 28;

        // --- Mode toggle ---
        String mode = CantStopWinningClient.CONFIG.corpseMode;
        ButtonWidget modeBtn = ButtonWidget.builder(
                Text.literal("Mode: " + (mode.equals("simple") ? "Simple (-)" : "Percentage (%)")), btn -> {
            if (CantStopWinningClient.CONFIG.corpseMode.equals("simple")) {
                CantStopWinningClient.CONFIG.corpseMode = "percentage";
            } else {
                CantStopWinningClient.CONFIG.corpseMode = "simple";
            }
            CantStopWinningClient.CONFIG.save();
            init();
        }).dimensions(width / 2 - 100, y, 200, 20).build();
        addDrawableChild(modeBtn);
        corpseButtons.add(modeBtn);
        y += 28;

        // --- Percentage mode settings ---
        if (mode.equals("percentage")) {
            // Threshold slider
            double thresh = CantStopWinningClient.CONFIG.corpseThreshold / 100.0;
            addDrawableChild(new SliderWidget(width / 2 - 100, y, 200, 20,
                    Text.literal("Loss Threshold: " + (int)(CantStopWinningClient.CONFIG.corpseThreshold) + "%"), thresh) {
                @Override
                protected void updateMessage() {
                    setMessage(Text.literal("Loss Threshold: " + (int)(value * 100) + "%"));
                }
                @Override
                protected void applyValue() {
                    CantStopWinningClient.CONFIG.corpseThreshold = value * 100;
                    CantStopWinningClient.CONFIG.save();
                }
            });
            y += 30;

            // Key costs are auto-detected — just show them as labels in render
            // (detected from Bazaar purchases and tooltip hover via SkyHanni)
        }
    }

    private String formatCost(double millions) {
        if (millions == (long) millions) {
            return String.valueOf((long) millions);
        }
        return String.valueOf(millions);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        // Title
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFFFF);

        if (activeTab.equals("triggers")) {
            renderTriggersTab(context);
        } else {
            renderCorpseTab(context);
        }
    }

    private void renderTriggersTab(DrawContext context) {
        // "Triggers:" label
        context.drawTextWithShadow(textRenderer, Text.literal("Triggers:"), width / 2 - 150, 110, 0xFFAAAAAA);

        // Trigger list with bordered rows
        List<String> msgs = CantStopWinningClient.CONFIG.triggerMessages;
        int startIdx = page * TRIGGERS_PER_PAGE;
        int endIdx = Math.min(startIdx + TRIGGERS_PER_PAGE, msgs.size());

        int listLeft = width / 2 - 152;
        int listRight = width / 2 + 143;
        int y = 120;
        int rowH = 20;

        for (int i = startIdx; i < endIdx; i++) {
            // Row background (dark fill)
            context.fill(listLeft, y, listRight, y + rowH, 0x80000000);
            // Border: top, bottom, left, right (thin gray lines)
            int border = 0xFF555555;
            context.fill(listLeft, y, listRight, y + 1, border);
            context.fill(listLeft, y + rowH - 1, listRight, y + rowH, border);
            context.fill(listLeft, y, listLeft + 1, y + rowH, border);
            context.fill(listRight - 1, y, listRight, y + rowH, border);

            // Text (vertically centered in row)
            String msg = msgs.get(i);
            if (textRenderer.getWidth(msg) > 250) {
                while (textRenderer.getWidth(msg + "...") > 250 && msg.length() > 1) {
                    msg = msg.substring(0, msg.length() - 1);
                }
                msg += "...";
            }
            context.drawTextWithShadow(textRenderer, Text.literal(msg), listLeft + 4, y + (rowH - 8) / 2, 0xFFFFFFFF);
            y += rowH;
        }

        // Page indicator
        int totalPages = Math.max(1, (msgs.size() + TRIGGERS_PER_PAGE - 1) / TRIGGERS_PER_PAGE);
        String pageText = "Page " + (page + 1) + "/" + totalPages;
        context.drawCenteredTextWithShadow(textRenderer, pageText, width / 2, height - 51, 0xFFAAAAAA);
    }

    private void renderCorpseTab(DrawContext context) {
        // Description — requires SkyHanni for profit detection
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Requires SkyHanni for profit messages"),
                width / 2, height - 58, 0xFF666666);

        String mode = CantStopWinningClient.CONFIG.corpseMode;
        if (mode.equals("percentage")) {
            int y = 141;
            // Key costs header
            context.drawTextWithShadow(textRenderer, Text.literal("Key Prices (auto-detected):"),
                    width / 2 - 100, y, 0xFFAAAAAA);
            y += 16;

            // Show detected prices with bordered rows
            int left = width / 2 - 100;
            int right = width / 2 + 100;
            int rowH = 16;

            String[] labels = {"Umber Key:", "Tungsten Key:", "Skeleton Key:"};
            double[] costs = {
                CantStopWinningClient.CONFIG.umberKeyCost,
                CantStopWinningClient.CONFIG.tungstenKeyCost,
                CantStopWinningClient.CONFIG.vanguardKeyCost
            };

            for (int i = 0; i < labels.length; i++) {
                // Row background
                context.fill(left, y, right, y + rowH, 0x80000000);
                int border = 0xFF555555;
                context.fill(left, y, right, y + 1, border);
                context.fill(left, y + rowH - 1, right, y + rowH, border);
                context.fill(left, y, left + 1, y + rowH, border);
                context.fill(right - 1, y, right, y + rowH, border);

                context.drawTextWithShadow(textRenderer, Text.literal(labels[i]),
                        left + 4, y + 4, 0xFFFFFFFF);
                String priceText = costs[i] > 0 ? formatCost(costs[i]) + "M" : "unknown";
                int priceColor = costs[i] > 0 ? 0xFF55FF55 : 0xFFFF5555;
                context.drawTextWithShadow(textRenderer, Text.literal(priceText),
                        right - 4 - textRenderer.getWidth(priceText), y + 4, priceColor);
                y += rowH;
            }

            y += 6;
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("Hover a key or buy from Bazaar to update"),
                    width / 2, y, 0xFF666666);
        }

        // Mode description
        int descY = height - 70;
        if (mode.equals("simple")) {
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("Any negative profit triggers the loss overlay"),
                    width / 2, descY, 0xFF888888);
        } else {
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("Loss triggers when loss > threshold % of key cost"),
                    width / 2, descY, 0xFF888888);
        }
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }
}
