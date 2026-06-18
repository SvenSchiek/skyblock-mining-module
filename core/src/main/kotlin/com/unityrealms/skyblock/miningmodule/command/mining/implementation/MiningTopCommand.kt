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

import net.kyori.adventure.text.Component

import org.bukkit.Bukkit
import org.bukkit.entity.Player

/** Displays mining leaderboards. */
class MiningTopCommand(private val permission: String) : Command {

  override fun execute(player: Player, argumentArray: Array<String>): Boolean {
    if (!(player.hasPermission(this.permission))) {
      player.sendMessage(ComponentTransformer.transform(MiningCore.resolveMessage("message.command.no_permission")))
      return true
    }

    val type = argumentArray.firstOrNull()?.lowercase() ?: "prestige"
    val profileList = when (type) {
      "blocks", "mined" -> MiningCore.miningProfileManager.topByBlocks(10)
      else -> MiningCore.miningProfileManager.topByPrestige(10)
    }

    if (profileList.isEmpty()) {
      player.sendMessage(ComponentTransformer.transform(MiningCore.resolveMessage("message.command.mining.top_empty")))
      return true
    }

    player.sendMessage(Component.empty())
    player.sendMessage(
      ComponentTransformer.transform(
        MiningCore.resolveMessage("message.command.mining.top_header", if (type == "blocks" || type == "mined") "Blocks" else "Prestige")
      )
    )

    profileList.forEachIndexed { index, profile ->
      val name = Bukkit.getOfflinePlayer(profile.identifier).name ?: profile.identifier.toString().take(8)
      player.sendMessage(
        ComponentTransformer.transform(
          MiningCore.resolveMessage(
            "message.command.mining.top_entry",
            index + 1,
            name,
            profile.prestige,
            profile.level,
            profile.totalBlocksMined
          )
        )
      )
    }

    player.sendMessage(Component.empty())
    return true
  }

  override fun tabComplete(player: Player, argumentArray: Array<String>): List<String> {
    return if (argumentArray.size == 1) {
      listOf("prestige", "blocks").filter { value -> value.startsWith(argumentArray[0], ignoreCase = true) }
    } else {
      emptyList()
    }
  }
}
