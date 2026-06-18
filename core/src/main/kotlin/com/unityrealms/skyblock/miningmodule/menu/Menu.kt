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

import net.kyori.adventure.text.Component

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack

/** Represents a menu. */
abstract class Menu(protected var inventory: Inventory) {

  /** Represents the underlying menu inventory. */
  class Inventory(titleComponent: Component, private val slotCount: Int) {

    /** Represents the inventory size. */
    data class Size(val slotCount: Int)

    private val inventoryContentArray: Array<ItemStack?> = arrayOfNulls(this.slotCount)

    private val openInventoryMap = mutableMapOf<UUID, org.bukkit.inventory.Inventory>()

    val size: Size
      get() = Size(this.slotCount)

    var title: Component = titleComponent
      set(value) {
        field = value

        for ((playerIdentifier, inventory) in this.openInventoryMap.toMap()) {
          val player = Bukkit.getPlayer(playerIdentifier)

          if (player == null || player.openInventory.topInventory !== inventory) {
            this.openInventoryMap.remove(playerIdentifier)
            continue
          }

          val newInventory = Bukkit.createInventory(null, this.slotCount, value)

          for (index in this.inventoryContentArray.indices) {
            newInventory.setItem(index, this.inventoryContentArray[index])
          }

          val registeredMenu = MenuRegistry.getMenu(playerIdentifier)

          if (registeredMenu != null) {
            MenuRegistry.register(playerIdentifier, registeredMenu, newInventory)
          }

          player.openInventory(newInventory)
          this.openInventoryMap[playerIdentifier] = newInventory
        }
      }

    fun removeViewer(player: Player) {
      this.openInventoryMap.remove(player.uniqueId)
    }

    fun setContentArray(itemStack: ItemStack?, slot: Int) {
      if (slot < 0 || slot >= this.slotCount) {
        return
      }

      this.inventoryContentArray[slot] = itemStack

      for ((_, inventory) in this.openInventoryMap) {
        inventory.setItem(slot, itemStack)
      }
    }

    fun clear() {
      for (slot in this.inventoryContentArray.indices) {
        this.setContentArray(null, slot)
      }
    }

    fun openAndFetchBukkitInventory(player: Player): org.bukkit.inventory.Inventory? {
      val inventory = Bukkit.createInventory(null, this.slotCount, this.title)

      for (index in this.inventoryContentArray.indices) {
        inventory.setItem(index, this.inventoryContentArray[index])
      }

      player.openInventory(inventory)
      this.openInventoryMap[player.uniqueId] = inventory

      return try {
        player.openInventory.topInventory
      } catch (_: Exception) {
        null
      }
    }
  }

  fun open(player: Player) {
    val inventory = this.inventory.openAndFetchBukkitInventory(player)
    MenuRegistry.register(player.uniqueId, this, inventory)
    this.onOpen(player)
  }

  fun close(player: Player) {
    this.inventory.removeViewer(player)
    this.onClose(player)
  }

  abstract fun onOpen(player: Player)

  abstract fun onClose(player: Player)

  abstract fun onClick(clickType: ClickType, slot: Int, player: Player)
}
