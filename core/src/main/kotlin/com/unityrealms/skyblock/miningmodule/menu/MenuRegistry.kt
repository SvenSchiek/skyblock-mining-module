/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.menu

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

import org.bukkit.inventory.Inventory

/** Registry for active player menus. */
object MenuRegistry {

  data class RegisteredEntry(val menu: Menu, val inventory: Inventory?)

  private val registry = ConcurrentHashMap<UUID, RegisteredEntry>()

  fun register(playerIdentifier: UUID, menu: Menu, inventory: Inventory?) {
    this.registry[playerIdentifier] = RegisteredEntry(menu, inventory)
  }

  fun unregister(playerIdentifier: UUID) {
    this.registry.remove(playerIdentifier)
  }

  fun getMenu(playerIdentifier: UUID): Menu? = this.registry[playerIdentifier]?.menu

  fun getInventory(playerIdentifier: UUID): Inventory? = this.registry[playerIdentifier]?.inventory

  fun isMenu(playerIdentifier: UUID): Boolean = this.registry.containsKey(playerIdentifier)
}
