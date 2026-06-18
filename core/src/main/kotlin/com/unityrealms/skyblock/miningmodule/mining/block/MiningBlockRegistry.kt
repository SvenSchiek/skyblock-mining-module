/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.mining.block

import java.util.concurrent.ConcurrentHashMap

import org.bukkit.Material

/** Registry for configured mining blocks. */
class MiningBlockRegistry(blockList: List<MiningBlock>) {

  private val identifierMap = ConcurrentHashMap<String, MiningBlock>()

  private val materialMap = ConcurrentHashMap<Material, MiningBlock>()

  init {
    this.replace(blockList)
  }

  val size: Int
    get() = this.identifierMap.size

  /** Replaces all configured mining blocks. */
  fun replace(blockList: List<MiningBlock>) {
    this.identifierMap.clear()
    this.materialMap.clear()

    for (miningBlock in blockList) {
      this.identifierMap[miningBlock.identifier.lowercase()] = miningBlock
      this.materialMap[miningBlock.material] = miningBlock
    }
  }

  fun get(identifier: String): MiningBlock? = this.identifierMap[identifier.lowercase()]

  fun get(material: Material): MiningBlock? = this.materialMap[material]

  fun getAll(): List<MiningBlock> = this.identifierMap.values.sortedBy { miningBlock -> miningBlock.requiredLevel }
}
