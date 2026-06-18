/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.tool.enchantment

import com.unityrealms.skyblock.miningmodule.tool.FragmentRarity
import com.unityrealms.skyblock.miningmodule.tool.configuration.ToolConfiguration

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration

/**
 * Loads enchantment definitions from YAML.
 */
object EnchantmentConfiguration {

  fun load(fileConfiguration: FileConfiguration): List<EnchantmentDefinition> {
    val rootSection = fileConfiguration.getConfigurationSection("enchantments") ?: return emptyList()

    return rootSection.getKeys(false).mapNotNull enchantmentMap@ { identifier ->
      val section = rootSection.getConfigurationSection(identifier) ?: return@enchantmentMap null
      val levelDefinitionMap = this.loadLevels(section.getConfigurationSection("levels"))

      EnchantmentDefinition(
        identifier = identifier.lowercase(),
        displayName = section.getString("display_name") ?: identifier,
        descriptionList = section.getStringList("description"),
        maximumLevel = section.getInt("maximum_level", levelDefinitionMap.keys.maxOrNull() ?: 1).coerceAtLeast(1),
        unlockRequirementList = ToolConfiguration.Loader.loadRequirementList(
          section.getConfigurationSection("unlock_requirements") ?: section.getConfigurationSection("unlock")
        ),
        levelDefinitionMap = levelDefinitionMap,
        effectIdentifier = section.getString("effect") ?: identifier.lowercase(),
        effectConfiguration = this.loadDoubleMap(section.getConfigurationSection("effect_configuration")),
        enabled = section.getBoolean("enabled", true)
      )
    }
  }

  private fun loadLevels(section: ConfigurationSection?): Map<Int, EnchantmentLevelDefinition> {
    if (section == null) return emptyMap()

    return section.getKeys(false).mapNotNull levelMap@ { rawLevel ->
      val level = rawLevel.toIntOrNull() ?: return@levelMap null
      val levelSection = section.getConfigurationSection(rawLevel) ?: return@levelMap null
      val breakthroughSection = levelSection.getConfigurationSection("breakthrough")
      val breakthrough = if (breakthroughSection?.getBoolean("enabled", true) == true) {
        BreakthroughDefinition(
          identifier = breakthroughSection.getString("identifier") ?: "level_$level",
          displayName = breakthroughSection.getString("display_name") ?: "Breakthrough $level",
          descriptionList = breakthroughSection.getStringList("description"),
          effectConfiguration = this.loadDoubleMap(breakthroughSection.getConfigurationSection("effect_configuration"))
        )
      } else {
        null
      }

      level to EnchantmentLevelDefinition(
        enchantmentPoints = levelSection.getInt("enchantment_points", 1).coerceAtLeast(0),
        fragmentCostMap = this.loadFragmentMap(levelSection.getConfigurationSection("fragments")),
        effectConfiguration = this.loadDoubleMap(levelSection.getConfigurationSection("effect_configuration")).ifEmpty {
          mapOf("value" to levelSection.getDouble("value", 0.0))
        },
        breakthrough = breakthrough
      )
    }.toMap()
  }

  private fun loadFragmentMap(section: ConfigurationSection?): Map<FragmentRarity, Int> {
    if (section == null) return emptyMap()
    return FragmentRarity.entries.mapNotNull { rarity ->
      section.getInt(rarity.name.lowercase(), 0).takeIf { it > 0 }?.let { rarity to it }
    }.toMap()
  }

  private fun loadDoubleMap(section: ConfigurationSection?): Map<String, Double> {
    if (section == null) return emptyMap()
    return section.getKeys(false).associateWith { section.getDouble(it) }
  }
}
