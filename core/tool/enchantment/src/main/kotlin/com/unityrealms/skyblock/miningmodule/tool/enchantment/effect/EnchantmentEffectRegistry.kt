/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.tool.enchantment.effect

import java.util.concurrent.ConcurrentHashMap

/**
 * Stores enchantment effect implementations by their configuration identifier.
 */
class EnchantmentEffectRegistry(effectList: List<EnchantmentEffect> = emptyList()) {

  private val effectMap = ConcurrentHashMap<String, EnchantmentEffect>()

  init {
    effectList.forEach(this::register)
  }

  fun register(effect: EnchantmentEffect) {
    this.effectMap[effect.identifier.lowercase()] = effect
  }

  fun unregister(identifier: String) {
    this.effectMap.remove(identifier.lowercase())
  }

  fun get(identifier: String): EnchantmentEffect? = this.effectMap[identifier.lowercase()]

  fun identifiers(): Set<String> = this.effectMap.keys.toSet()
}
