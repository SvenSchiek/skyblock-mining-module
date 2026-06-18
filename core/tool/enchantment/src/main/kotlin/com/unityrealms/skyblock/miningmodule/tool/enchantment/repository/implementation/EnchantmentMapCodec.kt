/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.tool.enchantment.repository.implementation

import com.unityrealms.skyblock.miningmodule.tool.FragmentRarity

/**
 * Encodes invested fragment maps for JDBC text columns.
 */
internal object EnchantmentMapCodec {

  fun encode(valueMap: Map<FragmentRarity, Int>): String = valueMap.entries
    .sortedBy { it.key.name }
    .joinToString(";") { (rarity, value) -> "${rarity.name}=$value" }

  fun decode(rawValue: String?): Map<FragmentRarity, Int> {
    if (rawValue.isNullOrBlank()) return emptyMap()

    return rawValue.split(';').mapNotNull { entry ->
      val split = entry.split('=', limit = 2)
      if (split.size != 2) return@mapNotNull null
      val rarity = runCatching { FragmentRarity.valueOf(split[0]) }.getOrNull() ?: return@mapNotNull null
      val value = split[1].toIntOrNull() ?: return@mapNotNull null
      rarity to value
    }.toMap()
  }
}
