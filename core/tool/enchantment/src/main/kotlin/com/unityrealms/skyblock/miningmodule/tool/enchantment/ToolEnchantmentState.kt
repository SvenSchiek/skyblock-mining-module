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

import java.util.UUID

/**
 * Represents the persisted state of one enchantment on one mining tool.
 */
data class ToolEnchantmentState(
  val toolIdentifier: UUID,
  val enchantmentIdentifier: String,
  val unlocked: Boolean = false,
  val level: Int = 0,
  val investedEnchantmentPoints: Int = 0,
  val investedFragments: Map<FragmentRarity, Int> = emptyMap(),
  val updatedAt: Long = System.currentTimeMillis()
)
