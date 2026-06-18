/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.mining.session

import com.unityrealms.skyblock.miningmodule.mining.block.BlockPosition

import java.util.UUID

/** Represents an active custom mining session. */
data class MiningSession(
  val playerIdentifier: UUID,
  val blockPosition: BlockPosition,
  val miningBlockIdentifier: String,
  val currentDamage: Double,
  val maximumDamage: Double,
  val startedAt: Long,
  val lastHitAt: Long
)
