/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.mining.profile

/**
 * Represents the result of awarding mining experience.
 */
data class MiningExperienceResult(
  val previousProfile: MiningProfile,
  val profile: MiningProfile,
  val awardedExperience: Long,
  val gainedLevelList: List<Int>,
  val requiredExperience: Long
) {

  val leveledUp: Boolean
    get() = this.gainedLevelList.isNotEmpty()
}

/**
 * Represents the result of a mining prestige operation.
 */
data class MiningPrestigeResult(
  val success: Boolean,
  val message: String,
  val previousProfile: MiningProfile,
  val profile: MiningProfile
)
