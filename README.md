# Skyblock Mining Module

The `skyblock-mining-module` provides persistent mining progression, custom block mining, configurable mines, progression-enabled tools, enchantments, fragments, soft pity, abilities, and prestige progression.

## Project structure

```text
api
core
core:mine
core:tool
core:tool:enchantment
```

`core` contains the Paper plugin, commands, listeners, menus, and the validated mining pipeline.

`core:mine` contains mine domain objects and region resolution.

`core:tool` contains tool identity, Pickaxe progression, Prestige, fragments, soft pity, abilities, statistics, repositories, caches, and atomic JDBC transactions.

`core:tool:enchantment` contains enchantment definitions, states, unlocking, upgrades, breakthroughs, the effect registry, and concrete effect implementations.

## Requirements

- Java 21
- Purpur 1.21.11
- Kotlin 2.3.10
- `server-message-framework` 1.0.0
- `server-library-injector`
- SQLite, MySQL, or MariaDB

## Build

```shell
gradle clean :core:shadowJar
```

The generated plugin JAR is written to:

```text
core/build/libs/skyblock-mining-module-1.0.0.jar
```

## Tool progression

Every progression-enabled mining tool stores only the following identity metadata on the item:

```text
tool identifier
tool type
tool version
owner identifier
```

The database remains the source of truth for:

- Pickaxe Level and Experience
- Prestige
- Enchantment Points
- Enchantment states
- Fragment balances
- independent soft-pity counters
- Ability states and cooldowns
- mining statistics

Every gained Pickaxe Level grants one Enchantment Point. Prestige resets only Pickaxe Level and current Pickaxe Experience. Enchantments, points, fragments, pity progress, statistics, Abilities, and permanent bonuses remain.

## Tool commands

```text
/tool
/tool menu
/tool create
/tool enchantments
/tool abilities
/tool upgrade <enchantment>
/tool ability-upgrade <ability>
/tool select <ability>
/tool ability
/tool prestige
```

The selected Ability may also be activated through its configured interaction type.

## Enchantment configuration

Enchantments are configured in:

```text
core/src/main/resources/tool/enchantments.yml
```

Example:

```yaml
enchantments:
  excavator:
    display_name: "#E9C523Excavator"
    enabled: true
    maximum_level: 5
    effect: excavator

    unlock_requirements:
      groups:
        progression:
          pickaxe_level: 25
          prestige: 0
          total_blocks_mined: 1000
          block_category: ore
          block_category_amount: 250

    levels:
      1:
        enchantment_points: 2
        effect_configuration:
          chance: 0.08
          radius: 1
          maximum_blocks: 1
          allow_fragment_rolls: 0

      3:
        enchantment_points: 4
        fragments:
          rare: 2
        effect_configuration:
          chance: 0.16
          radius: 1
          maximum_blocks: 3
          allow_fragment_rolls: 0
        breakthrough:
          enabled: true
          identifier: expanded_excavation
          display_name: "Expanded Excavation"
          effect_configuration:
            radius: 2
            maximum_blocks: 6
```

`ExcavatorEnchantmentEffect` is the included example effect. It mines additional nearby configured blocks through the same validated completion pipeline. Its level-three breakthrough permanently expands the radius and maximum affected block count for all later levels.

## Creating a new Enchantment effect

Create an implementation in:

```text
core/tool/enchantment/src/main/kotlin/com/unityrealms/skyblock/miningmodule/tool/enchantment/effect/implementation
```

```kotlin
class ExampleEnchantmentEffect : EnchantmentEffect {

  override val identifier: String = "example"

  override fun onBlockMined(
    definition: EnchantmentDefinition,
    levelDefinition: EnchantmentLevelDefinition,
    state: ToolEnchantmentState,
    context: ToolMiningContext
  ) {
    // Execute the effect through the supplied validated context.
  }
}
```

Register the effect in `mining/MiningCore.kt`:

```kotlin
val enchantmentEffectRegistry = EnchantmentEffectRegistry(
  listOf(
    DamageMultiplierEnchantmentEffect(),
    FortuneBonusEnchantmentEffect(),
    ExcavatorEnchantmentEffect(),
    ExampleEnchantmentEffect()
  )
)
```

The YAML `effect` value must match the registered effect identifier.

## Breakthrough levels

A breakthrough is attached to a specific enchantment level. Fragment costs and breakthrough behavior are independent:

```yaml
3:
  enchantment_points: 4
  fragments:
    rare: 2
  breakthrough:
    enabled: true
    identifier: expanded_effect
    display_name: "Expanded Effect"
    effect_configuration:
      radius: 2
```

All reached breakthrough configurations are retained when the enchantment is upgraded beyond the breakthrough level.

## Fragments and soft pity

The following fragment rarities are supported:

```text
COMMON
RARE
EPIC
MYTHIC
```

Every rarity stores its own balance and `failedEligibleRolls` value. A successful Common Fragment roll does not reset Rare, Epic, or Mythic pity.

The effective chance uses:

```text
base chance
× Prestige base-chance multiplier
× permanent tool fragment multiplier
+ soft-pity bonus
```

Optional hard pity guarantees the corresponding rarity after the configured number of failed eligible rolls.

Fragment rolls only occur after valid custom mining completions. Secondary blocks additionally require both the global safety option and the rarity/effect-specific option.

## Ability system

Abilities are configured in:

```text
core/src/main/resources/tool/configuration.yml
```

Included Abilities:

- `overdrive`: temporary damage and Pickaxe Experience multipliers
- `seismic_burst`: processes nearby configured blocks through the controlled mining pipeline

Cooldowns are persisted as absolute `cooldownEndsAt` timestamps. Reconnecting, moving the tool, changing the selected Ability, or restarting the server does not reset a cooldown.

## Persistence

Frequent updates use write-behind caches:

- Pickaxe Experience
- mining statistics
- fragment pity increments
- fragment drops
- automatic unlock state

Important operations use JDBC transactions and are persisted immediately:

- Enchantment upgrades
- Ability upgrades
- Ability selection
- Ability activation cooldowns
- Prestige

## Safety limits

The shared mining context enforces:

- maximum affected blocks per activation
- maximum recursion depth
- one execution of the same effect per chain
- configurable secondary Experience
- configurable secondary drops
- configurable secondary fragment eligibility
- no synchronous database read during the mining completion pipeline

## Configuration files

```text
core/src/main/resources/database/configuration.yml
core/src/main/resources/messages.yml
core/src/main/resources/mining/configuration.yml
core/src/main/resources/tool/configuration.yml
core/src/main/resources/tool/enchantments.yml
```

## Documentation

- `MODIFIED_EXISTING_FILES.md` lists every changed pre-existing file and final line range.
- `TOOL_SYSTEM_VALIDATION.md` maps the complete Tool and Enchantment concept to its implementation.
- `VALIDATION.md` records the performed validation steps and remaining environment limitation.
