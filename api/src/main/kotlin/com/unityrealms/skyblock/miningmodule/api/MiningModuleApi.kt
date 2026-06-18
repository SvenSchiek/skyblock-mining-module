/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.api

import java.util.UUID

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Represents the public API for the mining module.
 */
@Suppress("unused")
interface MiningModuleApi {

  /**
   * Gets the mining profile for the specified player.
   *
   * @param playerIdentifier The unique identifier of the player whose mining profile is being retrieved.
   *
   * @return The mining profile view of the specified player, or null if no profile exists for the player.
   */
  fun getMiningProfile(playerIdentifier: UUID): MiningProfileView?

  /**
   * Adds experience to the mining profile of a player.
   *
   * @param playerIdentifier The unique identifier of the player whose mining profile is being updated.
   * @param experience The amount of experience to add to the mining profile.
   */
  fun addExperience(playerIdentifier: UUID, experience: Long)

  /**
   * Checks if a player can prestige based on their mining profile.
   *
   * @param playerIdentifier The unique identifier of the player to check.
   *
   * @return True if the player can prestige, false otherwise.
   */
  fun canPrestige(playerIdentifier: UUID): Boolean

  /**
   * Checks if a material is configured as a mining block.
   *
   * @param material The material to check.
   *
   * @return True if the material is a mining block, false otherwise.
   */
  fun isMiningBlock(material: Material): Boolean

  /**
   * Checks if a mine exists at the specified location.
   *
   * @param location The location to check.
   *
   * @return True if a mine exists at the location, false otherwise.
   */
  fun hasMine(location: Location): Boolean

  /**
   * Gets the tool profile from an item stack.
   *
   * @param itemStack The item stack to resolve.
   *
   * @return The tool profile view, or null if the item is not a mining tool.
   */
  fun getToolProfile(itemStack: ItemStack): ToolProfileView?

  /**
   * Creates a mining tool for the specified player.
   *
   * @param player The player receiving the mining tool.
   *
   * @return The created mining tool item stack.
   */
  fun createTool(player: Player): ItemStack
}
