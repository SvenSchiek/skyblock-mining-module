/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.api

import java.util.UUID

/**
 * Represents a read-only mining profile view.
 */
data class MiningProfileView(
  val identifier: UUID,
  val level: Int,
  val experience: Long,
  val prestige: Int,
  val totalBlocksMined: Long,
  val totalExperienceEarned: Long
)
