package org.dba.rummiscore.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.dba.rummiscore.data.entity.Match
import org.dba.rummiscore.data.entity.Player
import org.dba.rummiscore.data.entity.Round
import org.dba.rummiscore.data.repository.PlayerWithMatchScore
import org.dba.rummiscore.data.repository.ScoreRepository
import javax.inject.Inject

data class MatchUiState(
    val players: List<Player> = emptyList(),
    val activeMatch: Match? = null,
    val scores: List<PlayerWithMatchScore> = emptyList(),
    val rounds: List<Round> = emptyList(),
    val selectedWinnerId: Long? = null,
    val loser1Points: String = "",
    val loser2Points: String = "",
    val message: String? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class MatchViewModel @Inject constructor(
    private val repository: ScoreRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MatchUiState())
    val state: StateFlow<MatchUiState> = _uiState.asStateFlow()

    private var scoresJob: Job? = null
    private var roundsJob: Job? = null
    private var currentMatchId: Long? = null

    init {
        viewModelScope.launch {
            repository.ensureDefaultPlayers()
        }
        viewModelScope.launch {
            repository.players.collect { players ->
                _uiState.update { it.copy(players = players) }
                // Re-map scores if we already have raw scores for a match
                remapScores(players)
            }
        }
        viewModelScope.launch {
            repository.activeMatch.collect { match ->
                _uiState.update { it.copy(activeMatch = match) }
                if (match != null) {
                    observeMatchDetails(match.id)
                } else {
                    currentMatchId = null
                    scoresJob?.cancel()
                    roundsJob?.cancel()
                    _uiState.update { it.copy(scores = emptyList(), rounds = emptyList()) }
                }
            }
        }
    }

    private fun observeMatchDetails(matchId: Long) {
        if (currentMatchId == matchId) return
        currentMatchId = matchId
        scoresJob?.cancel()
        roundsJob?.cancel()

        scoresJob = viewModelScope.launch {
            repository.getScoresForMatch(matchId).collect { scores ->
                val players = _uiState.value.players
                val mapped = scores.mapNotNull { s ->
                    players.find { it.id == s.playerId }?.let { p ->
                        PlayerWithMatchScore(p, s.points, s.wins)
                    }
                }.sortedBy { it.player.id }
                _uiState.update { it.copy(scores = mapped) }
            }
        }
        roundsJob = viewModelScope.launch {
            repository.getRoundsForMatch(matchId).collect { rounds ->
                _uiState.update { it.copy(rounds = rounds) }
            }
        }
    }

    private fun remapScores(players: List<Player>) {
        val current = _uiState.value.scores
        if (current.isEmpty()) return
        val mapped = current.mapNotNull { sc ->
            players.find { it.id == sc.player.id }?.let { p ->
                sc.copy(player = p)
            }
        }.sortedBy { it.player.id }
        _uiState.update { it.copy(scores = mapped) }
    }

    fun startNewMatch() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            repository.startNewMatch()
            // Reset selection when a new match starts; activeMatch flow will refresh scores/rounds
            currentMatchId = null
            _uiState.update {
                it.copy(
                    selectedWinnerId = null,
                    loser1Points = "",
                    loser2Points = "",
                    isLoading = false,
                    message = "New match started"
                )
            }
        }
    }

    fun selectWinner(playerId: Long) {
        _uiState.update {
            it.copy(selectedWinnerId = playerId, message = null)
        }
    }

    fun setLoser1Points(value: String) {
        if (value.isEmpty() || value.all { it.isDigit() }) {
            _uiState.update { it.copy(loser1Points = value) }
        }
    }

    fun setLoser2Points(value: String) {
        if (value.isEmpty() || value.all { it.isDigit() }) {
            _uiState.update { it.copy(loser2Points = value) }
        }
    }

    fun submitRound() {
        val snapshot = _uiState.value
        val match = snapshot.activeMatch ?: run {
            _uiState.update { it.copy(message = "Start a match first") }
            return
        }
        val winnerId = snapshot.selectedWinnerId ?: run {
            _uiState.update { it.copy(message = "Select a winner") }
            return
        }
        val losers = snapshot.players.filter { it.id != winnerId }
        if (losers.size != 2) {
            _uiState.update { it.copy(message = "Need exactly 3 players") }
            return
        }
        val l1 = snapshot.loser1Points.toIntOrNull()
        val l2 = snapshot.loser2Points.toIntOrNull()
        if (l1 == null || l2 == null || l1 < 0 || l2 < 0) {
            _uiState.update { it.copy(message = "Enter valid points for both losers") }
            return
        }
        if (match.roundsPlayed >= 6) {
            _uiState.update { it.copy(message = "Match already has 6 rounds") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.recordRound(
                matchId = match.id,
                winnerId = winnerId,
                loser1Id = losers[0].id,
                loser1Points = l1,
                loser2Id = losers[1].id,
                loser2Points = l2
            )
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        selectedWinnerId = null,
                        loser1Points = "",
                        loser2Points = "",
                        message = "Round ${match.roundsPlayed + 1} recorded",
                        isLoading = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        message = result.exceptionOrNull()?.message ?: "Error",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun undoLastRound() {
        val match = _uiState.value.activeMatch ?: run {
            _uiState.update { it.copy(message = "No active match") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.undoLastRound(match.id)
            _uiState.update {
                it.copy(
                    message = if (result.isSuccess) "Last round undone"
                    else result.exceptionOrNull()?.message,
                    isLoading = false
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun updatePlayerName(playerId: Long, name: String) {
        viewModelScope.launch {
            repository.updatePlayerName(playerId, name)
        }
    }
}