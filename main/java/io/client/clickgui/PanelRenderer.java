package io.client.clickgui;

import io.client.clickgui.screens.ClickGuiScreen;
import io.client.managers.ModuleManager;
import io.client.modules.templates.Category;
import io.client.modules.templates.Module;
import io.client.settings.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public class PanelRenderer {
    private static int PANEL_WIDTH = 102;
    private static int MODULE_HEIGHT = 12;
    private static int SETTING_HEIGHT = 13;
    private static int TITLE_BAR_HEIGHT = 15;
    private static int CONTENT_PADDING = 4;
    private static int ROW_GAP = 2;
    private static float SCALE = 1.0f;

    private final Map<String, Float> textScrollOffsets = new HashMap<>();
    private Theme theme;

    public PanelRenderer(Theme theme) {
        this.theme = theme;
    }

    public static void setScale(float scale) {
        SCALE = scale;
        PANEL_WIDTH = (int) (102 * scale);
        MODULE_HEIGHT = (int) (12 * scale);
        SETTING_HEIGHT = (int) (13 * scale);
        TITLE_BAR_HEIGHT = (int) (15 * scale);
        CONTENT_PADDING = Math.max(3, (int) (4 * scale));
        ROW_GAP = Math.max(1, (int) (2 * scale));
    }

    public static int getPanelWidth() {
        return PANEL_WIDTH;
    }

    public static int getTitleBarHeight() {
        return TITLE_BAR_HEIGHT;
    }

    public static int getModuleHeight() {
        return MODULE_HEIGHT;
    }

    public static int getSettingHeight() {
        return SETTING_HEIGHT;
    }

    public static int getContentPadding() {
        return CONTENT_PADDING;
    }

    public static int getRowGap() {
        return ROW_GAP;
    }

    public void setTheme(Theme theme) {
        this.theme = theme;
    }

    public String renderPanel(DrawContext graphics, TextRenderer font, CategoryPanel panel, int mouseX, int mouseY) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();
        panel.x = Math.max(0, Math.min(panel.x, screenWidth - PANEL_WIDTH));
        panel.y = Math.max(0, Math.min(panel.y, screenHeight - TITLE_BAR_HEIGHT));

        List<Module> modules = ModuleManager.INSTANCE.getModulesByCategory(panel.category);
        int contentHeight = calculateContentHeight(modules, panel.collapsed);
        int actualHeight = panel.collapsed ? TITLE_BAR_HEIGHT : TITLE_BAR_HEIGHT + CONTENT_PADDING + contentHeight;

        renderTitleBar(graphics, font, panel, mouseX, mouseY);

        String hoveredDescription = null;
        if (!panel.collapsed) {
            renderPanelBackground(graphics, panel, actualHeight);
            hoveredDescription = renderModules(graphics, font, panel, modules, mouseX, mouseY);
        }

        return hoveredDescription;
    }

    private int calculateContentHeight(List<Module> modules, boolean collapsed) {
        if (collapsed) return 0;

        int height = 0;
        for (Module module : modules) {
            height += MODULE_HEIGHT;
            if (module.isExtended()) {
                for (Setting setting : module.getSettings()) {
                    height += SETTING_HEIGHT + ROW_GAP;
                    if (setting instanceof CategorySetting catSetting && catSetting.isExpanded()) {
                        height += catSetting.getSettings().size() * (SETTING_HEIGHT + ROW_GAP);
                    }
                }
            }
            height += ROW_GAP;
        }
        return Math.max(0, height - ROW_GAP + CONTENT_PADDING);
    }

    private void renderTitleBar(DrawContext graphics, TextRenderer font, CategoryPanel panel, int mouseX, int mouseY) {
        graphics.fill(panel.x, panel.y, panel.x + PANEL_WIDTH, panel.y + TITLE_BAR_HEIGHT, theme.titleBar);
        graphics.fill(panel.x, panel.y, panel.x + PANEL_WIDTH, panel.y + 1, 0x55FFFFFF);
        graphics.fill(panel.x, panel.y + TITLE_BAR_HEIGHT - 1, panel.x + PANEL_WIDTH, panel.y + TITLE_BAR_HEIGHT,
                0x22000000 | (theme.moduleEnabled & 0x00FFFFFF));

        graphics.getMatrices().pushMatrix();
        graphics.getMatrices().translate(panel.x + 5 * SCALE, panel.y + 3 * SCALE);
        graphics.getMatrices().scale(SCALE, SCALE);
        drawGuiText(graphics, font, panel.category.name(), 0, 0, 0xFFFFFFFF, false);
        graphics.getMatrices().popMatrix();

        int collapseX = panel.x + PANEL_WIDTH - (int)(11 * SCALE);
        graphics.getMatrices().pushMatrix();
        graphics.getMatrices().translate(collapseX, panel.y + 3 * SCALE);
        graphics.getMatrices().scale(SCALE, SCALE);
        drawGuiText(graphics, font, panel.collapsed ? "+" : "-", 0, 0, 0xFFAAAAAA, false);
        graphics.getMatrices().popMatrix();

        if (panel.category == Category.COMBAT) {
            int targetsX = panel.x + PANEL_WIDTH - (int)(28 * SCALE);
            graphics.getMatrices().pushMatrix();
            graphics.getMatrices().translate(targetsX, panel.y + 2 * SCALE);
            graphics.getMatrices().scale(SCALE, SCALE);
            graphics.getMatrices().popMatrix();
        }
    }

    private void renderPanelBackground(DrawContext graphics, CategoryPanel panel, int actualHeight) {
        graphics.fill(panel.x, panel.y + TITLE_BAR_HEIGHT, panel.x + PANEL_WIDTH, panel.y + actualHeight, theme.panelBackground);
        graphics.fill(panel.x, panel.y + TITLE_BAR_HEIGHT, panel.x + PANEL_WIDTH, panel.y + TITLE_BAR_HEIGHT + 1, 0x22FFFFFF);
        graphics.fill(panel.x, panel.y + actualHeight - 1, panel.x + PANEL_WIDTH, panel.y + actualHeight, 0x20000000);
        graphics.fill(panel.x, panel.y, panel.x + 1, panel.y + actualHeight, 0x18000000);
        graphics.fill(panel.x + PANEL_WIDTH - 1, panel.y, panel.x + PANEL_WIDTH, panel.y + actualHeight, 0x18000000);
    }

    private String renderModules(DrawContext graphics, TextRenderer font, CategoryPanel panel, List<Module> modules, int mouseX, int mouseY) {
        String hoveredDescription = null;
        int yOffset = panel.y + TITLE_BAR_HEIGHT + CONTENT_PADDING;

        for (Module module : modules) {
            String desc = renderModule(graphics, font, panel, module, yOffset, mouseX, mouseY);
            if (desc != null) hoveredDescription = desc;

            yOffset += MODULE_HEIGHT + ROW_GAP;
            if (module.isExtended()) {
                yOffset = renderSettings(graphics, font, panel, module.getSettings(), yOffset, mouseX, mouseY);
            }
        }

        return hoveredDescription;
    }

    private String renderModule(DrawContext graphics, TextRenderer font, CategoryPanel panel, Module module, int yOffset, int mouseX, int mouseY) {
        int moduleColor = module.isEnabled() ? theme.moduleEnabled : theme.moduleDisabled;
        int inset = Math.max(2, (int) (2 * SCALE));
        int x1 = panel.x + inset, y1 = yOffset, x2 = panel.x + PANEL_WIDTH - inset, y2 = yOffset + MODULE_HEIGHT;

        boolean isModuleHovered = mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2;
        if (isModuleHovered) {
            graphics.fill(x1, y1, x2, y2, theme.hoverHighlight);
        }

        if (module.isEnabled()) {
            graphics.fill(x1, y1, x1 + Math.max(1, (int) SCALE), y2, theme.moduleEnabled);
        }

        if (!module.getSettings().isEmpty()) {
            String indicator = module.isExtended() ? "^" : ">";
            graphics.getMatrices().pushMatrix();
            graphics.getMatrices().translate(panel.x + PANEL_WIDTH - 11 * SCALE, yOffset + 2 * SCALE);
            graphics.getMatrices().scale(SCALE, SCALE);
            drawGuiText(graphics, font, indicator, 0, 0, 0xFF888888, false);
            graphics.getMatrices().popMatrix();
        }

        graphics.getMatrices().pushMatrix();
        graphics.getMatrices().translate(panel.x + 6 * SCALE, yOffset + 2 * SCALE);
        graphics.getMatrices().scale(SCALE, SCALE);
        drawGuiText(graphics, font, module.getName(), 0, 0, moduleColor, false);
        graphics.getMatrices().popMatrix();

        return isModuleHovered ? module.getDescription() : null;
    }

    private int renderSettings(DrawContext graphics, TextRenderer font, CategoryPanel panel, List<Setting> settings, int yOffset, int mouseX, int mouseY) {
        for (Setting setting : settings) {
            if (setting instanceof CategorySetting catSetting) {
                yOffset = renderCategorySetting(graphics, font, panel, catSetting, yOffset, mouseX, mouseY);
            } else if (setting instanceof BooleanSetting boolSetting) {
                yOffset = renderBooleanSetting(graphics, font, panel, boolSetting, yOffset, mouseX, mouseY, 3);
            } else if (setting instanceof NumberSetting numSetting) {
                yOffset = renderNumberSetting(graphics, font, panel, numSetting, yOffset, mouseX, mouseY, 3);
            } else if (setting instanceof StringSetting strSetting) {
                yOffset = renderStringSetting(graphics, font, panel, strSetting, yOffset, mouseX, mouseY);
            } else if (setting instanceof RadioSetting radioSetting) {
                yOffset = renderRadioSetting(graphics, font, panel, radioSetting, yOffset, mouseX, mouseY, 3);
            }
        }
        return yOffset;
    }

    private int renderCategorySetting(DrawContext graphics, TextRenderer font, CategoryPanel panel, CategorySetting catSetting, int yOffset, int mouseX, int mouseY) {
        int inset = Math.max(2, (int) (2 * SCALE));
        int settingX1 = panel.x + inset, settingY1 = yOffset, settingX2 = panel.x + PANEL_WIDTH - inset, settingY2 = yOffset + SETTING_HEIGHT;

        boolean isSettingHovered = mouseX >= settingX1 && mouseX <= settingX2 && mouseY >= settingY1 && mouseY <= settingY2;
        if (isSettingHovered) {
            graphics.fill(settingX1, settingY1, settingX2, settingY2, theme.hoverHighlight);
        }

        String catIndicator = catSetting.isExpanded() ? "^" : ">";
        graphics.getMatrices().pushMatrix();
        graphics.getMatrices().translate(panel.x + PANEL_WIDTH - 11 * SCALE, yOffset + 2 * SCALE);
        graphics.getMatrices().scale(SCALE, SCALE);
        drawGuiText(graphics, font, catIndicator, 0, 0, theme.moduleDisabled, false);
        graphics.getMatrices().popMatrix();

        graphics.getMatrices().pushMatrix();
        graphics.getMatrices().translate(panel.x + 6 * SCALE, yOffset + 2 * SCALE);
        graphics.getMatrices().scale(SCALE, SCALE);
        drawGuiText(graphics, font, catSetting.getName(), 0, 0, theme.moduleEnabled, false);
        graphics.getMatrices().popMatrix();

        yOffset += SETTING_HEIGHT + ROW_GAP;

        if (catSetting.isExpanded()) {
            for (Object catItem : catSetting.getSettings()) {
                int catItemY1 = yOffset, catItemY2 = yOffset + SETTING_HEIGHT;

                boolean isCatItemHovered = mouseX >= panel.x + inset && mouseX <= panel.x + PANEL_WIDTH - inset && mouseY >= catItemY1 && mouseY <= catItemY2;
                if (isCatItemHovered) {
                    graphics.fill(panel.x + inset, catItemY1, panel.x + PANEL_WIDTH - inset, catItemY2, theme.hoverHighlight);
                }

                if (catItem instanceof BooleanSetting boolSetting) {
                    renderBooleanSetting(graphics, font, panel, boolSetting, yOffset, mouseX, mouseY, 8);
                    yOffset += SETTING_HEIGHT + ROW_GAP;
                } else if (catItem instanceof NumberSetting numSetting) {
                    renderNumberSetting(graphics, font, panel, numSetting, yOffset, mouseX, mouseY, 8);
                    yOffset += SETTING_HEIGHT + ROW_GAP;
                } else if (catItem instanceof StringSetting strSetting) {
                    String displayValue = strSetting.getValue();
                    String label = strSetting.getName() + ": \"" + displayValue + "\"";
                    graphics.getMatrices().pushMatrix();
                    graphics.getMatrices().translate(panel.x + 10 * SCALE, yOffset + 2 * SCALE);
                    graphics.getMatrices().scale(SCALE, SCALE);
                    drawGuiText(graphics, font, label, 0, 0, 0xFFFFFFFF, false);
                    graphics.getMatrices().popMatrix();
                    yOffset += SETTING_HEIGHT + ROW_GAP;
                } else if (catItem instanceof RadioSetting radioSetting) {
                    renderRadioSetting(graphics, font, panel, radioSetting, yOffset, mouseX, mouseY, 8);
                    yOffset += SETTING_HEIGHT + ROW_GAP;
                }
            }
        }

        return yOffset;
    }

    private int renderBooleanSetting(DrawContext graphics, TextRenderer font, CategoryPanel panel, BooleanSetting boolSetting, int yOffset, int mouseX, int mouseY, int indent) {
        int inset = Math.max(2, (int) (2 * SCALE));
        int settingX1 = panel.x + inset, settingY1 = yOffset, settingX2 = panel.x + PANEL_WIDTH - inset, settingY2 = yOffset + SETTING_HEIGHT;

        boolean isSettingHovered = mouseX >= settingX1 && mouseX <= settingX2 && mouseY >= settingY1 && mouseY <= settingY2;
        if (isSettingHovered && indent == 3) {
            graphics.fill(settingX1, settingY1, settingX2, settingY2, theme.hoverHighlight);
        }

        int textColor = boolSetting.isEnabled() ? theme.moduleEnabled : 0xFFFFFFFF;
        float indicatorX = panel.x + (indent + 2) * SCALE;
        float indicatorWidth = 0;

        String settingName = boolSetting.getName();
        int maxWidth = (int)((PANEL_WIDTH - indicatorWidth - indent * SCALE - 6 * SCALE) / SCALE);

        if (font.getWidth(settingName) > maxWidth) {
            String key = "bool_" + indent + "_" + boolSetting.getName();
            if (isSettingHovered) {
                float offset = textScrollOffsets.getOrDefault(key, 0f);
                offset += 0.5f;
                float textWidth = font.getWidth(settingName + "  " + settingName + "  ");
                if (offset > textWidth) offset = 0;
                textScrollOffsets.put(key, offset);
            } else {
                textScrollOffsets.put(key, 0f);
            }

            float offset = textScrollOffsets.get(key);
            String scrollText = settingName + "  " + settingName + "  " + settingName;

            int scissorX1 = (int)(indicatorX + indicatorWidth + 4 * SCALE);
            int scissorX2 = (int)(panel.x + PANEL_WIDTH - 6 * SCALE);
            graphics.enableScissor(scissorX1, yOffset, scissorX2, yOffset + SETTING_HEIGHT);
            graphics.getMatrices().pushMatrix();
            graphics.getMatrices().translate(indicatorX + indicatorWidth + 4 * SCALE - offset * SCALE, yOffset + 3 * SCALE);
            graphics.getMatrices().scale(SCALE, SCALE);
            drawGuiText(graphics, font, scrollText, 0, 0, textColor, false);
            graphics.getMatrices().popMatrix();
            graphics.disableScissor();
        } else {
            graphics.getMatrices().pushMatrix();
            graphics.getMatrices().translate(indicatorX + indicatorWidth + 4 * SCALE, yOffset + 3 * SCALE);
            graphics.getMatrices().scale(SCALE, SCALE);
            drawGuiText(graphics, font, settingName, 0, 0, textColor, false);
            graphics.getMatrices().popMatrix();
        }

        return yOffset + SETTING_HEIGHT + ROW_GAP;
    }

    private int renderNumberSetting(DrawContext graphics, TextRenderer font, CategoryPanel panel, NumberSetting numSetting, int yOffset, int mouseX, int mouseY, int indent) {
        int inset = Math.max(2, (int) (2 * SCALE));
        int settingX1 = panel.x + inset, settingY1 = yOffset, settingX2 = panel.x + PANEL_WIDTH - inset, settingY2 = yOffset + SETTING_HEIGHT;

        boolean isSettingHovered = mouseX >= settingX1 && mouseX <= settingX2 && mouseY >= settingY1 && mouseY <= settingY2;
        if (isSettingHovered && indent == 3) {
            graphics.fill(settingX1, settingY1, settingX2, settingY2, theme.hoverHighlight);
        }

        float range = numSetting.getMax() - numSetting.getMin();
        int barWidth = (int) ((numSetting.getValue() - numSetting.getMin()) / range * (PANEL_WIDTH - indent * SCALE - 7 * SCALE));

        int sliderY = yOffset + (int)(11 * SCALE);
        graphics.fill(panel.x + (int)((indent + 2) * SCALE), sliderY, panel.x + PANEL_WIDTH - (int)(6 * SCALE), sliderY + Math.max(1, (int)SCALE), theme.sliderBackground);
        graphics.fill(panel.x + (int)((indent + 2) * SCALE), sliderY, panel.x + (int)((indent + 2) * SCALE) + barWidth, sliderY + Math.max(1, (int)SCALE), theme.sliderForeground);

        String label = numSetting.getName() + ": " + String.format("%.1f", numSetting.getValue());
        int maxWidth = (int)((PANEL_WIDTH - indent * SCALE - 7 * SCALE) / SCALE);

        if (font.getWidth(label) > maxWidth) {
            String key = "num_" + indent + "_" + numSetting.getName();
            if (isSettingHovered) {
                float offset = textScrollOffsets.getOrDefault(key, 0f);
                offset += 0.5f;
                float textWidth = font.getWidth(label + "  " + label + "  ");
                if (offset > textWidth) offset = 0;
                textScrollOffsets.put(key, offset);
            } else {
                textScrollOffsets.put(key, 0f);
            }

            float offset = textScrollOffsets.get(key);
            String scrollText = label + "  " + label + "  " + label;

            int scissorX1 = panel.x + (int)(indent * SCALE);
            int scissorX2 = panel.x + PANEL_WIDTH - (int)(6 * SCALE);
            graphics.enableScissor(scissorX1, yOffset, scissorX2, yOffset + SETTING_HEIGHT);
            graphics.getMatrices().pushMatrix();
            graphics.getMatrices().translate(panel.x + (indent + 2) * SCALE - offset * SCALE, yOffset + 2 * SCALE);
            graphics.getMatrices().scale(SCALE, SCALE);
            drawGuiText(graphics, font, scrollText, 0, 0, 0xFFFFFFFF, false);
            graphics.getMatrices().popMatrix();
            graphics.disableScissor();
        } else {
            graphics.getMatrices().pushMatrix();
            graphics.getMatrices().translate(panel.x + (indent + 2) * SCALE, yOffset + 2 * SCALE);
            graphics.getMatrices().scale(SCALE, SCALE);
            drawGuiText(graphics, font, label, 0, 0, 0xFFFFFFFF, false);
            graphics.getMatrices().popMatrix();
        }

        return yOffset + SETTING_HEIGHT + ROW_GAP;
    }

    private int renderStringSetting(DrawContext graphics, TextRenderer font, CategoryPanel panel, StringSetting strSetting, int yOffset, int mouseX, int mouseY) {
        int inset = Math.max(2, (int) (2 * SCALE));
        int settingX1 = panel.x + inset, settingY1 = yOffset, settingX2 = panel.x + PANEL_WIDTH - inset, settingY2 = yOffset + SETTING_HEIGHT;

        boolean isSettingHovered = mouseX >= settingX1 && mouseX <= settingX2 && mouseY >= settingY1 && mouseY <= settingY2;
        if (isSettingHovered) {
            graphics.fill(settingX1, settingY1, settingX2, settingY2, theme.hoverHighlight);
        }

        String displayValue = strSetting.getValue();
        String label = strSetting.getName() + ": \"" + displayValue + "\"";

        int maxWidth = (int)((PANEL_WIDTH - 6 * SCALE) / SCALE);
        if (font.getWidth(label) > maxWidth) {
            String key = "str_" + strSetting.getName();
            if (isSettingHovered) {
                float offset = textScrollOffsets.getOrDefault(key, 0f);
                offset += 0.3f;
                float textWidth = font.getWidth(label + "  " + label + "  ");
                if (offset > textWidth) offset = 0;
                textScrollOffsets.put(key, offset);
            } else {
                textScrollOffsets.put(key, 0f);
            }

            float offset = textScrollOffsets.get(key);
            String scrollText = label + "  " + label + "  " + label;

            int scissorX1 = panel.x + (int)(3 * SCALE);
            int scissorX2 = panel.x + PANEL_WIDTH - (int)(6 * SCALE);
            graphics.enableScissor(scissorX1, yOffset, scissorX2, yOffset + SETTING_HEIGHT);
            graphics.getMatrices().pushMatrix();
            graphics.getMatrices().translate(panel.x + 5 * SCALE - offset * SCALE, yOffset + 3 * SCALE);
            graphics.getMatrices().scale(SCALE, SCALE);
            drawGuiText(graphics, font, scrollText, 0, 0, 0xFFFFFFFF, false);
            graphics.getMatrices().popMatrix();
            graphics.disableScissor();
        } else {
            graphics.getMatrices().pushMatrix();
            graphics.getMatrices().translate(panel.x + 5 * SCALE, yOffset + 3 * SCALE);
            graphics.getMatrices().scale(SCALE, SCALE);
            drawGuiText(graphics, font, label, 0, 0, 0xFFFFFFFF, false);
            graphics.getMatrices().popMatrix();
        }

        return yOffset + SETTING_HEIGHT + ROW_GAP;
    }

    private int renderRadioSetting(DrawContext graphics, TextRenderer font, CategoryPanel panel, RadioSetting radioSetting, int yOffset, int mouseX, int mouseY, int indent) {
        int inset = Math.max(2, (int) (2 * SCALE));
        int settingX1 = panel.x + inset, settingY1 = yOffset, settingX2 = panel.x + PANEL_WIDTH - inset, settingY2 = yOffset + SETTING_HEIGHT;

        boolean isSettingHovered = mouseX >= settingX1 && mouseX <= settingX2 && mouseY >= settingY1 && mouseY <= settingY2;
        if (isSettingHovered && indent == 3) {
            graphics.fill(settingX1, settingY1, settingX2, settingY2, theme.hoverHighlight);
        }

        String selected = radioSetting.getSelectedOption();
        int indicatorColor = theme.moduleEnabled;
        String indicatorText = ">";
        float indicatorX = panel.x + (indent + 2) * SCALE;

        graphics.getMatrices().pushMatrix();
        graphics.getMatrices().translate(indicatorX, yOffset + 2 * SCALE);
        graphics.getMatrices().scale(SCALE, SCALE);
        drawGuiText(graphics, font, indicatorText, 0, 0, indicatorColor, false);
        graphics.getMatrices().popMatrix();

        String label = radioSetting.getName() + ": " + selected;
        int maxWidth = (int)((PANEL_WIDTH - indent * SCALE - 12 * SCALE) / SCALE);

        if (font.getWidth(label) > maxWidth) {
            String key = "radio_" + indent + "_" + radioSetting.getName();
            if (isSettingHovered) {
                float offset = textScrollOffsets.getOrDefault(key, 0f);
                offset += 0.5f;
                float textWidth = font.getWidth(label + "  " + label + "  ");
                if (offset > textWidth) offset = 0;
                textScrollOffsets.put(key, offset);
            } else {
                textScrollOffsets.put(key, 0f);
            }

            float offset = textScrollOffsets.get(key);
            String scrollText = label + "  " + label + "  " + label;

            int scissorX1 = (int)(indicatorX + 12 * SCALE);
            int scissorX2 = panel.x + PANEL_WIDTH - (int)(6 * SCALE);
            graphics.enableScissor(scissorX1, yOffset, scissorX2, yOffset + SETTING_HEIGHT);
            graphics.getMatrices().pushMatrix();
            graphics.getMatrices().translate(indicatorX + 12 * SCALE - offset * SCALE, yOffset + 3 * SCALE);
            graphics.getMatrices().scale(SCALE, SCALE);
            drawGuiText(graphics, font, scrollText, 0, 0, 0xFFFFFFFF, false);
            graphics.getMatrices().popMatrix();
            graphics.disableScissor();
        } else {
            graphics.getMatrices().pushMatrix();
            graphics.getMatrices().translate(indicatorX + 12 * SCALE, yOffset + 3 * SCALE);
            graphics.getMatrices().scale(SCALE, SCALE);
            drawGuiText(graphics, font, label, 0, 0, 0xFFFFFFFF, false);
            graphics.getMatrices().popMatrix();
        }

        return yOffset + SETTING_HEIGHT + ROW_GAP;
    }

    private void drawGuiText(
            DrawContext graphics,
            TextRenderer font,
            String text,
            int x,
            int y,
            int color,
            boolean shadow
    ) {
        if (!ClickGuiScreen.useCustomGuiFont()) {
            graphics.drawText(font, text, x, y, color, shadow);
            return;
        }
        graphics.drawText(font, ClickGuiScreen.styledGuiText(text), x, y, color, shadow);
    }
}


