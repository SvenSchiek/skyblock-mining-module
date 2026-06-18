/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.tool

import com.unityrealms.skyblock.miningmodule.tool.profile.ToolProfile

/**
 * Represents the result of a tool prestige transaction.
 */
data class ToolPrestigeResult(
  val previousProfile: ToolProfile,
  val profile: ToolProfile,
  val reward: ToolPrestigeConfiguration.Reward
)
