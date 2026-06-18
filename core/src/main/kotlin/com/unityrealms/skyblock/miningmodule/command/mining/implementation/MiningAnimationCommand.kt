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

import org.bukkit.entity.Player

/** Changes personal mining animation preferences. */
class MiningAnimationCommand(private val permission: String) : Command {

  override fun execute(player: Player, argumentArray: Array<String>): Boolean {
    if (!(player.hasPermission(this.permission))) {
      player.sendMessage(ComponentTransformer.transform(MiningCore.resolveMessage("message.command.no_permission")))
      return true
    }

    if (argumentArray.isEmpty()) {
      player.sendMessage(ComponentTransformer.transform(MiningCore.resolveMessage("message.command.mining.animation_usage")))
      return true
    }

    val currentProfile = MiningCore.miningProfileManager.getOrCreate(player.uniqueId)
    val enabled = when (argumentArray[0].lowercase()) {
      "on", "enable", "enabled" -> true
      "off", "disable", "disabled" -> false
      "toggle" -> !(currentProfile.animationEnabled)
      else -> {
        player.sendMessage(ComponentTransformer.transform(MiningCore.resolveMessage("message.command.mining.animation_usage")))
        return true
      }
    }

    MiningCore.miningProfileManager.setAnimationEnabled(player.uniqueId, enabled)
    player.sendMessage(
      ComponentTransformer.transform(
        MiningCore.resolveMessage(if (enabled) "message.command.mining.animation_enabled" else "message.command.mining.animation_disabled")
      )
    )
    return true
  }

  override fun tabComplete(player: Player, argumentArray: Array<String>): List<String> {
    return if (argumentArray.size == 1) {
      listOf("on", "off", "toggle").filter { value -> value.startsWith(argumentArray[0], ignoreCase = true) }
    } else {
      emptyList()
    }
  }
}
