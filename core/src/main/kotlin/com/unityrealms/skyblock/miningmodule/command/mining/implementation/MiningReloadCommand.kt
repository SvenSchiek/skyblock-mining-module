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
import com.unityrealms.skyblock.miningmodule.MiningModule
import com.unityrealms.skyblock.miningmodule.command.Command
import com.unityrealms.skyblock.miningmodule.mining.MiningCore

import org.bukkit.entity.Player

/** Reloads mining and tool configuration. */
class MiningReloadCommand(
  private val miningModule: MiningModule,
  private val permission: String
) : Command {

  override fun execute(player: Player, argumentArray: Array<String>): Boolean {
    if (!(player.hasPermission(this.permission))) {
      player.sendMessage(ComponentTransformer.transform(MiningCore.resolveMessage("message.command.no_permission")))
      return true
    }

    val successful = MiningCore.reload(this.miningModule)
    player.sendMessage(
      ComponentTransformer.transform(
        MiningCore.resolveMessage(
          if (successful) "message.command.mining.reload_success" else "message.command.mining.reload_failure"
        )
      )
    )
    return true
  }

  override fun tabComplete(player: Player, argumentArray: Array<String>): List<String> = emptyList()
}
