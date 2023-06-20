package dev.mfazio.pennydrop.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import dev.mfazio.pennydrop.types.Player
import java.time.OffsetDateTime

@Dao
abstract class PennyDropDao {
    @Query("SELECT * FROM players WHERE playerName = :playerName")
    abstract fun getPlayer(playerName: String): Player?

    @Insert
    abstract suspend fun insertGame(game: Game): Long

    @Insert
    abstract suspend fun insertPlayer(player: Player): Long

    @Insert
    abstract suspend fun insertPlayers(players: List<Player>): List<Long>

    @Update
    abstract suspend fun updateGame(game: Game)

    @Transaction
    @Query("SELECT * FROM games ORDER BY startTime DESC LIMIT 2")
    abstract fun getCurrentGameWithPlayers(): LiveData<GameWithPlayers>

    @Transaction
    @Query(
        """
        SELECT * FROM game_statuses
        WHERE gameId = (
            SELECT gameId FROM games
            WHERE endTime IS NULL
            ORDER BY startTime DESC
            LIMIT 1)
        ORDER BY gamePlayerNumber
        """
    )
    abstract fun getCurrentGameStatuses(): LiveData<List<GameStatus>>

    @Query(
        """
        UPDATE games
        SET endTime = :endDate, gameState = :gameState
        WHERE endTime IS NULL
        """
    )
    abstract suspend fun closeOpenGames(
        endDate: OffsetDateTime = OffsetDateTime.now(),
        gameState: GameState = GameState.Cancelled
    )

    @Insert
    abstract suspend fun insertGameStatuses(gameStatuses: List<GameStatus>)

    @Transaction
    open suspend fun startGame(players: List<Player>): Long {
        this.closeOpenGames()

        val gameId = this.insertGame(
            Game(
                gameState = GameState.Started,
                currentTurnText = "The game has begun!\n",
                canRoll = true
            )
        )
        val playerIds =
            players.map { player -> getPlayer(player.playerName)?.playerId ?: insertPlayer(player) }
        this.insertGameStatuses(
            playerIds.mapIndexed { index, playerId ->
                GameStatus(
                    gameId, playerId, index, index == 0
                )
            }
        )

        return gameId
    }

    @Update
    abstract suspend fun updateGameStatuses(gameStatuses: List<GameStatus>)

    @Transaction
    open suspend fun updateGameAndStatuses(game: Game, statuses: List<GameStatus>) {
        this.updateGame(game)
        this.updateGameStatuses(statuses)
    }
}