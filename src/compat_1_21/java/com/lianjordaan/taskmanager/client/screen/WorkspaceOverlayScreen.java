package com.lianjordaan.taskmanager.client.screen;

import com.lianjordaan.taskmanager.client.render.MarkdownRenderer;
import com.lianjordaan.taskmanager.client.render.WorkspaceNoteRenderer;
import com.lianjordaan.taskmanager.client.widget.MultilineTextEditorWidget;
import com.lianjordaan.taskmanager.client.workspace.WorkspaceContext;
import com.lianjordaan.taskmanager.client.workspace.WorkspaceManager;
import com.lianjordaan.taskmanager.client.workspace.WorkspaceNote;
import com.lianjordaan.taskmanager.client.workspace.WorkspaceScope;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class WorkspaceOverlayScreen extends Screen {
    private static final int TOP_BAR_HEIGHT = 32;
    private static final int SIDEBAR_WIDTH = 196;
    private static final int INSPECTOR_WIDTH = 304;
    private static final int PADDING = 12;
    private static final int LIST_ROW_HEIGHT = 22;
    private static final int SNAP_SIZE = 12;

    private final WorkspaceManager workspaceManager = WorkspaceManager.getInstance();

    private boolean quickCreateRequested;
    private boolean previewVisible = true;
    private boolean snapToGrid = true;
    private boolean syncingEditor;
    private boolean layoutDirty;

    private WorkspaceNote selectedNote;
    private InteractionMode interactionMode = InteractionMode.NONE;
    private float interactionOffsetX;
    private float interactionOffsetY;
    private float interactionStartWidth;
    private float interactionStartHeight;
    private float interactionStartMouseX;
    private float interactionStartMouseY;

    private Button addGlobalButton;
    private Button addContextButton;
    private Button previewButton;
    private Button snapButton;
    private Button hideButton;
    private Button lockButton;
    private Button deleteButton;
    private Button scaleDownButton;
    private Button scaleUpButton;
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

        int inspectorLeft = inspectorLeft();
        int toolbarY = 104;
        int previewHeight = previewVisible ? 144 : 28;
        int editorY = 208;
        int editorHeight = Math.max(92, height - editorY - previewHeight - 20);

        clearWidgets();

        addGlobalButton = addRenderableWidget(Button.builder(Component.literal("Add Global"), button -> createNote(WorkspaceScope.GLOBAL))
            .bounds(216, 6, 92, 20)
            .build());
        addContextButton = addRenderableWidget(Button.builder(Component.literal("Add Context"), button -> createNote(WorkspaceScope.CONTEXT))
            .bounds(314, 6, 96, 20)
            .build());

        previewButton = addRenderableWidget(Button.builder(Component.literal("Preview"), button -> previewVisible = !previewVisible)
            .bounds(inspectorLeft + 12, toolbarY, 78, 20)
            .build());
        snapButton = addRenderableWidget(Button.builder(Component.literal("Snap"), button -> snapToGrid = !snapToGrid)
            .bounds(inspectorLeft + 96, toolbarY, 66, 20)
            .build());
        hideButton = addRenderableWidget(Button.builder(Component.literal("Hide"), button -> toggleHidden())
            .bounds(inspectorLeft + 168, toolbarY, 76, 20)
            .build());
        lockButton = addRenderableWidget(Button.builder(Component.literal("Lock"), button -> toggleLocked())
            .bounds(inspectorLeft + 12, toolbarY + 24, 78, 20)
            .build());
        deleteButton = addRenderableWidget(Button.builder(Component.literal("Delete"), button -> deleteSelected())
            .bounds(inspectorLeft + 96, toolbarY + 24, 76, 20)
            .build());
        scaleDownButton = addRenderableWidget(Button.builder(Component.literal("-"), button -> adjustScale(-0.05F))
            .bounds(inspectorLeft + 178, toolbarY + 24, 32, 20)
            .build());
        scaleUpButton = addRenderableWidget(Button.builder(Component.literal("+"), button -> adjustScale(0.05F))
            .bounds(inspectorLeft + 216, toolbarY + 24, 32, 20)
            .build());

        headingButton = addRenderableWidget(Button.builder(Component.literal("H1"), button -> insertSnippet("# ", 0))
            .bounds(inspectorLeft + 12, toolbarY + 54, 42, 20)
            .build());
        boldButton = addRenderableWidget(Button.builder(Component.literal("B"), button -> insertSnippet("****", 2))
            .bounds(inspectorLeft + 58, toolbarY + 54, 42, 20)
            .build());
        italicButton = addRenderableWidget(Button.builder(Component.literal("I"), button -> insertSnippet("**", 1))
            .bounds(inspectorLeft + 104, toolbarY + 54, 42, 20)
            .build());
        listButton = addRenderableWidget(Button.builder(Component.literal("Li"), button -> insertSnippet("- ", 0))
            .bounds(inspectorLeft + 150, toolbarY + 54, 42, 20)
            .build());
        quoteButton = addRenderableWidget(Button.builder(Component.literal(">"), button -> insertSnippet("> ", 0))
            .bounds(inspectorLeft + 196, toolbarY + 54, 42, 20)
            .build());
        codeButton = addRenderableWidget(Button.builder(Component.literal("{}"), button -> insertSnippet("```\n\n```", 4))
            .bounds(inspectorLeft + 242, toolbarY + 54, 42, 20)
            .build());

        editorWidget = addRenderableWidget(new MultilineTextEditorWidget(font, inspectorLeft + 12, editorY, INSPECTOR_WIDTH - 24, editorHeight));
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

        refreshButtonStates();
        renderWorkspaceBackground(guiGraphics);
        guiGraphics.fill(0, 0, width, height, 0xB0121622);
        guiGraphics.fill(0, 0, width, TOP_BAR_HEIGHT, 0xF01B2230);
        guiGraphics.fill(8, 36, SIDEBAR_WIDTH, height - 8, 0xED1A2130);
        guiGraphics.fill(inspectorLeft(), 36, width - 8, height - 8, 0xED1A2130);
        guiGraphics.fill(canvasLeft(), canvasTop(), canvasRight(), canvasBottom(), 0xCC101722);

        guiGraphics.drawString(font, title, 14, 10, 0xFFF5F7FA, false);
        WorkspaceContext context = workspaceManager.getCurrentContext();
        guiGraphics.drawString(font, Component.literal(context.isAvailable() ? "Global + " + context.getLabel() : "Global workspace"), 118, 10, 0xFF8FB3C9, false);
        guiGraphics.drawString(font, Component.literal("Notes"), 18, 44, 0xFFF5F7FA, false);
        guiGraphics.drawString(font, Component.literal("Inspector"), inspectorLeft() + 10, 44, 0xFFF5F7FA, false);
        guiGraphics.drawString(font, Component.literal("Canvas"), canvasLeft() + 8, 44, 0xFFF5F7FA, false);

        renderSidebar(guiGraphics, mouseX, mouseY);
        renderCanvas(guiGraphics);
        renderInspector(guiGraphics);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
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
        if (super.mouseClicked(context, false)) {
            return true;
        }

        if (context.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }

        double mouseX = context.x();
        double mouseY = context.y();
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

        if (mouseX >= canvasLeft() && mouseX <= canvasRight() && mouseY >= canvasTop() && mouseY <= canvasBottom()) {
            WorkspaceNote hoveredNote = findTopmostNote(mouseX, mouseY);
            if (hoveredNote != null) {
                selectNote(hoveredNote);
                workspaceManager.bringToFront(hoveredNote);
                if (!hoveredNote.isLocked() && WorkspaceNoteRenderer.isResizeHandleHit(hoveredNote, mouseX, mouseY)) {
                    interactionMode = InteractionMode.RESIZE;
                    interactionStartWidth = hoveredNote.getWidth();
                    interactionStartHeight = hoveredNote.getHeight();
                    interactionStartMouseX = (float) mouseX;
                    interactionStartMouseY = (float) mouseY;
                } else if (!hoveredNote.isLocked() && WorkspaceNoteRenderer.isHeaderHit(hoveredNote, mouseX, mouseY)) {
                    interactionMode = InteractionMode.DRAG;
                    interactionOffsetX = (float) mouseX - hoveredNote.getX();
                    interactionOffsetY = (float) mouseY - hoveredNote.getY();
                }
                return true;
            }

            selectedNote = null;
            syncEditorWithSelection();
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent context, double dragX, double dragY) {
        if (selectedNote == null || context.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT || interactionMode == InteractionMode.NONE) {
            return super.mouseDragged(context, dragX, dragY);
        }

        layoutDirty = true;
        if (interactionMode == InteractionMode.DRAG) {
            selectedNote.setX(clamp(snap((float) context.x() - interactionOffsetX, context.hasShiftDown()), canvasLeft() + 6.0F, canvasRight() - selectedNote.getRenderedWidth() - 6.0F));
            selectedNote.setY(clamp(snap((float) context.y() - interactionOffsetY, context.hasShiftDown()), canvasTop() + 6.0F, canvasBottom() - selectedNote.getRenderedHeight() - 6.0F));
        } else if (interactionMode == InteractionMode.RESIZE) {
            float widthDelta = ((float) context.x() - interactionStartMouseX) / Math.max(0.65F, selectedNote.getScale());
            float heightDelta = ((float) context.y() - interactionStartMouseY) / Math.max(0.65F, selectedNote.getScale());
            selectedNote.setWidth(Math.max(160.0F, interactionStartWidth + widthDelta));
            selectedNote.setHeight(Math.max(96.0F, interactionStartHeight + heightDelta));
        }

        clampSelectedNoteToCanvas();
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent context) {
        if (interactionMode != InteractionMode.NONE && context.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            interactionMode = InteractionMode.NONE;
            if (layoutDirty && selectedNote != null) {
                workspaceManager.saveNote(selectedNote);
                layoutDirty = false;
            }
            return true;
        }

        return super.mouseReleased(context);
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
    public boolean keyPressed(KeyEvent context) {
        if (super.keyPressed(context)) {
            return true;
        }

        if (context.key() == GLFW.GLFW_KEY_DELETE && selectedNote != null && !editorWidget.isFocused()) {
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
        int gridColor = 0x221D2735;
        for (int x = canvasLeft(); x < canvasRight(); x += 16) {
            guiGraphics.fill(x, canvasTop(), x + 1, canvasBottom(), gridColor);
        }
        for (int y = canvasTop(); y < canvasBottom(); y += 16) {
            guiGraphics.fill(canvasLeft(), y, canvasRight(), y + 1, gridColor);
        }
    }

    private void renderSidebar(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int listX = 18;
        int rowWidth = SIDEBAR_WIDTH - 24;
        int y = 64;

        WorkspaceContext context = workspaceManager.getCurrentContext();
        guiGraphics.drawString(font, Component.literal("Visible notes float on the HUD."), listX, y, 0xFFD7E0EA, false);
        guiGraphics.drawString(font, Component.literal(context.isAvailable() ? "Context: " + context.getLabel() : "No active context"), listX, y + 14, 0xFF8FB3C9, false);
        guiGraphics.drawString(font, Component.literal("Click a row to edit or H/S to hide."), listX, y + 28, 0xFF9DB0C3, false);

        y = 108;
        y = renderNoteSection(guiGraphics, mouseX, mouseY, listX, y, rowWidth, "Global", workspaceManager.getNotes(WorkspaceScope.GLOBAL));
        renderNoteSection(guiGraphics, mouseX, mouseY, listX, y + 10, rowWidth, context.isAvailable() ? "Context" : "Context (inactive)", workspaceManager.getNotes(WorkspaceScope.CONTEXT));
    }

    private int renderNoteSection(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y, int rowWidth, String label, List<WorkspaceNote> notes) {
        guiGraphics.drawString(font, Component.literal(label), x, y, 0xFFF5F7FA, false);
        int rowY = y + 14;
        for (WorkspaceNote note : notes) {
            boolean hovered = mouseX >= x && mouseX <= x + rowWidth && mouseY >= rowY && mouseY <= rowY + LIST_ROW_HEIGHT;
            boolean selected = note == selectedNote;
            guiGraphics.fill(x, rowY, x + rowWidth, rowY + LIST_ROW_HEIGHT, selected ? 0xA03A4A60 : hovered ? 0x60344456 : 0x40242D3A);
            guiGraphics.fill(x, rowY, x + 4, rowY + LIST_ROW_HEIGHT, note.getScope() == WorkspaceScope.GLOBAL ? 0xFF58BFD7 : 0xFFF0B96B);
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

    private void renderCanvas(GuiGraphics guiGraphics) {
        guiGraphics.drawString(font, Component.literal("Drag the header, resize from the lower-right corner, scroll to scale."), canvasLeft() + 12, canvasTop() + 10, 0xFF8FA3B9, false);
        for (WorkspaceNote note : workspaceManager.getCombinedNotes(true)) {
            WorkspaceNoteRenderer.render(guiGraphics, font, note, true, note == selectedNote);
        }

        if (workspaceManager.getCombinedNotes(true).isEmpty()) {
            guiGraphics.drawString(font, Component.literal("Press N for a quick context note or add one from the top bar."), canvasLeft() + 20, canvasTop() + 42, 0xFFE7ECF5, false);
            guiGraphics.drawString(font, Component.literal("Visible notes persist to config/taskmanager and float in-game."), canvasLeft() + 20, canvasTop() + 56, 0xFF8FB3C9, false);
        }
    }

    private void renderInspector(GuiGraphics guiGraphics) {
        int left = inspectorLeft() + 12;
        int previewHeight = previewVisible ? 144 : 28;
        int previewTop = height - previewHeight - 12;

        if (selectedNote == null) {
            guiGraphics.drawString(font, Component.literal("Select a note to edit markdown, visibility, and layout."), left, 64, 0xFFD7E0EA, false);
            guiGraphics.drawString(font, Component.literal("Global notes follow every world. Context notes are per server or save."), left, 80, 0xFF9DB0C3, false);
            guiGraphics.drawString(font, Component.literal("Toolbar snippets help you sketch headings, lists, quotes, and code blocks."), left, 94, 0xFF9DB0C3, false);
        } else {
            guiGraphics.drawString(font, Component.literal(selectedNote.getSummary()), left, 64, 0xFFF5F7FA, false);
            guiGraphics.drawString(font, Component.literal("Scope: " + selectedNote.getScope().getDisplayName()), left, 80, 0xFF8FB3C9, false);
            guiGraphics.drawString(font, Component.literal("State: " + (selectedNote.isHidden() ? "Hidden" : "Visible") + " | " + (selectedNote.isLocked() ? "Locked" : "Free")), left, 94, 0xFFD7E0EA, false);
            guiGraphics.drawString(font, Component.literal("Scale: " + Math.round(selectedNote.getScale() * 100.0F) + "%"), left + 174, 132, 0xFFD7E0EA, false);
        }

        guiGraphics.fill(left, previewTop, width - 20, height - 12, 0xE918202C);
        guiGraphics.fill(left - 1, previewTop - 1, width - 19, height - 11, 0x6039495B);
        guiGraphics.drawString(font, Component.literal(previewVisible ? "Preview" : "Preview hidden"), left + 8, previewTop + 8, 0xFFF5F7FA, false);

        if (previewVisible && selectedNote != null) {
            MarkdownRenderer.render(guiGraphics, font, selectedNote.getContent(), left + 8, previewTop + 26, INSPECTOR_WIDTH - 40, previewHeight - 36, 255);
        } else if (previewVisible) {
            guiGraphics.drawString(font, Component.literal("Select a note to preview its markdown."), left + 8, previewTop + 28, 0xFF9DB0C3, false);
        }
    }

    private void createNote(WorkspaceScope scope) {
        if (scope == WorkspaceScope.CONTEXT && !workspaceManager.hasContext()) {
            return;
        }

        WorkspaceNote note = workspaceManager.createNote(scope, canvasLeft(), canvasTop(), canvasRight(), canvasBottom());
        selectNote(note);
    }

    private void selectNote(WorkspaceNote note) {
        selectedNote = note;
        syncEditorWithSelection();
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
        clampSelectedNoteToCanvas();
        workspaceManager.saveNote(selectedNote);
        refreshButtonStates();
    }

    private void insertSnippet(String snippet, int cursorBacktrack) {
        if (selectedNote == null) {
            return;
        }
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
        previewButton.setMessage(Component.literal(previewVisible ? "Preview On" : "Preview Off"));
        snapButton.setMessage(Component.literal(snapToGrid ? "Snap On" : "Snap Off"));

        hideButton.active = hasSelection;
        lockButton.active = hasSelection;
        deleteButton.active = hasSelection;
        scaleDownButton.active = hasSelection;
        scaleUpButton.active = hasSelection;
        headingButton.active = hasSelection;
        boldButton.active = hasSelection;
        italicButton.active = hasSelection;
        listButton.active = hasSelection;
        quoteButton.active = hasSelection;
        codeButton.active = hasSelection;

        hideButton.setMessage(Component.literal(hasSelection && selectedNote.isHidden() ? "Unhide" : "Hide"));
        lockButton.setMessage(Component.literal(hasSelection && selectedNote.isLocked() ? "Unlock" : "Lock"));
    }

    private void clampSelectedNoteToCanvas() {
        if (selectedNote == null) {
            return;
        }

        float maxWidth = Math.max(160.0F, (canvasRight() - canvasLeft() - 12.0F) / Math.max(0.65F, selectedNote.getScale()));
        float maxHeight = Math.max(96.0F, (canvasBottom() - canvasTop() - 12.0F) / Math.max(0.65F, selectedNote.getScale()));
        selectedNote.setWidth(clamp(selectedNote.getWidth(), 160.0F, maxWidth));
        selectedNote.setHeight(clamp(selectedNote.getHeight(), 96.0F, maxHeight));

        selectedNote.setX(clamp(selectedNote.getX(), canvasLeft() + 6.0F, canvasRight() - selectedNote.getRenderedWidth() - 6.0F));
        selectedNote.setY(clamp(selectedNote.getY(), canvasTop() + 6.0F, canvasBottom() - selectedNote.getRenderedHeight() - 6.0F));
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

    private NoteRowHit findNoteRowHit(double mouseX, double mouseY) {
        int x = 18;
        int rowWidth = SIDEBAR_WIDTH - 24;
        int y = 122;

        NoteRowHit hit = findNoteRowHit(mouseX, mouseY, x, y, rowWidth, workspaceManager.getNotes(WorkspaceScope.GLOBAL));
        if (hit != null) {
            return hit;
        }

        y = y + sectionHeight(workspaceManager.getNotes(WorkspaceScope.GLOBAL)) + 24;
        return findNoteRowHit(mouseX, mouseY, x, y, rowWidth, workspaceManager.getNotes(WorkspaceScope.CONTEXT));
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

    private float snap(float value, boolean shiftDown) {
        if (!snapToGrid || shiftDown) {
            return value;
        }
        return Math.round(value / SNAP_SIZE) * SNAP_SIZE;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private int canvasLeft() {
        return SIDEBAR_WIDTH + PADDING;
    }

    private int canvasRight() {
        return width - INSPECTOR_WIDTH - PADDING;
    }

    private int canvasTop() {
        return TOP_BAR_HEIGHT + PADDING;
    }

    private int canvasBottom() {
        return height - PADDING;
    }

    private int inspectorLeft() {
        return width - INSPECTOR_WIDTH;
    }

    private enum InteractionMode {
        NONE,
        DRAG,
        RESIZE
    }

    private record NoteRowHit(WorkspaceNote note, boolean visibilityToggle) {
    }
}