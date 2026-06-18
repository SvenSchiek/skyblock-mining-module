/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.command.mining.implementation

import com.unityrealms.server.messageframework.message.component.ComponentTransformer
import com.unityrealms.skyblock.miningmodule.mining.MiningCore
import com.unityrealms.skyblock.miningmodule.command.Command
import com.unityrealms.skyblock.miningmodule.command.CommandValidator
import com.unityrealms.skyblock.miningmodule.command.mining.MiningCommand

import org.bukkit.entity.Player

/** Displays paginated mining command help. */
class MiningHelpCommand(private val permission: String) : Command {

  override fun execute(player: Player, argumentArray: Array<String>): Boolean {
    if (!(player.hasPermission(this.permission))) {
      player.sendMessage(ComponentTransformer.transform(MiningCore.resolveMessage("message.command.no_permission")))
      return true
    }

    val page = argumentArray.firstOrNull()?.toIntOrNull() ?: 1
    CommandValidator.sendUsagePage(page, 6, player, MiningCommand.getUsageEntryListFor(player))
    return true
  }

  override fun tabComplete(player: Player, argumentArray: Array<String>): List<String> = emptyList()
}
