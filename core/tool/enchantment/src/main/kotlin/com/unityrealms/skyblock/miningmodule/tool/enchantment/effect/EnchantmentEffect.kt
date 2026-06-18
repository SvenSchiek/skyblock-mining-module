/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.tool.enchantment.effect

import com.unityrealms.skyblock.miningmodule.tool.enchantment.EnchantmentDefinition
import com.unityrealms.skyblock.miningmodule.tool.enchantment.EnchantmentLevelDefinition
import com.unityrealms.skyblock.miningmodule.tool.enchantment.ToolEnchantmentState
import com.unityrealms.skyblock.miningmodule.tool.mining.ToolMiningContext

/**
 * Represents an executable passive enchantment effect.
 */
interface EnchantmentEffect {

  val identifier: String

  fun damageMultiplier(
    definition: EnchantmentDefinition,
    levelDefinition: EnchantmentLevelDefinition,
    state: ToolEnchantmentState,
    context: ToolMiningContext?
  ): Double = 1.0

  fun fortuneBonus(
    definition: EnchantmentDefinition,
    levelDefinition: EnchantmentLevelDefinition,
    state: ToolEnchantmentState,
    context: ToolMiningContext?
  ): Int = 0

  fun onBlockMined(
    definition: EnchantmentDefinition,
    levelDefinition: EnchantmentLevelDefinition,
    state: ToolEnchantmentState,
    context: ToolMiningContext
  ) {}
}
