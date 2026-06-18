/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.command.mining.implementation.level

import com.unityrealms.server.messageframework.message.component.ComponentTransformer
import com.unityrealms.skyblock.miningmodule.command.Command
import com.unityrealms.skyblock.miningmodule.command.CommandValidator
import com.unityrealms.skyblock.miningmodule.mining.MiningCore

import org.bukkit.entity.Player

/** Sets a player's mining level. */
class MiningLevelCommand(private val permission: String) : Command {

  override fun execute(player: Player, argumentArray: Array<String>): Boolean {
    if (!(player.hasPermission(this.permission))) {
      player.sendMessage(ComponentTransformer.transform(MiningCore.resolveMessage("message.command.no_permission")))
      return true
    }

    if (argumentArray.size < 3 || !(argumentArray[0].equals("set", ignoreCase = true))) {
      player.sendMessage(ComponentTransformer.transform(MiningCore.resolveMessage("message.command.mining.level_usage")))
      return true
    }

    val target = CommandValidator.resolveOfflinePlayer(argumentArray[1], player) ?: return true
    val level = CommandValidator.parseInt(argumentArray[2], player) ?: return true

    if (level !in 1..MiningCore.miningLevelManager.maximumLevel) {
      player.sendMessage(ComponentTransformer.transform(MiningCore.resolveMessage("message.command.mining.level_range", MiningCore.miningLevelManager.maximumLevel)))
      return true
    }

    MiningCore.miningProfileManager.setLevel(target.uniqueId, level)
    player.sendMessage(ComponentTransformer.transform(MiningCore.resolveMessage("message.command.mining.level_success", target.name ?: argumentArray[1], level)))
    return true
  }

  override fun tabComplete(player: Player, argumentArray: Array<String>): List<String> = when (argumentArray.size) {
    1 -> listOf("set").filter { value -> value.startsWith(argumentArray[0], ignoreCase = true) }
    2 -> CommandValidator.onlinePlayerSuggestions(argumentArray[1])
    else -> emptyList()
  }
}
