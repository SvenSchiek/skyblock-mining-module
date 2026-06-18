/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.menu.listener

import com.unityrealms.skyblock.miningmodule.menu.MenuRegistry

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerQuitEvent

/** Listener for mining menus. */
class MenuListener : Listener {

  @Suppress("unused")
  @EventHandler(priority = EventPriority.LOWEST)
  fun onInventoryClick(event: InventoryClickEvent) {
    val player = event.whoClicked as? Player ?: return
    val playerIdentifier = player.uniqueId

    if (!(MenuRegistry.isMenu(playerIdentifier))) {
      return
    }

    val registeredInventory = MenuRegistry.getInventory(playerIdentifier) ?: return

    if (event.view.topInventory !== registeredInventory) {
      return
    }

    event.isCancelled = true

    if (event.clickedInventory !== registeredInventory) {
      return
    }

    MenuRegistry.getMenu(playerIdentifier)?.onClick(event.click, event.slot, player)
  }

  @Suppress("unused")
  @EventHandler(priority = EventPriority.LOWEST)
  fun onInventoryClose(event: InventoryCloseEvent) {
    val player = event.player as? Player ?: return
    val playerIdentifier = player.uniqueId
    val registeredInventory = MenuRegistry.getInventory(playerIdentifier) ?: return

    if (event.inventory === registeredInventory) {
      MenuRegistry.getMenu(playerIdentifier)?.close(player)
      MenuRegistry.unregister(playerIdentifier)
    }
  }

  @Suppress("unused")
  @EventHandler
  fun onPlayerQuit(event: PlayerQuitEvent) {
    MenuRegistry.unregister(event.player.uniqueId)
  }
}
