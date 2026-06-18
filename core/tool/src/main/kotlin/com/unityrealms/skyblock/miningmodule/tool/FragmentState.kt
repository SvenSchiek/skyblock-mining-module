/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.tool

import java.util.UUID

/**
 * Represents a fragment balance and its independent pity progress.
 */
data class FragmentState(
  val toolIdentifier: UUID,
  val rarity: FragmentRarity,
  val amount: Int = 0,
  val failedEligibleRolls: Long = 0L,
  val updatedAt: Long = System.currentTimeMillis()
)
