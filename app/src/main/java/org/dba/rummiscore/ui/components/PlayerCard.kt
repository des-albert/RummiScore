package org.dba.rummiscore.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.dba.rummiscore.R
import org.dba.rummiscore.data.entity.Player

@Composable
fun PlayerAvatar(
    player: Player,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    size: Dp = 72.dp
) {
    val border = if (isSelected) {
        BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    }

    Image(
        painter = painterResource(id = player.imageRes),
        contentDescription = player.name,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .border(border, CircleShape)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            )
    )
}

@Composable
fun PlayerScoreCard(
    player: Player,
    matchPoints: Int,
    matchWins: Int,
    isSelected: Boolean = false,
    onSelect: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .padding(4.dp)
            .then(if (onSelect != null) Modifier.clickable(onClick = onSelect) else Modifier),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.inversePrimary
            else
                MaterialTheme.colorScheme.onError
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 2.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(12.dp)
        ) {
            PlayerAvatar(player = player, isSelected = isSelected, onClick = onSelect, size = 64.dp)
            Spacer(Modifier.height(8.dp))
            Text(
                text = player.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Wins: $matchWins",
                style = MaterialTheme.typography.bodyLarge,
                color = if (matchWins > 0) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error
            )
            Text(
                text = "Points: $matchPoints",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}