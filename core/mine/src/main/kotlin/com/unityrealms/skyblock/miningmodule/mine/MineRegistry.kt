/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.mine

import java.util.concurrent.ConcurrentHashMap

import org.bukkit.Location

/** Registry and resolver for configured mines. */
class MineRegistry(
  mineList: List<Mine>,
  private val regionModuleBridge: RegionModuleBridge
) {

  private val mineMap = ConcurrentHashMap<String, Mine>()

  init {
    this.replace(mineList)
  }

  val size: Int
    get() = this.mineMap.size

  fun replace(mineList: List<Mine>) {
    this.mineMap.clear()

    for (mine in mineList) {
      this.mineMap[mine.identifier.lowercase()] = mine
    }
  }

  fun get(identifier: String): Mine? = this.mineMap[identifier.lowercase()]

  fun getAll(): List<Mine> = this.mineMap.values.sortedBy { mine -> mine.requiredLevel }

  /** Resolves the active mine at a location. */
  fun resolve(location: Location): Mine? {
    val regionIdentifier = this.regionModuleBridge.resolveRegionIdentifier(location)

    if (regionIdentifier != null) {
      this.mineMap.values.firstOrNull { mine ->
        mine.enabled && mine.regionIdentifier == regionIdentifier
      }?.let {
        return it
      }
    }

    return this.mineMap.values.firstOrNull { mine ->
      mine.enabled && mine.contains(location)
    }
  }
}
