package com.unityrealms.skyblock.miningmodule.ability.state.repository.implementation

import com.unityrealms.skyblock.miningmodule.ability.state.AbilityState
import com.unityrealms.skyblock.miningmodule.ability.state.repository.AbilityStateRepository

import java.util.UUID
import javax.sql.DataSource

/**
 * Represents a JDBC implementation of the ability state repository.
 *
 * @property dataSource The data source to use for database connections.
 */
class JdbcAbilityStateRepository(private val dataSource: DataSource) : AbilityStateRepository {

  /**
   * Saves the given ability state.
   *
   * @param abilityState The ability state to save.
   */
  override fun save(abilityState: AbilityState) {
    this.dataSource.connection.use { connection ->
      connection.prepareStatement(
        "UPDATE mining_tool_ability_state_data SET unlocked = ?, level = ?, cooldown_expired_at = ?, updated_at = ? WHERE tool_identifier = ? AND ability_identifier = ?"
      ).use { preparedStatement ->
        preparedStatement.setBoolean(1, abilityState.unlocked)
        preparedStatement.setInt(2, abilityState.level)
        preparedStatement.setObject(3, abilityState.cooldownExpiredAt)
        preparedStatement.setLong(4, abilityState.updatedAt)
        preparedStatement.setString(5, abilityState.toolIdentifier.toString())
        preparedStatement.setString(6, abilityState.abilityIdentifier)

        if (preparedStatement.executeUpdate() == 0) {
          connection.prepareStatement(
            "INSERT INTO mining_tool_ability_state_data(tool_identifier, ability_identifier, unlocked, level, cooldown_ends_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)"
          ).use { insertPreparedStatement ->
            insertPreparedStatement.setString(1, abilityState.toolIdentifier.toString())
            insertPreparedStatement.setString(2, abilityState.abilityIdentifier)
            insertPreparedStatement.setBoolean(3, abilityState.unlocked)
            insertPreparedStatement.setInt(4, abilityState.level)
            insertPreparedStatement.setObject(5, abilityState.cooldownExpiredAt)
            insertPreparedStatement.setLong(6, abilityState.updatedAt)

            insertPreparedStatement.executeUpdate()
          }
        }
      }
    }
  }

  /**
   * Loads the ability state with the specified tool and ability identifiers.
   *
   * @param toolIdentifier The unique identifier of the tool for which the ability state is being loaded.
   * @param abilityIdentifier The unique identifier of the ability for which the state is being loaded.
   *
   * @return The ability state with the specified tool and ability identifiers, or null if no state is found with those identifiers.
   */
  override fun load(toolIdentifier: UUID, abilityIdentifier: String): AbilityState? {
    this.dataSource.connection.use { connection ->
      connection.prepareStatement("SELECT unlocked, level, cooldown_expired_at, updated_at FROM mining_tool_ability_state_data WHERE tool_identifier = ? AND ability_identifier = ?").use { preparedStatement ->
        preparedStatement.setString(1, toolIdentifier.toString())
        preparedStatement.setString(2, abilityIdentifier.lowercase())

        preparedStatement.executeQuery().use { resultSet ->
          if (!(resultSet.next())) {
            return null
          }

          val cooldownExpiredAt = resultSet.getLong("cooldown_expired_at").let {
            if (resultSet.wasNull()) {
              null
            } else {
              it
            }
          }

          return AbilityState(toolIdentifier, abilityIdentifier.lowercase(), resultSet.getBoolean("unlocked"), resultSet.getInt("level"), cooldownExpiredAt, resultSet.getLong("updated_at"))
        }
      }
    }
  }

  /**
   * Loads all ability states for the specified tool identifier.
   *
   * @param toolIdentifier The unique identifier of the tool for which the ability states are being loaded.
   *
   * @return A list of all ability states for the specified tool identifier, or an empty list if no states are found for that identifier.
   */
  override fun loadAll(toolIdentifier: UUID): List<AbilityState> {
    this.dataSource.connection.use { connection ->
      connection.prepareStatement("SELECT ability_identifier, unlocked, level, cooldown_expired_at, updated_at FROM mining_tool_ability_state_data WHERE tool_identifier = ?").use { preparedStatement ->
        preparedStatement.setString(1, toolIdentifier.toString())

        preparedStatement.executeQuery().use { resultSet ->
          val abilityStateList = mutableListOf<AbilityState>()

          while (resultSet.next()) {
            val cooldownExpiredAt = resultSet.getLong("cooldown_expired_at").let {
              if (resultSet.wasNull()) {
                null
              } else {
                it
              }
            }

            abilityStateList.add(AbilityState(toolIdentifier, resultSet.getString("ability_identifier"), resultSet.getBoolean("unlocked"), resultSet.getInt("level"), cooldownExpiredAt, resultSet.getLong("updated_at")))
          }

          return abilityStateList
        }
      }
    }
  }


  /**
   * Deletes the ability state with the specified tool and ability identifiers.
   *
   * @param toolIdentifier The unique identifier of the tool for which the ability state is being deleted.
   */
  override fun deleteAll(toolIdentifier: UUID) {
    this.dataSource.connection.use { connection ->
      connection.prepareStatement("DELETE FROM mining_tool_ability_state_data WHERE tool_identifier = ?").use { preparedStatement ->
        preparedStatement.setString(1, toolIdentifier.toString())

        preparedStatement.executeUpdate()
      }
    }
  }
}
