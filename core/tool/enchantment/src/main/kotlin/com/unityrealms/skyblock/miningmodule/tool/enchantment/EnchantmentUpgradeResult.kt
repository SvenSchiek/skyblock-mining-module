/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.tool.enchantment

/**
 * Represents the result of an atomic enchantment upgrade.
 */
data class EnchantmentUpgradeResult(
  val state: ToolEnchantmentState,
  val definition: EnchantmentDefinition,
  val levelDefinition: EnchantmentLevelDefinition,
  val breakthrough: BreakthroughDefinition?
)
