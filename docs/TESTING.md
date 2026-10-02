# TaskManager compatibility checks

## 1.1.0 release candidate

All **23 version-specific Fabric JARs** passed an exact-hash real-client check. The frozen artifact manifest is `testing/taskmanager/candidates/manifest-1.1.0.json` in the Modrinth workspace. It records full candidate paths, SHA-512 values, Java class levels and production source revisions. The release gate verifies the saved-state result marker, raw log, UI screenshots and production JAR against each matching receipt.

The 1.20.1–1.21.8 candidates came from production source revision `9769b4601fc5358c7b4dfac5494696f8bfb476d7`. The unchanged 1.21.9–1.21.11 JARs came from `7561d3ea0626a9887c39de04f2b1bedece3132b6`, and the unchanged 26.1–26.3 JARs came from `23db2e78f764400ee72b6746e0d2ae55b252c24b`. Test-harness and documentation commits after these revisions do not change the frozen production JARs.

| Minecraft | Production JAR SHA-512 | Real-client check |
| --- | --- | --- |
| 1.20.1 | `febf73a32be6aece6e500bcc8a41e1b39121f13a06ef7a2e901c58c1251c05dc1b23d3a790ed66d40a7f2b94b5f8d5d98cf84fc61a2d93f288e3d671a066d026` | Passed — Paper build 196, isolated server |
| 1.20.2 | `bb327d28693c83d8c4d859263ed5e0aeb3ab8b1c746d6c7b61bc883f2a6933649eb51487f12836ad90579f4107446047ce7aa78b727d2894c5460aa8f48b2c02` | Passed — Paper build 318, isolated server |
| 1.20.3 | `857bf32ea8abe0c3f1ccb7679c8d886640bff41da67c11bd1e50159169699d60a9472cc76fa6f805456a84b0782cb321951690cd9e1634aed28f58de5add1793` | Passed — Vanilla, isolated server |
| 1.20.4 | `abcaf460193bb6e4d9e64fb72a18e50e49c062d53fe8e070d4b9d8e36f79638c5fa236cc9db328345035883c7536d0043e3dac78dc5355825630aecff6e15c28` | Passed — Paper build 499, isolated server |
| 1.20.5 | `d51e161f2b89d31a7df2dce0f22534709c4ec200a6b092458a79e097dfd29700b2b03a491023308116a45d3f90cc14410cabc728d9a4ab916e924c4a874c9c74` | Passed — Paper build 22 ALPHA, isolated server |
| 1.20.6 | `e42a8385dae1d202ece27f63dcb286d3cf6035294d181ce65001a581014ef9d3c2b7a02a390393ef7d097a325160742a36abc139960fc3f0cc082b6ee79cc568` | Passed — Paper build 151, isolated server |
| 1.21 | `eda551deac46d81f8dbee39b1d262fed076c0048bab38194ff581cf823db432c53a74b77320e13172bde3ba15a4476331541f4d7878fa82ebd0087ec941448aa` | Passed — Paper build 130, isolated server |
| 1.21.1 | `454b53e68b7a4d9279f3a23fc14d465cf3c474b729442dbc5e14753e47e4dca6107cfb88489962ea1d38f7a0d7ed3346b61be6d9ed4bcae4cf175e7ec018c57d` | Passed — Paper build 133, isolated server |
| 1.21.2 | `c19afe7d034ebb75b43ee3026ffb3ccb6371631245275d58dc82e03d375ceb6523164cab0775460cb662b5d62f3e92b1bd9a99e9d511d81bfabe0645da5f87d2` | Passed — Vanilla, isolated server |
| 1.21.3 | `31420edd51ec2a34036bdfc78936a376d17e49459c4d57a1c59d089683e7e885ba5bab8da7041f35341a5ff7ffa711ec7ddc1f55247258f8ba144cdce264bbd0` | Passed — Paper build 83, isolated server |
| 1.21.4 | `46ac3d0f4d7dcca48e996a36ed2a8394ba8eb7e315d949b15612b9caf1bc9927518b76b8ec1feedbd360a7049555b99567d4f6c6dd1f4325dcc7c8b01e2f54de` | Passed — Fabric singleplayer |
| 1.21.5 | `da2b783be85e1e6a0893bd62d4e9cf828f6814cf04640a14af04f64df3f1bac7f490b01c57d5bc4001fedec715d25f2ada6f0b80e9066fa10d9f90b2c19f20a9` | Passed — Fabric singleplayer |
| 1.21.6 | `629456362829fc3ca87323d906698fe6c02c387c779f18fd45b9b4fe4fe99b0babd302bbc3c312bdea33a533e7f34e295250e4ef7e759c3556b31f640936cd0a` | Passed — Fabric singleplayer |
| 1.21.7 | `8883e0b713158614be6adc371d5967f9031c00e1adaf6751b608a82a05a4af59016470478a4bcf3a98365a6e819bbe7e7653ce12e8031c2e46ede618e95b1f67` | Passed — Fabric singleplayer |
| 1.21.8 | `669c2c1a97fb08e6be47d9fd2f3ce8379e70abecfa6b7057732af0a83a4db8d677d641bcf15a7bc0dd5cf1b8590db8fef91a2121c6953fa0a9b18800c78419e7` | Passed — Fabric singleplayer |
| 1.21.9 | `e9fb342046ae6e70a81ddaa20c6bb0920bc67e93d73c91fafd112745efccb3f8fdd387842a0a42e272fea92e0a1958a46624e2bb35d9f835750091194da0150f` | Passed — Fabric singleplayer |
| 1.21.10 | `8a1991ac88e7759c04018816d7a0a22ac2f38c499dd60a5213da61a7bcf5f5aa30cbb4eb9b1c5621879132a8f544e1049dfe5be7f927e8454166b57218a50515` | Passed — Fabric singleplayer |
| 1.21.11 | `90319d0dfd674cbb3d4917ff2818b7d6a79744400751d56cd074918604c3be5871e31c2b22719c96f2fabb290f1c03175686012c418e75e98c837313dd9f72bc` | Passed — Fabric singleplayer |
| 26.1 | `7239cde7e84716065abf92d68241bc6f335cb2e4c7ebdc2f30f4b1093df7e205991800a184eee37be5ca7721a041295580182716c0dc7501b7b12fdda7519098` | Passed — Fabric singleplayer |
| 26.1.1 | `003667bc1b99e4f722025b032376fc60d6d5609104aeecccaea15ffb0ee25404ea234c9a069feb622b7578bfcb977945e4bea0f4e006cc74710cacff4cd1bde7` | Passed — Fabric singleplayer |
| 26.1.2 | `a4a55cee5ea851e35d4f72f145e0e322bf4154ae0282cb8b3cef54fd57b29b9e5f3f263a58bb1d06d349b1252aefd30fe307e737fd9cdd15546fa924e5c03024` | Passed — Fabric singleplayer |
| 26.2 | `42409abfc4d28d0a9b24210ad7122abf3117530311f98687c33d27571a63267b87cd84be7639a4c907511044a928ada32078dcf32e9205218e5f09e16cd0c668` | Passed — Fabric singleplayer |
| 26.3 | `07eb5e8f08b8f69a194d48b272c56b8d75174061342e347ce6f394d64e196f0f9bb6aef752da659c673c598205a00d933c44df7f33fc361906d6f7d0d3b083c4` | Passed — Fabric singleplayer |

The 1.20.1–1.21.3 checks used a private Fabric client with the exact production JAR on an isolated, version-matched, standalone offline-mode server. The harness opened and rendered the workspace, clicked a visible card checkbox, confirmed saved context JSON and the HUD, and captured three original screenshots per target. The server then stopped; its world remains available for review. The clients used Java 21; the 1.20.1–1.20.4 servers used Java 17 and newer servers used their pinned required runtime. Java 17 **client** execution for these versions was not separately verified.

The 1.21.4–26.3 checks used a private Fabric client test in a real singleplayer world. They verified the exact loaded JAR, rendered overlay and checkbox interaction, saved context and HUD, and captured the same three screenshot types. The later clients ran with Java 25. No check used a real authenticated multiplayer account, so the results do not claim authentication or third-party modpack compatibility.

The 1.20.3 and 1.21.2 server checks used pinned Vanilla builds. The 1.20.5 server check used Paper build 22 **ALPHA**; this is not a stable Paper-build claim. Minecraft 26.3 is a stable release; its earlier Paper beta status is unrelated to this Fabric singleplayer check.

The legacy UI was also checked at a realized 400×250 GUI viewport on 1.20.3 and at 800×450 on 1.20.4. The compact title, card, editor and controls stayed separated, and the checkbox and save flow worked. Earlier blurred or overlapping candidate receipts remain historical and are excluded by the release gate. The five [gallery screenshots](screenshots/) are unedited originals tied byte-for-byte to qualified 1.21.11, 26.2 and 26.3 receipts.

Detailed machine-readable receipts, screenshots and logs are under `testing/taskmanager/live/` in the workspace. Run `python testing/taskmanager/publish.py` there for a read-only release plan; it selects only matching live-pass receipts. Build success alone does not qualify a version.

## Earlier 1.0.0 candidate

The older 1.0.0 checks remain historical. They do not qualify the changed 1.1.0 JARs.
