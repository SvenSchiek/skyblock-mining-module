# Tool and Enchantment System Validation

## 1. Core progression

Implemented in `core:tool`:

- stable tool identity through `ToolItemService`
- `ToolProfile` with level, Experience, Prestige, Enchantment Points, permanent multipliers, and selected Ability
- complete cached tool state through `ToolStateService`
- persistent tool statistics through `ToolMiningStatisticsService`

## 2. Pickaxe Experience and levels

Implemented by `ToolProgressionService`:

- configurable Experience curve
- permanent Prestige Experience multiplier
- temporary Experience multiplier
- multiple levels from one reward
- one Enchantment Point for every gained level
- no additional levels or points beyond maximum Pickaxe Level

## 3. Automatic Enchantment unlocking

Implemented by:

- `ToolUnlockRequirement`
- `UnlockRequirementEvaluator`
- `EnchantmentUnlockService`

Supported deterministic requirements:

- Pickaxe Level
- Prestige
- total blocks mined
- total ores mined
- block category count
- ore category count
- fragments obtained by rarity
- multiple requirement groups with combined AND semantics

Unlocks are stored in `mining_tool_enchantment_state_data` and remain after Prestige.

## 4. Atomic Enchantment upgrades

Implemented by:

- `EnchantmentUpgradeService`
- `JdbcToolTransactionRepository.upgradeEnchantment`

The transaction validates and updates:

- unlocked state
- expected current level
- fixed maximum level
- available Enchantment Points
- available fragments
- invested Enchantment Points
- invested fragments
- resulting Enchantment level

Profile, fragments, and Enchantment state are committed or rolled back together.

## 5. Enchantment Effect Registry

Implemented by:

- `EnchantmentEffect`
- `EnchantmentEffectRegistry`
- `EnchantmentEffectService`

Registered effects:

- `damage_multiplier`
- `fortune_bonus`
- `excavator`

Configured identifiers were validated against registered implementations.

## 6. Example Enchantment and Breakthrough

`excavator` is configured in `tool/enchantments.yml` and implemented by `ExcavatorEnchantmentEffect`.

At level three:

- two Rare Fragments are required
- the `expanded_excavation` breakthrough is unlocked
- radius and maximum affected blocks increase
- the breakthrough configuration remains active at later levels
- secondary fragment generation remains disabled

## 7. Fragments

Implemented by:

- `FragmentRarity`
- `FragmentDefinition`
- `FragmentState`
- `FragmentService`
- `CacheFragmentStateRepository`
- `JdbcFragmentStateRepository`

Supported rarities:

- Common
- Rare
- Epic
- Mythic

Every rarity is available at Prestige 0 and has independent configuration for base chance, eligibility, soft pity, hard pity, and Prestige modifiers.

## 8. Independent soft pity

Every `(toolIdentifier, rarity)` pair has its own persisted `failedEligibleRolls` value.

Processing rules:

1. validate activation source and secondary-block eligibility
2. calculate effective chance
3. trigger hard pity when configured threshold is reached
4. increment only the failed rarity
5. reset only the successful rarity
6. buffer the updated state for persistence

Pity state survives inventory movement, reconnects, restarts, and Prestige.

## 9. Tool Prestige

Implemented by:

- `ToolPrestigeConfiguration`
- `ToolPrestigeService`
- `JdbcToolTransactionRepository.prestige`

Maximum Prestige is clamped to two.

Reset values:

- Pickaxe Level to 1
- current Pickaxe Experience to 0

Retained values:

- Enchantment unlocks and levels
- available and invested Enchantment Points
- fragments
- soft-pity progress
- statistics
- Abilities
- selected Ability
- permanent multipliers

Prestige requirements are deterministic and do not require random drops.

## 10. Ability system

Implemented by:

- `AbilityDefinition`
- `ToolAbilityState`
- `AbilityRegistry`
- `AbilityService`
- `AbilityEffectRegistry`
- `ToolAbilityListener`

Included effects:

- `OverdriveAbilityEffect`
- `SeismicBurstAbilityEffect`

The system supports unlock requirements, exactly one selected Ability, optional levels, Enchantment Point costs, fragment milestone costs, configured activation type, and absolute persisted cooldown timestamps.

## 11. Tool identity and display

The item stores only:

- tool identifier
- tool type
- tool version
- optional owner identifier

Authoritative progression remains in the database. Lore is rebuilt from cached domain state and contains derived level, Experience, Prestige, points, fragments, Ability, and Enchantment summaries.

## 12. Shared mining execution pipeline

Implemented through:

- `ToolMiningContext`
- `MiningChainState`
- `MiningSessionManager.processBlockCompletion`
- `ToolRuntimeService.handleMiningCompletion`

Normal mining, Enchantment secondary blocks, and Ability secondary blocks use the same validation, drop, Experience, statistics, respawn, and event pipeline.

## 13. Safety

Implemented limits:

- maximum affected blocks per activation
- maximum recursion depth
- effect identifier execution guard
- disabled recursive secondary Enchantments
- secondary Experience multiplier
- secondary drop multiplier
- global and per-effect fragment eligibility
- cached-only mining-path progression updates

## 14. Persistence strategy

Write-behind caches are used for frequent state:

- tool profiles
- statistics
- fragment state and pity
- automatic Enchantment unlocks
- automatic Ability unlocks

Immediate JDBC transactions are used for:

- Enchantment upgrades
- Ability upgrades
- Ability selection
- Ability cooldown activation
- Prestige

State is flushed periodically, on held-item changes, on disconnect, on explicit unload, and during shutdown.

## 15. Player interface

Implemented menus:

- `ToolMainMenu`
- `ToolEnchantmentMenu`
- `ToolAbilityMenu`
- `ToolPrestigeMenu`
- `ToolPrestigeConfirmMenu`

Enchantment menu states:

- locked
- unlocked at level zero
- partially upgraded
- maximum level

The menus display requirement progress, current and next effect values, point costs, fragment costs, breakthrough information, Ability selection/cooldown data, and Prestige reset/retention information.
