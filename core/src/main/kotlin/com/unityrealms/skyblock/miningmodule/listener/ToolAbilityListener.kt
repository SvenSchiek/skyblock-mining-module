/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.listener

import com.unityrealms.server.messageframework.message.component.ComponentTransformer
import com.unityrealms.skyblock.miningmodule.mining.MiningCore
import com.unityrealms.skyblock.miningmodule.mining.session.MiningSessionManager
import com.unityrealms.skyblock.miningmodule.tool.runtime.ToolRuntimeService

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot

/**
 * Activates the selected mining tool ability through player interaction.
 */
class ToolAbilityListener(
  private val miningSessionManager: MiningSessionManager,
  private val toolRuntimeService: ToolRuntimeService
) : Listener {

  @Suppress("unused")
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  fun onPlayerInteract(event: PlayerInteractEvent) {
    if (event.hand != EquipmentSlot.HAND) {
      return
    }

    if (event.action !in setOf(Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK)) {
      return
    }

    val player = event.player

    if (!(this.toolRuntimeService.isProgressionTool(player.inventory.itemInMainHand))) {
      return
    }

    if (!(this.toolRuntimeService.matchesAbilityActivation(player, player.isSneaking))) {
      return
    }

    event.isCancelled = true
    val result = this.miningSessionManager.activateSelectedAbility(player)
    val messagePath = if (result.success) {
      "message.command.tool.ability.success"
    } else {
      "message.command.tool.ability.failure"
    }
    val message = if (result.success) {
      MiningCore.resolveMessage(messagePath, result.definition?.displayName ?: "Ability")
    } else {
      MiningCore.resolveMessage(messagePath, result.message)
    }
    player.sendMessage(ComponentTransformer.transform(message))
  }
}
