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

/**
 * Represents one configured enchantment level.
 */
data class EnchantmentLevelDefinition(
  val enchantmentPoints: Int,
  val fragmentCostMap: Map<FragmentRarity, Int> = emptyMap(),
  val effectConfiguration: Map<String, Double> = emptyMap(),
  val breakthrough: BreakthroughDefinition? = null
)
