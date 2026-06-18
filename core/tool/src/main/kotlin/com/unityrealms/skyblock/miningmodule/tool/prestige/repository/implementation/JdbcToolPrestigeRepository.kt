package com.unityrealms.skyblock.miningmodule.tool.prestige.repository.implementation

import com.unityrealms.skyblock.miningmodule.tool.prestige.repository.ToolPrestigeRepository

import java.util.UUID
import javax.sql.DataSource

/**
 * Represents a JDBC implementation of the tool prestige repository.
 *
 * @property dataSource The data source to use for database connections.
 */
class JdbcToolPrestigeRepository(private val dataSource: DataSource) : ToolPrestigeRepository {

  /**
   * Saves a tool prestige entry with the given information.
   *
   * @param toolIdentifier The unique identifier of the tool for which the prestige entry is being saved.
   * @param previousPrestige The previous prestige level of the tool before the change.
   * @param newPrestige The new prestige level of the tool after the change.
   * @param timestamp The timestamp when the prestige change occurred, represented as milliseconds since the epoch
   */
  override fun save(toolIdentifier: UUID, previousPrestige: Int, newPrestige: Int, timestamp: Long) {
    this.dataSource.connection.use { connection ->
      connection.prepareStatement(
        "INSERT INTO mining_tool_prestige_history_data(tool_identifier, previous_prestige, new_prestige, prestiged_at) VALUES (?, ?, ?, ?)"
      ).use { preparedStatement ->
        preparedStatement.setString(1, toolIdentifier.toString())
        preparedStatement.setInt(2, previousPrestige)
        preparedStatement.setInt(3, newPrestige)
        preparedStatement.setLong(4, timestamp)

        preparedStatement.executeUpdate()
      }
    }
  }
}
