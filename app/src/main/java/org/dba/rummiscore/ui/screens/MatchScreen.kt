package org.dba.rummiscore.ui.screens


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.dba.rummiscore.ui.components.PlayerScoreCard
import org.dba.rummiscore.viewmodel.MatchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchScreen(
    viewModel: MatchViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Rummikub Match") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header / New match
            val match = state.activeMatch
            val canScore = match != null && !match.isFinished && match.roundsPlayed < 6

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        match == null -> "No active match"
                        match.isFinished -> "Match finished (${match.roundsPlayed}/6)"
                        else -> "Round ${match.roundsPlayed + 1} of 6"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Button(onClick = { viewModel.startNewMatch() }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("New Match")
                }
            }

            Spacer(Modifier.height(16.dp))

            // Player score cards – click to select winner
            Text(
                text = "Tap a player to select as winner",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val scoreMap = state.scores.associateBy { it.player.id }
                state.players.forEach { player ->
                    val sc = scoreMap[player.id]
                    PlayerScoreCard(
                        player = player,
                        matchPoints = sc?.matchPoints ?: 0,
                        matchWins = sc?.matchWins ?: 0,
                        isSelected = state.selectedWinnerId == player.id,
                        onSelect = if (canScore) {
                            { viewModel.selectWinner(player.id) }
                        } else null
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            if (canScore) {
                // Loser points input
                val losers = state.players.filter { it.id != state.selectedWinnerId }
                if (state.selectedWinnerId != null && losers.size == 2) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Enter remaining tile points for losers",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(12.dp))

                            OutlinedTextField(
                                value = state.loser1Points,
                                onValueChange = viewModel::setLoser1Points,
                                label = { Text("${losers[0].name} points") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = state.loser2Points,
                                onValueChange = viewModel::setLoser2Points,
                                label = { Text("${losers[1].name} points") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(12.dp))
                            val l1 = state.loser1Points.toIntOrNull() ?: 0
                            val l2 = state.loser2Points.toIntOrNull() ?: 0
                            Text(
                                text = "Winner will receive +${l1 + l2}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.submitRound() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !state.isLoading
                            ) {
                                Text("Record Round")
                            }
                        }
                    }
                } else if (state.selectedWinnerId == null) {
                    Text(
                        text = "Select the winner of this round above",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (match != null && match.isFinished) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Text(
                        text = "Match complete (${match.roundsPlayed}/6). You can still undo, or start a New Match.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Undo + optional early finish
            if (match != null && state.rounds.isNotEmpty()) {
                OutlinedButton(
                    onClick = { viewModel.undoLastRound() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading
                ) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Undo last round")
                }
            }
            if (match != null && !match.isFinished && match.roundsPlayed in 1..5) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.finishMatch() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading
                ) {
                    Text("Finish match early")
                }
            }

            Spacer(Modifier.height(20.dp))

            // Round history for current match
            if (state.rounds.isNotEmpty()) {
                Text(
                    text = "This match rounds",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(Modifier.height(8.dp))
                state.rounds.reversed().forEach { round ->
                    val winner = state.players.find { it.id == round.winnerPlayerId }
                    val l1 = state.players.find { it.id == round.loser1PlayerId }
                    val l2 = state.players.find { it.id == round.loser2PlayerId }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Round ${round.roundNumber}",
                                fontWeight = FontWeight.Bold
                            )
                            Text("Winner: ${winner?.name ?: "?"} (+${round.loser1Points + round.loser2Points})")
                            Text("${l1?.name}: -${round.loser1Points}   ${l2?.name}: -${round.loser2Points}")
                        }
                    }
                }
            }
        }
    }
}