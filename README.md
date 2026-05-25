# TaskManager

Fabric multiversion setup for Minecraft **1.20.1 through 1.21.11**.

## Build

Use JDK 21 to run the build:

```bash
./gradlew build
```

That builds one jar per Minecraft version.

## Layout

- Shared code lives in `src/main/java` and `src/main/resources`.
- Version-specific overrides can live in `versions/<minecraft-version>/src/main/java` and `versions/<minecraft-version>/src/main/resources`.
- Built jars are written to `versions/<minecraft-version>/build/libs/`.

## Build One Version

```bash
./gradlew :mc1_20_1:build
./gradlew :mc1_21_11:build
```
