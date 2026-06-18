/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.command

import org.bukkit.entity.Player

/** Represents an internal command implementation. */
interface Command {

  fun execute(player: Player, argumentArray: Array<String>): Boolean

  fun tabComplete(player: Player, argumentArray: Array<String>): List<String>
}
