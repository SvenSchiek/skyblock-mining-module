/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.tool.ability.effect

import java.util.concurrent.ConcurrentHashMap

/**
 * Stores executable ability effect implementations by identifier.
 */
class AbilityEffectRegistry(effectList: List<AbilityEffect> = emptyList()) {

  private val effectMap = ConcurrentHashMap<String, AbilityEffect>()

  init {
    effectList.forEach(this::register)
  }

  fun register(effect: AbilityEffect) {
    this.effectMap[effect.identifier.lowercase()] = effect
  }

  fun unregister(identifier: String) {
    this.effectMap.remove(identifier.lowercase())
  }

  fun get(identifier: String): AbilityEffect? = this.effectMap[identifier.lowercase()]

  fun identifiers(): Set<String> = this.effectMap.keys.toSet()
}
