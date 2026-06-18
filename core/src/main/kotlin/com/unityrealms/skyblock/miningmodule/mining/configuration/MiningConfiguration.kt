/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.mining.configuration

import com.unityrealms.skyblock.miningmodule.mining.block.MiningBlock
import com.unityrealms.skyblock.miningmodule.mining.block.MiningToolType
import com.unityrealms.skyblock.miningmodule.mine.Mine

import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration

/**
 * Represents the complete mining configuration.
 */
data class MiningConfiguration(
  val requireRegisteredMine: Boolean,
  val level: Level,
  val prestige: Prestige,
  val session: Session,
  val animation: Animation,
  val blockList: List<MiningBlock>,
  val mineList: List<Mine>
) {

  /** Represents mining level configuration. */
  data class Level(
    val maximumLevel: Int,
    val baseExperience: Long,
    val experienceMultiplier: Double
  )

  /** Represents mining prestige configuration. */
  data class Prestige(
    val maximumPrestige: Int,
    val experienceBonusPerPrestige: Double,
    val damageBonusPerPrestige: Double
  )

  /** Represents custom mining session configuration. */
  data class Session(
    val tickInterval: Long,
    val maximumDistance: Double,
    val depletedMaterial: Material,
    val particleViewDistance: Double
  )

  /** Represents visual mining animation configuration. */
  data class Animation(
    val enabled: Boolean,
    val hitParticleAmount: Int,
    val breakParticleAmount: Int,
    val respawnParticleAmount: Int
  )

  /**
   * Loads mining configuration from YAML.
   */
  object Loader {

    /**
     * Loads a mining configuration.
     *
     * @param fileConfiguration The YAML configuration.
     *
     * @return The loaded mining configuration.
     */
    fun fromYaml(fileConfiguration: FileConfiguration): MiningConfiguration {
      val level = Level(
        maximumLevel = fileConfiguration.getInt("mining.level.maximum_level", 100).coerceAtLeast(1),
        baseExperience = fileConfiguration.getLong("mining.level.base_experience", 100L).coerceAtLeast(1L),
        experienceMultiplier = fileConfiguration.getDouble("mining.level.experience_multiplier", 1.2).coerceAtLeast(1.0)
      )
      val prestige = Prestige(
        maximumPrestige = fileConfiguration.getInt("mining.prestige.maximum_prestige", 10).coerceAtLeast(0),
        experienceBonusPerPrestige = fileConfiguration.getDouble("mining.prestige.experience_bonus_per_prestige", 0.05).coerceAtLeast(0.0),
        damageBonusPerPrestige = fileConfiguration.getDouble("mining.prestige.damage_bonus_per_prestige", 0.025).coerceAtLeast(0.0)
      )
      val session = Session(
        tickInterval = fileConfiguration.getLong("mining.session.tick_interval", 2L).coerceAtLeast(1L),
        maximumDistance = fileConfiguration.getDouble("mining.session.maximum_distance", 6.0).coerceAtLeast(1.0),
        depletedMaterial = Material.matchMaterial(fileConfiguration.getString("mining.session.depleted_material") ?: "BEDROCK") ?: Material.BEDROCK,
        particleViewDistance = fileConfiguration.getDouble("mining.session.particle_view_distance", 24.0).coerceAtLeast(1.0)
      )
      val animation = Animation(
        enabled = fileConfiguration.getBoolean("mining.animation.enabled", true),
        hitParticleAmount = fileConfiguration.getInt("mining.animation.hit_particle_amount", 3).coerceAtLeast(0),
        breakParticleAmount = fileConfiguration.getInt("mining.animation.break_particle_amount", 22).coerceAtLeast(0),
        respawnParticleAmount = fileConfiguration.getInt("mining.animation.respawn_particle_amount", 16).coerceAtLeast(0)
      )

      return MiningConfiguration(
        requireRegisteredMine = fileConfiguration.getBoolean("mining.require_registered_mine", true),
        level = level,
        prestige = prestige,
        session = session,
        animation = animation,
        blockList = this.loadBlocks(fileConfiguration.getConfigurationSection("blocks")),
        mineList = this.loadMines(fileConfiguration.getConfigurationSection("mines"))
      )
    }

    private fun loadBlocks(section: ConfigurationSection?): List<MiningBlock> {
      if (section == null) {
        return emptyList()
      }

      val blockList = mutableListOf<MiningBlock>()

      for (identifier in section.getKeys(false)) {
        val blockSection = section.getConfigurationSection(identifier) ?: continue
        val material = Material.matchMaterial(blockSection.getString("material") ?: continue) ?: continue
        val toolType = runCatching {
          MiningToolType.valueOf((blockSection.getString("required_tool") ?: "ANY").uppercase())
        }.getOrDefault(MiningToolType.ANY)
        val dropList = blockSection.getMapList("drops").mapNotNull { rawDropMap ->
          val dropMaterial = Material.matchMaterial(rawDropMap["material"]?.toString() ?: return@mapNotNull null) ?: return@mapNotNull null
          val minimumAmount = rawDropMap.number("minimum_amount", 1).toInt().coerceAtLeast(1)
          val maximumAmount = rawDropMap.number("maximum_amount", minimumAmount).toInt().coerceAtLeast(minimumAmount)
          val chance = rawDropMap.number("chance", 1.0).toDouble().coerceIn(0.0, 1.0)

          MiningBlock.Drop(dropMaterial, minimumAmount, maximumAmount, chance)
        }

        blockList.add(
          MiningBlock(
            identifier = identifier.lowercase(),
            displayName = blockSection.getString("display_name") ?: identifier,
            material = material,
            blockCategory = (blockSection.getString("block_category") ?: identifier).lowercase(),
            oreCategory = blockSection.getString("ore_category")?.lowercase()
              ?: identifier.takeIf { material.name.endsWith("ORE") }?.lowercase(),
            requiredLevel = blockSection.getInt("required_level", 1).coerceAtLeast(1),
            requiredPrestige = blockSection.getInt("required_prestige", 0).coerceAtLeast(0),
            requiredToolType = toolType,
            durability = blockSection.getDouble("durability", 10.0).coerceAtLeast(0.1),
            experience = blockSection.getLong("experience", 1L).coerceAtLeast(0L),
            respawnTicks = blockSection.getLong("respawn_ticks", 100L).coerceAtLeast(1L),
            dropList = dropList
          )
        )
      }

      return blockList
    }

    private fun loadMines(section: ConfigurationSection?): List<Mine> {
      if (section == null) {
        return emptyList()
      }

      return section.getKeys(false).mapNotNull { identifier ->
        val mineSection = section.getConfigurationSection(identifier) ?: return@mapNotNull null

        Mine(
          identifier = identifier.lowercase(),
          displayName = mineSection.getString("display_name") ?: identifier,
          enabled = mineSection.getBoolean("enabled", true),
          regionIdentifier = mineSection.getString("region_identifier")?.lowercase(),
          worldName = mineSection.getString("world_name"),
          minimum = mineSection.getConfigurationSection("minimum")?.toCoordinate(),
          maximum = mineSection.getConfigurationSection("maximum")?.toCoordinate(),
          requiredLevel = mineSection.getInt("required_level", 1).coerceAtLeast(1),
          requiredPrestige = mineSection.getInt("required_prestige", 0).coerceAtLeast(0),
          experienceMultiplier = mineSection.getDouble("experience_multiplier", 1.0).coerceAtLeast(0.0),
          damageMultiplier = mineSection.getDouble("damage_multiplier", 1.0).coerceAtLeast(0.0)
        )
      }
    }

    private fun ConfigurationSection.toCoordinate(): Mine.Coordinate = Mine.Coordinate(
      x = this.getInt("x"),
      y = this.getInt("y"),
      z = this.getInt("z")
    )

    private fun Map<*, *>.number(key: String, fallback: Number): Number {
      val value = this[key] ?: return fallback

      return when (value) {
        is Number -> value
        is String -> value.toDoubleOrNull() ?: fallback
        else -> fallback
      }
    }
  }
}
