# Validation

## Source compilation

The project contains 149 Kotlin source files. The modules were compiled independently with Kotlin and local Paper/message-framework API stubs in dependency order:

```text
core:mine
core:tool
core:tool:enchantment
api
core
```

Result:

```text
All modules compiled successfully.
```

The Tool, Enchantment, mine, and API modules were compiled independently. After the final GitHub-layout alignment, the complete `core` module was compiled again successfully against those compiled module artifacts.

The local validation compiler supports JVM bytecode targets up to 20, so the stub compilation used target 17. The Gradle project itself remains configured for the Java 21 toolchain used by the server project.

## Configuration validation

The following YAML files were parsed successfully:

```text
database/configuration.yml
messages.yml
mining/configuration.yml
paper-plugin.yml
tool/configuration.yml
tool/enchantments.yml
```

## Message validation

All literal `MiningCore.resolveMessage(...)` paths were extracted from the Kotlin sources and checked against `messages.yml`.

```text
45 paths checked
0 missing paths
```

## Effect registry validation

Configured Enchantment effects:

```text
damage_multiplier
fortune_bonus
excavator
```

Registered Enchantment effects:

```text
damage_multiplier
fortune_bonus
excavator
```

Configured Ability effects:

```text
overdrive
seismic_burst
```

Registered Ability effects:

```text
overdrive
seismic_burst
```

No configured effect identifier is missing an implementation.

## Database schema validation

All mining and Tool schema statements were executed against an in-memory SQLite database.

Created tables:

```text
mining_profile_data
mining_block_statistic_data
mining_prestige_history_data
mining_tool_profile_data
mining_tool_enchantment_state_data
mining_tool_fragment_state_data
mining_tool_ability_state_data
mining_tool_statistic_data
mining_tool_prestige_history_data
```

## Persistence review

The following important operations use explicit JDBC transactions:

- Enchantment upgrade
- Ability upgrade
- Ability selection
- Ability cooldown activation
- Prestige

The mining completion path accesses warmed cache state and does not perform a synchronous profile, statistics, fragment, Ability, or Enchantment database read.

## Environment limitation

A complete Gradle dependency build was not executed in the isolated environment because Gradle and the private Unity Realms Maven artifacts are not installed there. The complete Kotlin source graph was instead compiled against local API stubs, and all configuration, message, registry, and schema checks passed.

The normal project build remains:

```shell
gradle clean :core:shadowJar
```
