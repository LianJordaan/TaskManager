package com.lianjordaan.taskmanager.client.screen;

import com.lianjordaan.taskmanager.client.render.WorkspaceNoteRenderer;
import com.lianjordaan.taskmanager.client.widget.MultilineTextEditorWidget;
import com.lianjordaan.taskmanager.client.workspace.WorkspaceContext;
import com.lianjordaan.taskmanager.client.workspace.WorkspaceManager;
import com.lianjordaan.taskmanager.client.workspace.WorkspaceNote;
import com.lianjordaan.taskmanager.client.workspace.WorkspaceOverlayLayoutState;
import com.lianjordaan.taskmanager.client.workspace.WorkspaceOverlayLayoutStorage;
import com.lianjordaan.taskmanager.client.workspace.WorkspaceOverlayPanel;
import com.lianjordaan.taskmanager.client.workspace.WorkspaceScope;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;

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
    private static final int DRAWER_TOGGLE_WIDTH = 78;
    private static final int DRAWER_TOGGLE_HEIGHT = 20;
    private static final int PANEL_CONTROL_SIZE = 14;
    private static final int PANEL_RESIZE_HANDLE_SIZE = 14;
    private static final long HEADER_DOUBLE_CLICK_MS = 300L;
    private static final int NAV_TOGGLE_SIZE = 20;
    private static final int ADD_GLOBAL_BUTTON_WIDTH = 72;
    private static final int ADD_CONTEXT_BUTTON_WIDTH = 82;
    private static final int SNAP_BUTTON_WIDTH = 56;
    private static final int RESET_BUTTON_WIDTH = 62;
    private static final int TOP_BUTTON_GAP = 5;
    private static final int DRAWER_ACTION_WIDTH = 18;
    private static final int DRAWER_LIST_TOP = 64;
    private static final int DRAWER_LIST_BOTTOM_PADDING = 14;

    private static final int SETTINGS_PANEL_WIDTH = 320;
    private static final int SETTINGS_PANEL_HEIGHT = 236;
    private static final int EDITOR_PANEL_WIDTH = 320;
    private static final int EDITOR_PANEL_HEIGHT = 296;
    private static final int TOOLBAR_BUTTON_WIDTH = 44;
    private static final int TOOLBAR_BUTTON_GAP = 6;
    private static final int TOOLBAR_ROW_HEIGHT = 24;
    private static final int COLOR_INPUT_WIDTH = 92;

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
    private int drawerScrollOffset;

    private WorkspaceOverlayLayoutState overlayLayout;
    private WorkspaceNote selectedNote;
    private WorkspaceNote titleEditingNote;
    private WorkspaceNote lastHeaderClickedNote;
    private WorkspaceNote colorEditorNote;
    private WorkspaceOverlayPanel activePanel;
    private NoteInteractionMode noteInteractionMode = NoteInteractionMode.NONE;
    private PanelInteractionMode panelInteractionMode = PanelInteractionMode.NONE;
    private long lastHeaderClickAt;
    private float interactionOffsetX;
    private float interactionOffsetY;
    private float interactionStartWidth;
    private float interactionStartHeight;
    private float interactionStartMouseX;
    private float interactionStartMouseY;

    private Button navToggleButton;
    private Button addGlobalButton;
    private Button addContextButton;
    private Button snapButton;
    private Button resetLayoutButton;
    private Button scaleDownButton;
    private Button scaleUpButton;
    private Button opacityDownButton;
    private Button opacityUpButton;
    private Button applyTintButton;
    private Button resetTintButton;
    private Button[] swatchButtons;
    private Button headingButton;
    private Button boldButton;
    private Button italicButton;
    private Button listButton;
    private Button quoteButton;
    private Button codeButton;

    private MultilineTextEditorWidget editorWidget;
    private MultilineTextEditorWidget titleEditorWidget;
    private MultilineTextEditorWidget colorEditorWidget;

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

        navToggleButton = addRenderableWidget(Button.builder(Component.literal("^"), button -> toggleTopBar())
            .bounds(width - NAV_TOGGLE_SIZE - 8, 6, NAV_TOGGLE_SIZE, 20)
            .build());
        addGlobalButton = addRenderableWidget(Button.builder(Component.literal("+ Global"), button -> createNote(WorkspaceScope.GLOBAL))
            .bounds(SIDEBAR_RIGHT + 18, 6, ADD_GLOBAL_BUTTON_WIDTH, 20)
            .build());
        addContextButton = addRenderableWidget(Button.builder(Component.literal("+ Context"), button -> createNote(WorkspaceScope.CONTEXT))
            .bounds(SIDEBAR_RIGHT + 116, 6, ADD_CONTEXT_BUTTON_WIDTH, 20)
            .build());
        snapButton = addRenderableWidget(Button.builder(Component.literal("Grid"), button -> toggleSnap())
            .bounds(width - 204, 6, SNAP_BUTTON_WIDTH, 20)
            .build());
        resetLayoutButton = addRenderableWidget(Button.builder(Component.literal("Reset UI"), button -> resetOverlayLayout())
            .bounds(width - 110, 6, RESET_BUTTON_WIDTH, 20)
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
        applyTintButton = addRenderableWidget(Button.builder(Component.literal("Apply"), button -> applyCustomTint())
            .bounds(0, 0, 58, 20)
            .build());
        resetTintButton = addRenderableWidget(Button.builder(Component.literal("Default"), button -> resetSelectedTint())
            .bounds(0, 0, 68, 20)
            .build());

        headingButton = addRenderableWidget(Button.builder(Component.literal("H1"), button -> insertSnippet("# ", 0))
            .bounds(0, 0, 48, 20)
            .build());
        boldButton = addRenderableWidget(Button.builder(Component.literal("B"), button -> insertSnippet("****", 2))
            .bounds(0, 0, 48, 20)
            .build());
        italicButton = addRenderableWidget(Button.builder(Component.literal("I"), button -> insertSnippet("**", 1))
            .bounds(0, 0, 48, 20)
            .build());
        listButton = addRenderableWidget(Button.builder(Component.literal("Task"), button -> insertSnippet("- [ ] ", 0))
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

        titleEditorWidget = addRenderableWidget(new MultilineTextEditorWidget(font, 0, 0, 140, 20));
        titleEditorWidget.setSingleLine(true);
        titleEditorWidget.setEditable(false);
        titleEditorWidget.setResponder(value -> {
            if (titleEditingNote == null) {
                return;
            }
            titleEditingNote.setTitle(value);
            workspaceManager.saveNote(titleEditingNote);
        });
        titleEditorWidget.visible = false;

        colorEditorWidget = addRenderableWidget(new MultilineTextEditorWidget(font, 0, 0, COLOR_INPUT_WIDTH, 20));
        colorEditorWidget.setSingleLine(true);
        colorEditorWidget.setResponder(value -> {
        });
        colorEditorWidget.visible = false;

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
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        workspaceManager.refreshContext(minecraft);
        if (selectedNote != null && !workspaceManager.contains(selectedNote)) {
            selectedNote = null;
            syncEditorWithSelection();
        }
        if (titleEditingNote != null && !workspaceManager.contains(titleEditingNote)) {
            stopTitleEditing();
        }

        ensurePanelLayouts();
        positionWidgets();
        refreshButtonStates();

        guiGraphics.fill(0, 0, width, height, 0xB0121622);
        renderWorkspaceBackground(guiGraphics);
        renderFloatingPanels(guiGraphics, mouseX, mouseY, partialTick);
        renderNotes(guiGraphics, mouseX, mouseY);
        renderChrome(guiGraphics, mouseX, mouseY);
        renderTopLevelWidgets(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void tick() {
        workspaceManager.refreshContext(minecraft);
        consumeQuickCreateRequest();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent context, boolean alreadyHandled) {
        if (alreadyHandled) {
            return true;
        }

        if (titleEditingNote != null && titleEditorWidget.visible && titleEditorWidget.isMouseOver(context.x(), context.y())) {
            return mouseClickedTopLevelWidgets(context);
        }
        if (titleEditingNote != null) {
            stopTitleEditing();
        }

        if (context.button() == InputConstants.MOUSE_BUTTON_LEFT && isDrawerToggleHit(context.x(), context.y())) {
            overlayLayout.setNoteDrawerOpen(!overlayLayout.isNoteDrawerOpen());
            persistOverlayLayout();
            return true;
        }

        if (overlayLayout.isNoteDrawerOpen()) {
            NoteRowHit rowHit = findNoteRowHit(context.x(), context.y());
            if (rowHit != null) {
                switch (rowHit.action) {
                    case VISIBILITY -> {
                        rowHit.note.setHidden(!rowHit.note.isHidden());
                        workspaceManager.saveNote(rowHit.note);
                    }
                    case DELETE -> deleteNote(rowHit.note);
                    case SELECT -> selectNote(rowHit.note);
                }
                return true;
            }
            if (isDrawerBodyHit(context.x(), context.y())) {
                return true;
            }
        }

        if (mouseClickedTopLevelWidgets(context)) {
            return true;
        }

        if (!overlayLayout.isTopBarCollapsed() && context.y() <= TOP_BAR_HEIGHT) {
            return true;
        }

        if (context.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }

        WorkspaceNote hoveredNote = findTopmostNote(context.x(), context.y());
        if (hoveredNote != null) {
            selectNote(hoveredNote);
            workspaceManager.bringToFront(hoveredNote);
            if (WorkspaceNoteRenderer.isHideButtonHit(hoveredNote, font, context.x(), context.y())) {
                hoveredNote.setHidden(!hoveredNote.isHidden());
                workspaceManager.saveNote(hoveredNote);
                refreshButtonStates();
                return true;
            }
            if (WorkspaceNoteRenderer.isLockButtonHit(hoveredNote, font, context.x(), context.y())) {
                hoveredNote.setLocked(!hoveredNote.isLocked());
                workspaceManager.saveNote(hoveredNote);
                refreshButtonStates();
                return true;
            }
            if (WorkspaceNoteRenderer.isDeleteButtonHit(hoveredNote, font, context.x(), context.y())) {
                deleteNote(hoveredNote);
                return true;
            }
            if (WorkspaceNoteRenderer.toggleCheckboxAt(hoveredNote, font, context.x(), context.y())) {
                workspaceManager.saveNote(hoveredNote);
                syncEditorWithSelection();
                return true;
            }
            if (WorkspaceNoteRenderer.isHeaderHit(hoveredNote, context.x(), context.y()) && shouldStartTitleEditing(hoveredNote)) {
                startTitleEditing(hoveredNote);
                return true;
            }
            if (!hoveredNote.isLocked() && WorkspaceNoteRenderer.isResizeHandleHit(hoveredNote, context.x(), context.y())) {
                noteInteractionMode = NoteInteractionMode.RESIZE;
                interactionStartWidth = hoveredNote.getWidth();
                interactionStartHeight = hoveredNote.getHeight();
                interactionStartMouseX = (float) context.x();
                interactionStartMouseY = (float) context.y();
            } else if (!hoveredNote.isLocked() && WorkspaceNoteRenderer.isHeaderHit(hoveredNote, context.x(), context.y())) {
                noteInteractionMode = NoteInteractionMode.DRAG;
                interactionOffsetX = (float) context.x() - hoveredNote.getX();
                interactionOffsetY = (float) context.y() - hoveredNote.getY();
            }
            return true;
        }

        WorkspaceOverlayPanel hoveredPanel = findTopmostPanel(context.x(), context.y());
        if (hoveredPanel != null) {
            focusPanel(hoveredPanel);
            WorkspaceOverlayLayoutState.PanelLayout layout = panelLayout(hoveredPanel);
            if (layout != null) {
                if (isPanelMinimizeHit(layout, context.x(), context.y())) {
                    layout.setMinimized(!layout.isMinimized());
                    clampPanelToViewport(hoveredPanel, layout);
                    positionWidgets();
                    refreshButtonStates();
                    persistOverlayLayout();
                    return true;
                }
                if (!layout.isMinimized() && isPanelResizeHandleHit(layout, context.x(), context.y())) {
                    panelInteractionMode = PanelInteractionMode.RESIZE;
                    interactionStartWidth = layout.getWidth();
                    interactionStartHeight = layout.getHeight();
                    interactionStartMouseX = (float) context.x();
                    interactionStartMouseY = (float) context.y();
                    return true;
                }
                if (panelHeaderContains(layout, context.x(), context.y())) {
                    panelInteractionMode = PanelInteractionMode.DRAG;
                    interactionOffsetX = (float) context.x() - layout.getX();
                    interactionOffsetY = (float) context.y() - layout.getY();
                    return true;
                }
                if (!layout.isMinimized() && mouseClickedPanelWidgets(hoveredPanel, context)) {
                    return true;
                }
            }
            return true;
        }

        selectedNote = null;
        syncEditorWithSelection();
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent context, double dragX, double dragY) {
        if (context.button() == InputConstants.MOUSE_BUTTON_LEFT && panelInteractionMode != PanelInteractionMode.NONE && activePanel != null) {
            WorkspaceOverlayLayoutState.PanelLayout layout = panelLayout(activePanel);
            if (layout != null) {
                if (panelInteractionMode == PanelInteractionMode.DRAG) {
                    layout.setX((int) Math.round(context.x() - interactionOffsetX));
                    layout.setY((int) Math.round(context.y() - interactionOffsetY));
                } else if (panelInteractionMode == PanelInteractionMode.RESIZE) {
                    layout.setWidth(Math.max(panelMinWidth(activePanel), Math.round(interactionStartWidth + ((float) context.x() - interactionStartMouseX))));
                    layout.setHeight(Math.max(panelMinHeight(activePanel), Math.round(interactionStartHeight + ((float) context.y() - interactionStartMouseY))));
                }
                clampPanelToViewport(activePanel, layout);
                positionWidgets();
                panelLayoutDirty = true;
            }
            return true;
        }

        if (selectedNote == null || context.button() != InputConstants.MOUSE_BUTTON_LEFT || noteInteractionMode == NoteInteractionMode.NONE) {
            return super.mouseDragged(context, dragX, dragY);
        }

        noteLayoutDirty = true;
        if (noteInteractionMode == NoteInteractionMode.DRAG) {
            selectedNote.setX(snap((float) context.x() - interactionOffsetX, context.hasShiftDown()));
            selectedNote.setY(snap((float) context.y() - interactionOffsetY, context.hasShiftDown()));
        } else if (noteInteractionMode == NoteInteractionMode.RESIZE) {
            float widthDelta = ((float) context.x() - interactionStartMouseX) / Math.max(0.65F, selectedNote.getScale());
            float heightDelta = ((float) context.y() - interactionStartMouseY) / Math.max(0.65F, selectedNote.getScale());
            selectedNote.setWidth(Math.max(NOTE_MIN_WIDTH, interactionStartWidth + widthDelta));
            selectedNote.setHeight(Math.max(NOTE_MIN_HEIGHT, interactionStartHeight + heightDelta));
        }

        clampSelectedNoteToViewport();
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent context) {
        if (context.button() == InputConstants.MOUSE_BUTTON_LEFT && panelInteractionMode != PanelInteractionMode.NONE) {
            panelInteractionMode = PanelInteractionMode.NONE;
            if (panelLayoutDirty) {
                persistOverlayLayout();
                panelLayoutDirty = false;
            }
            return true;
        }

        if (context.button() == InputConstants.MOUSE_BUTTON_LEFT && noteInteractionMode != NoteInteractionMode.NONE) {
            noteInteractionMode = NoteInteractionMode.NONE;
            if (noteLayoutDirty && selectedNote != null) {
                workspaceManager.saveNote(selectedNote);
                noteLayoutDirty = false;
            }
            return true;
        }

        return super.mouseReleased(context);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (overlayLayout.isNoteDrawerOpen() && isDrawerBodyHit(mouseX, mouseY)) {
            scrollDrawer(scrollY);
            return true;
        }

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
    public boolean keyPressed(KeyEvent context) {
        if (titleEditingNote != null && titleEditorWidget.isFocused()) {
            if (context.key() == InputConstants.KEY_RETURN || context.key() == InputConstants.KEY_NUMPADENTER || context.key() == InputConstants.KEY_ESCAPE) {
                stopTitleEditing();
                return true;
            }
        }

        if (super.keyPressed(context)) {
            return true;
        }

        if (context.key() == InputConstants.KEY_DELETE && selectedNote != null && !editorWidget.isFocused()) {
            deleteSelected();
            return true;
        }

        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void renderWorkspaceBackground(GuiGraphicsExtractor guiGraphics) {
        int minorColor = overlayLayout.isSnapToGrid() ? 0x2E2C4157 : 0x131E2A39;
        int majorColor = overlayLayout.isSnapToGrid() ? 0x5A446783 : 0x1C304455;
        int top = workspaceTop();
        guiGraphics.fill(0, top, width, height, overlayLayout.isSnapToGrid() ? 0x78101722 : 0x5A101722);
        for (int x = 0; x < width; x += SNAP_SIZE) {
            int color = x % (SNAP_SIZE * 4) == 0 ? majorColor : minorColor;
            guiGraphics.fill(x, top, x + 1, height, color);
        }
        for (int y = top; y < height; y += SNAP_SIZE) {
            int color = (y - top) % (SNAP_SIZE * 4) == 0 ? majorColor : minorColor;
            guiGraphics.fill(0, y, width, y + 1, color);
        }
    }

    private void renderNotes(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        for (WorkspaceNote note : workspaceManager.getCombinedNotes(true)) {
            boolean selected = note == selectedNote;
            boolean hovered = WorkspaceNoteRenderer.contains(note, mouseX, mouseY);
            WorkspaceNoteRenderer.render(guiGraphics, font, note, true, selected, selected || hovered);
        }
    }

    private void renderTopLevelWidgets(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        boolean[] previousVisibility = setPanelWidgetsRawVisible(false);
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        restorePanelWidgetVisibility(previousVisibility);
    }

    private void renderChrome(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        if (!overlayLayout.isTopBarCollapsed()) {
            guiGraphics.fill(0, 0, width, TOP_BAR_HEIGHT, 0xF01B2230);

            int titleX = 14;
            int titleY = 10;
            WorkspaceContext context = workspaceManager.getCurrentContext();
            int firstButtonX = addGlobalButton.getX();
            int titleWidth = font.width(title.getString());
            int titleMaxWidth = Math.max(0, firstButtonX - titleX - 12);
            if (titleWidth + 14 < titleMaxWidth) {
                guiGraphics.text(font, title, titleX, titleY, 0xFFF5F7FA, false);
                int contextX = titleX + titleWidth + 14;
                int contextWidth = Math.max(0, firstButtonX - contextX - 10);
                drawTrimmedString(guiGraphics, context.isAvailable() ? "Global + " + context.getLabel() : "Global workspace", contextX, titleY, contextWidth, 0xFF8FB3C9);
            } else {
                drawTrimmedString(guiGraphics, title.getString(), titleX, titleY, titleMaxWidth, 0xFFF5F7FA);
            }
        }

        renderDrawerToggle(guiGraphics, mouseX, mouseY);
        if (overlayLayout.isNoteDrawerOpen()) {
            guiGraphics.fill(SIDEBAR_X, 36, SIDEBAR_RIGHT, height - 8, 0xD81A2130);
            guiGraphics.text(font, Component.literal("Notes"), SIDEBAR_TEXT_X, 44, 0xFFF5F7FA, false);
            renderSidebar(guiGraphics, mouseX, mouseY);
        }
    }

    private void renderSidebar(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        int rowWidth = SIDEBAR_RIGHT - 24;
        int listBottom = drawerListBottom();
        clampDrawerScroll();
        int y = DRAWER_LIST_TOP - drawerScrollOffset;
        y = renderNoteSection(guiGraphics, mouseX, mouseY, SIDEBAR_TEXT_X, y, rowWidth, "Global", workspaceManager.getNotes(WorkspaceScope.GLOBAL), DRAWER_LIST_TOP, listBottom);
        renderNoteSection(guiGraphics, mouseX, mouseY, SIDEBAR_TEXT_X, y + 10, rowWidth, workspaceManager.hasContext() ? "Context" : "Context notes", workspaceManager.getNotes(WorkspaceScope.CONTEXT), DRAWER_LIST_TOP, listBottom);

        int maxScroll = maxDrawerScroll();
        if (maxScroll > 0) {
            int trackTop = DRAWER_LIST_TOP;
            int trackBottom = listBottom;
            int thumbHeight = Math.max(18, (trackBottom - trackTop) * (trackBottom - trackTop) / Math.max(trackBottom - trackTop, drawerContentHeight()));
            int thumbTop = trackTop + Math.round((trackBottom - trackTop - thumbHeight) * (drawerScrollOffset / (float) maxScroll));
            guiGraphics.fill(SIDEBAR_RIGHT - 11, trackTop, SIDEBAR_RIGHT - 8, trackBottom, 0x40384A5F);
            guiGraphics.fill(SIDEBAR_RIGHT - 11, thumbTop, SIDEBAR_RIGHT - 8, thumbTop + thumbHeight, 0xB07CE2C2);
        }
    }

    private int renderNoteSection(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, int x, int y, int rowWidth, String label, List<WorkspaceNote> notes, int listTop, int listBottom) {
        if (y + 10 >= listTop && y <= listBottom) {
            drawTrimmedString(guiGraphics, label, x, y, rowWidth, 0xFFF5F7FA);
        }
        int rowY = y + 14;
        for (WorkspaceNote note : notes) {
            if (rowY + LIST_ROW_HEIGHT >= listTop && rowY <= listBottom) {
                int clippedTop = Math.max(rowY, listTop);
                int clippedBottom = Math.min(rowY + LIST_ROW_HEIGHT, listBottom);
                boolean hovered = mouseX >= x && mouseX <= x + rowWidth && mouseY >= clippedTop && mouseY <= clippedBottom;
                boolean selected = note == selectedNote;
                int tint = note.getTintColor();
                int baseFill = selected ? withAlpha(blend(0x263244, tint, 0.36F), 184) : hovered ? withAlpha(blend(0x22303F, tint, 0.20F), 124) : 0x40242D3A;
                guiGraphics.fill(x, clippedTop, x + rowWidth, clippedBottom, baseFill);
                guiGraphics.fill(x, clippedTop, x + 4, clippedBottom, withAlpha(tint, 255));
                drawTrimmedString(guiGraphics, note.getSummary(), x + 10, rowY + 7, rowWidth - 56, note.isHidden() ? 0xFF99A9BA : 0xFFE7ECF5);
                drawTrimmedString(guiGraphics, note.isHidden() ? "S" : "H", x + rowWidth - 35, rowY + 7, 12, 0xFFF5F7FA);
                drawTrimmedString(guiGraphics, "x", x + rowWidth - 15, rowY + 7, 8, 0xFFFFD8DD);
            }
            rowY += LIST_ROW_HEIGHT + 4;
        }

        if (notes.isEmpty()) {
            if (rowY + 12 >= listTop && rowY <= listBottom) {
                drawTrimmedString(guiGraphics, "No notes yet.", x, rowY + 2, rowWidth, 0xFF90A1B4);
            }
            rowY += 18;
        }
        return rowY;
    }

    private void renderFloatingPanels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        for (WorkspaceOverlayPanel panel : overlayLayout.orderedPanels()) {
            if (!isPanelEnabled(panel)) {
                continue;
            }
            switch (panel) {
                case SETTINGS -> renderSettingsPanel(guiGraphics, panelLayout(panel), mouseX, mouseY, partialTick);
                case EDITOR -> renderEditorPanel(guiGraphics, panelLayout(panel), mouseX, mouseY, partialTick);
                default -> {
                }
            }
        }
    }

    private void renderSettingsPanel(GuiGraphicsExtractor guiGraphics, WorkspaceOverlayLayoutState.PanelLayout layout, int mouseX, int mouseY, float partialTick) {
        if (layout == null) {
            return;
        }

        renderPanelFrame(guiGraphics, layout, "Card Settings", selectedNote == null ? "Select a note" : selectedNote.getSummary());
        if (layout.isMinimized()) {
            return;
        }
        int left = layout.getX() + PANEL_PADDING;
        int top = layout.getY() + PANEL_HEADER_HEIGHT + 8;
        int contentWidth = layout.getWidth() - PANEL_PADDING * 2;

        if (selectedNote == null) {
            drawTrimmedString(guiGraphics, "Pick a note to change scale, tint, and card opacity.", left, top, contentWidth, 0xFFD7E0EA);
            drawTrimmedString(guiGraphics, "Use note header icons for hide, lock, and delete.", left, top + 14, contentWidth, 0xFF9DB0C3);
            return;
        }

        drawTrimmedString(guiGraphics, "Scope: " + selectedNote.getScope().getDisplayName(), left, top, contentWidth, 0xFF8FB3C9);
        drawTrimmedString(guiGraphics, "State: " + (selectedNote.isHidden() ? "Hidden" : "Visible") + " | " + (selectedNote.isLocked() ? "Locked" : "Free"), left, top + 14, contentWidth, 0xFFD7E0EA);
        drawTrimmedString(guiGraphics, "Scale " + Math.round(selectedNote.getScale() * 100.0F) + "%", left, top + 38, 80, 0xFFD7E0EA);
        drawTrimmedString(guiGraphics, "Fade " + Math.round(selectedNote.getCardOpacity() * 100.0F) + "%", left, top + 62, 80, 0xFFD7E0EA);
        drawTrimmedString(guiGraphics, "Tint", left, top + 90, 38, 0xFFD7E0EA);
        guiGraphics.fill(left + 40, top + 88, left + 74, top + 100, withAlpha(selectedNote.getTintColor(), 255));
        drawTrimmedString(guiGraphics, "Custom", left, top + 156, 58, 0xFFD7E0EA);
        renderSettingsWidgets(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderEditorPanel(GuiGraphicsExtractor guiGraphics, WorkspaceOverlayLayoutState.PanelLayout layout, int mouseX, int mouseY, float partialTick) {
        if (layout == null) {
            return;
        }

        renderPanelFrame(guiGraphics, layout, "Editor", selectedNote == null ? "No note selected" : "Markdown source");
        if (layout.isMinimized()) {
            return;
        }
        if (selectedNote == null) {
            drawTrimmedString(guiGraphics, "Select a note or create one from the top bar.", layout.getX() + PANEL_PADDING, layout.getY() + PANEL_HEADER_HEIGHT + 8, layout.getWidth() - PANEL_PADDING * 2, 0xFFD7E0EA);
            return;
        }
        renderEditorWidgets(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderPanelFrame(GuiGraphicsExtractor guiGraphics, WorkspaceOverlayLayoutState.PanelLayout layout, String title, String subtitle) {
        int left = layout.getX();
        int top = layout.getY();
        int right = layout.right();
        int bottom = panelBottom(layout);
        boolean focused = activePanel != null && panelLayout(activePanel) == layout;

        guiGraphics.fill(left - 1, top - 1, right + 1, bottom + 1, focused ? 0xFF7CE2C2 : 0x70445265);
        if (!layout.isMinimized()) {
            guiGraphics.fill(left, top, right, bottom, 0xED18202C);
        }
        guiGraphics.fill(left, top, right, top + PANEL_HEADER_HEIGHT, 0xF0223140);
        guiGraphics.text(font, Component.literal(title), left + 8, top + 7, 0xFFF5F7FA, false);
        if (subtitle != null && !subtitle.isBlank()) {
            int subtitleX = left + 8 + font.width(title) + 14;
            int subtitleWidth = Math.max(0, right - PANEL_CONTROL_SIZE - 18 - subtitleX);
            drawTrimmedString(guiGraphics, subtitle, subtitleX, top + 7, subtitleWidth, 0xFF8FB3C9);
        }
        int controlLeft = right - PANEL_CONTROL_SIZE - 8;
        guiGraphics.fill(controlLeft, top + 4, controlLeft + PANEL_CONTROL_SIZE, top + 4 + PANEL_CONTROL_SIZE, 0x60435466);
        guiGraphics.text(font, layout.isMinimized() ? "+" : "-", controlLeft + 4, top + 7, 0xFFF5F7FA, false);
        if (!layout.isMinimized()) {
            guiGraphics.fill(right - PANEL_RESIZE_HANDLE_SIZE, bottom - PANEL_RESIZE_HANDLE_SIZE, right, bottom, 0xFF7CE2C2);
        }
    }

    private void positionWidgets() {
        positionTopBarWidgets();

        WorkspaceOverlayLayoutState.PanelLayout settings = panelLayout(WorkspaceOverlayPanel.SETTINGS);
        boolean settingsVisible = settings != null && !settings.isMinimized() && selectedNote != null;
        setWidgetVisible(scaleDownButton, settingsVisible);
        setWidgetVisible(scaleUpButton, settingsVisible);
        setWidgetVisible(opacityDownButton, settingsVisible);
        setWidgetVisible(opacityUpButton, settingsVisible);
        setWidgetVisible(applyTintButton, settingsVisible);
        setWidgetVisible(resetTintButton, settingsVisible);
        setWidgetVisible(colorEditorWidget, settingsVisible);
        colorEditorWidget.setEditable(settingsVisible);
        for (Button swatchButton : swatchButtons) {
            setWidgetVisible(swatchButton, settingsVisible);
        }
        if (settingsVisible) {
            int left = settings.getX() + PANEL_PADDING;
            int top = settings.getY() + PANEL_HEADER_HEIGHT + 8;

            scaleDownButton.setX(left + 88);
            scaleDownButton.setY(top + 32);
            scaleUpButton.setX(left + 166);
            scaleUpButton.setY(top + 32);
            opacityDownButton.setX(left + 88);
            opacityDownButton.setY(top + 56);
            opacityUpButton.setX(left + 166);
            opacityUpButton.setY(top + 56);

            for (int index = 0; index < swatchButtons.length; index++) {
                int row = index / 3;
                int column = index % 3;
                swatchButtons[index].setX(left + column * 92);
                swatchButtons[index].setY(top + 104 + row * 24);
            }
            colorEditorWidget.setBounds(left + 68, top + 150, COLOR_INPUT_WIDTH, 20);
            applyTintButton.setX(left + 166);
            applyTintButton.setY(top + 150);
            resetTintButton.setX(left + 228);
            resetTintButton.setY(top + 150);
        }

        WorkspaceOverlayLayoutState.PanelLayout editor = panelLayout(WorkspaceOverlayPanel.EDITOR);
        boolean editorVisible = editor != null && !editor.isMinimized() && selectedNote != null;
        setWidgetVisible(headingButton, editorVisible);
        setWidgetVisible(boldButton, editorVisible);
        setWidgetVisible(italicButton, editorVisible);
        setWidgetVisible(listButton, editorVisible);
        setWidgetVisible(quoteButton, editorVisible);
        setWidgetVisible(codeButton, editorVisible);
        setWidgetVisible(editorWidget, editorVisible);
        if (editorVisible) {
            int left = editor.getX() + PANEL_PADDING;
            int toolbarY = editor.getY() + PANEL_HEADER_HEIGHT + 8;
            int toolbarRows = layoutToolbarButtons(left, toolbarY, editor.getWidth() - PANEL_PADDING * 2);
            int editorTop = toolbarY + toolbarRows * TOOLBAR_ROW_HEIGHT + 4;
            int editorHeight = Math.max(36, editor.getY() + editor.getHeight() - PANEL_PADDING - editorTop);
            editorWidget.setBounds(left, editorTop, editor.getWidth() - PANEL_PADDING * 2, editorHeight);
        }

        boolean titleEditorVisible = titleEditingNote != null && workspaceManager.contains(titleEditingNote);
        setWidgetVisible(titleEditorWidget, titleEditorVisible);
        titleEditorWidget.setEditable(titleEditorVisible);
        if (titleEditorVisible) {
            int headerX = Math.round(titleEditingNote.getX()) + 8;
            int headerY = Math.round(titleEditingNote.getY()) + 4;
            int scopeLabelWidth = font.width(titleEditingNote.getScope() == WorkspaceScope.GLOBAL ? "GLOBAL" : "CONTEXT");
            int titleWidth = Math.max(108, titleEditingNote.getRenderedWidth() - scopeLabelWidth - WorkspaceNoteRenderer.getRenderedActionStripWidth(titleEditingNote) - 44);
            int titleHeight = Math.max(18, WorkspaceNoteRenderer.getRenderedHeaderHeight(titleEditingNote) - 8);
            titleEditorWidget.setBounds(headerX, headerY, titleWidth, titleHeight);
        }
    }

    private void positionTopBarWidgets() {
        boolean topVisible = !overlayLayout.isTopBarCollapsed();
        navToggleButton.setX(Math.max(8, width - NAV_TOGGLE_SIZE - 8));
        navToggleButton.setY(6);
        setWidgetVisible(navToggleButton, true);

        boolean showUtilityButtons = width >= 360;
        boolean showContextButton = width >= 250;
        int right = navToggleButton.getX() - TOP_BUTTON_GAP;
        int resetX = showUtilityButtons ? right - RESET_BUTTON_WIDTH : right;
        int snapX = showUtilityButtons ? resetX - TOP_BUTTON_GAP - SNAP_BUTTON_WIDTH : resetX;
        int contextX = showContextButton ? snapX - TOP_BUTTON_GAP - ADD_CONTEXT_BUTTON_WIDTH : snapX;
        int globalX = showContextButton ? contextX - TOP_BUTTON_GAP - ADD_GLOBAL_BUTTON_WIDTH : contextX - TOP_BUTTON_GAP - ADD_GLOBAL_BUTTON_WIDTH;
        if (globalX < 36) {
            globalX = 36;
            if (showContextButton) {
                contextX = globalX + ADD_GLOBAL_BUTTON_WIDTH + TOP_BUTTON_GAP;
            }
        }

        addGlobalButton.setX(globalX);
        addGlobalButton.setY(6);
        addContextButton.setX(contextX);
        addContextButton.setY(6);
        snapButton.setX(snapX);
        snapButton.setY(6);
        resetLayoutButton.setX(resetX);
        resetLayoutButton.setY(6);

        setWidgetVisible(addGlobalButton, topVisible);
        setWidgetVisible(addContextButton, topVisible && showContextButton);
        setWidgetVisible(snapButton, topVisible && showUtilityButtons);
        setWidgetVisible(resetLayoutButton, topVisible && showUtilityButtons);
    }

    private void ensurePanelLayouts() {
        int top = workspaceTop();
        WorkspaceOverlayLayoutState.PanelLayout editor = overlayLayout.ensurePanel(
            WorkspaceOverlayPanel.EDITOR,
            width - EDITOR_PANEL_WIDTH - 16,
            top + 18,
            EDITOR_PANEL_WIDTH,
            Math.min(EDITOR_PANEL_HEIGHT, Math.max(170, height - top - 70))
        );
        boolean newSettingsPanel = overlayLayout.getPanel(WorkspaceOverlayPanel.SETTINGS) == null;
        WorkspaceOverlayLayoutState.PanelLayout settings = overlayLayout.ensurePanel(
            WorkspaceOverlayPanel.SETTINGS,
            width - SETTINGS_PANEL_WIDTH - 16,
            Math.min(height - PANEL_HEADER_HEIGHT - 8, editor.getY() + editor.getHeight() + 8),
            SETTINGS_PANEL_WIDTH,
            SETTINGS_PANEL_HEIGHT
        );
        if (newSettingsPanel) {
            settings.setMinimized(true);
        }

        for (WorkspaceOverlayPanel panel : WorkspaceOverlayPanel.values()) {
            if (!isPanelEnabled(panel)) {
                continue;
            }
            WorkspaceOverlayLayoutState.PanelLayout layout = panelLayout(panel);
            if (layout != null) {
                clampPanelToViewport(panel, layout);
            }
        }
        staggerPanelHeaders();
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

        WorkspaceNote note = workspaceManager.createNote(scope, SIDEBAR_RIGHT - 10, workspaceTop() + 18, width - 20, height - 20);
        clampNoteToViewport(note);
        workspaceManager.saveNote(note);
        selectNote(note);
    }

    private void selectNote(WorkspaceNote note) {
        if (titleEditingNote != null && titleEditingNote != note) {
            stopTitleEditing();
        }
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

    private void toggleTopBar() {
        overlayLayout.setTopBarCollapsed(!overlayLayout.isTopBarCollapsed());
        clampSelectedNoteToViewport();
        for (WorkspaceOverlayPanel panel : WorkspaceOverlayPanel.values()) {
            if (!isPanelEnabled(panel)) {
                continue;
            }
            WorkspaceOverlayLayoutState.PanelLayout layout = panelLayout(panel);
            if (layout != null) {
                clampPanelToViewport(panel, layout);
            }
        }
        persistOverlayLayout();
        positionWidgets();
        refreshButtonStates();
    }

    private void setSelectedTint(int tintColor) {
        if (selectedNote == null) {
            return;
        }
        selectedNote.setTintColor(tintColor);
        workspaceManager.saveNote(selectedNote);
        syncColorEditorWithSelection();
    }

    private void applyCustomTint() {
        if (selectedNote == null) {
            return;
        }
        Integer parsedTint = parseTint(colorEditorWidget.getValue());
        if (parsedTint == null) {
            syncColorEditorWithSelection();
            return;
        }
        setSelectedTint(parsedTint);
    }

    private void resetSelectedTint() {
        if (selectedNote == null) {
            return;
        }
        setSelectedTint(WorkspaceNote.defaultTintColor(selectedNote.getScope()));
    }

    private Integer parseTint(String value) {
        String text = value == null ? "" : value.trim();
        if (text.startsWith("#")) {
            text = text.substring(1);
        }
        if (text.length() != 6) {
            return null;
        }
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            boolean hex = character >= '0' && character <= '9'
                || character >= 'a' && character <= 'f'
                || character >= 'A' && character <= 'F';
            if (!hex) {
                return null;
            }
        }
        return Integer.parseInt(text, 16) & 0x00FFFFFF;
    }

    private void adjustCardOpacity(float delta) {
        if (selectedNote == null) {
            return;
        }
        selectedNote.setCardOpacity(clamp(selectedNote.getCardOpacity() + delta, 0.10F, 1.0F));
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
        deleteNote(selectedNote);
    }

    private void deleteNote(WorkspaceNote note) {
        if (note == null) {
            return;
        }
        if (titleEditingNote == note) {
            stopTitleEditing();
        }
        workspaceManager.deleteNote(note);
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
        editorWidget.setEditable(selectedNote != null && isPanelExpanded(WorkspaceOverlayPanel.EDITOR));
        editorWidget.setValue(selectedNote == null ? "" : selectedNote.getContent());
        syncingEditor = false;
        syncColorEditorWithSelection();
        refreshButtonStates();
    }

    private void syncColorEditorWithSelection() {
        if (colorEditorWidget == null) {
            return;
        }
        if (selectedNote == null) {
            colorEditorNote = null;
            colorEditorWidget.setValue("");
            return;
        }
        if (colorEditorNote != selectedNote || !colorEditorWidget.isFocused()) {
            colorEditorNote = selectedNote;
            colorEditorWidget.setValue(String.format("#%06X", selectedNote.getTintColor()));
        }
    }

    private void startTitleEditing(WorkspaceNote note) {
        titleEditingNote = note;
        titleEditorWidget.setValue(note.getTitle());
        titleEditorWidget.setEditable(true);
        titleEditorWidget.visible = true;
        titleEditorWidget.setFocused(true);
        positionWidgets();
    }

    private void stopTitleEditing() {
        titleEditingNote = null;
        titleEditorWidget.setFocused(false);
        titleEditorWidget.setEditable(false);
        titleEditorWidget.visible = false;
    }

    private boolean shouldStartTitleEditing(WorkspaceNote note) {
        long now = System.currentTimeMillis();
        boolean doubleClick = lastHeaderClickedNote == note && now - lastHeaderClickAt <= HEADER_DOUBLE_CLICK_MS;
        lastHeaderClickedNote = note;
        lastHeaderClickAt = now;
        return doubleClick;
    }

    private void refreshButtonStates() {
        boolean hasSelection = selectedNote != null;
        addContextButton.active = workspaceManager.hasContext();
        navToggleButton.setMessage(Component.literal(overlayLayout.isTopBarCollapsed() ? "v" : "^"));
        snapButton.setMessage(Component.literal(overlayLayout.isSnapToGrid() ? "Grid" : "Free"));

        scaleDownButton.active = hasSelection && isPanelExpanded(WorkspaceOverlayPanel.SETTINGS);
        scaleUpButton.active = hasSelection && isPanelExpanded(WorkspaceOverlayPanel.SETTINGS);
        opacityDownButton.active = hasSelection && isPanelExpanded(WorkspaceOverlayPanel.SETTINGS);
        opacityUpButton.active = hasSelection && isPanelExpanded(WorkspaceOverlayPanel.SETTINGS);
        applyTintButton.active = hasSelection && isPanelExpanded(WorkspaceOverlayPanel.SETTINGS);
        resetTintButton.active = hasSelection && isPanelExpanded(WorkspaceOverlayPanel.SETTINGS);
        headingButton.active = hasSelection && isPanelExpanded(WorkspaceOverlayPanel.EDITOR);
        boldButton.active = hasSelection && isPanelExpanded(WorkspaceOverlayPanel.EDITOR);
        italicButton.active = hasSelection && isPanelExpanded(WorkspaceOverlayPanel.EDITOR);
        listButton.active = hasSelection && isPanelExpanded(WorkspaceOverlayPanel.EDITOR);
        quoteButton.active = hasSelection && isPanelExpanded(WorkspaceOverlayPanel.EDITOR);
        codeButton.active = hasSelection && isPanelExpanded(WorkspaceOverlayPanel.EDITOR);

        for (Button swatchButton : swatchButtons) {
            swatchButton.active = hasSelection && isPanelExpanded(WorkspaceOverlayPanel.SETTINGS);
        }
    }

    private void clampSelectedNoteToViewport() {
        if (selectedNote == null) {
            return;
        }
        clampNoteToViewport(selectedNote);
    }

    private void clampNoteToViewport(WorkspaceNote note) {
        int top = workspaceTop();
        float maxWidth = Math.max(NOTE_MIN_WIDTH, (width - 16.0F) / Math.max(0.65F, note.getScale()));
        float maxHeight = Math.max(NOTE_MIN_HEIGHT, (height - top - 16.0F) / Math.max(0.65F, note.getScale()));
        note.setWidth(clamp(note.getWidth(), NOTE_MIN_WIDTH, maxWidth));
        note.setHeight(clamp(note.getHeight(), NOTE_MIN_HEIGHT, maxHeight));

        float visibleWidth = Math.min(note.getRenderedWidth(), NOTE_EDGE_VISIBILITY);
        float visibleHeight = Math.min(note.getRenderedHeight(), WorkspaceNoteRenderer.HEADER_HEIGHT + 16.0F);
        note.setX(clamp(note.getX(), 8.0F - note.getRenderedWidth() + visibleWidth, width - visibleWidth - 8.0F));
        note.setY(clamp(note.getY(), top + 4.0F, height - visibleHeight - 8.0F));
    }

    private void clampPanelToViewport(WorkspaceOverlayPanel panel, WorkspaceOverlayLayoutState.PanelLayout layout) {
        int top = workspaceTop();
        int maxWidth = Math.max(panelMinWidth(panel), width - 24);
        int maxHeight = Math.max(panelMinHeight(panel), height - top - 16);
        layout.setWidth(Math.max(panelMinWidth(panel), Math.min(layout.getWidth(), maxWidth)));
        layout.setHeight(Math.max(panelMinHeight(panel), Math.min(layout.getHeight(), maxHeight)));
        int visibleHeight = panelVisibleHeight(layout);
        int minX = 8 - layout.getWidth() + PANEL_EDGE_VISIBILITY;
        int maxX = width - PANEL_EDGE_VISIBILITY - 8;
        layout.setX(Math.round(clamp(layout.getX(), minX, maxX)));
        layout.setY(Math.round(clamp(layout.getY(), top + 4, height - visibleHeight - 8)));
    }

    private void staggerPanelHeaders() {
        List<WorkspaceOverlayPanel> orderedPanels = overlayLayout.orderedPanels();
        for (int index = 0; index < orderedPanels.size(); index++) {
            WorkspaceOverlayPanel panel = orderedPanels.get(index);
            if (!isPanelEnabled(panel)) {
                continue;
            }
            WorkspaceOverlayLayoutState.PanelLayout layout = panelLayout(panel);
            if (layout == null) {
                continue;
            }
            for (int previousIndex = 0; previousIndex < index; previousIndex++) {
                WorkspaceOverlayPanel previousPanel = orderedPanels.get(previousIndex);
                if (!isPanelEnabled(previousPanel)) {
                    continue;
                }
                WorkspaceOverlayLayoutState.PanelLayout previousLayout = panelLayout(previousPanel);
                if (previousLayout != null && headersOverlap(layout, previousLayout)) {
                    layout.setX(layout.getX() + 18);
                    layout.setY(layout.getY() + 18);
                    clampPanelToViewport(panel, layout);
                }
            }
        }
    }

    private boolean headersOverlap(WorkspaceOverlayLayoutState.PanelLayout first, WorkspaceOverlayLayoutState.PanelLayout second) {
        return first.getX() < second.right()
            && first.right() > second.getX()
            && first.getY() < second.getY() + PANEL_HEADER_HEIGHT
            && first.getY() + PANEL_HEADER_HEIGHT > second.getY();
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
            if (!isPanelEnabled(panel)) {
                continue;
            }
            WorkspaceOverlayLayoutState.PanelLayout layout = panelLayout(panel);
            if (layout != null && panelContains(layout, mouseX, mouseY)) {
                return panel;
            }
        }
        return null;
    }

    private WorkspaceOverlayLayoutState.PanelLayout panelLayout(WorkspaceOverlayPanel panel) {
        return overlayLayout.getPanel(panel);
    }

    private NoteRowHit findNoteRowHit(double mouseX, double mouseY) {
        if (!overlayLayout.isNoteDrawerOpen()) {
            return null;
        }
        int rowWidth = SIDEBAR_RIGHT - 24;
        int listBottom = drawerListBottom();
        if (mouseX < SIDEBAR_TEXT_X || mouseX > SIDEBAR_TEXT_X + rowWidth || mouseY < DRAWER_LIST_TOP || mouseY > listBottom) {
            return null;
        }
        int y = DRAWER_LIST_TOP - drawerScrollOffset;

        NoteRowHit hit = findNoteRowHit(mouseX, mouseY, SIDEBAR_TEXT_X, y, rowWidth, workspaceManager.getNotes(WorkspaceScope.GLOBAL), DRAWER_LIST_TOP, listBottom);
        if (hit != null) {
            return hit;
        }

        y = y + sectionHeight(workspaceManager.getNotes(WorkspaceScope.GLOBAL)) + 10;
        return findNoteRowHit(mouseX, mouseY, SIDEBAR_TEXT_X, y, rowWidth, workspaceManager.getNotes(WorkspaceScope.CONTEXT), DRAWER_LIST_TOP, listBottom);
    }

    private NoteRowHit findNoteRowHit(double mouseX, double mouseY, int x, int y, int rowWidth, List<WorkspaceNote> notes, int listTop, int listBottom) {
        int rowY = y + 14;
        for (WorkspaceNote note : notes) {
            int clippedTop = Math.max(rowY, listTop);
            int clippedBottom = Math.min(rowY + LIST_ROW_HEIGHT, listBottom);
            if (clippedTop < clippedBottom && mouseX >= x && mouseX <= x + rowWidth && mouseY >= clippedTop && mouseY <= clippedBottom) {
                if (mouseX >= x + rowWidth - DRAWER_ACTION_WIDTH) {
                    return new NoteRowHit(note, NoteRowAction.DELETE);
                }
                if (mouseX >= x + rowWidth - DRAWER_ACTION_WIDTH * 2) {
                    return new NoteRowHit(note, NoteRowAction.VISIBILITY);
                }
                return new NoteRowHit(note, NoteRowAction.SELECT);
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

    private int drawerContentHeight() {
        return sectionHeight(workspaceManager.getNotes(WorkspaceScope.GLOBAL)) + 10 + sectionHeight(workspaceManager.getNotes(WorkspaceScope.CONTEXT));
    }

    private int drawerListBottom() {
        return Math.max(DRAWER_LIST_TOP + 16, height - DRAWER_LIST_BOTTOM_PADDING);
    }

    private int maxDrawerScroll() {
        return Math.max(0, drawerContentHeight() - (drawerListBottom() - DRAWER_LIST_TOP));
    }

    private void clampDrawerScroll() {
        drawerScrollOffset = Math.max(0, Math.min(drawerScrollOffset, maxDrawerScroll()));
    }

    private void scrollDrawer(double amount) {
        drawerScrollOffset -= (int) Math.signum(amount) * LIST_ROW_HEIGHT;
        clampDrawerScroll();
    }

    private float snap(float value, boolean shiftDown) {
        if (!overlayLayout.isSnapToGrid() || shiftDown) {
            return value;
        }
        return Math.round(value / SNAP_SIZE) * SNAP_SIZE;
    }

    private void renderDrawerToggle(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        int left = SIDEBAR_X;
        int top = drawerToggleTop();
        boolean hovered = isDrawerToggleHit(mouseX, mouseY);
        guiGraphics.fill(left, top, left + DRAWER_TOGGLE_WIDTH, top + DRAWER_TOGGLE_HEIGHT, hovered ? 0xC02A3A4E : 0xA01F2B39);
        guiGraphics.text(font, Component.literal(overlayLayout.isNoteDrawerOpen() ? "Hide Notes" : "Show Notes"), left + 8, top + 6, 0xFFF5F7FA, false);
    }

    private boolean isDrawerToggleHit(double mouseX, double mouseY) {
        int left = SIDEBAR_X;
        int top = drawerToggleTop();
        return mouseX >= left && mouseX <= left + DRAWER_TOGGLE_WIDTH && mouseY >= top && mouseY <= top + DRAWER_TOGGLE_HEIGHT;
    }

    private boolean isDrawerBodyHit(double mouseX, double mouseY) {
        return mouseX >= SIDEBAR_X && mouseX <= SIDEBAR_RIGHT && mouseY >= 36 && mouseY <= height - 8;
    }

    private int drawerToggleTop() {
        return overlayLayout.isTopBarCollapsed() ? 8 : TOP_BAR_HEIGHT + 8;
    }

    private int workspaceTop() {
        return overlayLayout.isTopBarCollapsed() ? 0 : TOP_BAR_HEIGHT;
    }

    private boolean isPanelEnabled(WorkspaceOverlayPanel panel) {
        return panel != WorkspaceOverlayPanel.PREVIEW;
    }

    private boolean isPanelExpanded(WorkspaceOverlayPanel panel) {
        WorkspaceOverlayLayoutState.PanelLayout layout = panelLayout(panel);
        return layout != null && !layout.isMinimized();
    }

    private void setWidgetVisible(AbstractWidget widget, boolean visible) {
        widget.visible = visible;
        if (!visible) {
            widget.setFocused(false);
        }
    }

    private int panelVisibleHeight(WorkspaceOverlayLayoutState.PanelLayout layout) {
        return layout.isMinimized() ? PANEL_HEADER_HEIGHT : layout.getHeight();
    }

    private int panelBottom(WorkspaceOverlayLayoutState.PanelLayout layout) {
        return layout.getY() + panelVisibleHeight(layout);
    }

    private boolean panelContains(WorkspaceOverlayLayoutState.PanelLayout layout, double mouseX, double mouseY) {
        return mouseX >= layout.getX() && mouseX <= layout.right() && mouseY >= layout.getY() && mouseY <= panelBottom(layout);
    }

    private boolean panelHeaderContains(WorkspaceOverlayLayoutState.PanelLayout layout, double mouseX, double mouseY) {
        return mouseX >= layout.getX() && mouseX <= layout.right() && mouseY >= layout.getY() && mouseY <= layout.getY() + PANEL_HEADER_HEIGHT;
    }

    private boolean isPanelMinimizeHit(WorkspaceOverlayLayoutState.PanelLayout layout, double mouseX, double mouseY) {
        int left = layout.right() - PANEL_CONTROL_SIZE - 8;
        int top = layout.getY() + 4;
        return mouseX >= left && mouseX <= left + PANEL_CONTROL_SIZE && mouseY >= top && mouseY <= top + PANEL_CONTROL_SIZE;
    }

    private boolean isPanelResizeHandleHit(WorkspaceOverlayLayoutState.PanelLayout layout, double mouseX, double mouseY) {
        int bottom = panelBottom(layout);
        return mouseX >= layout.right() - PANEL_RESIZE_HANDLE_SIZE && mouseX <= layout.right()
            && mouseY >= bottom - PANEL_RESIZE_HANDLE_SIZE && mouseY <= bottom;
    }

    private int panelMinWidth(WorkspaceOverlayPanel panel) {
        return switch (panel) {
            case SETTINGS -> 336;
            case EDITOR -> 320;
            case PREVIEW -> 280;
        };
    }

    private int layoutToolbarButtons(int left, int toolbarY, int contentWidth) {
        Button[] buttons = {headingButton, boldButton, italicButton, listButton, quoteButton, codeButton};
        int row = 0;
        int x = left;
        int rowRight = left + Math.max(TOOLBAR_BUTTON_WIDTH, contentWidth);
        for (Button button : buttons) {
            if (x > left && x + TOOLBAR_BUTTON_WIDTH > rowRight) {
                row++;
                x = left;
            }
            button.setX(x);
            button.setY(toolbarY + row * TOOLBAR_ROW_HEIGHT);
            x += TOOLBAR_BUTTON_WIDTH + TOOLBAR_BUTTON_GAP;
        }
        return row + 1;
    }

    private void renderSettingsWidgets(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderWidget(scaleDownButton, guiGraphics, mouseX, mouseY, partialTick);
        renderWidget(scaleUpButton, guiGraphics, mouseX, mouseY, partialTick);
        renderWidget(opacityDownButton, guiGraphics, mouseX, mouseY, partialTick);
        renderWidget(opacityUpButton, guiGraphics, mouseX, mouseY, partialTick);
        for (Button swatchButton : swatchButtons) {
            renderWidget(swatchButton, guiGraphics, mouseX, mouseY, partialTick);
        }
        renderWidget(colorEditorWidget, guiGraphics, mouseX, mouseY, partialTick);
        renderWidget(applyTintButton, guiGraphics, mouseX, mouseY, partialTick);
        renderWidget(resetTintButton, guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderEditorWidgets(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderWidget(headingButton, guiGraphics, mouseX, mouseY, partialTick);
        renderWidget(boldButton, guiGraphics, mouseX, mouseY, partialTick);
        renderWidget(italicButton, guiGraphics, mouseX, mouseY, partialTick);
        renderWidget(listButton, guiGraphics, mouseX, mouseY, partialTick);
        renderWidget(quoteButton, guiGraphics, mouseX, mouseY, partialTick);
        renderWidget(codeButton, guiGraphics, mouseX, mouseY, partialTick);
        renderWidget(editorWidget, guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderWidget(AbstractWidget widget, GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (widget.visible) {
            widget.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    private boolean mouseClickedTopLevelWidgets(MouseButtonEvent context) {
        boolean[] previousVisibility = setPanelWidgetsRawVisible(false);
        boolean handled = super.mouseClicked(context, false);
        restorePanelWidgetVisibility(previousVisibility);
        return handled;
    }

    private boolean mouseClickedPanelWidgets(WorkspaceOverlayPanel panel, MouseButtonEvent context) {
        if (panel == WorkspaceOverlayPanel.SETTINGS) {
            return clickWidget(scaleDownButton, context)
                || clickWidget(scaleUpButton, context)
                || clickWidget(opacityDownButton, context)
                || clickWidget(opacityUpButton, context)
                || clickSwatchWidgets(context)
                || clickWidget(colorEditorWidget, context)
                || clickWidget(applyTintButton, context)
                || clickWidget(resetTintButton, context);
        }
        if (panel == WorkspaceOverlayPanel.EDITOR) {
            return clickWidget(headingButton, context)
                || clickWidget(boldButton, context)
                || clickWidget(italicButton, context)
                || clickWidget(listButton, context)
                || clickWidget(quoteButton, context)
                || clickWidget(codeButton, context)
                || clickWidget(editorWidget, context);
        }
        return false;
    }

    private boolean clickSwatchWidgets(MouseButtonEvent context) {
        for (Button swatchButton : swatchButtons) {
            if (clickWidget(swatchButton, context)) {
                return true;
            }
        }
        return false;
    }

    private boolean clickWidget(AbstractWidget widget, MouseButtonEvent context) {
        if (widget.visible && widget.mouseClicked(context, false)) {
            setFocused(widget);
            return true;
        }
        return false;
    }

    private boolean[] setPanelWidgetsRawVisible(boolean visible) {
        AbstractWidget[] widgets = panelWidgets();
        boolean[] previousVisibility = new boolean[widgets.length];
        for (int index = 0; index < widgets.length; index++) {
            previousVisibility[index] = widgets[index].visible;
            widgets[index].visible = visible && widgets[index].visible;
        }
        return previousVisibility;
    }

    private void restorePanelWidgetVisibility(boolean[] previousVisibility) {
        AbstractWidget[] widgets = panelWidgets();
        for (int index = 0; index < widgets.length && index < previousVisibility.length; index++) {
            widgets[index].visible = previousVisibility[index];
        }
    }

    private AbstractWidget[] panelWidgets() {
        return new AbstractWidget[]{
            scaleDownButton,
            scaleUpButton,
            opacityDownButton,
            opacityUpButton,
            swatchButtons[0],
            swatchButtons[1],
            swatchButtons[2],
            swatchButtons[3],
            swatchButtons[4],
            swatchButtons[5],
            colorEditorWidget,
            applyTintButton,
            resetTintButton,
            headingButton,
            boldButton,
            italicButton,
            listButton,
            quoteButton,
            codeButton,
            editorWidget
        };
    }

    private void drawTrimmedString(GuiGraphicsExtractor guiGraphics, String text, int x, int y, int maxWidth, int color) {
        if (maxWidth <= 0) {
            return;
        }
        guiGraphics.text(font, trimToWidth(text == null ? "" : text, maxWidth), x, y, color, false);
    }

    private String trimToWidth(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        int end = text.length();
        while (end > 1 && font.width(text.substring(0, end) + ellipsis) > maxWidth) {
            end--;
        }
        return text.substring(0, Math.max(0, end)) + ellipsis;
    }

    private int panelMinHeight(WorkspaceOverlayPanel panel) {
        return switch (panel) {
            case SETTINGS -> 220;
            case EDITOR -> 188;
            case PREVIEW -> 176;
        };
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
        DRAG,
        RESIZE
    }

    private enum NoteRowAction {
        SELECT,
        VISIBILITY,
        DELETE
    }

    private record NoteRowHit(WorkspaceNote note, NoteRowAction action) {
    }
}
