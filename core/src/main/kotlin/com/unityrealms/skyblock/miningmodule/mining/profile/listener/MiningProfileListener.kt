/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.mining.profile.listener

import com.unityrealms.skyblock.miningmodule.mining.profile.MiningProfileManager
import com.unityrealms.skyblock.miningmodule.mining.session.MiningSessionManager
import com.unityrealms.skyblock.miningmodule.mining.statistic.MiningStatisticManager
import com.unityrealms.skyblock.miningmodule.tool.runtime.ToolRuntimeService

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.plugin.Plugin

/**
 * Represents a listener for player events related to mining profiles and tool state.
 */
class MiningProfileListener(
  private val plugin: Plugin,
  private val miningSessionManager: MiningSessionManager,
  private val miningProfileManager: MiningProfileManager,
  private val miningStatisticManager: MiningStatisticManager,
  private val toolRuntimeService: ToolRuntimeService
) : Listener {

  @Suppress("unused")
  @EventHandler(priority = EventPriority.MONITOR)
  fun onPlayerJoin(playerJoinEvent: PlayerJoinEvent) {
    val player = playerJoinEvent.player
    this.miningProfileManager.warm(player.uniqueId)
    this.plugin.server.scheduler.runTask(this.plugin, Runnable {
      player.inventory.contents.filterNotNull().forEach { itemStack ->
        this.toolRuntimeService.warmItem(player, itemStack)
      }
    })
  }

  @Suppress("unused")
  @EventHandler(priority = EventPriority.MONITOR)
  fun onPlayerQuit(playerQuitEvent: PlayerQuitEvent) {
    val player = playerQuitEvent.player
    this.miningSessionManager.cancel(player.uniqueId)
    player.inventory.contents.filterNotNull().forEach(this.toolRuntimeService::unloadItem)
    this.miningProfileManager.unload(player.uniqueId)
    this.miningStatisticManager.unload(player.uniqueId)
  }

  @Suppress("unused")
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  fun onPlayerTeleport(playerTeleportEvent: PlayerTeleportEvent) {
    this.miningSessionManager.cancel(playerTeleportEvent.player.uniqueId)
  }

  @Suppress("unused")
  @EventHandler(priority = EventPriority.MONITOR)
  fun onPlayerChangedWorld(playerChangedWorldEvent: PlayerChangedWorldEvent) {
    this.miningSessionManager.cancel(playerChangedWorldEvent.player.uniqueId)
  }

  @Suppress("unused")
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  fun onPlayerItemHeld(playerItemHeldEvent: PlayerItemHeldEvent) {
    val player = playerItemHeldEvent.player
    this.miningSessionManager.cancel(player.uniqueId)
    this.toolRuntimeService.unloadItem(player.inventory.getItem(playerItemHeldEvent.previousSlot))
    this.plugin.server.scheduler.runTask(this.plugin, Runnable {
      this.toolRuntimeService.warmItem(player, player.inventory.itemInMainHand)
    })
  }
}
