/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.tool.runtime

import com.unityrealms.skyblock.miningmodule.tool.AbilityDefinition
import com.unityrealms.skyblock.miningmodule.tool.FragmentDropResult
import com.unityrealms.skyblock.miningmodule.tool.ToolProgressionResult
import com.unityrealms.skyblock.miningmodule.tool.enchantment.EnchantmentDefinition

/**
 * Represents all tool progression changes caused by one mining completion.
 */
data class ToolMiningCompletionResult(
  val progressionResult: ToolProgressionResult?,
  val fragmentDropResultList: List<FragmentDropResult>,
  val unlockedEnchantmentList: List<EnchantmentDefinition>,
  val unlockedAbilityList: List<AbilityDefinition>
)
