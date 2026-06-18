/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.mining.level

import com.unityrealms.skyblock.miningmodule.mining.configuration.MiningConfiguration

import kotlin.math.ceil
import kotlin.math.pow

/**
 * Calculates mining experience requirements.
 */
class MiningLevelManager(configuration: MiningConfiguration.Level) {

  @Volatile
  private var configuration: MiningConfiguration.Level = configuration

  val maximumLevel: Int
    get() = this.configuration.maximumLevel

  /** Updates the level configuration. */
  fun update(configuration: MiningConfiguration.Level) {
    this.configuration = configuration
  }

  /**
   * Returns the experience required to advance from the specified level.
   *
   * @param level The current level.
   *
   * @return The required experience, or 0 at maximum level.
   */
  fun requiredExperience(level: Int): Long {
    if (level >= this.maximumLevel) {
      return 0L
    }

    val safeLevel = level.coerceAtLeast(1)
    val calculated = this.configuration.baseExperience.toDouble() *
      this.configuration.experienceMultiplier.pow((safeLevel - 1).toDouble())

    if (!(calculated.isFinite()) || calculated >= Long.MAX_VALUE.toDouble()) {
      return Long.MAX_VALUE
    }

    return ceil(calculated).toLong().coerceAtLeast(1L)
  }

  /** Returns the progress between 0 and 1 for a level and experience value. */
  fun progress(level: Int, experience: Long): Double {
    val requiredExperience = this.requiredExperience(level)

    if (requiredExperience <= 0L) {
      return 1.0
    }

    return experience.toDouble().div(requiredExperience.toDouble()).coerceIn(0.0, 1.0)
  }
}
