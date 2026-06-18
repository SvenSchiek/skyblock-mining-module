/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.tool.repository.implementation

import com.unityrealms.skyblock.miningmodule.tool.FragmentRarity

/**
 * Encodes simple maps for JDBC text columns without external serialization dependencies.
 */
internal object ToolMapCodec {

  fun encodeStringLongMap(valueMap: Map<String, Long>): String = valueMap.entries
    .sortedBy { it.key }
    .joinToString(";") { (key, value) -> "${this.escape(key)}=$value" }

  fun decodeStringLongMap(rawValue: String?): Map<String, Long> {
    if (rawValue.isNullOrBlank()) {
      return emptyMap()
    }

    return rawValue.split(';').mapNotNull { entry ->
      val separatorIndex = entry.lastIndexOf('=')

      if (separatorIndex <= 0) {
        return@mapNotNull null
      }

      val key = this.unescape(entry.substring(0, separatorIndex))
      val value = entry.substring(separatorIndex + 1).toLongOrNull() ?: return@mapNotNull null
      key to value
    }.toMap()
  }

  fun encodeRarityIntMap(valueMap: Map<FragmentRarity, Int>): String = valueMap.entries
    .sortedBy { it.key.name }
    .joinToString(";") { (key, value) -> "${key.name}=$value" }

  fun decodeRarityIntMap(rawValue: String?): Map<FragmentRarity, Int> {
    if (rawValue.isNullOrBlank()) {
      return emptyMap()
    }

    return rawValue.split(';').mapNotNull { entry ->
      val split = entry.split('=', limit = 2)

      if (split.size != 2) {
        return@mapNotNull null
      }

      val rarity = runCatching { FragmentRarity.valueOf(split[0]) }.getOrNull() ?: return@mapNotNull null
      val value = split[1].toIntOrNull() ?: return@mapNotNull null
      rarity to value
    }.toMap()
  }

  fun encodeRarityLongMap(valueMap: Map<FragmentRarity, Long>): String = valueMap.entries
    .sortedBy { it.key.name }
    .joinToString(";") { (key, value) -> "${key.name}=$value" }

  fun decodeRarityLongMap(rawValue: String?): Map<FragmentRarity, Long> {
    if (rawValue.isNullOrBlank()) {
      return emptyMap()
    }

    return rawValue.split(';').mapNotNull { entry ->
      val split = entry.split('=', limit = 2)

      if (split.size != 2) {
        return@mapNotNull null
      }

      val rarity = runCatching { FragmentRarity.valueOf(split[0]) }.getOrNull() ?: return@mapNotNull null
      val value = split[1].toLongOrNull() ?: return@mapNotNull null
      rarity to value
    }.toMap()
  }

  private fun escape(value: String): String = value.replace("%", "%25").replace(";", "%3B").replace("=", "%3D")

  private fun unescape(value: String): String = value.replace("%3D", "=").replace("%3B", ";").replace("%25", "%")
}
