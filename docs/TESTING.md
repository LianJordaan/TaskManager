# TaskManager compatibility checks

The full 23-target Fabric build matrix passes with JDK 25. This checks compilation and JAR packaging for Minecraft 1.20.1–1.21.11 and 26.1–26.3; it does not prove that every target works in play.

| Minecraft | Runtime | Exact production JAR SHA-512 | Live result |
| --- | --- | --- | --- |
| 1.21.11 | Java 21 | `3e85aee223ca1be2cdea71140ffcb96fc123f50d5e67ce53db200483599da6bfbc486854fb4a7bf11e5862c64b935705e4861ab52529ba06c2dac28b0c4c6228` | Passed |
| 26.2 | Java 25 | `b7e5fed603e97c2ccd4c4e4d6b9cca0b57b590fc3e0a0c666340e7ca07a34f812acaf2d91449142a5092ee2e292ecbb5af360c2dd40f9117323b1ab1c41e29c7` | Passed |
| 26.3 | Java 25 | `0bc9dbbe26f44edf50812ca6b99d65c22e4c6bc30bcc1382d8123dff1a66ccec7aa60328de980a2cb31e2e12e8ca16fbc3a94027b5a1672d3cc9893121a47f24` | Unqualified: the Windows client loaded the mod and initialized Vulkan, then its renderer crashed with `VK_ERROR_DEVICE_LOST` before the private test entered. The earlier OpenGL run exited with `0xC0000005` during initial resource reload. |

The private Fabric client test is separate from the production JAR. On each passing target it checked the loaded JAR's SHA-512, opened a real singleplayer world, created and edited a context card, clicked a rendered checkbox, verified that the checked state was saved, and captured workspace and HUD images. Logs remain under `build/taskmanager_client_layout_final2_1_21_11.txt` and `build/taskmanager_client_layout_final2_26_2.txt` in the local checkout. The 26.3 crash log is `build/taskmanager_client_layout_final3_26_3.txt`. [Gallery screenshots](screenshots/) are direct, unedited captures of the passing runs.

The other 20 built targets have not yet passed a live client check. They should not be listed as verified on a release page.
