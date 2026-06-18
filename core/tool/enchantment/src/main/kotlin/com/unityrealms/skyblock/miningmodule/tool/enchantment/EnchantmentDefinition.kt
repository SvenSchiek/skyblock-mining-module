/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.tool.enchantment

import com.unityrealms.skyblock.miningmodule.tool.ToolUnlockRequirement

/**
 * Represents a configured passive mining enchantment.
 */
data class EnchantmentDefinition(
  val identifier: String,
  val displayName: String,
  val descriptionList: List<String>,
  val maximumLevel: Int,
  val unlockRequirementList: List<ToolUnlockRequirement>,
  val levelDefinitionMap: Map<Int, EnchantmentLevelDefinition>,
  val effectIdentifier: String,
  val effectConfiguration: Map<String, Double> = emptyMap(),
  val enabled: Boolean = true
)
