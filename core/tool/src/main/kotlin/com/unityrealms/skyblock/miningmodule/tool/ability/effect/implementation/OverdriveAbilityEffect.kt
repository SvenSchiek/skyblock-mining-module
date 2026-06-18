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
import com.unityrealms.skyblock.miningmodule.tool.TemporaryToolModifierService
import com.unityrealms.skyblock.miningmodule.tool.ToolAbilityState
import com.unityrealms.skyblock.miningmodule.tool.mining.ToolMiningContext
import com.unityrealms.skyblock.miningmodule.tool.ability.effect.AbilityEffect

/**
 * Temporarily increases pickaxe damage and experience gain.
 */
class OverdriveAbilityEffect(
  private val modifierService: TemporaryToolModifierService
) : AbilityEffect {

  override val identifier: String = "overdrive"

  override fun execute(definition: AbilityDefinition, state: ToolAbilityState, context: ToolMiningContext) {
    val levelConfiguration = definition.levelDefinitionMap[state.level]?.effectConfiguration.orEmpty()
    val damageMultiplier = levelConfiguration["damage_multiplier"]
      ?: definition.effectConfiguration["damage_multiplier"]
      ?: 1.5
    val experienceMultiplier = levelConfiguration["experience_multiplier"]
      ?: definition.effectConfiguration["experience_multiplier"]
      ?: 1.25
    val durationMillis = (
      levelConfiguration["duration_seconds"]
        ?: definition.effectConfiguration["duration_seconds"]
        ?: 10.0
      ).times(1000.0).toLong()

    this.modifierService.activate(
      toolIdentifier = context.toolProfile.toolIdentifier,
      damageMultiplier = damageMultiplier,
      experienceMultiplier = experienceMultiplier,
      durationMillis = durationMillis
    )
  }
}
