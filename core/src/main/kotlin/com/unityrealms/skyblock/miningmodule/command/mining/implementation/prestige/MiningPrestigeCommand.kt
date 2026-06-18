/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.command.mining.implementation.prestige

import com.unityrealms.server.messageframework.message.component.ComponentTransformer
import com.unityrealms.skyblock.miningmodule.command.Command
import com.unityrealms.skyblock.miningmodule.command.CommandValidator
import com.unityrealms.skyblock.miningmodule.menu.implementation.MiningPrestigeMenu
import com.unityrealms.skyblock.miningmodule.mining.MiningCore

import org.bukkit.entity.Player

/** Opens prestige progression or sets a player's prestige. */
class MiningPrestigeCommand(private val permission: String) : Command {

  override fun execute(player: Player, argumentArray: Array<String>): Boolean {
    if (!(player.hasPermission(this.permission))) {
      player.sendMessage(ComponentTransformer.transform(MiningCore.resolveMessage("message.command.no_permission")))
      return true
    }

    if (argumentArray.isEmpty()) {
      MiningPrestigeMenu(player.uniqueId).open(player)
      return true
    }

    if (argumentArray.size < 3 || !(argumentArray[0].equals("set", ignoreCase = true))) {
      player.sendMessage(ComponentTransformer.transform(MiningCore.resolveMessage("message.command.mining.prestige_usage")))
      return true
    }

    val target = CommandValidator.resolveOfflinePlayer(argumentArray[1], player) ?: return true
    val prestige = CommandValidator.parseInt(argumentArray[2], player) ?: return true

    if (prestige !in 0..MiningCore.miningProfileManager.maximumPrestige) {
      player.sendMessage(ComponentTransformer.transform(MiningCore.resolveMessage("message.command.mining.prestige_range", MiningCore.miningProfileManager.maximumPrestige)))
      return true
    }

    MiningCore.miningProfileManager.setPrestige(target.uniqueId, prestige)
    player.sendMessage(ComponentTransformer.transform(MiningCore.resolveMessage("message.command.mining.prestige_set_success", target.name ?: argumentArray[1], prestige)))
    return true
  }

  override fun tabComplete(player: Player, argumentArray: Array<String>): List<String> = when (argumentArray.size) {
    1 -> listOf("set").filter { value -> value.startsWith(argumentArray[0], ignoreCase = true) }
    2 -> CommandValidator.onlinePlayerSuggestions(argumentArray[1])
    else -> emptyList()
  }
}
