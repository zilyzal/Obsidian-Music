package com.obsidian.music.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.obsidian.music.R

/** First launch: say what we ask for and why, then ask. */
@Composable
fun WelcomeScreen(accent: Color, blocked: Boolean, onAllow: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.45f))

        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = "Obsidian Music logo",
            modifier = Modifier
                .size(132.dp)
                .shadow(28.dp, RoundedCornerShape(34.dp), ambientColor = accent, spotColor = accent),
        )

        Spacer(Modifier.height(30.dp))
        Text(
            "Let's find your music",
            style = ObsidianType.Welcome,
            color = Obsidian.TextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Obsidian plays the songs already on your phone. Nothing is uploaded and nothing leaves the device.",
            style = ObsidianType.Body,
            color = Obsidian.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 300.dp),
        )

        Spacer(Modifier.height(32.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .glassPanel(RoundedCornerShape(30.dp), accent, tintAlpha = 0.10f, sheen = 0.10f, edge = 0.28f)
                .padding(8.dp),
        ) {
            AskRow(Icons.Rounded.MusicNote, "Audio files", "To find the songs on your phone")
            Box(
                Modifier
                    .padding(start = 68.dp, end = 14.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.08f))
            )
            AskRow(Icons.Rounded.Notifications, "Notifications", "For controls on your lock screen")
        }

        Spacer(Modifier.weight(0.55f))

        Box(
            Modifier
                .fillMaxWidth()
                .height(58.dp)
                .shadow(24.dp, CircleShape, ambientColor = accent, spotColor = accent)
                .bouncyClick(0.97f, onAllow)
                .clip(RoundedCornerShape(29.dp))
                .background(Obsidian.TextPrimary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (blocked) "Open Settings" else "Allow access",
                style = ObsidianType.ButtonLarge,
                color = Obsidian.Ink,
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            if (blocked) "Android needs you to switch audio access on in Settings."
            else "You can change this any time in Settings.",
            style = ObsidianType.Caption,
            color = Obsidian.TextTertiary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun AskRow(icon: ImageVector, title: String, subtitle: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.09f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Obsidian.TextPrimary, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.size(14.dp))
        Column {
            Text(title, style = ObsidianType.RowTitle, color = Obsidian.TextPrimary)
            Text(subtitle, style = ObsidianType.RowSub, color = Obsidian.TextSecondary)
        }
    }
}
