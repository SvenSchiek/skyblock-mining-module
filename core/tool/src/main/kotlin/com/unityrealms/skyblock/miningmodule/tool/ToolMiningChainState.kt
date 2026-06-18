/*
 * Copyright (c) 2026 Unity Realms
 *
 * This file is part of the project and is covered by the license of this project.
 * See the top-level LICENSE file for full terms and conditions.
 *
 * SPDX-License-Identifier: MIT
 */
package com.unityrealms.skyblock.miningmodule.tool

import java.util.concurrent.atomic.AtomicInteger

/**
 * Tracks safety state shared by one mining activation chain.
 */
class ToolMiningChainState(
  val maximumAffectedBlocks: Int,
  val maximumRecursionDepth: Int
) {

  private val affectedBlockCount = AtomicInteger(0)
  private val executedEffectIdentifierSet = linkedSetOf<String>()

  /** Gets the current number of affected blocks. */
  fun getAffectedBlockCount(): Int = this.affectedBlockCount.get()

  /** Attempts to reserve one affected block. */
  fun tryReserveBlock(): Boolean {
    while (true) {
      val current = this.affectedBlockCount.get()

      if (current >= this.maximumAffectedBlocks) {
        return false
      }

      if (this.affectedBlockCount.compareAndSet(current, current + 1)) {
        return true
      }
    }
  }

  /** Registers an effect once for this activation chain. */
  @Synchronized
  fun registerEffect(identifier: String): Boolean = this.executedEffectIdentifierSet.add(identifier.lowercase())
}
