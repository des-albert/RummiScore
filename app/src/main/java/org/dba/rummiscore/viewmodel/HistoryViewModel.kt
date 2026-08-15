package org.dba.rummiscore.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.dba.rummiscore.data.entity.Match
import org.dba.rummiscore.data.entity.Player
import org.dba.rummiscore.data.repository.MatchSummary
import org.dba.rummiscore.data.repository.ScoreRepository
import javax.inject.Inject

data class HistoryUiState(
    val players: List<Player> = emptyList(),
    val matchHistory: List<MatchSummary> = emptyList()
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    repository: ScoreRepository
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.players,
        repository.getMatchHistory()
    ) { players, matchHistory ->
        HistoryUiState(
            players = players.sortedByDescending { it.totalPoints },
            matchHistory = matchHistory
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState()
    )
}