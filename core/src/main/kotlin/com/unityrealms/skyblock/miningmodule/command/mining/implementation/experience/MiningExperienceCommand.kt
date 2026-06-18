/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.command.mining.implementation.experience

import com.unityrealms.server.messageframework.message.component.ComponentTransformer
import com.unityrealms.skyblock.miningmodule.command.Command
import com.unityrealms.skyblock.miningmodule.command.CommandValidator
import com.unityrealms.skyblock.miningmodule.mining.MiningCore

import java.util.Locale

import org.bukkit.entity.Player

/** Adds or sets mining experience. */
class MiningExperienceCommand(private val permission: String) : Command {

  override fun execute(player: Player, argumentArray: Array<String>): Boolean {
    if (!(player.hasPermission(this.permission))) {
      player.sendMessage(ComponentTransformer.transform(MiningCore.resolveMessage("message.command.no_permission")))
      return true
    }

    if (argumentArray.size < 3) {
      player.sendMessage(ComponentTransformer.transform(MiningCore.resolveMessage("message.command.mining.experience_usage")))
      return true
    }

    val target = CommandValidator.resolveOfflinePlayer(argumentArray[1], player) ?: return true
    val amount = CommandValidator.parseLong(argumentArray[2], player) ?: return true

    if (amount < 0L) {
      player.sendMessage(ComponentTransformer.transform(MiningCore.resolveMessage("message.command.mining.non_negative")))
      return true
    }

    when (argumentArray[0].lowercase(Locale.ROOT)) {
      "add" -> MiningCore.miningProfileManager.addExperience(target.uniqueId, amount)
      "set" -> MiningCore.miningProfileManager.setExperience(target.uniqueId, amount)
      else -> {
        player.sendMessage(ComponentTransformer.transform(MiningCore.resolveMessage("message.command.mining.experience_usage")))
        return true
      }
    }

    player.sendMessage(
      ComponentTransformer.transform(
        MiningCore.resolveMessage(
          "message.command.mining.experience_success",
          argumentArray[0].lowercase(Locale.ROOT),
          amount,
          target.name ?: argumentArray[1]
        )
      )
    )
    return true
  }

  override fun tabComplete(player: Player, argumentArray: Array<String>): List<String> = when (argumentArray.size) {
    1 -> listOf("add", "set").filter { value -> value.startsWith(argumentArray[0], ignoreCase = true) }
    2 -> CommandValidator.onlinePlayerSuggestions(argumentArray[1])
    else -> emptyList()
  }
}
