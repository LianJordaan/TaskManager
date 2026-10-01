# TaskManager compatibility checks

## 1.1.0 release candidate

Source revision `23db2e7` passes the full 23-target Fabric build matrix with JDK 25. The local candidate manifest is `testing/taskmanager/candidates/manifest-1.1.0.json`. All 23 JARs have checked Fabric metadata and Java class levels; that packaging result alone does not qualify them for a compatibility claim.

| Minecraft | Exact production JAR SHA-512 | Real-client result |
| --- | --- | --- |
| 1.21.9 | `558e20b9420f11d065a947b91ac85520269d009dd77fc62883d1e1e186402fcce854b1d0581979444e03ff5fdabf39df37ec6c2adbb5524a7d4b93dd9756b90c` | Passed with Java 25, Xvfb and Mesa lavapipe |
| 1.21.10 | `8e1762956b27979b698f1456cade11748adf3c8ef055faf523206d661fbbd8f83c0bad2d8ac10a38ba5e67a7972b080db8dd551a7fee227a954fa190a43e1338` | Passed with Java 25, Xvfb and Mesa lavapipe |
| 1.21.11 | `321986b3c12b5a88e90d01afe3dbb6e4b9a02c365e99c3357174efe123c1c56cb0d32d77d9551ee3bda223edd25ff8b7eeb38718238012303ca7f6cce89f0578` | Passed with Java 25 |
| 26.1 | `7239cde7e84716065abf92d68241bc6f335cb2e4c7ebdc2f30f4b1093df7e205991800a184eee37be5ca7721a041295580182716c0dc7501b7b12fdda7519098` | Passed with Java 25, Xvfb and Mesa lavapipe |
| 26.1.1 | `003667bc1b99e4f722025b032376fc60d6d5609104aeecccaea15ffb0ee25404ea234c9a069feb622b7578bfcb977945e4bea0f4e006cc74710cacff4cd1bde7` | Passed with Java 25, Xvfb and Mesa lavapipe |
| 26.1.2 | `a4a55cee5ea851e35d4f72f145e0e322bf4154ae0282cb8b3cef54fd57b29b9e5f3f263a58bb1d06d349b1252aefd30fe307e737fd9cdd15546fa924e5c03024` | Passed with Java 25, Xvfb and Mesa lavapipe |
| 26.2 | `42409abfc4d28d0a9b24210ad7122abf3117530311f98687c33d27571a63267b87cd84be7639a4c907511044a928ada32078dcf32e9205218e5f09e16cd0c668` | Passed with Java 25 |
| 26.3 | `07eb5e8f08b8f69a194d48b272c56b8d75174061342e347ce6f394d64e196f0f9bb6aef752da659c673c598205a00d933c44df7f33fc361906d6f7d0d3b083c4` | Passed with Java 25, Xvfb and Mesa lavapipe; experimental Minecraft release |

The private Fabric client test verifies the loaded production JAR's hash, enters a real singleplayer world, creates and edits a context card, clicks its rendered checkbox, checks that the result saved, and captures workspace and HUD screenshots. Receipts, raw logs and three screenshots per run are under `testing/taskmanager/live/taskmanager-1.1.0-20261002/` in the workspace. The five [gallery screenshots](screenshots/) are direct, unedited captures from these 1.1.0 runs. The Linux 26.3 run logged a recoverable Vulkan swapchain warning during world creation, then completed all checks. Linux runs also logged a harmless Xvfb cursor-shape error at shutdown after the test passed. The test clients used FabricMC test credentials rather than a real authenticated account.

The other 15 built targets have not passed the 1.1.0 client check and are not verified release targets.

## 1.0.0 earlier candidate

The earlier 23-target Fabric build matrix also passed with JDK 25. Its separate live checks must not be transferred to the changed 1.1.0 JARs.

| Minecraft | Runtime | Exact production JAR SHA-512 | Live result |
| --- | --- | --- | --- |
| 1.21.11 | Java 21 | `3e85aee223ca1be2cdea71140ffcb96fc123f50d5e67ce53db200483599da6bfbc486854fb4a7bf11e5862c64b935705e4861ab52529ba06c2dac28b0c4c6228` | Passed |
| 26.2 | Java 25 | `b7e5fed603e97c2ccd4c4e4d6b9cca0b57b590fc3e0a0c666340e7ca07a34f812acaf2d91449142a5092ee2e292ecbb5af360c2dd40f9117323b1ab1c41e29c7` | Passed |
| 26.3 | Java 25 | `0bc9dbbe26f44edf50812ca6b99d65c22e4c6bc30bcc1382d8123dff1a66ccec7aa60328de980a2cb31e2e12e8ca16fbc3a94027b5a1672d3cc9893121a47f24` | Unqualified: the Windows client loaded the mod and initialized Vulkan, then its renderer crashed with `VK_ERROR_DEVICE_LOST` before the private test entered. The earlier OpenGL run exited with `0xC0000005` during initial resource reload. |

The old private Fabric client logs remain under `build/taskmanager_client_layout_final2_1_21_11.txt` and `build/taskmanager_client_layout_final2_26_2.txt` in the local checkout. The 26.3 renderer crash log is `build/taskmanager_client_layout_final3_26_3.txt`.

The other 20 built targets have not yet passed a live client check. They should not be listed as verified on a release page.
