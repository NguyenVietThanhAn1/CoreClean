package com.coreclean.app.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Shimmer brush — use inside composables that need a skeleton placeholder. */
@Composable
fun shimmerBrush(widthPx: Float = 800f): Brush {
    val base  = MaterialTheme.colorScheme.surfaceVariant
    val sheen = MaterialTheme.colorScheme.surface
    val transition = rememberInfiniteTransition(label = "shimmer")
    val offset by transition.animateFloat(
        initialValue  = -widthPx,
        targetValue   = widthPx,
        animationSpec = infiniteRepeatable(
            animation  = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer-x"
    )
    return Brush.linearGradient(
        colors = listOf(base, sheen, sheen, base),
        start  = Offset(offset, 0f),
        end    = Offset(offset + widthPx, 0f)
    )
}

/** Full-screen centered loading — gradient spinner with optional scanning label. */
@Composable
fun FullScreenLoading(message: String? = null, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GradientSpinner(size = 56.dp)
            if (message != null) {
                Text(
                    text      = message,
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.padding(horizontal = 32.dp)
                )
            }
        }
    }
}

/** Tiny spinner sized for use inside Buttons. Default tint = onPrimary. */
@Composable
fun ButtonLoading(
    size: Dp           = 18.dp,
    strokeWidth: Dp    = 2.dp,
    tint: Color        = Color.Unspecified,
    modifier: Modifier = Modifier
) {
    val color = if (tint == Color.Unspecified) MaterialTheme.colorScheme.onPrimary else tint
    GradientSpinner(size = size, strokeWidth = strokeWidth, tint = color, modifier = modifier)
}

/** Animated gradient arc spinner. */
@Composable
fun GradientSpinner(
    size: Dp           = 48.dp,
    strokeWidth: Dp    = 4.dp,
    tint: Color        = Color.Unspecified,
    modifier: Modifier = Modifier
) {
    val color = if (tint == Color.Unspecified) MaterialTheme.colorScheme.primary else tint
    val transition = rememberInfiniteTransition(label = "spinner")
    val angle by transition.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin-angle"
    )

    Canvas(modifier.size(size)) {
        val strokePx = strokeWidth.toPx()
        val radius   = (this.size.minDimension / 2f) - strokePx / 2f
        val cx       = this.size.width  / 2f
        val cy       = this.size.height / 2f
        val arcSize  = Size(radius * 2f, radius * 2f)
        val topLeft  = Offset(cx - radius, cy - radius)
        val style    = Stroke(width = strokePx, cap = StrokeCap.Round)

        drawArc(
            color = color.copy(alpha = 0.15f),
            startAngle = 0f, sweepAngle = 360f, useCenter = false,
            topLeft = topLeft, size = arcSize, style = Stroke(strokePx)
        )
        drawArc(
            color = color.copy(alpha = 0.35f),
            startAngle = angle + 210f, sweepAngle = 60f, useCenter = false,
            topLeft = topLeft, size = arcSize, style = style
        )
        drawArc(
            color = color,
            startAngle = angle, sweepAngle = 270f, useCenter = false,
            topLeft = topLeft, size = arcSize, style = style
        )
    }
}

/** Shimmer skeleton for list screens (contacts, app lists). */
@Composable
fun ShimmerListPlaceholder(
    itemCount: Int     = 7,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val brush   = shimmerBrush(widthPx)

        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(itemCount) { ShimmerRow(brush) }
        }
    }
}

@Composable
private fun ShimmerRow(brush: Brush) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(brush))
        Column(
            modifier            = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(Modifier.fillMaxWidth(0.65f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(brush))
            Box(Modifier.fillMaxWidth(0.40f).height(10.dp).clip(RoundedCornerShape(4.dp)).background(brush))
        }
        Box(Modifier.width(52.dp).height(16.dp).clip(RoundedCornerShape(4.dp)).background(brush))
    }
}

/**
 * Blocking overlay shown during destructive operations (e.g. cleaning junk).
 * Wraps content in a scrim + centered card with spinner + message.
 */
@Composable
fun GaugeLoadingOverlay(
    message: String,
    visible: Boolean    = true,
    modifier: Modifier  = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter   = fadeIn(tween(200)),
        exit    = fadeOut(tween(200)),
        modifier = modifier
    ) {
        Box(
            modifier           = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f)),
            contentAlignment   = Alignment.Center
        ) {
            Card(
                shape  = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier            = Modifier.padding(horizontal = 40.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    GradientSpinner(size = 56.dp, strokeWidth = 5.dp)
                    Text(
                        text      = message,
                        style     = MaterialTheme.typography.bodyMedium,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
