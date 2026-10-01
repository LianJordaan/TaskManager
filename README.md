# TaskManager

TaskManager is a client-side Fabric workspace for keeping tasks and notes in view while you play. Open the workspace with **O**, or press **N** to open it and create a task card. Both keys can be changed in Minecraft's Controls screen.

Cards can belong to your **Global** workspace, which follows you everywhere, or the **Context** workspace for the current singleplayer world or multiplayer server. Drag or resize a card on the overlay; lock it to avoid accidental moves, hide it from the HUD, or change its color, scale and opacity. Use the **Task** editor button or type `- [ ]` to add a task. Click its checkbox on the card to mark it done or reopen it. The editor also supports basic Markdown headings, emphasis, lists, quotes and code. The panel layout, task state and card positions are saved automatically.

The **+ Global** and **+ Context** buttons make cards for either scope. The editor opens alongside a new card, with card settings collapsed below it; drag the panel headers or use **Reset UI** to restore this layout.

Version 1.1.0 has passed real-client checks on Fabric for Minecraft **1.21.9, 1.21.10, 1.21.11, 26.1, 26.1.1, 26.1.2, 26.2 and experimental 26.3**. Each check loaded its exact release-candidate JAR in a singleplayer world and exercised the workspace, checkbox saving and HUD. See [the compatibility receipts](docs/TESTING.md) for hashes and test limits. Other compiled targets are not yet verified.

## Screenshots

These are direct, unedited Minecraft client captures from the live 1.1.0 checks on 1.21.11, 26.2 and experimental 26.3:

- [Workspace and editor](docs/screenshots/workspace-editor-1.21.11.png)
- [Checked task](docs/screenshots/task-checked-1.21.11.png)
- [In-game HUD](docs/screenshots/in-game-hud-1.21.11.png)
- [Workspace on 26.2](docs/screenshots/workspace-editor-26.2.png)
- [Workspace on 26.3](docs/screenshots/workspace-editor-26.3.png)

## Your data

TaskManager stores notes locally in `.minecraft/config/taskmanager/global-workspace.json` and `.minecraft/config/taskmanager/contexts/`. The overlay layout lives in `overlay-layout.json` in the same config directory. The mod saves with a temporary file and keeps a `.bak` copy of the previous complete JSON. If the current file is damaged, the last complete backup loads automatically. An incomplete file is preserved with a `.corrupt` suffix when the next save succeeds. Back up the whole `taskmanager` directory before moving to a new computer or reinstalling Minecraft.

## Building

Use JDK 25 to run Gradle for the full build matrix. Minecraft 1.20.1 through 1.21.11 use Java 17 bytecode, and Minecraft 26.1 through 26.3 use Java 25 bytecode. Each Minecraft version has a separate Fabric JAR:

```bash
./gradlew build
```

To build only one target:

```bash
./gradlew :mc1_20_1:build
./gradlew :mc1_21_11:build
./gradlew :mc26_3:build
```

Shared code is in `src/main`, version compatibility code is in `src/compat_*`, and each JAR is written to `versions/<minecraft-version>/build/libs/`. Build success alone does not establish in-game compatibility; release claims follow the [live client results](docs/TESTING.md).
