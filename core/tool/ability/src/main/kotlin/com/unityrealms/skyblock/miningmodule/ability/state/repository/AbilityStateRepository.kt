package com.unityrealms.skyblock.miningmodule.ability.state.repository

import com.unityrealms.skyblock.miningmodule.ability.state.AbilityState
import java.util.UUID

/**
 * Represents a repository for ability states.
 */
interface AbilityStateRepository {

  /**
   * Saves the given ability state.
   *
   * @param abilityState The ability state to save.
   */
  fun save(abilityState: AbilityState)

  /**
   * Loads the ability state with the specified tool and ability identifiers.
   *
   * @param toolIdentifier The unique identifier of the tool for which the ability state is being loaded.
   * @param abilityIdentifier The unique identifier of the ability for which the state is being loaded.
   *
   * @return The ability state with the specified tool and ability identifiers, or null if no state is found with those identifiers.
   */
  fun load(toolIdentifier: UUID, abilityIdentifier: String): AbilityState?

  /**
   * Loads all ability states for the specified tool identifier.
   *
   * @param toolIdentifier The unique identifier of the tool for which the ability states are being loaded.
   *
   * @return A list of all ability states for the specified tool identifier, or an empty list if no states are found for that identifier.
   */
  fun loadAll(toolIdentifier: UUID): List<AbilityState>


  /**
   * Deletes the ability state with the specified tool and ability identifiers.
   *
   * @param toolIdentifier The unique identifier of the tool for which the ability state is being deleted.
   */
  fun deleteAll(toolIdentifier: UUID)
}
