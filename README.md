# TaskManager

TaskManager is a client-side Fabric workspace for keeping notes in view while you play. Open the workspace with **O**, or press **N** to open it and create a note. Both keys can be changed in Minecraft's Controls screen.

Notes can belong to your **Global** workspace, which follows you everywhere, or the **Context** workspace for the current singleplayer world or multiplayer server. Drag or resize a note on the overlay; lock it to avoid accidental moves, hide it from the HUD, or change its color, scale and opacity. The editor supports basic Markdown headings, emphasis, lists, quotes and code. The panel layout and note positions are saved automatically.

## Your data

TaskManager stores notes locally in `.minecraft/config/taskmanager/global-workspace.json` and `.minecraft/config/taskmanager/contexts/`. The overlay layout lives in `overlay-layout.json` in the same config directory. The mod saves with a temporary file and keeps a `.bak` copy of the previous complete JSON. If the current file is damaged, the last complete backup loads automatically. An incomplete file is preserved with a `.corrupt` suffix when the next save succeeds. Back up the whole `taskmanager` directory before moving to a new computer or reinstalling Minecraft.

## Building

Use JDK 21 to run Gradle. The existing build matrix targets Minecraft 1.20.1 through 1.21.11, with a separate Fabric JAR for each version:

```bash
./gradlew build
```

To build only one target:

```bash
./gradlew :mc1_20_1:build
./gradlew :mc1_21_11:build
```

Shared code is in `src/main`, version compatibility code is in `src/compat_*`, and each JAR is written to `versions/<minecraft-version>/build/libs/`. Build success alone does not establish in-game compatibility; release claims will follow live client tests.
