/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.ability.activation

import com.unityrealms.skyblock.miningmodule.ability.Ability

/**
 * Represents the result of attempting to activate an ability.
 */
data class AbilityActivationResult(
  val success: Boolean,
  val message: String,

  val definition: Ability?,

  val cooldownEndsAt: Long? = null
)
