package com.shifadaily.app.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shifadaily.app.R
import com.shifadaily.app.domain.dose.DoseStatus
import com.shifadaily.app.domain.meal.Tier
import com.shifadaily.app.ui.theme.LocalExtraColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShifaTopBar(title: String, onBack: (() -> Unit)? = null, actions: @Composable () -> Unit = {}) {
    TopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    // AutoMirrored: points the right way in RTL.
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                }
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.padding(top = 8.dp, bottom = 4.dp).semantics { heading() },
    )
}

@Composable
fun ShifaCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    container: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(containerColor = container)
    val border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth(), colors = colors, border = border) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
        }
    } else {
        Card(modifier = modifier.fillMaxWidth(), colors = colors, border = border) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
        }
    }
}

@Composable
fun NoticeCard(text: String, modifier: Modifier = Modifier, warning: Boolean = false, action: (@Composable () -> Unit)? = null) {
    val extra = LocalExtraColors.current
    val bg = if (warning) extra.warningContainer else MaterialTheme.colorScheme.primaryContainer
    val fg = if (warning) extra.onWarningContainer else MaterialTheme.colorScheme.onPrimaryContainer
    Surface(color = bg, shape = MaterialTheme.shapes.medium, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(if (warning) Icons.Filled.WarningAmber else Icons.Filled.Info, contentDescription = null, tint = fg)
                Spacer(Modifier.width(10.dp))
                Text(text, color = fg, style = MaterialTheme.typography.bodyMedium)
            }
            if (action != null) {
                Box(Modifier.fillMaxWidth().padding(top = 4.dp), contentAlignment = Alignment.CenterEnd) { action() }
            }
        }
    }
}

@StringRes
fun statusLabel(status: DoseStatus): Int = when (status) {
    DoseStatus.PENDING -> R.string.status_pending
    DoseStatus.TAKEN -> R.string.status_taken
    DoseStatus.SKIPPED -> R.string.status_skipped
    DoseStatus.SNOOZED -> R.string.status_snoozed
    DoseStatus.MISSED -> R.string.status_missed
}

@Composable
fun StatusChip(@StringRes label: Int, kind: ChipKind) {
    val extra = LocalExtraColors.current
    val (bg, fg) = when (kind) {
        ChipKind.GOOD -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        ChipKind.WARN -> extra.warningContainer to extra.onWarningContainer
        ChipKind.BAD -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        ChipKind.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(color = bg, shape = RoundedCornerShape(50)) {
        Text(
            stringResource(label),
            color = fg,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

enum class ChipKind { GOOD, WARN, BAD, NEUTRAL }

fun chipKind(status: DoseStatus) = when (status) {
    DoseStatus.TAKEN -> ChipKind.GOOD
    DoseStatus.PENDING, DoseStatus.SNOOZED -> ChipKind.WARN
    DoseStatus.MISSED -> ChipKind.BAD
    DoseStatus.SKIPPED -> ChipKind.NEUTRAL
}

@StringRes
fun tierLabel(tier: Tier) = when (tier) {
    Tier.FREELY -> R.string.tier_freely_short
    Tier.LIMITED -> R.string.tier_limited_short
    Tier.OCCASIONAL -> R.string.tier_occasional_short
    Tier.NOT_LISTED -> R.string.tier_not_listed_short
}

@Composable
fun tierColor(tier: Tier): Color {
    val t = LocalExtraColors.current.tiers
    return when (tier) {
        Tier.FREELY -> t.freely
        Tier.LIMITED -> t.limited
        Tier.OCCASIONAL -> t.occasional
        Tier.NOT_LISTED -> t.neutral
    }
}

/** Coloured dot + text label, so the tier is never conveyed by colour alone. */
@Composable
fun TierBadge(tier: Tier, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(tierColor(tier), CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(tierLabel(tier)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(16.dp).size(32.dp))
        }
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (action != null) action()
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

val ScreenPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)
