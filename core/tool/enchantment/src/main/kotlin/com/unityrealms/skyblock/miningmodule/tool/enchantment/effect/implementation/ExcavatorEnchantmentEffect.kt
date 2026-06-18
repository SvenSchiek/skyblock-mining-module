/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.tool.enchantment.effect.implementation

import com.unityrealms.skyblock.miningmodule.tool.mining.ToolMiningContext
import com.unityrealms.skyblock.miningmodule.tool.enchantment.EnchantmentDefinition
import com.unityrealms.skyblock.miningmodule.tool.enchantment.EnchantmentLevelDefinition
import com.unityrealms.skyblock.miningmodule.tool.enchantment.ToolEnchantmentState
import com.unityrealms.skyblock.miningmodule.tool.enchantment.effect.EnchantmentEffect

import java.util.concurrent.ThreadLocalRandom
import kotlin.math.roundToInt

/**
 * Example enchantment that processes nearby blocks through the controlled mining pipeline.
 *
 * Its level-three breakthrough expands the radius and the maximum affected block count.
 */
class ExcavatorEnchantmentEffect : EnchantmentEffect {

  override val identifier: String = "excavator"

  override fun onBlockMined(
    definition: EnchantmentDefinition,
    levelDefinition: EnchantmentLevelDefinition,
    state: ToolEnchantmentState,
    context: ToolMiningContext
  ) {
    val baseConfiguration = definition.effectConfiguration + levelDefinition.effectConfiguration
    val breakthroughConfiguration = levelDefinition.breakthrough?.effectConfiguration.orEmpty()
    val configuration = baseConfiguration + breakthroughConfiguration
    val chance = (configuration["chance"] ?: 0.05).coerceIn(0.0, 1.0)

    if (ThreadLocalRandom.current().nextDouble() >= chance) {
      return
    }

    val radius = (configuration["radius"] ?: 1.0).roundToInt().coerceIn(1, 4)
    val maximumBlocks = (configuration["maximum_blocks"] ?: 1.0).roundToInt().coerceAtLeast(1)
    val allowFragmentRolls = configuration["allow_fragment_rolls"] == 1.0
    var processedBlocks = 0

    for (x in -radius..radius) {
      for (y in -radius..radius) {
        for (z in -radius..radius) {
          if (processedBlocks >= maximumBlocks) {
            return
          }

          if (x == 0 && y == 0 && z == 0) {
            continue
          }

          val block = context.originBlock.getRelative(x, y, z)

          if (context.processSecondaryBlock(block, allowFragmentRolls)) {
            processedBlocks++
          }
        }
      }
    }
  }
}
