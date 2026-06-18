/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.mine

import org.bukkit.Location

/**
 * Represents a configured mine.
 *
 * @property identifier The unique mine identifier.
 * @property displayName The human-readable mine name.
 * @property enabled Whether the mine is active.
 * @property regionIdentifier The optional region-module identifier.
 * @property worldName The optional world name for cuboid fallback lookup.
 * @property minimum The optional minimum cuboid position.
 * @property maximum The optional maximum cuboid position.
 * @property requiredLevel The required mining level.
 * @property requiredPrestige The required mining prestige.
 * @property experienceMultiplier The mine experience multiplier.
 * @property damageMultiplier The mine damage multiplier.
 */
data class Mine(
  val identifier: String,
  val displayName: String,
  val enabled: Boolean,
  val regionIdentifier: String?,
  val worldName: String?,
  val minimum: Coordinate?,
  val maximum: Coordinate?,
  val requiredLevel: Int,
  val requiredPrestige: Int,
  val experienceMultiplier: Double,
  val damageMultiplier: Double
) {

  /**
   * Represents an integer coordinate.
   */
  data class Coordinate(val x: Int, val y: Int, val z: Int)

  /**
   * Checks whether a location lies inside the configured fallback cuboid.
   *
   * @param location The location to check.
   *
   * @return True when the location lies inside the cuboid.
   */
  fun contains(location: Location): Boolean {
    val minimum = this.minimum ?: return false
    val maximum = this.maximum ?: return false
    val worldName = this.worldName ?: return false

    if (location.world?.name != worldName) {
      return false
    }

    return location.blockX in minOf(minimum.x, maximum.x)..maxOf(minimum.x, maximum.x) &&
      location.blockY in minOf(minimum.y, maximum.y)..maxOf(minimum.y, maximum.y) &&
      location.blockZ in minOf(minimum.z, maximum.z)..maxOf(minimum.z, maximum.z)
  }
}
