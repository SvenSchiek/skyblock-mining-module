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
import com.unityrealms.skyblock.miningmodule.command.Command
import com.unityrealms.skyblock.miningmodule.command.CommandValidator
import com.unityrealms.skyblock.miningmodule.mining.MiningCore

import org.bukkit.entity.Player

/** Reads the mining profile of a player. */
class MiningGetCommand(private val permission: String) : Command {

  override fun execute(player: Player, argumentArray: Array<String>): Boolean {
    if (!(player.hasPermission(this.permission))) {
      player.sendMessage(ComponentTransformer.transform(MiningCore.resolveMessage("message.command.no_permission")))
      return true
    }

    if (argumentArray.isEmpty()) {
      player.sendMessage(ComponentTransformer.transform(MiningCore.resolveMessage("message.command.mining.get_usage")))
      return true
    }

    val target = CommandValidator.resolveOfflinePlayer(argumentArray[0], player) ?: return true
    val profile = MiningCore.miningProfileManager.getOrCreate(target.uniqueId)
    val requiredExperience = MiningCore.miningLevelManager.requiredExperience(profile.level)
    player.sendMessage(
      ComponentTransformer.transform(
        MiningCore.resolveMessage(
          "message.command.mining.profile",
          target.name ?: argumentArray[0],
          profile.prestige,
          profile.level,
          profile.experience,
          requiredExperience,
          profile.totalBlocksMined
        )
      )
    )
    return true
  }

  override fun tabComplete(player: Player, argumentArray: Array<String>): List<String> = when (argumentArray.size) {
    1 -> CommandValidator.onlinePlayerSuggestions(argumentArray[0])
    else -> emptyList()
  }
}
