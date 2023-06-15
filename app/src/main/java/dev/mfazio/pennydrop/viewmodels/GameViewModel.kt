package dev.mfazio.pennydrop.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.mfazio.pennydrop.game.GameHandler
import dev.mfazio.pennydrop.game.TurnResult
import dev.mfazio.pennydrop.types.Player
import dev.mfazio.pennydrop.types.Slot
import dev.mfazio.pennydrop.types.clear
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {
    private var players: List<Player> = emptyList()
    val slots = MutableLiveData((1..6).map { slotNumber -> Slot(slotNumber, slotNumber != 6) })

    val currentPlayer = MutableLiveData<Player?>()

    val canRoll = MutableLiveData(false)
    val canPass = MutableLiveData(false)

    val currentTurnText = MutableLiveData("")
    val currentStandingsText = MutableLiveData("")

    private var clearText = false

    fun startGame(playersForNewGame: List<Player>) {
        this.players = playersForNewGame
        this.currentPlayer.value = this.players.firstOrNull().apply {
            this?.isRolling = true
        }
        canRoll.value = true
        canPass.value = true

        slots.value?.clear()
        slots.notifyChange()

        currentTurnText.value = "The game has begun!\n"
        currentStandingsText.value = generateCurrentStandings(this.players)
    }

    fun roll() {
        slots.value?.let { currentSlots ->
            val currentPlayer = players.firstOrNull { it.isRolling }
            if (currentPlayer != null && canRoll.value == true) {
                updateFromGameHandler(GameHandler.roll(players, currentPlayer, currentSlots))
            }
        }
    }

    fun pass() {
        val currentPlayer = players.firstOrNull { it.isRolling }
        if (currentPlayer != null && canPass.value == true) {
            updateFromGameHandler(GameHandler.pass(players, currentPlayer))
        }
    }

    private fun updateFromGameHandler(result: TurnResult) {
        if (result.currentPlayer != null) {
            currentPlayer.value?.addPennies(result.coinChangeCount ?: 0)
            currentPlayer.value = result.currentPlayer
            this.players.forEach { player ->
                player.isRolling = result.currentPlayer == player
            }
        }

        if (result.lastRoll != null) {
            slots.value?.let { currentSlots ->
                updateSlots(result, currentSlots, result.lastRoll)
            }
        }
        currentTurnText.value = generateTurnText(result)
        currentStandingsText.value = generateCurrentStandings(this.players)

        canRoll.value = result.canRoll
        canPass.value = result.canPass

        if (!result.isGameOver && result.currentPlayer?.isHuman == false) {
            canRoll.value = false
            canPass.value = false
            playAITurn()
        }
    }

    private fun updateSlots(result: TurnResult, currentSlots: List<Slot>, lastRoll: Int) {
        if (result.clearSlots) {
            currentSlots.clear()
        }

        currentSlots.firstOrNull { it.lastRolled }?.apply { lastRolled = false }
        currentSlots.getOrNull(lastRoll - 1)?.also { slot ->
            if (!result.clearSlots && slot.canBeFilled) slot.isFilled = true
            slot.lastRolled = true
        }
        slots.notifyChange()
    }

    private fun generateTurnText(result: TurnResult): String {
        if (clearText) currentTurnText.value = ""
        clearText = result.turnEnd != null

        val currentText = currentTurnText.value ?: ""
        val currentPlayerName = result.currentPlayer?.playerName ?: "???"

        return when {
            result.isGameOver ->
                """
                    |Game Over!
                    |$currentPlayerName is the winner!
                    |
                    |${generateCurrentStandings(this.players, "Final Scores\n")}
                    }}
                """.trimIndent()

            result.turnEnd == TurnResult.TurnEnd.Bust -> "${result.previousPlayer?.playerName} rolled a ${result.lastRoll}. They collected ${result.coinChangeCount} pennies for a total of ${result.previousPlayer?.pennies}.\n$currentPlayerName"
            result.turnEnd == TurnResult.TurnEnd.Pass -> "${result.previousPlayer?.playerName} passed. They currently have ${result.previousPlayer?.pennies} pennies.\n$currentText"
            result.turnEnd == TurnResult.TurnEnd.Pass -> "${result.previousPlayer?.playerName} passed. They currently have ${result.previousPlayer?.pennies} pennies.\n$currentText"
            result.lastRoll != null -> "$currentText\n$currentPlayerName rolled a ${result.lastRoll}."
            else -> ""
        }
    }

    private fun playAITurn() {
        viewModelScope.launch {
            delay(1000)
            slots.value?.let { currentSlots ->
                val currentPlayer = players.firstOrNull { it.isRolling }
                if (currentPlayer != null && !currentPlayer.isHuman) {
                    GameHandler.playAITurn(
                        players,
                        currentPlayer,
                        currentSlots,
                        canPass.value == true
                    )?.let { result -> updateFromGameHandler(result) }
                }
            }
        }
    }
}

private fun <T> MutableLiveData<List<T>>.notifyChange() {
    this.value = this.value
}

private fun generateCurrentStandings(
    players: List<Player>,
    headerText: String = "Current Standings"
) = players.sortedBy { it.pennies }.joinToString(
    separator = "\n", prefix = "$headerText\n"
) { "\t${it.playerName} - ${it.pennies} pennies" }