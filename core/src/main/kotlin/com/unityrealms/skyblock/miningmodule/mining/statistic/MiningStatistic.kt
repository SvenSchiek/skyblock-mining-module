/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.mining.statistic

import java.util.UUID

/**
 * Represents per-block mining statistics of a player.
 */
data class MiningStatistic(
  val playerIdentifier: UUID,
  val blockIdentifier: String,
  val blocksMined: Long,
  val experienceEarned: Long,
  val lastMinedAt: Long?
)
