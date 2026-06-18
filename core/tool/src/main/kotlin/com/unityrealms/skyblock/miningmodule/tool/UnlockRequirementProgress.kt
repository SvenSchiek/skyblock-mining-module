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
 * Represents one requirement line used by menus and unlock feedback.
 */
data class UnlockRequirementProgress(
  val description: String,
  val current: Long,
  val required: Long,
  val completed: Boolean
)
