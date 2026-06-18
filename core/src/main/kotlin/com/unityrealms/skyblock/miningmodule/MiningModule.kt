/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule

import com.unityrealms.skyblock.miningmodule.api.MiningModuleApi
import com.unityrealms.skyblock.miningmodule.mining.MiningCore

import org.bukkit.event.HandlerList
import org.bukkit.plugin.ServicePriority
import org.bukkit.plugin.java.JavaPlugin

/**
 * Represents the main class of the plugin.
 */
class MiningModule : JavaPlugin() {

  companion object {
    lateinit var miningModuleApi: MiningModuleApi
  }

  /**
   * Called when the plugin is enabled.
   */
  override fun onEnable() {
    if (super.dataFolder.mkdirs()) {
      super.logger.info("Successfully created the data folder.")
    }

    MiningCore.startup(this)

    super.server.servicesManager.register(MiningModuleApi::class.java, miningModuleApi, this, ServicePriority.Normal)
  }

  /**
   * Called when the plugin is disabled.
   */
  override fun onDisable() {
    HandlerList.unregisterAll(this)

    MiningCore.shutdown()

    this.server.servicesManager.unregisterAll(this)
  }
}
