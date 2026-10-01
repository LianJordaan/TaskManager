package com.lianjordaan.taskmanager.client.workspace;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.server.IntegratedServer;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class WorkspaceManager {
    private static final WorkspaceManager INSTANCE = new WorkspaceManager();
    private static final Comparator<WorkspaceNote> NOTE_ORDER = Comparator.comparingLong(WorkspaceNote::getZIndex);

    private final WorkspaceStorage storage = new WorkspaceStorage();
    private final List<WorkspaceNote> globalNotes = new ArrayList<>();
    private List<WorkspaceNote> contextNotes = new ArrayList<>();

    private WorkspaceContext currentContext = WorkspaceContext.none();
    private boolean initialized;
    private long nextZIndex = 1L;

    private WorkspaceManager() {
    }

    public static WorkspaceManager getInstance() {
        return INSTANCE;
    }

    public void initialize() {
        if (initialized) {
            return;
        }

        globalNotes.clear();
        globalNotes.addAll(storage.loadGlobalNotes());
        recalculateNextZIndex();
        initialized = true;
    }

    public void refreshContext(Minecraft client) {
        initialize();

        WorkspaceContext resolvedContext = resolveContext(client);
        if (resolvedContext.equals(currentContext)) {
            return;
        }

        currentContext = resolvedContext;
        contextNotes = currentContext.isAvailable() ? storage.loadContextNotes(currentContext) : new ArrayList<>();
        recalculateNextZIndex();
    }

    public WorkspaceContext getCurrentContext() {
        return currentContext;
    }

    public boolean hasContext() {
        return currentContext.isAvailable();
    }

    public List<WorkspaceNote> getNotes(WorkspaceScope scope) {
        return scope == WorkspaceScope.GLOBAL ? globalNotes : contextNotes;
    }

    public List<WorkspaceNote> getCombinedNotes(boolean includeHidden) {
        List<WorkspaceNote> orderedNotes = new ArrayList<>();
        orderedNotes.addAll(globalNotes);
        orderedNotes.addAll(contextNotes);
        if (!includeHidden) {
            orderedNotes.removeIf(WorkspaceNote::isHidden);
        }
        orderedNotes.sort(NOTE_ORDER);
        return orderedNotes;
    }

    public boolean contains(WorkspaceNote note) {
        return globalNotes.contains(note) || contextNotes.contains(note);
    }

    public WorkspaceNote createNote(WorkspaceScope scope, int minX, int minY, int maxX, int maxY) {
        List<WorkspaceNote> notes = getNotes(scope);
        int existingCount = notes.size();
        float noteWidth = 230.0F;
        float noteHeight = 150.0F;
        float x = Math.max(minX + 24.0F, Math.min(maxX - noteWidth - 12.0F, minX + 28.0F + existingCount * 18.0F));
        float y = Math.max(minY + 24.0F, Math.min(maxY - noteHeight - 12.0F, minY + 28.0F + existingCount * 14.0F));

        WorkspaceNote note = new WorkspaceNote(
            UUID.randomUUID(),
            scope,
            "# Tasks\n\n- [ ] New task",
            x,
            y,
            noteWidth,
            noteHeight,
            1.0F,
            false,
            false,
            nextZIndex++
        );
        notes.add(note);
        persistScope(scope);
        return note;
    }

    public void deleteNote(WorkspaceNote note) {
        getNotes(note.getScope()).remove(note);
        persistScope(note.getScope());
    }

    public void bringToFront(WorkspaceNote note) {
        note.setZIndex(nextZIndex++);
        persistScope(note.getScope());
    }

    public void saveNote(WorkspaceNote note) {
        persistScope(note.getScope());
    }

    public WorkspaceScope getPreferredCreationScope() {
        return hasContext() ? WorkspaceScope.CONTEXT : WorkspaceScope.GLOBAL;
    }

    private void persistScope(WorkspaceScope scope) {
        if (scope == WorkspaceScope.GLOBAL) {
            storage.saveGlobalNotes(globalNotes);
            return;
        }

        if (currentContext.isAvailable()) {
            storage.saveContextNotes(currentContext, contextNotes);
        }
    }

    private void recalculateNextZIndex() {
        nextZIndex = 1L;
        for (WorkspaceNote note : globalNotes) {
            nextZIndex = Math.max(nextZIndex, note.getZIndex() + 1L);
        }
        for (WorkspaceNote note : contextNotes) {
            nextZIndex = Math.max(nextZIndex, note.getZIndex() + 1L);
        }
    }

    private WorkspaceContext resolveContext(Minecraft client) {
        if (client == null || client.player == null) {
            return WorkspaceContext.none();
        }

        IntegratedServer integratedServer = client.getSingleplayerServer();
        if (integratedServer != null) {
            String levelName = integratedServer.getWorldData().getLevelName();
            String key = "singleplayer-" + WorkspaceStorage.sanitizeFragment(levelName) + "-" + Integer.toHexString(levelName.hashCode());
            return WorkspaceContext.singleplayer(key, levelName);
        }

        ClientPacketListener connection = client.getConnection();
        if (connection == null) {
            return WorkspaceContext.none();
        }

        SocketAddress remoteAddress = connection.getConnection().getRemoteAddress();
        if (remoteAddress == null) {
            return WorkspaceContext.none();
        }

        String address = remoteAddress.toString().replace("/", "");
        String key = "multiplayer-" + WorkspaceStorage.sanitizeFragment(address) + "-" + Integer.toHexString(address.hashCode());
        return WorkspaceContext.multiplayer(key, address);
    }
}
