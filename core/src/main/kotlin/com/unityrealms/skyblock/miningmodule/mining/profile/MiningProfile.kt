/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.mining.profile

import java.util.UUID

/**
 * Represents the persistent mining profile of a player.
 */
data class MiningProfile(
  val identifier: UUID,
  val level: Int = 1,
  val experience: Long = 0L,
  val prestige: Int = 0,
  val totalBlocksMined: Long = 0L,
  val totalExperienceEarned: Long = 0L,
  val animationEnabled: Boolean = true,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
  val lastMinedAt: Long? = null
) {

  init {
    require(this.level >= 1) {
      "The mining level must be at least 1."
    }

    require(this.experience >= 0L) {
      "The mining experience must not be negative."
    }

    require(this.prestige >= 0) {
      "The mining prestige must not be negative."
    }

    require(this.totalBlocksMined >= 0L) {
      "The total mined block amount must not be negative."
    }
  }
}
