package com.obsidian.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The one filled button in the app: ink on a bright pill. */
@Composable
fun BrightButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
    large: Boolean = false,
) {
    Box(
        modifier
            .height(height)
            .bouncyClick(0.96f, onClick)
            .clip(RoundedCornerShape(height / 2))
            .background(Obsidian.TextPrimary)
            .padding(horizontal = 26.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = if (large) ObsidianType.ButtonLarge else ObsidianType.Button,
            color = Obsidian.Ink,
        )
    }
}

/** Floating glass card for "nothing here" moments: empty Favorites, no music found. */
@Composable
fun StateCard(
    icon: ImageVector,
    title: String,
    body: String,
    accent: Color,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .glassPanel(RoundedCornerShape(36.dp), accent, tintAlpha = 0.10f, sheen = 0.10f, edge = 0.28f)
            .padding(start = 28.dp, end = 28.dp, top = 40.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.07f))
                .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Obsidian.TextSecondary, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(22.dp))
        Text(title, style = ObsidianType.CardTitle, color = Obsidian.TextPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            body,
            style = ObsidianType.BodySmall,
            color = Obsidian.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 250.dp),
        )
        Spacer(Modifier.height(26.dp))
        BrightButton(actionLabel, onAction)
    }
}
