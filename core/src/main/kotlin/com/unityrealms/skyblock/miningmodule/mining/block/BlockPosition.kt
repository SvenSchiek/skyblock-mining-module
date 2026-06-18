/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.mining.block

import java.util.UUID

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Block

/**
 * Represents an immutable block position.
 *
 * @property worldIdentifier The identifier of the world.
 * @property worldName The fallback name of the world.
 * @property x The block X coordinate.
 * @property y The block Y coordinate.
 * @property z The block Z coordinate.
 */
data class BlockPosition(
  val worldIdentifier: UUID?,
  val worldName: String,
  val x: Int,
  val y: Int,
  val z: Int
) {

  companion object {

    /**
     * Creates a block position from a block.
     *
     * @param block The source block.
     *
     * @return The block position.
     */
    fun fromBlock(block: Block): BlockPosition = BlockPosition(
      worldIdentifier = block.world.uid,
      worldName = block.world.name,
      x = block.x,
      y = block.y,
      z = block.z
    )
  }

  /**
   * Resolves the world of this block position.
   *
   * @return The resolved world, or null if it is unavailable.
   */
  fun resolveWorld(): World? = this.worldIdentifier?.let(Bukkit::getWorld) ?: Bukkit.getWorld(this.worldName)

  /**
   * Resolves this position as a Bukkit location.
   *
   * @return The location, or null if the world is unavailable.
   */
  fun toLocation(): Location? = this.resolveWorld()?.let { world ->
    Location(world, this.x.toDouble(), this.y.toDouble(), this.z.toDouble())
  }

  /**
   * Resolves the block at this position.
   *
   * @return The block, or null if the world is unavailable.
   */
  fun toBlock(): Block? = this.resolveWorld()?.getBlockAt(this.x, this.y, this.z)
}
