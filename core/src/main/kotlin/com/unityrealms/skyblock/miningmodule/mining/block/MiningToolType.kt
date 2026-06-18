/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.mining.block

import org.bukkit.Material

/**
 * Represents the tool category required to mine a configured block.
 */
enum class MiningToolType {
  ANY,
  PICKAXE,
  AXE,
  SHOVEL,
  HOE;

  /**
   * Checks whether a material belongs to this tool category.
   *
   * @param material The material to check.
   *
   * @return True if the material is accepted, false otherwise.
   */
  fun matches(material: Material): Boolean = when (this) {
    ANY -> true
    PICKAXE -> material.name.endsWith("_PICKAXE")
    AXE -> material.name.endsWith("_AXE")
    SHOVEL -> material.name.endsWith("_SHOVEL")
    HOE -> material.name.endsWith("_HOE")
  }
}
