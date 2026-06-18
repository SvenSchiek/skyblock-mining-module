/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.mining.prestige

import java.util.UUID

/**
 * Represents a persisted mining prestige transition.
 */
data class MiningPrestigeHistory(
  val identifier: UUID = UUID.randomUUID(),
  val playerIdentifier: UUID,
  val previousPrestige: Int,
  val newPrestige: Int,
  val timestamp: Long = System.currentTimeMillis()
)
