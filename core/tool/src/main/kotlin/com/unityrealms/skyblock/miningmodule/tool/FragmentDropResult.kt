/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.tool

/**
 * Represents the result of one fragment rarity roll.
 */
data class FragmentDropResult(
  val rarity: FragmentRarity,
  val dropped: Boolean,
  val effectiveChance: Double,
  val hardPityTriggered: Boolean,
  val state: FragmentState
)
