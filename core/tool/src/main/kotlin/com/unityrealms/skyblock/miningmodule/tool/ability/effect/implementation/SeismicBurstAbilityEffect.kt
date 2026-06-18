/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.tool.ability.effect.implementation

import com.unityrealms.skyblock.miningmodule.tool.AbilityDefinition
import com.unityrealms.skyblock.miningmodule.tool.ToolAbilityState
import com.unityrealms.skyblock.miningmodule.tool.mining.ToolMiningContext
import com.unityrealms.skyblock.miningmodule.tool.ability.effect.AbilityEffect

import kotlin.math.roundToInt

/**
 * Processes nearby blocks through the validated secondary mining pipeline.
 */
class SeismicBurstAbilityEffect : AbilityEffect {

  override val identifier: String = "seismic_burst"

  override fun execute(definition: AbilityDefinition, state: ToolAbilityState, context: ToolMiningContext) {
    val levelConfiguration = definition.levelDefinitionMap[state.level]?.effectConfiguration.orEmpty()
    val radius = (
      levelConfiguration["radius"]
        ?: definition.effectConfiguration["radius"]
        ?: 2.0
      ).roundToInt().coerceIn(1, 5)
    val maximumBlocks = (
      levelConfiguration["maximum_blocks"]
        ?: definition.effectConfiguration["maximum_blocks"]
        ?: 12.0
      ).roundToInt().coerceAtLeast(1)
    val allowFragmentRolls = (
      levelConfiguration["allow_fragment_rolls"]
        ?: definition.effectConfiguration["allow_fragment_rolls"]
        ?: 0.0
      ) == 1.0
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
