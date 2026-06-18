/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.mining.block

import org.bukkit.Material

/**
 * Represents a configured custom mining block.
 *
 * @property identifier The unique block identifier.
 * @property displayName The human-readable block name.
 * @property material The represented Bukkit material.
 * @property blockCategory The unlock-relevant block category.
 * @property oreCategory The optional unlock-relevant ore category.
 * @property requiredLevel The required mining level.
 * @property requiredPrestige The required mining prestige.
 * @property requiredToolType The required tool category.
 * @property durability The custom durability of the block.
 * @property experience The base experience awarded on completion.
 * @property respawnTicks The number of ticks before the block respawns.
 * @property dropList The configured drop entries.
 */
data class MiningBlock(
  val identifier: String,
  val displayName: String,
  val material: Material,
  val blockCategory: String,
  val oreCategory: String?,
  val requiredLevel: Int,
  val requiredPrestige: Int,
  val requiredToolType: MiningToolType,
  val durability: Double,
  val experience: Long,
  val respawnTicks: Long,
  val dropList: List<Drop>
) {

  /**
   * Represents a possible drop from a mining block.
   *
   * @property material The dropped material.
   * @property minimumAmount The minimum amount.
   * @property maximumAmount The maximum amount.
   * @property chance The chance between 0 and 1.
   */
  data class Drop(
    val material: Material,
    val minimumAmount: Int,
    val maximumAmount: Int,
    val chance: Double
  ) {

    init {
      require(this.minimumAmount >= 1) {
        "The minimum drop amount must be at least 1."
      }

      require(this.maximumAmount >= this.minimumAmount) {
        "The maximum drop amount must not be lower than the minimum amount."
      }

      require(this.chance in 0.0..1.0) {
        "The drop chance must be between 0 and 1."
      }
    }
  }

  init {
    require(this.identifier.isNotBlank()) {
      "The mining block identifier must not be blank."
    }

    require(this.durability > 0.0) {
      "The mining block durability must be greater than 0."
    }

    require(this.experience >= 0L) {
      "The mining block experience must not be negative."
    }

    require(this.respawnTicks >= 1L) {
      "The mining block respawn duration must be at least one tick."
    }
  }
}
