/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.tool.ability.effect

import com.unityrealms.skyblock.miningmodule.tool.AbilityDefinition
import com.unityrealms.skyblock.miningmodule.tool.ToolAbilityState
import com.unityrealms.skyblock.miningmodule.tool.mining.ToolMiningContext

/**
 * Represents an executable tool ability effect.
 */
interface AbilityEffect {

  val identifier: String

  fun validate(definition: AbilityDefinition, state: ToolAbilityState, context: ToolMiningContext): String? = null

  fun execute(definition: AbilityDefinition, state: ToolAbilityState, context: ToolMiningContext)
}
