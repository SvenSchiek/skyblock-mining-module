# Modified Existing Files

This document lists only files that already existed in the connected `playunityrealms/skyblock-mining-module` repository and were changed for the final Tool and Enchantment implementation. Newly created files are intentionally excluded.

Line references point to the final files contained in this project.

## `api/build.gradle.kts`

- **Changed final lines:** L1-L18
- **Change:** Removed implementation-module compile-only dependencies so the API remains independent and can expose Tool views without a project dependency cycle.

## `api/src/main/kotlin/com/unityrealms/skyblock/miningmodule/api/MiningModuleApi.kt`

- **Changed final lines:** L1-L85
- **Change:** Extended the public API with read-only Tool progression access and progression-tool creation.

## `build.gradle.kts`

- **Changed final lines:** L1-L69
- **Change:** Extended Spotless source targets to the API, mine, Tool, and Tool Enchantment modules.

## `core/src/main/kotlin/com/unityrealms/skyblock/miningmodule/command/mining/MiningCommand.kt`

- **Changed final lines:** L1-L141
- **Change:** Retained the existing consolidated `/mining` command structure while wiring its current subcommands to the updated runtime.

## `core/src/main/kotlin/com/unityrealms/skyblock/miningmodule/database/DatabaseConnector.kt`

- **Changed final lines:** L1-L213
- **Change:** Added creation of the Tool/Enchantment database schema after the existing mining schema is initialized.

## `core/src/main/kotlin/com/unityrealms/skyblock/miningmodule/mining/MiningCore.kt`

- **Changed final lines:** L1-L377
- **Change:** Integrated Tool repositories, caches, progression, fragments, soft pity, Abilities, Effect registries, commands, menus, listeners, reload, and shutdown.

## `core/src/main/kotlin/com/unityrealms/skyblock/miningmodule/mining/animation/MiningAnimationManager.kt`

- **Changed final lines:** L1-L224
- **Change:** Adjusted the runtime update entry point used by the combined mining and Tool reload flow.

## `core/src/main/kotlin/com/unityrealms/skyblock/miningmodule/mining/block/MiningBlock.kt`

- **Changed final lines:** L1-L91
- **Change:** Added block and ore categories required for Tool unlock statistics and Enchantment requirements.

## `core/src/main/kotlin/com/unityrealms/skyblock/miningmodule/mining/configuration/MiningConfiguration.kt`

- **Changed final lines:** L1-L193
- **Change:** Loads block/ore categories and the secondary-mining configuration used by Tool effects.

## `core/src/main/kotlin/com/unityrealms/skyblock/miningmodule/mining/drop/MiningDropManager.kt`

- **Changed final lines:** L1-L53
- **Change:** Accepts an additional Fortune bonus supplied by registered Tool Enchantment effects.

## `core/src/main/kotlin/com/unityrealms/skyblock/miningmodule/mining/profile/listener/MiningProfileListener.kt`

- **Changed final lines:** L1-L81
- **Change:** Extended existing player lifecycle handling to warm, flush, and unload complete Tool state.

## `core/src/main/kotlin/com/unityrealms/skyblock/miningmodule/mining/respawn/MiningRespawnManager.kt`

- **Changed final lines:** L1-L111
- **Change:** Adjusted the runtime update entry point used by the combined mining and Tool reload flow.

## `core/src/main/kotlin/com/unityrealms/skyblock/miningmodule/mining/session/MiningSessionManager.kt`

- **Changed final lines:** L1-L405
- **Change:** Integrated progression-tool validation, Tool damage/Fortune, the shared secondary-block pipeline, Enchantment effects, Ability effects, Tool XP, statistics, fragment rolls, and unlock evaluation.

## `core/src/main/kotlin/com/unityrealms/skyblock/miningmodule/mining/session/listener/MiningSessionListener.kt`

- **Changed final lines:** L1-L66
- **Change:** Retained the existing listener path and adapted it to the Tool-aware MiningSessionManager API.

## `core/src/main/resources/messages.yml`

- **Changed final lines:** L1-L106
- **Change:** Added Tool, Enchantment, Ability, fragment, Breakthrough, and integrated command messages.

## `core/src/main/resources/mining/configuration.yml`

- **Changed final lines:** L1-L234
- **Change:** Added mining block categories and ore categories used by Tool statistics and unlock requirements.
