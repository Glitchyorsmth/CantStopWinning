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
    private TextFieldWidget inputField;
    private final List<ButtonWidget> removeButtons = new ArrayList<>();
    private int page = 0;
    private ButtonWidget prevBtn;
    private ButtonWidget nextBtn;

    public ConfigScreen(Screen parent) {
        super(Text.literal("CantStopWinning Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        removeButtons.clear();

        // --- Volume slider ---
        double vol = CantStopWinningClient.CONFIG.volume;
        addDrawableChild(new SliderWidget(width / 2 - 150, 30, 300, 20,
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
        inputField = new TextFieldWidget(textRenderer, width / 2 - 150, 60, 270, 20, Text.literal("Trigger message"));
        inputField.setMaxLength(256);
        addDrawableChild(inputField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Add"), btn -> {
            String text = inputField.getText().trim();
            if (!text.isEmpty() && !CantStopWinningClient.CONFIG.triggerMessages.contains(text)) {
                CantStopWinningClient.CONFIG.triggerMessages.add(text);
                CantStopWinningClient.CONFIG.save();
                inputField.setText("");
                rebuildList();
            }
        }).dimensions(width / 2 + 125, 60, 40, 20).build());

        // --- Page buttons ---
        prevBtn = ButtonWidget.builder(Text.literal("<"), btn -> {
            if (page > 0) { page--; rebuildList(); }
        }).dimensions(width / 2 - 80, height - 55, 20, 20).build();
        addDrawableChild(prevBtn);

        nextBtn = ButtonWidget.builder(Text.literal(">"), btn -> {
            if ((page + 1) * TRIGGERS_PER_PAGE < CantStopWinningClient.CONFIG.triggerMessages.size()) {
                page++; rebuildList();
            }
        }).dimensions(width / 2 + 60, height - 55, 20, 20).build();
        addDrawableChild(nextBtn);

        // --- Done ---
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), btn -> close())
            .dimensions(width / 2 - 50, height - 30, 100, 20).build());

        rebuildList();
    }

    private void rebuildList() {
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
        int y = 95;
        for (int i = startIdx; i < endIdx; i++) {
            final int idx = i;
            ButtonWidget btn = ButtonWidget.builder(Text.literal("X"), b -> {
                if (idx < CantStopWinningClient.CONFIG.triggerMessages.size()) {
                    CantStopWinningClient.CONFIG.triggerMessages.remove(idx);
                    CantStopWinningClient.CONFIG.save();
                    rebuildList();
                }
            }).dimensions(width / 2 + 120, y, 20, 18).build();
            addDrawableChild(btn);
            removeButtons.add(btn);
            y += 20;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        // Title
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 14, 0xFFFFFFFF);

        // "Triggers:" label
        context.drawTextWithShadow(textRenderer, Text.literal("Triggers:"), width / 2 - 150, 85, 0xFFAAAAAA);

        // Trigger list with bordered rows
        List<String> msgs = CantStopWinningClient.CONFIG.triggerMessages;
        int startIdx = page * TRIGGERS_PER_PAGE;
        int endIdx = Math.min(startIdx + TRIGGERS_PER_PAGE, msgs.size());

        int listLeft = width / 2 - 152;
        int listRight = width / 2 + 143;
        int y = 95;
        int rowH = 20;

        for (int i = startIdx; i < endIdx; i++) {
            // Row background (dark fill)
            context.fill(listLeft, y, listRight, y + rowH, 0x80000000);
            // Border: top, bottom, left, right (thin gray lines)
            int border = 0xFF555555;
            context.fill(listLeft, y, listRight, y + 1, border);             // top
            context.fill(listLeft, y + rowH - 1, listRight, y + rowH, border); // bottom
            context.fill(listLeft, y, listLeft + 1, y + rowH, border);        // left
            context.fill(listRight - 1, y, listRight, y + rowH, border);      // right

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

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }
}
