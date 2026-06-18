/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.tool

/**
 * Represents fragment drop configuration for one rarity.
 */
data class FragmentDefinition(
  val rarity: FragmentRarity,
  val baseChance: Double,
  val eligibleActivationSourceSet: Set<ActivationSource>,
  val allowSecondaryBlocks: Boolean,
  val softPityStart: Long,
  val pityGrowth: Double,
  val hardPityThreshold: Long?,
  val prestigeBaseChanceMultiplier: Double,
  val prestigeSoftPityReduction: Long,
  val prestigePityGrowthMultiplier: Double
) {

  init {
    require(this.baseChance in 0.0..1.0) {
      "The fragment base chance must be between 0 and 1."
    }

    require(this.softPityStart >= 0L) {
      "The fragment soft-pity start must not be negative."
    }

    require(this.pityGrowth >= 0.0) {
      "The fragment pity growth must not be negative."
    }
  }
}
