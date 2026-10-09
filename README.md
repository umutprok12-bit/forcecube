# Force Cube (Minecraft 1.12.2 Forge)

## Build option A - in the cloud (nothing heavy on your PC)
1. Make a free GitHub account, create a new repository, upload everything in this folder
   (including the hidden .github folder).
2. Open the repo's "Actions" tab, wait for the "build" run to finish (about 5-10 min).
3. Open the run, download the "forcecube-jar" artifact, unzip it, use the jar WITHOUT "-sources" in its name.

## Build option B - locally, lightweight
1. Install JDK 8 (Temurin 8).
2. Download the Forge 1.12.2 MDK (1.12.2-14.23.5.2860), unzip it.
3. Replace the MDK's src/ folder with this project's src/. Copy gradle.properties over too.
4. In that folder run:  gradlew setupCIWorkspace   then   gradlew build --no-daemon
   (setupCIWorkspace skips the heavy decompile step that setupDecompWorkspace does.)
5. Jar is in build/libs/.

Install: put the jar in .minecraft/mods with Forge 1.12.2 installed.

In game: hold V (rebind in Options > Controls > Force Cube) - it lights your aim point like a torch.
Release to spawn the cube. It's heavy: you can't push it by hand, but tools/swords launch it
and mobs can shove it. It crushes things it falls on. Shift + right-click removes it.
Config: config/forcecube.cfg
