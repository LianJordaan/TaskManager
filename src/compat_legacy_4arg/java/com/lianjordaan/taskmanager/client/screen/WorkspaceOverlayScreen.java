package com.lianjordaan.taskmanager.client.screen;

import com.lianjordaan.taskmanager.client.render.MarkdownRenderer;
import com.lianjordaan.taskmanager.client.render.WorkspaceNoteRenderer;
import com.lianjordaan.taskmanager.client.widget.MultilineTextEditorWidget;
import com.lianjordaan.taskmanager.client.workspace.WorkspaceContext;
import com.lianjordaan.taskmanager.client.workspace.WorkspaceManager;
import com.lianjordaan.taskmanager.client.workspace.WorkspaceNote;
import com.lianjordaan.taskmanager.client.workspace.WorkspaceOverlayLayoutState;
import com.lianjordaan.taskmanager.client.workspace.WorkspaceOverlayLayoutStorage;
import com.lianjordaan.taskmanager.client.workspace.WorkspaceOverlayPanel;
import com.lianjordaan.taskmanager.client.workspace.WorkspaceScope;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class WorkspaceOverlayScreen extends Screen {
    private static final int TOP_BAR_HEIGHT = 32;
    private static final int SIDEBAR_RIGHT = 196;
    private static final int SIDEBAR_X = 8;
    private static final int SIDEBAR_TEXT_X = 18;
    private static final int PANEL_HEADER_HEIGHT = 22;
    private static final int PANEL_PADDING = 10;
    private static final int LIST_ROW_HEIGHT = 22;
    private static final int SNAP_SIZE = 12;
    private static final int NOTE_MIN_WIDTH = 160;
    private static final int NOTE_MIN_HEIGHT = 96;
    private static final int NOTE_EDGE_VISIBILITY = 44;
    private static final int PANEL_EDGE_VISIBILITY = 96;

    private static final int SETTINGS_PANEL_WIDTH = 314;
    private static final int SETTINGS_PANEL_HEIGHT = 194;
    private static final int EDITOR_PANEL_WIDTH = 392;
    private static final int EDITOR_PANEL_HEIGHT = 296;
    private static final int PREVIEW_PANEL_WIDTH = 340;
    private static final int PREVIEW_PANEL_HEIGHT = 220;

    private static final int[] NOTE_SWATCHES = {
        0x58BFD7,
        0x6CCFA4,
        0xF0B96B,
        0xE88982,
        0xB38DE7,
        0x7E97B8
    };

    private static final String[] NOTE_SWATCH_LABELS = {
        "Sky",
        "Mint",
        "Gold",
        "Coral",
        "Violet",
        "Slate"
    };

    private final WorkspaceManager workspaceManager = WorkspaceManager.getInstance();
    private final WorkspaceOverlayLayoutStorage overlayLayoutStorage = new WorkspaceOverlayLayoutStorage();

    private boolean quickCreateRequested;
    private boolean syncingEditor;
    private boolean noteLayoutDirty;
    private boolean panelLayoutDirty;

    private WorkspaceOverlayLayoutState overlayLayout;
    private WorkspaceNote selectedNote;
    private WorkspaceOverlayPanel activePanel;
    private NoteInteractionMode noteInteractionMode = NoteInteractionMode.NONE;
    private PanelInteractionMode panelInteractionMode = PanelInteractionMode.NONE;
    private float interactionOffsetX;
    private float interactionOffsetY;
    private float interactionStartWidth;
    private float interactionStartHeight;
    private float interactionStartMouseX;
    private float interactionStartMouseY;

    private Button addGlobalButton;
    private Button addContextButton;
    private Button snapButton;
    private Button resetLayoutButton;
    private Button hideButton;
    private Button lockButton;
    private Button deleteButton;
    private Button scaleDownButton;
    private Button scaleUpButton;
    private Button opacityDownButton;
    private Button opacityUpButton;
    private Button[] swatchButtons;
    private Button headingButton;
    private Button boldButton;
    private Button italicButton;
    private Button listButton;
    private Button quoteButton;
    private Button codeButton;

    private MultilineTextEditorWidget editorWidget;

    public WorkspaceOverlayScreen(boolean quickCreateRequested) {
        super(Component.literal("TaskManager Workspace"));
        this.quickCreateRequested = quickCreateRequested;
    }

    @Override
    protected void init() {
        super.init();
        workspaceManager.refreshContext(minecraft);

        if (overlayLayout == null) {
            overlayLayout = overlayLayoutStorage.load();
        }

        ensurePanelLayouts();
        clearWidgets();

        addGlobalButton = addRenderableWidget(Button.builder(Component.literal("Add Global"), button -> createNote(WorkspaceScope.GLOBAL))
            .bounds(SIDEBAR_RIGHT + 18, 6, 92, 20)
            .build());
        addContextButton = addRenderableWidget(Button.builder(Component.literal("Add Context"), button -> createNote(WorkspaceScope.CONTEXT))
            .bounds(SIDEBAR_RIGHT + 116, 6, 100, 20)
            .build());
        snapButton = addRenderableWidget(Button.builder(Component.literal("Snap"), button -> toggleSnap())
            .bounds(width - 204, 6, 88, 20)
            .build());
        resetLayoutButton = addRenderableWidget(Button.builder(Component.literal("Reset Layout"), button -> resetOverlayLayout())
            .bounds(width - 110, 6, 96, 20)
            .build());

        hideButton = addRenderableWidget(Button.builder(Component.literal("Hide"), button -> toggleHidden())
            .bounds(0, 0, 92, 20)
            .build());
        lockButton = addRenderableWidget(Button.builder(Component.literal("Lock"), button -> toggleLocked())
            .bounds(0, 0, 92, 20)
            .build());
        deleteButton = addRenderableWidget(Button.builder(Component.literal("Delete"), button -> deleteSelected())
            .bounds(0, 0, 92, 20)
            .build());
        scaleDownButton = addRenderableWidget(Button.builder(Component.literal("Scale -"), button -> adjustScale(-0.05F))
            .bounds(0, 0, 72, 20)
            .build());
        scaleUpButton = addRenderableWidget(Button.builder(Component.literal("Scale +"), button -> adjustScale(0.05F))
            .bounds(0, 0, 72, 20)
            .build());
        opacityDownButton = addRenderableWidget(Button.builder(Component.literal("Fade -"), button -> adjustCardOpacity(-0.10F))
            .bounds(0, 0, 72, 20)
            .build());
        opacityUpButton = addRenderableWidget(Button.builder(Component.literal("Fade +"), button -> adjustCardOpacity(0.10F))
            .bounds(0, 0, 72, 20)
            .build());

        swatchButtons = new Button[NOTE_SWATCHES.length];
        for (int index = 0; index < NOTE_SWATCHES.length; index++) {
            int swatchColor = NOTE_SWATCHES[index];
            swatchButtons[index] = addRenderableWidget(Button.builder(Component.literal(NOTE_SWATCH_LABELS[index]), button -> setSelectedTint(swatchColor))
                .bounds(0, 0, 84, 20)
                .build());
        }

        headingButton = addRenderableWidget(Button.builder(Component.literal("H1"), button -> insertSnippet("# ", 0))
            .bounds(0, 0, 48, 20)
            .build());
        boldButton = addRenderableWidget(Button.builder(Component.literal("B"), button -> insertSnippet("****", 2))
            .bounds(0, 0, 48, 20)
            .build());
        italicButton = addRenderableWidget(Button.builder(Component.literal("I"), button -> insertSnippet("**", 1))
            .bounds(0, 0, 48, 20)
            .build());
        listButton = addRenderableWidget(Button.builder(Component.literal("Li"), button -> insertSnippet("- ", 0))
            .bounds(0, 0, 48, 20)
            .build());
        quoteButton = addRenderableWidget(Button.builder(Component.literal(">"), button -> insertSnippet("> ", 0))
            .bounds(0, 0, 48, 20)
            .build());
        codeButton = addRenderableWidget(Button.builder(Component.literal("{}"), button -> insertSnippet("```\n\n```", 4))
            .bounds(0, 0, 48, 20)
            .build());

        editorWidget = addRenderableWidget(new MultilineTextEditorWidget(font, 0, 0, 180, 120));
        editorWidget.setResponder(value -> {
            if (syncingEditor || selectedNote == null) {
                return;
            }
            selectedNote.setContent(value);
            workspaceManager.saveNote(selectedNote);
        });

        if (selectedNote != null && !workspaceManager.contains(selectedNote)) {
            selectedNote = null;
        }
        if (selectedNote == null) {
            selectedNote = firstAvailableNote();
        }

        positionWidgets();
        syncEditorWithSelection();
        refreshButtonStates();
        consumeQuickCreateRequest();
    }

    public void requestQuickCreate() {
        quickCreateRequested = true;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        workspaceManager.refreshContext(minecraft);
        if (selectedNote != null && !workspaceManager.contains(selectedNote)) {
            selectedNote = null;
            syncEditorWithSelection();
        }

        ensurePanelLayouts();
        positionWidgets();
        refreshButtonStates();

        guiGraphics.fill(0, 0, width, height, 0xB0121622);
        renderWorkspaceBackground(guiGraphics);
        renderNotes(guiGraphics);
        renderChrome(guiGraphics, mouseX, mouseY);
        renderFloatingPanels(guiGraphics);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void tick() {
        workspaceManager.refreshContext(minecraft);
        consumeQuickCreateRequest();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        WorkspaceOverlayPanel hoveredPanel = findTopmostPanel(mouseX, mouseY);
        if (hoveredPanel != null) {
            focusPanel(hoveredPanel);
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                WorkspaceOverlayLayoutState.PanelLayout layout = panelLayout(hoveredPanel);
                if (layout != null && layout.headerContains(mouseX, mouseY, PANEL_HEADER_HEIGHT)) {
                    panelInteractionMode = PanelInteractionMode.DRAG;
                    interactionOffsetX = (float) mouseX - layout.getX();
                    interactionOffsetY = (float) mouseY - layout.getY();
                    return true;
                }
            }
        }

        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (hoveredPanel != null) {
            return true;
        }

        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }

        NoteRowHit rowHit = findNoteRowHit(mouseX, mouseY);
        if (rowHit != null) {
            if (rowHit.visibilityToggle) {
                rowHit.note.setHidden(!rowHit.note.isHidden());
                workspaceManager.saveNote(rowHit.note);
            } else {
                selectNote(rowHit.note);
            }
            return true;
        }

        if (mouseY <= TOP_BAR_HEIGHT || (mouseX >= SIDEBAR_X && mouseX <= SIDEBAR_RIGHT && mouseY >= TOP_BAR_HEIGHT)) {
            return true;
        }

        WorkspaceNote hoveredNote = findTopmostNote(mouseX, mouseY);
        if (hoveredNote != null) {
            selectNote(hoveredNote);
            workspaceManager.bringToFront(hoveredNote);
            if (!hoveredNote.isLocked() && WorkspaceNoteRenderer.isResizeHandleHit(hoveredNote, mouseX, mouseY)) {
                noteInteractionMode = NoteInteractionMode.RESIZE;
                interactionStartWidth = hoveredNote.getWidth();
                interactionStartHeight = hoveredNote.getHeight();
                interactionStartMouseX = (float) mouseX;
                interactionStartMouseY = (float) mouseY;
            } else if (!hoveredNote.isLocked() && WorkspaceNoteRenderer.isHeaderHit(hoveredNote, mouseX, mouseY)) {
                noteInteractionMode = NoteInteractionMode.DRAG;
                interactionOffsetX = (float) mouseX - hoveredNote.getX();
                interactionOffsetY = (float) mouseY - hoveredNote.getY();
            }
            return true;
        }

        selectedNote = null;
        syncEditorWithSelection();
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && panelInteractionMode == PanelInteractionMode.DRAG && activePanel != null) {
            WorkspaceOverlayLayoutState.PanelLayout layout = panelLayout(activePanel);
            if (layout != null) {
                layout.setX((int) Math.round(mouseX - interactionOffsetX));
                layout.setY((int) Math.round(mouseY - interactionOffsetY));
                clampPanelToViewport(layout);
                positionWidgets();
                panelLayoutDirty = true;
            }
            return true;
        }

        if (selectedNote == null || button != GLFW.GLFW_MOUSE_BUTTON_LEFT || noteInteractionMode == NoteInteractionMode.NONE) {
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        noteLayoutDirty = true;
        if (noteInteractionMode == NoteInteractionMode.DRAG) {
            selectedNote.setX(snap((float) mouseX - interactionOffsetX));
            selectedNote.setY(snap((float) mouseY - interactionOffsetY));
        } else if (noteInteractionMode == NoteInteractionMode.RESIZE) {
            float widthDelta = ((float) mouseX - interactionStartMouseX) / Math.max(0.65F, selectedNote.getScale());
            float heightDelta = ((float) mouseY - interactionStartMouseY) / Math.max(0.65F, selectedNote.getScale());
            selectedNote.setWidth(Math.max(NOTE_MIN_WIDTH, interactionStartWidth + widthDelta));
            selectedNote.setHeight(Math.max(NOTE_MIN_HEIGHT, interactionStartHeight + heightDelta));
        }

        clampSelectedNoteToViewport();
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && panelInteractionMode != PanelInteractionMode.NONE) {
            panelInteractionMode = PanelInteractionMode.NONE;
            if (panelLayoutDirty) {
                persistOverlayLayout();
                panelLayoutDirty = false;
            }
            return true;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && noteInteractionMode != NoteInteractionMode.NONE) {
            noteInteractionMode = NoteInteractionMode.NONE;
            if (noteLayoutDirty && selectedNote != null) {
                workspaceManager.saveNote(selectedNote);
                noteLayoutDirty = false;
            }
            return true;
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }

        if (selectedNote != null && WorkspaceNoteRenderer.contains(selectedNote, mouseX, mouseY)) {
            adjustScale(scrollY > 0.0D ? 0.05F : -0.05F);
            return true;
        }

        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_DELETE && selectedNote != null && !editorWidget.isFocused()) {
            deleteSelected();
            return true;
        }

        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void renderWorkspaceBackground(GuiGraphics guiGraphics) {
        int minorColor = overlayLayout.isSnapToGrid() ? 0x2E2C4157 : 0x131E2A39;
        int majorColor = overlayLayout.isSnapToGrid() ? 0x5A446783 : 0x1C304455;
        guiGraphics.fill(0, TOP_BAR_HEIGHT, width, height, overlayLayout.isSnapToGrid() ? 0x78101722 : 0x5A101722);
        for (int x = 0; x < width; x += SNAP_SIZE) {
            int color = x % (SNAP_SIZE * 4) == 0 ? majorColor : minorColor;
            guiGraphics.fill(x, TOP_BAR_HEIGHT, x + 1, height, color);
        }
        for (int y = TOP_BAR_HEIGHT; y < height; y += SNAP_SIZE) {
            int color = (y - TOP_BAR_HEIGHT) % (SNAP_SIZE * 4) == 0 ? majorColor : minorColor;
            guiGraphics.fill(0, y, width, y + 1, color);
        }
    }

    private void renderNotes(GuiGraphics guiGraphics) {
        for (WorkspaceNote note : workspaceManager.getCombinedNotes(true)) {
            WorkspaceNoteRenderer.render(guiGraphics, font, note, true, note == selectedNote);
        }
    }

    private void renderChrome(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.fill(0, 0, width, TOP_BAR_HEIGHT, 0xF01B2230);
        guiGraphics.fill(SIDEBAR_X, 36, SIDEBAR_RIGHT, height - 8, 0xD81A2130);

        guiGraphics.drawString(font, title, 14, 10, 0xFFF5F7FA, false);
        WorkspaceContext context = workspaceManager.getCurrentContext();
        guiGraphics.drawString(font, Component.literal(context.isAvailable() ? "Global + " + context.getLabel() : "Global workspace"), 118, 10, 0xFF8FB3C9, false);
        guiGraphics.drawString(font, Component.literal("Notes"), SIDEBAR_TEXT_X, 44, 0xFFF5F7FA, false);
        guiGraphics.drawString(font, Component.literal("Panels are draggable. Hold Shift to ignore snap."), SIDEBAR_RIGHT + 18, 44, 0xFF8FA3B9, false);

        renderSidebar(guiGraphics, mouseX, mouseY);
    }

    private void renderSidebar(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int rowWidth = SIDEBAR_RIGHT - 24;
        int y = 64;

        WorkspaceContext context = workspaceManager.getCurrentContext();
        guiGraphics.drawString(font, Component.literal("Visible notes float on the HUD."), SIDEBAR_TEXT_X, y, 0xFFD7E0EA, false);
        guiGraphics.drawString(font, Component.literal(context.isAvailable() ? "Context: " + context.getLabel() : "No active context"), SIDEBAR_TEXT_X, y + 14, 0xFF8FB3C9, false);
        guiGraphics.drawString(font, Component.literal("Rows still hide/show. Notes can move behind the UI."), SIDEBAR_TEXT_X, y + 28, 0xFF9DB0C3, false);

        y = 108;
        y = renderNoteSection(guiGraphics, mouseX, mouseY, SIDEBAR_TEXT_X, y, rowWidth, "Global", workspaceManager.getNotes(WorkspaceScope.GLOBAL));
        renderNoteSection(guiGraphics, mouseX, mouseY, SIDEBAR_TEXT_X, y + 10, rowWidth, context.isAvailable() ? "Context" : "Context (inactive)", workspaceManager.getNotes(WorkspaceScope.CONTEXT));
    }

    private int renderNoteSection(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y, int rowWidth, String label, List<WorkspaceNote> notes) {
        guiGraphics.drawString(font, Component.literal(label), x, y, 0xFFF5F7FA, false);
        int rowY = y + 14;
        for (WorkspaceNote note : notes) {
            boolean hovered = mouseX >= x && mouseX <= x + rowWidth && mouseY >= rowY && mouseY <= rowY + LIST_ROW_HEIGHT;
            boolean selected = note == selectedNote;
            int tint = note.getTintColor();
            int baseFill = selected ? withAlpha(blend(0x263244, tint, 0.36F), 184) : hovered ? withAlpha(blend(0x22303F, tint, 0.20F), 124) : 0x40242D3A;
            guiGraphics.fill(x, rowY, x + rowWidth, rowY + LIST_ROW_HEIGHT, baseFill);
            guiGraphics.fill(x, rowY, x + 4, rowY + LIST_ROW_HEIGHT, withAlpha(tint, 255));
            guiGraphics.drawString(font, note.getSummary(), x + 10, rowY + 7, note.isHidden() ? 0xFF99A9BA : 0xFFE7ECF5, false);
            guiGraphics.drawString(font, note.isHidden() ? "S" : "H", x + rowWidth - 12, rowY + 7, 0xFFF5F7FA, false);
            rowY += LIST_ROW_HEIGHT + 4;
        }

        if (notes.isEmpty()) {
            guiGraphics.drawString(font, Component.literal("No notes yet."), x, rowY + 2, 0xFF90A1B4, false);
            rowY += 18;
        }
        return rowY;
    }

    private void renderFloatingPanels(GuiGraphics guiGraphics) {
        for (WorkspaceOverlayPanel panel : overlayLayout.orderedPanels()) {
            switch (panel) {
                case SETTINGS -> renderSettingsPanel(guiGraphics, panelLayout(panel));
                case EDITOR -> renderEditorPanel(guiGraphics, panelLayout(panel));
                case PREVIEW -> renderPreviewPanel(guiGraphics, panelLayout(panel));
                default -> {
                }
            }
        }
    }

    private void renderSettingsPanel(GuiGraphics guiGraphics, WorkspaceOverlayLayoutState.PanelLayout layout) {
        if (layout == null) {
            return;
        }

        renderPanelFrame(guiGraphics, layout, "Card Settings", selectedNote == null ? "Select a note" : selectedNote.getSummary());
        int left = layout.getX() + PANEL_PADDING;
        int top = layout.getY() + PANEL_HEADER_HEIGHT + 8;

        if (selectedNote == null) {
            guiGraphics.drawString(font, Component.literal("Pick a note to change visibility, scale, tint, and card opacity."), left, top, 0xFFD7E0EA, false);
            guiGraphics.drawString(font, Component.literal("Color affects the card theme. Fade changes note transparency."), left, top + 14, 0xFF9DB0C3, false);
            return;
        }

        guiGraphics.drawString(font, Component.literal("Scope: " + selectedNote.getScope().getDisplayName()), left, top, 0xFF8FB3C9, false);
        guiGraphics.drawString(font, Component.literal("State: " + (selectedNote.isHidden() ? "Hidden" : "Visible") + " | " + (selectedNote.isLocked() ? "Locked" : "Free")), left, top + 14, 0xFFD7E0EA, false);
        guiGraphics.drawString(font, Component.literal("Scale " + Math.round(selectedNote.getScale() * 100.0F) + "%"), left + 178, top + 42, 0xFFD7E0EA, false);
        guiGraphics.drawString(font, Component.literal("Fade " + Math.round(selectedNote.getCardOpacity() * 100.0F) + "%"), left + 178, top + 66, 0xFFD7E0EA, false);
        guiGraphics.drawString(font, Component.literal("Tint"), left, top + 92, 0xFFD7E0EA, false);
        guiGraphics.fill(left + 34, top + 90, left + 64, top + 100, withAlpha(selectedNote.getTintColor(), 255));
    }

    private void renderEditorPanel(GuiGraphics guiGraphics, WorkspaceOverlayLayoutState.PanelLayout layout) {
        if (layout == null) {
            return;
        }

        renderPanelFrame(guiGraphics, layout, "Editor", selectedNote == null ? "No note selected" : "Markdown source");
        if (selectedNote == null) {
            guiGraphics.drawString(font, Component.literal("Select a note or create one from the top bar."), layout.getX() + PANEL_PADDING, layout.getY() + PANEL_HEADER_HEIGHT + 8, 0xFFD7E0EA, false);
        }
    }

    private void renderPreviewPanel(GuiGraphics guiGraphics, WorkspaceOverlayLayoutState.PanelLayout layout) {
        if (layout == null) {
            return;
        }

        renderPanelFrame(guiGraphics, layout, "Preview", selectedNote == null ? "Markdown render" : selectedNote.getScope().getDisplayName());
        int left = layout.getX() + PANEL_PADDING;
        int top = layout.getY() + PANEL_HEADER_HEIGHT + 8;
        int contentWidth = layout.getWidth() - PANEL_PADDING * 2;
        int contentHeight = layout.getHeight() - PANEL_HEADER_HEIGHT - PANEL_PADDING - 8;
        if (selectedNote == null) {
            guiGraphics.drawString(font, Component.literal("Select a note to preview its markdown here."), left, top, 0xFF9DB0C3, false);
            return;
        }

        MarkdownRenderer.render(guiGraphics, font, selectedNote.getContent(), left, top, contentWidth, contentHeight, 255);
    }

    private void renderPanelFrame(GuiGraphics guiGraphics, WorkspaceOverlayLayoutState.PanelLayout layout, String title, String subtitle) {
        int left = layout.getX();
        int top = layout.getY();
        int right = layout.right();
        int bottom = layout.bottom();
        boolean focused = activePanel != null && panelLayout(activePanel) == layout;

        guiGraphics.fill(left - 1, top - 1, right + 1, bottom + 1, focused ? 0xFF7CE2C2 : 0x70445265);
        guiGraphics.fill(left, top, right, bottom, 0xED18202C);
        guiGraphics.fill(left, top, right, top + PANEL_HEADER_HEIGHT, 0xF0223140);
        guiGraphics.drawString(font, Component.literal(title), left + 8, top + 7, 0xFFF5F7FA, false);
        if (subtitle != null && !subtitle.isBlank()) {
            guiGraphics.drawString(font, Component.literal(subtitle), left + 78, top + 7, 0xFF8FB3C9, false);
        }
    }

    private void positionWidgets() {
        addGlobalButton.setX(SIDEBAR_RIGHT + 18);
        addGlobalButton.setY(6);
        addContextButton.setX(SIDEBAR_RIGHT + 116);
        addContextButton.setY(6);
        snapButton.setX(width - 204);
        snapButton.setY(6);
        resetLayoutButton.setX(width - 110);
        resetLayoutButton.setY(6);

        WorkspaceOverlayLayoutState.PanelLayout settings = panelLayout(WorkspaceOverlayPanel.SETTINGS);
        if (settings != null) {
            int left = settings.getX() + PANEL_PADDING;
            int top = settings.getY() + PANEL_HEADER_HEIGHT + 34;
            hideButton.setX(left);
            hideButton.setY(top);
            lockButton.setX(left + 96);
            lockButton.setY(top);
            deleteButton.setX(left + 192);
            deleteButton.setY(top);

            scaleDownButton.setX(left);
            scaleDownButton.setY(top + 28);
            scaleUpButton.setX(left + 78);
            scaleUpButton.setY(top + 28);
            opacityDownButton.setX(left);
            opacityDownButton.setY(top + 52);
            opacityUpButton.setX(left + 78);
            opacityUpButton.setY(top + 52);

            for (int index = 0; index < swatchButtons.length; index++) {
                int row = index / 3;
                int column = index % 3;
                swatchButtons[index].setX(left + column * 92);
                swatchButtons[index].setY(top + 82 + row * 24);
            }
        }

        WorkspaceOverlayLayoutState.PanelLayout editor = panelLayout(WorkspaceOverlayPanel.EDITOR);
        if (editor != null) {
            int left = editor.getX() + PANEL_PADDING;
            int toolbarY = editor.getY() + PANEL_HEADER_HEIGHT + 8;
            headingButton.setX(left);
            headingButton.setY(toolbarY);
            boldButton.setX(left + 54);
            boldButton.setY(toolbarY);
            italicButton.setX(left + 108);
            italicButton.setY(toolbarY);
            listButton.setX(left + 162);
            listButton.setY(toolbarY);
            quoteButton.setX(left + 216);
            quoteButton.setY(toolbarY);
            codeButton.setX(left + 270);
            codeButton.setY(toolbarY);

            editorWidget.setBounds(left, toolbarY + 28, editor.getWidth() - PANEL_PADDING * 2, editor.getHeight() - PANEL_HEADER_HEIGHT - 46);
        }
    }

    private void ensurePanelLayouts() {
        overlayLayout.ensurePanel(WorkspaceOverlayPanel.SETTINGS, width - SETTINGS_PANEL_WIDTH - 16, TOP_BAR_HEIGHT + 18, SETTINGS_PANEL_WIDTH, SETTINGS_PANEL_HEIGHT);
        overlayLayout.ensurePanel(WorkspaceOverlayPanel.EDITOR, width - EDITOR_PANEL_WIDTH - 22, Math.min(height - EDITOR_PANEL_HEIGHT - 16, TOP_BAR_HEIGHT + SETTINGS_PANEL_HEIGHT + 30), EDITOR_PANEL_WIDTH, Math.min(EDITOR_PANEL_HEIGHT, height - TOP_BAR_HEIGHT - 24));
        overlayLayout.ensurePanel(WorkspaceOverlayPanel.PREVIEW, SIDEBAR_RIGHT + 22, Math.max(TOP_BAR_HEIGHT + 36, height - PREVIEW_PANEL_HEIGHT - 16), PREVIEW_PANEL_WIDTH, PREVIEW_PANEL_HEIGHT);

        for (WorkspaceOverlayPanel panel : WorkspaceOverlayPanel.values()) {
            WorkspaceOverlayLayoutState.PanelLayout layout = panelLayout(panel);
            if (layout != null) {
                clampPanelToViewport(layout);
            }
        }
        if (activePanel == null) {
            activePanel = WorkspaceOverlayPanel.EDITOR;
        }
    }

    private void resetOverlayLayout() {
        overlayLayout = new WorkspaceOverlayLayoutState();
        overlayLayout.setSnapToGrid(true);
        activePanel = WorkspaceOverlayPanel.EDITOR;
        ensurePanelLayouts();
        positionWidgets();
        refreshButtonStates();
        persistOverlayLayout();
    }

    private void persistOverlayLayout() {
        overlayLayoutStorage.save(overlayLayout);
    }

    private void createNote(WorkspaceScope scope) {
        if (scope == WorkspaceScope.CONTEXT && !workspaceManager.hasContext()) {
            return;
        }

        WorkspaceNote note = workspaceManager.createNote(scope, SIDEBAR_RIGHT + 18, TOP_BAR_HEIGHT + 18, width - 20, height - 20);
        clampNoteToViewport(note);
        workspaceManager.saveNote(note);
        selectNote(note);
    }

    private void selectNote(WorkspaceNote note) {
        selectedNote = note;
        syncEditorWithSelection();
    }

    private void focusPanel(WorkspaceOverlayPanel panel) {
        activePanel = panel;
        overlayLayout.bringToFront(panel);
        panelLayoutDirty = true;
    }

    private void toggleSnap() {
        overlayLayout.setSnapToGrid(!overlayLayout.isSnapToGrid());
        persistOverlayLayout();
        refreshButtonStates();
    }

    private void setSelectedTint(int tintColor) {
        if (selectedNote == null) {
            return;
        }
        selectedNote.setTintColor(tintColor);
        workspaceManager.saveNote(selectedNote);
    }

    private void adjustCardOpacity(float delta) {
        if (selectedNote == null) {
            return;
        }
        selectedNote.setCardOpacity(clamp(selectedNote.getCardOpacity() + delta, 0.20F, 1.0F));
        workspaceManager.saveNote(selectedNote);
    }

    private void toggleHidden() {
        if (selectedNote == null) {
            return;
        }
        selectedNote.setHidden(!selectedNote.isHidden());
        workspaceManager.saveNote(selectedNote);
        refreshButtonStates();
    }

    private void toggleLocked() {
        if (selectedNote == null) {
            return;
        }
        selectedNote.setLocked(!selectedNote.isLocked());
        workspaceManager.saveNote(selectedNote);
        refreshButtonStates();
    }

    private void deleteSelected() {
        if (selectedNote == null) {
            return;
        }
        workspaceManager.deleteNote(selectedNote);
        selectedNote = firstAvailableNote();
        syncEditorWithSelection();
    }

    private void adjustScale(float delta) {
        if (selectedNote == null) {
            return;
        }
        selectedNote.setScale(clamp(selectedNote.getScale() + delta, 0.65F, 1.85F));
        clampSelectedNoteToViewport();
        workspaceManager.saveNote(selectedNote);
        refreshButtonStates();
    }

    private void insertSnippet(String snippet, int cursorBacktrack) {
        if (selectedNote == null) {
            return;
        }
        focusPanel(WorkspaceOverlayPanel.EDITOR);
        editorWidget.insertSnippet(snippet, cursorBacktrack);
    }

    private void consumeQuickCreateRequest() {
        if (!quickCreateRequested) {
            return;
        }
        quickCreateRequested = false;
        createNote(workspaceManager.getPreferredCreationScope());
    }

    private void syncEditorWithSelection() {
        syncingEditor = true;
        editorWidget.setEditable(selectedNote != null);
        editorWidget.setValue(selectedNote == null ? "" : selectedNote.getContent());
        syncingEditor = false;
        refreshButtonStates();
    }

    private void refreshButtonStates() {
        boolean hasSelection = selectedNote != null;
        addContextButton.active = workspaceManager.hasContext();
        snapButton.setMessage(Component.literal(overlayLayout.isSnapToGrid() ? "Snap On" : "Snap Off"));

        hideButton.active = hasSelection;
        lockButton.active = hasSelection;
        deleteButton.active = hasSelection;
        scaleDownButton.active = hasSelection;
        scaleUpButton.active = hasSelection;
        opacityDownButton.active = hasSelection;
        opacityUpButton.active = hasSelection;
        headingButton.active = hasSelection;
        boldButton.active = hasSelection;
        italicButton.active = hasSelection;
        listButton.active = hasSelection;
        quoteButton.active = hasSelection;
        codeButton.active = hasSelection;

        for (Button swatchButton : swatchButtons) {
            swatchButton.active = hasSelection;
        }

        hideButton.setMessage(Component.literal(hasSelection && selectedNote.isHidden() ? "Unhide" : "Hide"));
        lockButton.setMessage(Component.literal(hasSelection && selectedNote.isLocked() ? "Unlock" : "Lock"));
    }

    private void clampSelectedNoteToViewport() {
        if (selectedNote == null) {
            return;
        }
        clampNoteToViewport(selectedNote);
    }

    private void clampNoteToViewport(WorkspaceNote note) {
        float maxWidth = Math.max(NOTE_MIN_WIDTH, (width - 16.0F) / Math.max(0.65F, note.getScale()));
        float maxHeight = Math.max(NOTE_MIN_HEIGHT, (height - TOP_BAR_HEIGHT - 16.0F) / Math.max(0.65F, note.getScale()));
        note.setWidth(clamp(note.getWidth(), NOTE_MIN_WIDTH, maxWidth));
        note.setHeight(clamp(note.getHeight(), NOTE_MIN_HEIGHT, maxHeight));

        float visibleWidth = Math.min(note.getRenderedWidth(), NOTE_EDGE_VISIBILITY);
        float visibleHeight = Math.min(note.getRenderedHeight(), WorkspaceNoteRenderer.HEADER_HEIGHT + 16.0F);
        note.setX(clamp(note.getX(), 8.0F - note.getRenderedWidth() + visibleWidth, width - visibleWidth - 8.0F));
        note.setY(clamp(note.getY(), TOP_BAR_HEIGHT + 4.0F - note.getRenderedHeight() + visibleHeight, height - visibleHeight - 8.0F));
    }

    private void clampPanelToViewport(WorkspaceOverlayLayoutState.PanelLayout layout) {
        int maxWidth = Math.max(220, width - 24);
        int maxHeight = Math.max(PANEL_HEADER_HEIGHT + 60, height - TOP_BAR_HEIGHT - 16);
        layout.setWidth(Math.min(layout.getWidth(), maxWidth));
        layout.setHeight(Math.min(layout.getHeight(), maxHeight));
        layout.setX(Math.round(clamp(layout.getX(), 8 - layout.getWidth() + PANEL_EDGE_VISIBILITY, width - PANEL_EDGE_VISIBILITY - 8)));
        layout.setY(Math.round(clamp(layout.getY(), TOP_BAR_HEIGHT + 4, height - PANEL_HEADER_HEIGHT - 8)));
    }

    private WorkspaceNote firstAvailableNote() {
        List<WorkspaceNote> notes = workspaceManager.getCombinedNotes(true);
        return notes.isEmpty() ? null : notes.get(notes.size() - 1);
    }

    private WorkspaceNote findTopmostNote(double mouseX, double mouseY) {
        List<WorkspaceNote> notes = workspaceManager.getCombinedNotes(true);
        for (int index = notes.size() - 1; index >= 0; index--) {
            WorkspaceNote note = notes.get(index);
            if (WorkspaceNoteRenderer.contains(note, mouseX, mouseY)) {
                return note;
            }
        }
        return null;
    }

    private WorkspaceOverlayPanel findTopmostPanel(double mouseX, double mouseY) {
        List<WorkspaceOverlayPanel> orderedPanels = overlayLayout.orderedPanels();
        for (int index = orderedPanels.size() - 1; index >= 0; index--) {
            WorkspaceOverlayPanel panel = orderedPanels.get(index);
            WorkspaceOverlayLayoutState.PanelLayout layout = panelLayout(panel);
            if (layout != null && layout.contains(mouseX, mouseY)) {
                return panel;
            }
        }
        return null;
    }

    private WorkspaceOverlayLayoutState.PanelLayout panelLayout(WorkspaceOverlayPanel panel) {
        return overlayLayout.getPanel(panel);
    }

    private NoteRowHit findNoteRowHit(double mouseX, double mouseY) {
        int rowWidth = SIDEBAR_RIGHT - 24;
        int y = 122;

        NoteRowHit hit = findNoteRowHit(mouseX, mouseY, SIDEBAR_TEXT_X, y, rowWidth, workspaceManager.getNotes(WorkspaceScope.GLOBAL));
        if (hit != null) {
            return hit;
        }

        y = y + sectionHeight(workspaceManager.getNotes(WorkspaceScope.GLOBAL)) + 24;
        return findNoteRowHit(mouseX, mouseY, SIDEBAR_TEXT_X, y, rowWidth, workspaceManager.getNotes(WorkspaceScope.CONTEXT));
    }

    private NoteRowHit findNoteRowHit(double mouseX, double mouseY, int x, int y, int rowWidth, List<WorkspaceNote> notes) {
        int rowY = y;
        for (WorkspaceNote note : notes) {
            if (mouseX >= x && mouseX <= x + rowWidth && mouseY >= rowY && mouseY <= rowY + LIST_ROW_HEIGHT) {
                return new NoteRowHit(note, mouseX >= x + rowWidth - 20);
            }
            rowY += LIST_ROW_HEIGHT + 4;
        }
        return null;
    }

    private int sectionHeight(List<WorkspaceNote> notes) {
        if (notes.isEmpty()) {
            return 32;
        }
        return 14 + notes.size() * (LIST_ROW_HEIGHT + 4);
    }

    private float snap(float value) {
        if (!overlayLayout.isSnapToGrid() || Screen.hasShiftDown()) {
            return value;
        }
        return Math.round(value / SNAP_SIZE) * SNAP_SIZE;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    private int blend(int from, int to, float ratio) {
        float clamped = Math.max(0.0F, Math.min(1.0F, ratio));
        int fromRed = (from >> 16) & 0xFF;
        int fromGreen = (from >> 8) & 0xFF;
        int fromBlue = from & 0xFF;
        int toRed = (to >> 16) & 0xFF;
        int toGreen = (to >> 8) & 0xFF;
        int toBlue = to & 0xFF;
        int red = Math.round(fromRed + (toRed - fromRed) * clamped);
        int green = Math.round(fromGreen + (toGreen - fromGreen) * clamped);
        int blue = Math.round(fromBlue + (toBlue - fromBlue) * clamped);
        return (red << 16) | (green << 8) | blue;
    }

    private enum NoteInteractionMode {
        NONE,
        DRAG,
        RESIZE
    }

    private enum PanelInteractionMode {
        NONE,
        DRAG
    }

    private record NoteRowHit(WorkspaceNote note, boolean visibilityToggle) {
    }
}