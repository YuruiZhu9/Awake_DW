package com.awakedw.core.designsystem.art

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.awakedw.core.designsystem.ThemeSpec
import com.awakedw.core.designsystem.lolita.GOLD_TRIM
import com.awakedw.core.model.CatMood

/** Pure vector artwork for the Ragdoll mascot. Kept separate from state and animation code. */
internal fun DrawScope.drawVectorCat(
    mood: CatMood,
    theme: ThemeSpec,
    colorFilter: ColorFilter?,
) {
    val w = size.width
    val h = size.height
    val palette = catPaletteOf(theme.isDark)
    val outline = palette.point.copy(alpha = 0.48f)
    val softOutline = palette.point.copy(alpha = 0.18f)
    val furHighlight = Color.White.copy(alpha = 0.30f)
    val furLine = palette.body.copy(alpha = 0.44f)
    val blush = palette.nose.copy(alpha = 0.18f)

    // Grounding shadow: the figure should feel like a small plush ornament on paper.
    drawOval(
        color = softOutline,
        topLeft = Offset(w * 0.18f, h * 0.895f),
        size = Size(w * 0.64f, h * 0.075f),
        colorFilter = colorFilter,
    )

    // A filled, feathered tail reads more naturally than a single heavy stroke at 96dp.
    val tail =
        Path().apply {
            moveTo(w * 0.59f, h * 0.78f)
            cubicTo(w * 0.70f, h * 0.88f, w * 0.91f, h * 0.86f, w * 0.93f, h * 0.70f)
            cubicTo(w * 0.95f, h * 0.56f, w * 0.86f, h * 0.47f, w * 0.78f, h * 0.52f)
            cubicTo(w * 0.70f, h * 0.57f, w * 0.75f, h * 0.66f, w * 0.80f, h * 0.69f)
            cubicTo(w * 0.82f, h * 0.75f, w * 0.75f, h * 0.79f, w * 0.62f, h * 0.71f)
            close()
        }
    drawPath(path = tail, color = palette.point, colorFilter = colorFilter)
    drawPath(
        path = tail,
        color = outline,
        style = Stroke(width = w * 0.010f),
        colorFilter = colorFilter,
    )
    val tailHighlight =
        Path().apply {
            moveTo(w * 0.67f, h * 0.77f)
            cubicTo(w * 0.78f, h * 0.81f, w * 0.88f, h * 0.76f, w * 0.87f, h * 0.63f)
        }
    drawPath(
        path = tailHighlight,
        color = palette.tailTip.copy(alpha = 0.74f),
        style = Stroke(width = w * 0.045f, cap = StrokeCap.Round),
        colorFilter = colorFilter,
    )

    // Soft sitting body with a gentle pear silhouette.
    val body =
        Path().apply {
            moveTo(w * 0.30f, h * 0.53f)
            cubicTo(w * 0.23f, h * 0.63f, w * 0.24f, h * 0.82f, w * 0.35f, h * 0.91f)
            cubicTo(w * 0.43f, h * 0.98f, w * 0.57f, h * 0.98f, w * 0.65f, h * 0.91f)
            cubicTo(w * 0.76f, h * 0.82f, w * 0.77f, h * 0.63f, w * 0.70f, h * 0.53f)
            cubicTo(w * 0.61f, h * 0.47f, w * 0.39f, h * 0.47f, w * 0.30f, h * 0.53f)
            close()
        }
    drawPath(path = body, color = palette.body, colorFilter = colorFilter)
    drawPath(
        path = body,
        color = outline,
        style = Stroke(width = w * 0.012f),
        colorFilter = colorFilter,
    )
    drawOval(
        color = furHighlight,
        topLeft = Offset(w * 0.31f, h * 0.62f),
        size = Size(w * 0.13f, h * 0.22f),
        colorFilter = colorFilter,
    )
    drawOval(
        color = palette.point.copy(alpha = 0.08f),
        topLeft = Offset(w * 0.54f, h * 0.74f),
        size = Size(w * 0.14f, h * 0.15f),
        colorFilter = colorFilter,
    )

    // Layered ruff: small scallops make the long-haired chest legible without clutter.
    val ruff =
        Path().apply {
            moveTo(w * 0.31f, h * 0.50f)
            cubicTo(w * 0.36f, h * 0.58f, w * 0.64f, h * 0.58f, w * 0.69f, h * 0.50f)
            cubicTo(w * 0.70f, h * 0.61f, w * 0.65f, h * 0.72f, w * 0.58f, h * 0.75f)
            cubicTo(w * 0.54f, h * 0.77f, w * 0.53f, h * 0.69f, w * 0.50f, h * 0.75f)
            cubicTo(w * 0.47f, h * 0.69f, w * 0.46f, h * 0.77f, w * 0.42f, h * 0.75f)
            cubicTo(w * 0.35f, h * 0.72f, w * 0.30f, h * 0.61f, w * 0.31f, h * 0.50f)
            close()
        }
    drawPath(path = ruff, color = palette.ruff, colorFilter = colorFilter)
    drawPath(
        path = ruff,
        color = softOutline,
        style = Stroke(width = w * 0.009f),
        colorFilter = colorFilter,
    )
    repeat(5) { index ->
        drawCircle(
            color = palette.ruff,
            radius = w * 0.040f,
            center = Offset(w * (0.39f + index * 0.055f), h * 0.69f),
            colorFilter = colorFilter,
        )
    }

    // Small mitted paws, with a restrained toe mark rather than cartoon claws.
    drawOval(
        color = palette.body,
        topLeft = Offset(w * 0.34f, h * 0.83f),
        size = Size(w * 0.16f, h * 0.11f),
        colorFilter = colorFilter,
    )
    drawOval(
        color = palette.body,
        topLeft = Offset(w * 0.50f, h * 0.83f),
        size = Size(w * 0.16f, h * 0.11f),
        colorFilter = colorFilter,
    )
    val toes =
        Path().apply {
            moveTo(w * 0.41f, h * 0.875f)
            cubicTo(w * 0.41f, h * 0.85f, w * 0.42f, h * 0.85f, w * 0.43f, h * 0.875f)
            moveTo(w * 0.57f, h * 0.875f)
            cubicTo(w * 0.57f, h * 0.85f, w * 0.58f, h * 0.85f, w * 0.59f, h * 0.875f)
        }
    drawPath(
        path = toes,
        color = furLine,
        style = Stroke(width = w * 0.007f, cap = StrokeCap.Round),
        colorFilter = colorFilter,
    )

    // Head with broad cheeks and clean triangular ears.
    val head =
        Path().apply {
            moveTo(w * 0.22f, h * 0.28f)
            cubicTo(w * 0.19f, h * 0.18f, w * 0.23f, h * 0.08f, w * 0.32f, h * 0.10f)
            cubicTo(w * 0.38f, h * 0.11f, w * 0.40f, h * 0.18f, w * 0.43f, h * 0.22f)
            cubicTo(w * 0.47f, h * 0.20f, w * 0.53f, h * 0.20f, w * 0.57f, h * 0.22f)
            cubicTo(w * 0.60f, h * 0.18f, w * 0.62f, h * 0.11f, w * 0.68f, h * 0.10f)
            cubicTo(w * 0.77f, h * 0.08f, w * 0.81f, h * 0.18f, w * 0.78f, h * 0.28f)
            cubicTo(w * 0.86f, h * 0.36f, w * 0.81f, h * 0.49f, w * 0.71f, h * 0.56f)
            cubicTo(w * 0.61f, h * 0.63f, w * 0.39f, h * 0.63f, w * 0.29f, h * 0.56f)
            cubicTo(w * 0.19f, h * 0.49f, w * 0.14f, h * 0.36f, w * 0.22f, h * 0.28f)
            close()
        }
    drawPath(path = head, color = palette.body, colorFilter = colorFilter)
    drawPath(
        path = head,
        color = outline,
        style = Stroke(width = w * 0.012f),
        colorFilter = colorFilter,
    )

    // Distinct colourpoint ear caps and pink inner-ear folds.
    val leftEarPoint =
        Path().apply {
            moveTo(w * 0.22f, h * 0.28f)
            cubicTo(w * 0.21f, h * 0.18f, w * 0.24f, h * 0.09f, w * 0.32f, h * 0.10f)
            cubicTo(w * 0.36f, h * 0.12f, w * 0.38f, h * 0.18f, w * 0.39f, h * 0.23f)
            cubicTo(w * 0.33f, h * 0.20f, w * 0.27f, h * 0.23f, w * 0.22f, h * 0.28f)
            close()
        }
    drawPath(path = leftEarPoint, color = palette.point.copy(alpha = 0.88f), colorFilter = colorFilter)
    val rightEarPoint =
        Path().apply {
            moveTo(w * 0.78f, h * 0.28f)
            cubicTo(w * 0.79f, h * 0.18f, w * 0.76f, h * 0.09f, w * 0.68f, h * 0.10f)
            cubicTo(w * 0.64f, h * 0.12f, w * 0.62f, h * 0.18f, w * 0.61f, h * 0.23f)
            cubicTo(w * 0.67f, h * 0.20f, w * 0.73f, h * 0.23f, w * 0.78f, h * 0.28f)
            close()
        }
    drawPath(path = rightEarPoint, color = palette.point.copy(alpha = 0.88f), colorFilter = colorFilter)

    val leftEarInner =
        Path().apply {
            moveTo(w * 0.26f, h * 0.23f)
            cubicTo(w * 0.26f, h * 0.16f, w * 0.29f, h * 0.13f, w * 0.32f, h * 0.17f)
            cubicTo(w * 0.34f, h * 0.19f, w * 0.35f, h * 0.23f, w * 0.34f, h * 0.27f)
            cubicTo(w * 0.31f, h * 0.25f, w * 0.28f, h * 0.24f, w * 0.26f, h * 0.23f)
            close()
        }
    drawPath(path = leftEarInner, color = palette.innerEar, colorFilter = colorFilter)
    val rightEarInner =
        Path().apply {
            moveTo(w * 0.74f, h * 0.23f)
            cubicTo(w * 0.74f, h * 0.16f, w * 0.71f, h * 0.13f, w * 0.68f, h * 0.17f)
            cubicTo(w * 0.66f, h * 0.19f, w * 0.65f, h * 0.23f, w * 0.66f, h * 0.27f)
            cubicTo(w * 0.69f, h * 0.25f, w * 0.72f, h * 0.24f, w * 0.74f, h * 0.23f)
            close()
        }
    drawPath(path = rightEarInner, color = palette.innerEar, colorFilter = colorFilter)

    // Split colourpoint mask leaves a clear cream blaze down the centre of the face.
    val leftMask =
        Path().apply {
            moveTo(w * 0.23f, h * 0.31f)
            cubicTo(w * 0.29f, h * 0.26f, w * 0.38f, h * 0.27f, w * 0.45f, h * 0.31f)
            cubicTo(w * 0.46f, h * 0.37f, w * 0.43f, h * 0.47f, w * 0.37f, h * 0.52f)
            cubicTo(w * 0.31f, h * 0.54f, w * 0.25f, h * 0.48f, w * 0.22f, h * 0.41f)
            cubicTo(w * 0.21f, h * 0.37f, w * 0.21f, h * 0.34f, w * 0.23f, h * 0.31f)
            close()
        }
    drawPath(path = leftMask, color = palette.point.copy(alpha = 0.82f), colorFilter = colorFilter)
    val rightMask =
        Path().apply {
            moveTo(w * 0.77f, h * 0.31f)
            cubicTo(w * 0.71f, h * 0.26f, w * 0.62f, h * 0.27f, w * 0.55f, h * 0.31f)
            cubicTo(w * 0.54f, h * 0.37f, w * 0.57f, h * 0.47f, w * 0.63f, h * 0.52f)
            cubicTo(w * 0.69f, h * 0.54f, w * 0.75f, h * 0.48f, w * 0.78f, h * 0.41f)
            cubicTo(w * 0.79f, h * 0.37f, w * 0.79f, h * 0.34f, w * 0.77f, h * 0.31f)
            close()
        }
    drawPath(path = rightMask, color = palette.point.copy(alpha = 0.82f), colorFilter = colorFilter)
    drawOval(
        color = furHighlight.copy(alpha = 0.24f),
        topLeft = Offset(w * 0.43f, h * 0.25f),
        size = Size(w * 0.14f, h * 0.12f),
        colorFilter = colorFilter,
    )

    drawOval(
        color = blush,
        topLeft = Offset(w * 0.27f, h * 0.44f),
        size = Size(w * 0.12f, h * 0.06f),
        colorFilter = colorFilter,
    )
    drawOval(
        color = blush,
        topLeft = Offset(w * 0.61f, h * 0.44f),
        size = Size(w * 0.12f, h * 0.06f),
        colorFilter = colorFilter,
    )

    drawCatEyes(mood = mood, palette = palette, colorFilter = colorFilter)

    // Cream muzzle pads sit above the mask and keep the expression soft at small sizes.
    drawOval(
        color = palette.body,
        topLeft = Offset(w * 0.36f, h * 0.43f),
        size = Size(w * 0.15f, h * 0.13f),
        colorFilter = colorFilter,
    )
    drawOval(
        color = palette.body,
        topLeft = Offset(w * 0.49f, h * 0.43f),
        size = Size(w * 0.15f, h * 0.13f),
        colorFilter = colorFilter,
    )

    val nose =
        Path().apply {
            moveTo(w * 0.47f, h * 0.46f)
            cubicTo(w * 0.48f, h * 0.445f, w * 0.52f, h * 0.445f, w * 0.53f, h * 0.46f)
            cubicTo(w * 0.525f, h * 0.48f, w * 0.51f, h * 0.49f, w * 0.50f, h * 0.49f)
            cubicTo(w * 0.49f, h * 0.49f, w * 0.475f, h * 0.48f, w * 0.47f, h * 0.46f)
            close()
        }
    drawPath(path = nose, color = palette.nose, colorFilter = colorFilter)
    val mouth =
        Path().apply {
            moveTo(w * 0.50f, h * 0.488f)
            cubicTo(w * 0.50f, h * 0.50f, w * 0.49f, h * 0.51f, w * 0.475f, h * 0.515f)
            moveTo(w * 0.50f, h * 0.488f)
            cubicTo(w * 0.50f, h * 0.50f, w * 0.51f, h * 0.51f, w * 0.525f, h * 0.515f)
        }
    drawPath(
        path = mouth,
        color = outline,
        style = Stroke(width = w * 0.008f, cap = StrokeCap.Round),
        colorFilter = colorFilter,
    )

    val whiskers =
        Path().apply {
            moveTo(w * 0.35f, h * 0.47f)
            cubicTo(w * 0.27f, h * 0.46f, w * 0.21f, h * 0.445f, w * 0.16f, h * 0.425f)
            moveTo(w * 0.35f, h * 0.49f)
            cubicTo(w * 0.27f, h * 0.495f, w * 0.21f, h * 0.505f, w * 0.16f, h * 0.525f)
            moveTo(w * 0.65f, h * 0.47f)
            cubicTo(w * 0.73f, h * 0.46f, w * 0.79f, h * 0.445f, w * 0.84f, h * 0.425f)
            moveTo(w * 0.65f, h * 0.49f)
            cubicTo(w * 0.73f, h * 0.495f, w * 0.79f, h * 0.505f, w * 0.84f, h * 0.525f)
        }
    drawPath(
        path = whiskers,
        color = palette.whisker.copy(alpha = 0.82f),
        style = Stroke(width = w * 0.006f, cap = StrokeCap.Round),
        colorFilter = colorFilter,
    )

    // Permanent micro bow: the only Lolita cue inside the mascot silhouette.
    drawMiniBow(
        center = Offset(w * 0.50f, h * 0.595f),
        width = w * 0.20f,
        color = theme.primary.copy(alpha = 0.84f),
        knotColor = GOLD_TRIM.copy(alpha = 0.94f),
        colorFilter = colorFilter,
    )
}

/** Draw all three eye moods while preserving the same Ragdoll face geometry. */
private fun DrawScope.drawCatEyes(
    mood: CatMood,
    palette: CatVectorPalette,
    colorFilter: ColorFilter?,
) {
    val w = size.width
    val h = size.height
    val centers = listOf(w * 0.395f, w * 0.605f)
    when (mood) {
        CatMood.IDLE -> {
            centers.forEach { centerX ->
                drawOval(
                    color = palette.point.copy(alpha = 0.82f),
                    topLeft = Offset(centerX - w * 0.060f, h * 0.335f),
                    size = Size(w * 0.120f, h * 0.125f),
                    colorFilter = colorFilter,
                )
                drawOval(
                    color = palette.iris,
                    topLeft = Offset(centerX - w * 0.046f, h * 0.348f),
                    size = Size(w * 0.092f, h * 0.100f),
                    colorFilter = colorFilter,
                )
                drawOval(
                    color = palette.point.copy(alpha = 0.90f),
                    topLeft = Offset(centerX - w * 0.018f, h * 0.352f),
                    size = Size(w * 0.036f, h * 0.087f),
                    colorFilter = colorFilter,
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.96f),
                    radius = w * 0.014f,
                    center = Offset(centerX - w * 0.020f, h * 0.362f),
                    colorFilter = colorFilter,
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.62f),
                    radius = w * 0.006f,
                    center = Offset(centerX + w * 0.020f, h * 0.397f),
                    colorFilter = colorFilter,
                )
            }
        }

        CatMood.HAPPY -> {
            centers.forEach { centerX ->
                drawArc(
                    color = palette.iris,
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(centerX - w * 0.055f, h * 0.35f),
                    size = Size(w * 0.11f, h * 0.08f),
                    style = Stroke(width = w * 0.017f, cap = StrokeCap.Round),
                    colorFilter = colorFilter,
                )
            }
        }

        CatMood.SLEEPY -> {
            centers.forEach { centerX ->
                drawArc(
                    color = palette.iris.copy(alpha = 0.90f),
                    startAngle = 20f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(centerX - w * 0.055f, h * 0.35f),
                    size = Size(w * 0.11f, h * 0.08f),
                    style = Stroke(width = w * 0.016f, cap = StrokeCap.Round),
                    colorFilter = colorFilter,
                )
            }
        }
    }
}

/** Small fixed bow used as part of the mascot silhouette, never as mutable state. */
private fun DrawScope.drawMiniBow(
    center: Offset,
    width: Float,
    color: Color,
    knotColor: Color,
    colorFilter: ColorFilter?,
) {
    val left =
        Path().apply {
            moveTo(center.x, center.y)
            cubicTo(
                center.x - width * 0.22f,
                center.y - width * 0.30f,
                center.x - width * 0.56f,
                center.y - width * 0.22f,
                center.x - width * 0.48f,
                center.y,
            )
            cubicTo(
                center.x - width * 0.44f,
                center.y + width * 0.20f,
                center.x - width * 0.16f,
                center.y + width * 0.24f,
                center.x,
                center.y + width * 0.08f,
            )
            close()
        }
    val right =
        Path().apply {
            moveTo(center.x, center.y)
            cubicTo(
                center.x + width * 0.22f,
                center.y - width * 0.30f,
                center.x + width * 0.56f,
                center.y - width * 0.22f,
                center.x + width * 0.48f,
                center.y,
            )
            cubicTo(
                center.x + width * 0.44f,
                center.y + width * 0.20f,
                center.x + width * 0.16f,
                center.y + width * 0.24f,
                center.x,
                center.y + width * 0.08f,
            )
            close()
        }
    val leftTail =
        Path().apply {
            moveTo(center.x - width * 0.04f, center.y + width * 0.06f)
            lineTo(center.x - width * 0.16f, center.y + width * 0.42f)
            lineTo(center.x - width * 0.01f, center.y + width * 0.30f)
            close()
        }
    val rightTail =
        Path().apply {
            moveTo(center.x + width * 0.04f, center.y + width * 0.06f)
            lineTo(center.x + width * 0.16f, center.y + width * 0.42f)
            lineTo(center.x + width * 0.01f, center.y + width * 0.30f)
            close()
        }
    drawPath(path = leftTail, color = color.copy(alpha = 0.78f), colorFilter = colorFilter)
    drawPath(path = rightTail, color = color.copy(alpha = 0.78f), colorFilter = colorFilter)
    drawPath(path = left, color = color, colorFilter = colorFilter)
    drawPath(path = right, color = color, colorFilter = colorFilter)
    drawCircle(color = knotColor, radius = width * 0.12f, center = center, colorFilter = colorFilter)
    drawCircle(
        color = Color.White.copy(alpha = 0.46f),
        radius = width * 0.035f,
        center = Offset(center.x - width * 0.035f, center.y - width * 0.035f),
        colorFilter = colorFilter,
    )
}
