/*
 * Copyright (C) 2021-2022 DarkKronicle
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.darkkronicle.advancedchatcore.chat;

import io.github.darkkronicle.advancedchatcore.util.CursorUtil;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.util.KeyCodes;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.darkkronicle.advancedchatcore.AdvancedChatCore;
import io.github.darkkronicle.advancedchatcore.config.ConfigStorage;
import io.github.darkkronicle.advancedchatcore.config.gui.GuiConfigHandler;
import io.github.darkkronicle.advancedchatcore.gui.ContextMenu;
import io.github.darkkronicle.advancedchatcore.gui.IconButton;
import io.github.darkkronicle.advancedchatcore.interfaces.AdvancedChatScreenSection;
import io.github.darkkronicle.advancedchatcore.util.Color;
import io.github.darkkronicle.advancedchatcore.util.RowList;
import lombok.Getter;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;

public class AdvancedChatScreen extends GuiBase {

    public static boolean PERMANENT_FOCUS = false;

    private String finalHistory = "";
    private int messageHistorySize = -1;
    private int startHistory = -1;
    private boolean passEvents = false;

    /** Chat field at the bottom of the screen */
    @Getter protected AdvancedTextField chatField;

    /** What the chat box started out with */
    @Getter private String originalChatText = "";

    private static String last = "";
    private final List<AdvancedChatScreenSection> sections = new ArrayList<>();

    @Getter
    private final RowList<ButtonBase> rightSideButtons = new RowList<>();

    @Getter
    private final RowList<ButtonBase> leftSideButtons = new RowList<>();

    private ContextMenu menu = null;

    @Override
    protected void closeGui(boolean showParent) {
        if (ConfigStorage.ChatScreen.PERSISTENT_TEXT.config.getBooleanValue()) {
            last = chatField.getText();
        }
        super.closeGui(showParent);
    }

    public AdvancedChatScreen() {
        super();
        setupSections();
    }

    public AdvancedChatScreen(boolean passEvents) {
        this();
        this.passEvents = passEvents;
    }

    public AdvancedChatScreen(int indexOfLast) {
        this();
        startHistory = indexOfLast;
    }

    public AdvancedChatScreen(String originalChatText) {
        this();
        this.originalChatText = originalChatText;
    }

    private void setupSections() {
        for (Function<AdvancedChatScreen, AdvancedChatScreenSection> supplier : ChatScreenSectionHolder.getInstance().getSectionSuppliers()) {
            AdvancedChatScreenSection section = supplier.apply(this);
            if (section != null) {
                sections.add(section);
            }
        }
    }

    private Color getColor() {
        return ConfigStorage.ChatScreen.COLOR.config.get();
    }

    public void resetCurrentMessage() {
        try {
            this.messageHistorySize = this.mc.gui.hud.getChat().getRecentChat().size(); //dont ask
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        if (passEvents) {
            return true;
        }
        return super.charTyped(input);
    }

    public void initGui() {
        super.initGui();
        this.rightSideButtons.clear();
        this.leftSideButtons.clear();
        resetCurrentMessage();
        this.chatField =
                new AdvancedTextField(
                        this.font,
                        4,
                        this.height - 12,
                        this.width - 10,
                        12,
                        Component.translatable("chat.editBox")) {
                    protected MutableComponent createNarrationMessage() {
                        return null;
                    }
                };
        if (ConfigStorage.ChatScreen.MORE_TEXT.config.getBooleanValue()) {
            this.chatField.setMaxLength(64000);
        } else {
            this.chatField.setMaxLength(256);
        }
        this.chatField.setBordered(false);
        if (!this.originalChatText.equals("")) {
            this.chatField.setText(this.originalChatText);
        } else if (ConfigStorage.ChatScreen.PERSISTENT_TEXT.config.getBooleanValue()
                && !last.equals("")) {
            this.chatField.setText(last);
        }
        this.chatField.setResponder(this::onChatFieldUpdate);

        // Add settings button
        rightSideButtons.add("settings", new IconButton(0, 0, 14, 64, Identifier.fromNamespaceAndPath(AdvancedChatCore.MOD_ID, "textures/gui/settings.png"), (button) -> GuiBase.openGui(GuiConfigHandler.getInstance().getDefaultScreen())));

        this.addWidget(this.chatField);

        this.setInitialFocus(this.chatField);

        for (AdvancedChatScreenSection section : sections) {
            section.initGui();
        }

        int originalX = mc.getWindow().getGuiScaledWidth() - 1;
        int y = mc.getWindow().getGuiScaledHeight() - 30;
        for (int i = 0; i < rightSideButtons.rowSize(); i++) {
            List<ButtonBase> buttonList = rightSideButtons.get(i);
            int maxHeight = 0;
            int x = originalX;
            for (ButtonBase button : buttonList) {
                maxHeight = Math.max(maxHeight, button.getHeight());
                x -= button.getWidth() + 1;
                button.setPosition(x, y);
                addButton(button, null);
            }
            y -= maxHeight + 1;
        }
        originalX = 1;
        y = mc.getWindow().getGuiScaledHeight() - 30;
        for (int i = 0; i < leftSideButtons.rowSize(); i++) {
            List<ButtonBase> buttonList = leftSideButtons.get(i);
            int maxHeight = 0;
            int x = originalX;
            for (ButtonBase button : buttonList) {
                maxHeight = Math.max(maxHeight, button.getHeight());
                button.setPosition(x, y);
                addButton(button, null);
                x += button.getWidth() + 1;
            }
            y -= maxHeight + 1;
        }
        if (startHistory >= 0) {
            setChatFromHistory(-startHistory - 1);
        }

    }

    public void resize(int width, int height) {
        String string = this.chatField.getText();
        this.init(width, height);
        this.setText(string);
        for (AdvancedChatScreenSection section : sections) {
            section.resize(width, height);
        }
    }

    @Override
    public void removed() {
        for (AdvancedChatScreenSection section : sections) {
            section.removed();
        }
    }

    public void tick() {
        this.chatField.tick();
    }

    private void onChatFieldUpdate(String chatText) {
        String string = this.chatField.getText();
        for (AdvancedChatScreenSection section : sections) {
            section.onChatFieldUpdate(chatText, string);
        }
    }

    @Override
    public boolean keyReleased(KeyEvent input) {
        if (passEvents) {
            InputConstants.Key key = InputConstants.getKey(input);
            KeyMapping.set(key, false);
        }
        return false;
    }

    public boolean keyPressed(KeyEvent input) {
        int keyCode = input.key();
        int scanCode = input.scancode();
        int modifiers = input.modifiers();
        if (!passEvents) {
            for (AdvancedChatScreenSection section : sections) {
                if (section.keyPressed(keyCode, scanCode, modifiers)) {
                    return true;
                }
            }

            if (super.keyPressed(input)) {
                return true;
            }
        }
        if (keyCode == KeyCodes.KEY_ESCAPE) {
            // Exit out
            GuiBase.openGui(null);
            return true;
        }
        if (keyCode == KeyCodes.KEY_ENTER || keyCode == KeyCodes.KEY_KP_ENTER) {
            String string = this.chatField.getText().trim();
            // Strip message and send
            MessageSender.getInstance().sendMessage(string);
            this.chatField.setText("");
            last = "";
            // Exit
            GuiBase.openGui(null);
            return true;
        }
        if (keyCode == KeyCodes.KEY_UP) {
            // Go through previous history
            this.setChatFromHistory(-1);
            return true;
        }
        if (keyCode == KeyCodes.KEY_DOWN) {
            // Go through previous history
            this.setChatFromHistory(1);
            return true;
        }
        if (keyCode == KeyCodes.KEY_PAGE_UP) {
            // Scroll
            mc.gui.hud
                    .getChat()
                    .scrollChat(this.mc.gui.hud.getChat().getLinesPerPage() - 1);
            return true;
        }
        if (keyCode == KeyCodes.KEY_PAGE_DOWN) {
            // Scroll
            mc.gui.hud
                    .getChat()
                    .scrollChat(-this.mc.gui.hud.getChat().getLinesPerPage() + 1);
            return true;
        }
        if (passEvents) {
            this.chatField.setText("");
            InputConstants.Key key = InputConstants.getKey(input);
            KeyMapping.set(key, true);
            KeyMapping.click(key);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount > 1.0D) {
            verticalAmount = 1.0D;
        }

        if (verticalAmount < -1.0D) {
            verticalAmount = -1.0D;
        }

        for (AdvancedChatScreenSection section : sections) {
            if (section.mouseScrolled(mouseX, mouseY, verticalAmount)) {
                return true;
            }
        }
        if (!Minecraft.getInstance().hasShiftDown()) {
            verticalAmount *= 7.0D;
        }

        // Send to hud to scroll
        mc.gui.hud.getChat().scrollChat((int) verticalAmount);
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubleClick) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.buttonInfo().button();
        if (menu != null && menu.onMouseClicked(click, doubleClick)) {
            return true;
        }
        for (AdvancedChatScreenSection section : sections) {
            if (section.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        if (button == 1 && this.chatField.isMouseOverText(mouseX, mouseY)) {
            createContextMenu((int) mouseX, (int) mouseY);
            return true;
        }
        Style style = getTextStyleAt(mouseX, mouseY);
        if (handleTextClick(style)) {
            return true;
        }
        if (this.chatField.mouseClicked(click, doubleClick)) {
            setFocused(this.chatField);
            if (button == 0) {
                setDragging(true);
            }
            return true;
        }
        return super.mouseClicked(click, doubleClick);
    }


    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.buttonInfo().button();

        for (AdvancedChatScreenSection section : sections) {
            if (section.mouseReleased(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        for (AdvancedChatScreenSection section : sections) {
            if (section.mouseDragged(mouseX, mouseY, button, offsetX, offsetY)) {
                return true;
            }
        }

        return super.mouseDragged(click, offsetX, offsetY);
    }


    @Override
    protected void insertText(String text, boolean override) {
        if (override) {
            this.chatField.setText(text);
        } else {
            this.chatField.insertText(text);
        }
    }

    public void setChatFromHistory(int i) {
        int targetIndex = this.messageHistorySize + i;
        int maxIndex = this.mc.gui.hud.getChat().getRecentChat().size();
        targetIndex = Mth.clamp(targetIndex, 0, maxIndex);
        if (targetIndex != this.messageHistorySize) {
            if (targetIndex == maxIndex) {
                this.messageHistorySize = maxIndex;
                this.chatField.setText(this.finalHistory);
            } else {
                if (this.messageHistorySize == maxIndex) {
                    this.finalHistory = this.chatField.getText();
                }

                String hist = this.mc.gui.hud.getChat().getRecentChat().get(targetIndex);
                this.chatField.setText(hist);
                for (AdvancedChatScreenSection section : sections) {
                    section.setChatFromHistory(hist);
                }
                this.messageHistorySize = targetIndex;
            }
        }
    }

    public void createContextMenu(int mouseX, int mouseY) {
        LinkedHashMap<Component, ContextMenu.ContextConsumer> actions = new LinkedHashMap<>();
        String highlighted = this.chatField.getHighlighted();
        if (!highlighted.isEmpty()) {
            actions.put(Component.literal(StringUtils.translate("advancedchatcore.context.cut")), (x, y) -> {
                this.mc.keyboardHandler.setClipboard(highlighted);
                this.chatField.insertText("");
            });
            actions.put(Component.literal(StringUtils.translate("advancedchatcore.context.copy")), (x, y) ->
                    this.mc.keyboardHandler.setClipboard(highlighted));
        }
        actions.put(Component.literal(StringUtils.translate("advancedchatcore.context.paste")), (x, y) ->
                this.chatField.insertText(this.mc.keyboardHandler.getClipboard()));
        menu = new ContextMenu(mouseX, mouseY, actions, () -> menu = null);
    }

    public boolean handleTextClick(Style style) {
        if (style == null || style.getClickEvent() == null) {
            return false;
        }
        defaultHandleGameClickEvent(style.getClickEvent(), this.mc, this);
        return true;
    }

    public Style getTextStyleAt(double mouseX, double mouseY) {
        ActiveTextCollector.ClickableStyleFinder finder =
                new ActiveTextCollector.ClickableStyleFinder(this.font, (int) mouseX, (int) mouseY)
                        .includeInsertions(Minecraft.getInstance().hasShiftDown());
        this.mc.gui.hud.getChat().captureClickableText(
                finder,
                this.mc.getWindow().getGuiScaledHeight(),
                this.mc.gui.hud.getGuiTicks(),
                ChatComponent.DisplayMode.FOREGROUND);
        return finder.result();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float partialTicks) {
        this.mc.gui.hud.getChat().extractRenderState(
                context,
                this.font,
                this.mc.gui.hud.getGuiTicks(),
                mouseX,
                mouseY,
                ChatComponent.DisplayMode.FOREGROUND,
                Minecraft.getInstance().hasShiftDown());
        this.setFocused(this.chatField);
        this.chatField.setFocused(true);
        this.chatField.extractRenderState(context, mouseX, mouseY, partialTicks);
        super.extractRenderState(context, mouseX, mouseY, partialTicks);
        for (AdvancedChatScreenSection section : sections) {
            section.extractRenderState(context, mouseX, mouseY, partialTicks);
        }
        Style style = getTextStyleAt(mouseX, mouseY);
        if (style != null && (style.getClickEvent() != null
                || (Minecraft.getInstance().hasShiftDown() && style.getInsertion() != null))) {
            CursorUtil.requestCursor(context, CursorTypes.POINTING_HAND);
        }
        if (style != null && style.getHoverEvent() != null) {
            HoverEvent hover = style.getHoverEvent();
            if (hover instanceof HoverEvent.ShowItem showItem) {
                context.setTooltipForNextFrame(this.font, showItem.item().create(), mouseX, mouseY);
            } else if (hover instanceof HoverEvent.ShowEntity showEntity) {
                if (this.mc.options.advancedItemTooltips) {
                    context.setComponentTooltipForNextFrame(this.font, showEntity.entity().getTooltipLines(), mouseX, mouseY);
                }
            } else if (hover instanceof HoverEvent.ShowText showText) {
                context.setTooltipForNextFrame(this.font, showText.value(), mouseX, mouseY);
            }
            //this.renderTextHoverEffect(context, style, mouseX, mouseY);
        }
        if (menu != null) {
            menu.render(GuiContext.fromGuiGraphics(context), mouseX, mouseY, true);
        }
    }

    @Override
    protected void drawScreenBackground(GuiContext drawContext, int mouseX, int mouseY) {

    }

    private void setText(String text) {
        this.chatField.setText(text);
    }
}
