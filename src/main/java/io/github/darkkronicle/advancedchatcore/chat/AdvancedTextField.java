/*
 * Copyright (C) 2021 DarkKronicle
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.darkkronicle.advancedchatcore.chat;

import io.github.darkkronicle.advancedchatcore.util.CursorUtil;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import fi.dy.masa.malilib.util.KeyCodes;
import io.github.darkkronicle.advancedchatcore.config.ConfigStorage;
import io.github.darkkronicle.advancedchatcore.util.StringMatch;
import io.github.darkkronicle.advancedchatcore.util.StyleFormatter;
import io.github.darkkronicle.advancedchatcore.util.TextBuilder;
import io.github.darkkronicle.advancedchatcore.util.TextUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class AdvancedTextField extends EditBox {

    private final static int MAX_HISTORY = 50;

    /**
     * Stores the last saved snapshot of the box. This ensures that not every character update is
     * put in, but instead groups.
     */
    private String lastSaved = "";

    /** Snapshots of chat box */
    private final List<String> history = new ArrayList<>();

    private int focusedTicks = 0;
    private List<Component> renderLines = new ArrayList<>();
    private Font textRenderer;
    private String suggestion = null;
    private int maxLength = 32;
    private int selectionEnd;
    private int selectionStart;
    // TODO Split?
    private BiFunction<String, Integer, FormattedCharSequence> renderTextProvider = (string, firstCharacterIndex) -> FormattedCharSequence.forward(string, Style.EMPTY);

    private int historyIndex = -1;

    public AdvancedTextField(Font textRenderer, int x, int y, int width, int height, Component text) {
        this(textRenderer, x, y, width, height, null, text);
    }

    public AdvancedTextField(
            Font textRenderer,
            int x,
            int y,
            int width,
            int height,
            @Nullable EditBox copyFrom,
            Component text) {
        super(textRenderer, x, y, width, height, copyFrom, text);
        history.add("");
        this.textRenderer = textRenderer;
        updateRender();
    }

    //@Override
    public void tick() {
        focusedTicks++;
    }

    public void setRenderTextProvider(BiFunction<String, Integer, FormattedCharSequence> renderTextProvider) {
        this.renderTextProvider = renderTextProvider;
    }


    protected FormattedCharSequence getRenderableText() {
        return this.renderTextProvider.apply(this.getText(), 0);
    }

    @Override
    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
        super.setMaxLength(maxLength);
    }

    public static boolean isUndo(int code) {
        // Undo (Ctrl + Z)
        return code == KeyCodes.KEY_Z && Minecraft.getInstance().hasControlDown() && !Minecraft.getInstance().hasAltDown();
    }

    /** Triggers undo for the text box */
    public void undo() {
        // Save the current snapshot if it's been edited
        if (!this.lastSaved.equals(this.getText()) && historyIndex < 0) {
            addToHistory(getText());
        }
        // History index < 0 means not in the middle of undoing
        if (historyIndex < 0) {
            historyIndex = history.size() - 1;
        }
        // Check that we're not at index 0
        if (historyIndex != 0) {
            historyIndex--;
        }
        // Set the text but don't update
        setText(history.get(historyIndex), false);
    }

    public void redo() {
        if (historyIndex < 0 || historyIndex >= history.size() - 1) {
            // No stuff to redo...
            return;
        }
        historyIndex++;
        setText(history.get(historyIndex), false);
    }

    @Override
    public void insertText(String text) {
        super.insertText(text);
        updateHistory();
        updateRender();
    }

    @Override
    public void deleteCharsToPos(int pos) {
        super.deleteCharsToPos(pos);
        updateHistory();
        updateRender();
    }

    @Override
    public void setSuggestion(@Nullable String suggestion) {
        this.suggestion = suggestion;
    }

    public boolean isMouseOverText(double mouseX, double mouseY) {
        int renderY = getY() - (renderLines.size() - 1) * (textRenderer.lineHeight + 2);
        return mouseY >= renderY - 2
                && mouseY <= getY() + height + 2
                && mouseX >= getX() - 2
                && mouseX <= getX() + width + 4;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        if (!isMouseOverText(mouseX, mouseY)) {
            return false;
        }
        if (!this.isActive()) {
            return false;
        }
        if (this.isValidClickButton(click.buttonInfo())) {
            boolean inside = this.isMouseOver(mouseX, mouseY);
            if (inside) {
                this.playDownSound(Minecraft.getInstance().getSoundManager());
                this.onClick(click, doubled);
                return true;
            }
        }
        return false;
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        int color = 0xFFE0E0E0;
        int cursor = getCursorPosition();
        int cursorRow = renderLines.size() - 1;
        boolean renderCursor = this.isFocused() && focusedTicks / 6 % 2 == 0;
        int renderY = getY() - (renderLines.size() - 1) * (textRenderer.lineHeight + 2);
        int endX = 0;
        int charCount = 0;
        int cursorX = -1;
        boolean selection = selectionStart != selectionEnd;
        boolean started = false;
        boolean ended = false;
        int selStart;
        int selEnd;
        if (this.selectionStart < this.selectionEnd) {
            selStart = this.selectionStart;
            selEnd = this.selectionEnd;
        } else {
            selStart = this.selectionEnd;
            selEnd = this.selectionStart;
        }
        int x = getX();
        int y = getY()    ;
        context.fill(getX() - 2, renderY - 2, getX() + width + 4, getY() + height + 4, ConfigStorage.ChatScreen.COLOR.config.get().color());
        for (int line = 0; line < renderLines.size(); line++) {
            Component text = renderLines.get(line);
            if (cursor >= charCount && cursor < text.getString().length() + charCount) {
                cursorX = textRenderer.width(text.getString().substring(0, cursor - charCount));
                cursorRow = line;
            }
            endX = textRenderer.width(text);
            context.text(textRenderer, text, x, renderY, color);
            if (selection) {
                if (!started && selStart >= charCount && selStart <= text.getString().length() + charCount) {
                    started = true;
                    int startX = textRenderer.width(TextUtil.truncate(text, new StringMatch("", 0, selStart - charCount)));
                    if (selEnd > charCount && selEnd <= text.getString().length() + charCount) {
                        ended = true;
                        int sEndX = textRenderer.width(TextUtil.truncate(text, new StringMatch("", 0, selEnd - charCount)));
                        drawSelectionHighlight(context,x + startX, renderY - 1, x + sEndX, renderY + textRenderer.lineHeight + 1);
                    } else {
                        int sEndX = textRenderer.width(text);
                        drawSelectionHighlight(context, x + startX, renderY - 1, x + sEndX, renderY + textRenderer.lineHeight + 1);
                    }
                } else if (started && !ended) {
                    if (selEnd >= charCount && selEnd <= text.getString().length() + charCount) {
                        ended = true;
                        int sEndX = textRenderer.width(TextUtil.truncate(text, new StringMatch("", 0, selEnd - charCount)));
                        drawSelectionHighlight(context, x, renderY - 1, x + sEndX, renderY + textRenderer.lineHeight + 1);
                    } else {
                        int sEndX = textRenderer.width(text);
                        drawSelectionHighlight(context, x, renderY - 1, x + sEndX, renderY + textRenderer.lineHeight + 1);
                    }
                }
            }
            renderY += textRenderer.lineHeight + 2;
            charCount += text.getString().length();
        }
        if (cursorX < 0) {
            cursorX = endX;
        }
        boolean cursorAtEnd = getCursorPosition() == getText().length();
        if (!cursorAtEnd && this.suggestion != null) {
            context.text(textRenderer, this.suggestion, x + endX - 1, y, -8355712);
        }
        if (renderCursor) {
            int cursorY = y - (renderLines.size() - 1 - cursorRow) * (textRenderer.lineHeight + 2);
            if (cursorAtEnd) {
                context.fill(x + cursorX, cursorY - 1, x + cursorX + 1, cursorY + 1 + this.textRenderer.lineHeight, -3092272);
            } else {
                context.text(textRenderer, "_", x + cursorX, cursorY, color);
            }
        }
        if (isMouseOverText(mouseX, mouseY)) {
            CursorUtil.requestCursor(context, CursorTypes.IBEAM);
        }
    }

    private void drawSelectionHighlight(GuiGraphicsExtractor context, int x1, int y1, int x2, int y2) {
        int x = getX();
        int y = getY();
        if (x1 > x2) {
            int t = x1;
            x1 = x2;
            x2 = t;
        }
        if (y1 > y2) {
            int t = y1;
            y1 = y2;
            y2 = t;
        }
        if (x2 > x + this.width) x2 = x + this.width;
        if (x1 > x + this.width) x1 = x + this.width;
        context.textHighlight(x1, y1, x2, y2, true);
    }

    @Override
    public void setCursorPosition(int cursor) {
        this.selectionStart = Mth.clamp(cursor, 0, getText().length());
        super.setCursorPosition(cursor);
    }

    @Override
    public void setHighlightPos(int index) {
        int i = getText().length();
        this.selectionEnd = Mth.clamp(index, 0, i);
        super.setHighlightPos(index);
    }

    @Override
    public NarrationPriority narrationPriority() {
        return super.narrationPriority();
    }

    public String getText() {
        return getValue();
    }

    /**
     * Sets the text for the text field
     *
     * @param text Text to set
     * @param update Updates the history
     */
    public void setText(String text, boolean update) {
        // Wrapper class for setText
        super.setValue(text);
        if (update) {
            updateHistory();
        }
        updateRender();
    }

    public void setText(String text) {
        setText(text, true);
    }

    @Override
    public void setValue(String text) {
        setText(text, true);
    }

    private void updateRender() {
        FormattedCharSequence formatted = renderTextProvider.apply(getText(), 0);
        renderLines = StyleFormatter.wrapText(textRenderer, getWidth(), new TextBuilder().append(formatted).build());
    }

    private void updateHistory() {
        if (historyIndex >= 0) {
            // Remove all history after what has gone back
            pruneHistory(historyIndex + 1);
            historyIndex = -1;
        }
        // Check to see if it should log
        int dif = getText().length() - lastSaved.length();
        double sim = TextUtil.similarity(getText(), lastSaved);
        if (sim >= .3 && (dif < 5 && dif * -1 < 5) || (sim >= .9)) {
            return;
        }
        addToHistory(getText());
    }

    private void addToHistory(String text) {
        this.lastSaved = text;
        this.history.add(text);
        while (this.history.size() > MAX_HISTORY) {
            this.history.remove(0);
        }
    }

    /**
     * Remove's all history past a certain index
     *
     * @param index Index to prune from. Non-inclusive.
     */
    private void pruneHistory(int index) {
        if (index == 0) {
            history.clear();
            return;
        }
        while (history.size() > index) {
            history.remove(history.size() - 1);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (!this.isActive() || !this.isFocused()) {
            return false;
        }
        int keyCode = input.key();
        if (isUndo(keyCode)) {
            if (Minecraft.getInstance().hasShiftDown()) {
                redo();
            } else {
                undo();
            }
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput builder) {
        // Crashes here because Text is null
    }
}
