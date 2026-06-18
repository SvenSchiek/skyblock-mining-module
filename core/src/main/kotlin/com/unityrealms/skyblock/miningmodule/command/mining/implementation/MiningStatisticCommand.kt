/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.command.mining.implementation

import com.unityrealms.skyblock.miningmodule.command.Command

import org.bukkit.entity.Player

/** Compatibility command for the singular statistic subcommand. */
class MiningStatisticCommand(permission: String) : Command {

  private val delegate = MiningStatisticsCommand(permission)

  override fun execute(player: Player, argumentArray: Array<String>): Boolean = this.delegate.execute(player, argumentArray)

  override fun tabComplete(player: Player, argumentArray: Array<String>): List<String> = this.delegate.tabComplete(player, argumentArray)
}
