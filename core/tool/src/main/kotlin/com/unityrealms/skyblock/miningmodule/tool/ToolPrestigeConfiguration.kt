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
 * Represents deterministic prestige requirements and rewards.
 */
data class ToolPrestigeConfiguration(
  val maximumPrestige: Int,
  val requirementMap: Map<Int, Requirement>,
  val rewardMap: Map<Int, Reward>
) {

  data class Requirement(
    val maximumPickaxeLevelRequired: Boolean = true,
    val totalBlocksMined: Long = 0L,
    val totalOresMined: Long = 0L
  )

  data class Reward(
    val pickaxeExperienceMultiplier: Double = 0.0,
    val fragmentChanceMultiplier: Double = 0.0,
    val enchantmentPoints: Int = 0
  )
}
