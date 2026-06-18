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
 * Represents safety limits shared by enchantments and abilities.
 */
data class ToolSafetyConfiguration(
  val maximumAffectedBlocksPerActivation: Int,
  val maximumRecursionDepth: Int,
  val secondaryExperienceMultiplier: Double,
  val secondaryDropMultiplier: Double,
  val allowSecondaryFragmentRolls: Boolean
)
