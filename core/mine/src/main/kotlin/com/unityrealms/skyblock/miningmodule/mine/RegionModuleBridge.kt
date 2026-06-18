/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.mine

import org.bukkit.Bukkit
import org.bukkit.Location

/**
 * Resolves region identifiers through the optional region-module Bukkit service
 * without introducing a compile-time dependency on the region module.
 */
class RegionModuleBridge {

  companion object {
    private const val REGION_API_CLASS = "com.unityrealms.skyblock.regionmodule.api.RegionModuleApi"
  }

  @Volatile
  private var provider: Any? = null

  /** Resolves the region identifier at a location. */
  fun resolveRegionIdentifier(location: Location): String? {
    val provider = this.provider ?: this.resolveProvider()?.also {
      this.provider = it
    } ?: return null

    return try {
      val getRegionAtMethod = provider.javaClass.methods.firstOrNull { method ->
        method.name == "getRegionAt" && method.parameterCount == 1
      } ?: return null
      val region = getRegionAtMethod.invoke(provider, location) ?: return null
      val getIdentifierMethod = region.javaClass.methods.firstOrNull { method ->
        method.name == "getIdentifier" && method.parameterCount == 0
      } ?: return null

      getIdentifierMethod.invoke(region)?.toString()?.lowercase()
    } catch (_: Exception) {
      this.provider = null
      null
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun resolveProvider(): Any? {
    return try {
      val apiClass = Class.forName(REGION_API_CLASS) as Class<Any>
      Bukkit.getServicesManager().getRegistration(apiClass)?.provider
    } catch (_: Exception) {
      null
    }
  }
}
