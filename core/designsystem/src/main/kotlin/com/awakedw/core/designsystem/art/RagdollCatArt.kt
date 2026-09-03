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

/** Pure vector artwork for the Ragdoll mascot. Kept separate so visual iteration does not touch state/animation code. */
internal fun DrawScope.drawVectorCat(
    mood: CatMood,
    theme: ThemeSpec,
    colorFilter: ColorFilter?,
) {
    val w = size.width
    val h = size.height
    val palette = catPaletteOf(theme.isDark)
    val outline = palette.point.copy(alpha = 0.62f)
    val softOutline = palette.point.copy(alpha = 0.22f)
    val cheek = palette.nose.copy(alpha = 0.16f)

    // A small grounded shadow keeps the figure from floating above the page.
    drawOval(
        color = softOutline,
        topLeft = Offset(w * 0.24f, h * 0.875f),
        size = Size(w * 0.52f, h * 0.09f),
        colorFilter = colorFilter,
    )

    // Feathered tail, drawn first so the body naturally sits in front of it.
    val tail =
        Path().apply {
            moveTo(w * 0.66f, h * 0.80f)
            cubicTo(w * 0.82f, h * 0.88f, w * 0.95f, h * 0.78f, w * 0.91f, h * 0.61f)
            cubicTo(w * 0.89f, h * 0.51f, w * 0.79f, h * 0.46f, w * 0.73f, h * 0.52f)
        }
    drawPath(
        path = tail,
        color = palette.point,
        style = Stroke(width = w * 0.14f, cap = StrokeCap.Round),
        colorFilter = colorFilter,
    )
    val tailLight =
        Path().apply {
            moveTo(w * 0.70f, h * 0.80f)
            cubicTo(w * 0.83f, h * 0.84f, w * 0.90f, h * 0.75f, w * 0.87f, h * 0.62f)
            cubicTo(w * 0.85f, h * 0.56f, w * 0.80f, h * 0.54f, w * 0.77f, h * 0.57f)
        }
    drawPath(
        path = tailLight,
        color = palette.tailTip.copy(alpha = 0.66f),
        style = Stroke(width = w * 0.055f, cap = StrokeCap.Round),
        colorFilter = colorFilter,
    )

    // Compact sitting body: a calmer silhouette than the former segmented figure.
    val body =
        Path().apply {
            moveTo(w * 0.30f, h * 0.56f)
            cubicTo(w * 0.27f, h * 0.68f, w * 0.27f, h * 0.83f, w * 0.37f, h * 0.90f)
            cubicTo(w * 0.44f, h * 0.95f, w * 0.56f, h * 0.95f, w * 0.63f, h * 0.90f)
            cubicTo(w * 0.73f, h * 0.83f, w * 0.73f, h * 0.67f, w * 0.70f, h * 0.56f)
            cubicTo(w * 0.60f, h * 0.49f, w * 0.40f, h * 0.49f, w * 0.30f, h * 0.56f)
            close()
        }
    drawPath(path = body, color = palette.body, colorFilter = colorFilter)
    drawPath(
        path = body,
        color = outline,
        style = Stroke(width = w * 0.012f),
        colorFilter = colorFilter,
    )

    // Chest ruff and its scalloped hem: a fixed identity detail, not an unlockable accessory.
    val ruff =
        Path().apply {
            moveTo(w * 0.35f, h * 0.54f)
            cubicTo(w * 0.40f, h * 0.60f, w * 0.60f, h * 0.60f, w * 0.65f, h * 0.54f)
            cubicTo(w * 0.65f, h * 0.63f, w * 0.61f, h * 0.70f, w * 0.50f, h * 0.72f)
            cubicTo(w * 0.39f, h * 0.70f, w * 0.35f, h * 0.63f, w * 0.35f, h * 0.54f)
            close()
        }
    drawPath(path = ruff, color = palette.ruff, colorFilter = colorFilter)
    drawPath(
        path = ruff,
        color = softOutline,
        style = Stroke(width = w * 0.009f),
        colorFilter = colorFilter,
    )
    for (index in 0..4) {
        drawCircle(
            color = palette.ruff,
            radius = w * 0.045f,
            center = Offset(w * (0.39f + index * 0.055f), h * 0.675f),
            colorFilter = colorFilter,
        )
    }

    // Two neat paws and a few restrained toe marks.
    drawOval(
        color = palette.body,
        topLeft = Offset(w * 0.35f, h * 0.835f),
        size = Size(w * 0.15f, h * 0.10f),
        colorFilter = colorFilter,
    )
    drawOval(
        color = palette.body,
        topLeft = Offset(w * 0.50f, h * 0.835f),
        size = Size(w * 0.15f, h * 0.10f),
        colorFilter = colorFilter,
    )
    val toeStroke = Stroke(width = w * 0.008f, cap = StrokeCap.Round)
    drawArc(
        color = softOutline,
        startAngle = 200f,
        sweepAngle = 70f,
        useCenter = false,
        topLeft = Offset(w * 0.375f, h * 0.855f),
        size = Size(w * 0.10f, h * 0.06f),
        style = toeStroke,
        colorFilter = colorFilter,
    )
    drawArc(
        color = softOutline,
        startAngle = 200f,
        sweepAngle = 70f,
        useCenter = false,
        topLeft = Offset(w * 0.525f, h * 0.855f),
        size = Size(w * 0.10f, h * 0.06f),
        style = toeStroke,
        colorFilter = colorFilter,
    )

    // Head with soft triangular ears and a broad, recognisable Ragdoll silhouette.
    val head =
        Path().apply {
            moveTo(w * 0.24f, h * 0.24f)
            cubicTo(w * 0.22f, h * 0.15f, w * 0.25f, h * 0.08f, w * 0.32f, h * 0.10f)
            cubicTo(w * 0.37f, h * 0.11f, w * 0.40f, h * 0.18f, w * 0.42f, h * 0.21f)
            cubicTo(w * 0.47f, h * 0.19f, w * 0.53f, h * 0.19f, w * 0.58f, h * 0.21f)
            cubicTo(w * 0.60f, h * 0.18f, w * 0.63f, h * 0.11f, w * 0.68f, h * 0.10f)
            cubicTo(w * 0.75f, h * 0.08f, w * 0.78f, h * 0.15f, w * 0.76f, h * 0.24f)
            cubicTo(w * 0.84f, h * 0.32f, w * 0.81f, h * 0.46f, w * 0.72f, h * 0.53f)
            cubicTo(w * 0.62f, h * 0.61f, w * 0.38f, h * 0.61f, w * 0.28f, h * 0.53f)
            cubicTo(w * 0.19f, h * 0.46f, w * 0.16f, h * 0.32f, w * 0.24f, h * 0.24f)
            close()
        }
    drawPath(path = head, color = palette.body, colorFilter = colorFilter)
    drawPath(
        path = head,
        color = outline,
        style = Stroke(width = w * 0.012f),
        colorFilter = colorFilter,
    )

    // Warm colourpoint ear tips sit underneath the pink inner-ear wash.
    val leftEarPoint =
        Path().apply {
            moveTo(w * 0.24f, h * 0.24f)
            cubicTo(w * 0.23f, h * 0.16f, w * 0.25f, h * 0.09f, w * 0.32f, h * 0.10f)
            cubicTo(w * 0.35f, h * 0.12f, w * 0.37f, h * 0.17f, w * 0.38f, h * 0.21f)
            cubicTo(w * 0.33f, h * 0.19f, w * 0.28f, h * 0.21f, w * 0.24f, h * 0.24f)
            close()
        }
    drawPath(path = leftEarPoint, color = palette.point.copy(alpha = 0.86f), colorFilter = colorFilter)
    val rightEarPoint =
        Path().apply {
            moveTo(w * 0.76f, h * 0.24f)
            cubicTo(w * 0.77f, h * 0.16f, w * 0.75f, h * 0.09f, w * 0.68f, h * 0.10f)
            cubicTo(w * 0.65f, h * 0.12f, w * 0.63f, h * 0.17f, w * 0.62f, h * 0.21f)
            cubicTo(w * 0.67f, h * 0.19f, w * 0.72f, h * 0.21f, w * 0.76f, h * 0.24f)
            close()
        }
    drawPath(path = rightEarPoint, color = palette.point.copy(alpha = 0.86f), colorFilter = colorFilter)

    // Colourpoint ear inserts.
    val leftEar =
        Path().apply {
            moveTo(w * 0.27f, h * 0.20f)
            cubicTo(w * 0.27f, h * 0.15f, w * 0.29f, h * 0.13f, w * 0.32f, h * 0.17f)
            cubicTo(w * 0.34f, h * 0.19f, w * 0.35f, h * 0.23f, w * 0.34f, h * 0.27f)
            cubicTo(w * 0.31f, h * 0.25f, w * 0.29f, h * 0.23f, w * 0.27f, h * 0.20f)
            close()
        }
    drawPath(path = leftEar, color = palette.innerEar, colorFilter = colorFilter)
    val rightEar =
        Path().apply {
            moveTo(w * 0.73f, h * 0.20f)
            cubicTo(w * 0.73f, h * 0.15f, w * 0.71f, h * 0.13f, w * 0.68f, h * 0.17f)
            cubicTo(w * 0.66f, h * 0.19f, w * 0.65f, h * 0.23f, w * 0.66f, h * 0.27f)
            cubicTo(w * 0.69f, h * 0.25f, w * 0.71f, h * 0.23f, w * 0.73f, h * 0.20f)
            close()
        }
    drawPath(path = rightEar, color = palette.innerEar, colorFilter = colorFilter)

    // Soft mask: the broad shape and clean muzzle are what make the cat read as a Ragdoll.
    val mask =
        Path().apply {
            moveTo(w * 0.25f, h * 0.28f)
            cubicTo(w * 0.31f, h * 0.23f, w * 0.39f, h * 0.24f, w * 0.45f, h * 0.29f)
            cubicTo(w * 0.50f, h * 0.32f, w * 0.55f, h * 0.29f, w * 0.61f, h * 0.25f)
            cubicTo(w * 0.68f, h * 0.22f, w * 0.75f, h * 0.25f, w * 0.77f, h * 0.30f)
            cubicTo(w * 0.78f, h * 0.40f, w * 0.72f, h * 0.50f, w * 0.64f, h * 0.54f)
            cubicTo(w * 0.57f, h * 0.57f, w * 0.54f, h * 0.51f, w * 0.50f, h * 0.48f)
            cubicTo(w * 0.46f, h * 0.51f, w * 0.43f, h * 0.57f, w * 0.36f, h * 0.54f)
            cubicTo(w * 0.28f, h * 0.50f, w * 0.22f, h * 0.40f, w * 0.25f, h * 0.28f)
            close()
        }
    drawPath(path = mask, color = palette.point.copy(alpha = 0.90f), colorFilter = colorFilter)
    drawOval(
        color = Color.White.copy(alpha = 0.20f),
        topLeft = Offset(w * 0.34f, h * 0.235f),
        size = Size(w * 0.10f, h * 0.045f),
        colorFilter = colorFilter,
    )

    // Warm cheek tint is intentionally almost imperceptible at normal size.
    drawOval(
        color = cheek,
        topLeft = Offset(w * 0.27f, h * 0.43f),
        size = Size(w * 0.13f, h * 0.07f),
        colorFilter = colorFilter,
    )
    drawOval(
        color = cheek,
        topLeft = Offset(w * 0.60f, h * 0.43f),
        size = Size(w * 0.13f, h * 0.07f),
        colorFilter = colorFilter,
    )

    drawCatEyes(mood = mood, palette = palette, colorFilter = colorFilter)

    // Two small ivory muzzle pads soften the mask and keep the expression gentle.
    drawOval(
        color = palette.body,
        topLeft = Offset(w * 0.37f, h * 0.405f),
        size = Size(w * 0.14f, h * 0.12f),
        colorFilter = colorFilter,
    )
    drawOval(
        color = palette.body,
        topLeft = Offset(w * 0.49f, h * 0.405f),
        size = Size(w * 0.14f, h * 0.12f),
        colorFilter = colorFilter,
    )

    // Tiny pink nose and a fine mouth line.
    val nose =
        Path().apply {
            moveTo(w * 0.47f, h * 0.435f)
            cubicTo(w * 0.48f, h * 0.42f, w * 0.52f, h * 0.42f, w * 0.53f, h * 0.435f)
            cubicTo(w * 0.525f, h * 0.455f, w * 0.51f, h * 0.465f, w * 0.50f, h * 0.465f)
            cubicTo(w * 0.49f, h * 0.465f, w * 0.475f, h * 0.455f, w * 0.47f, h * 0.435f)
            close()
        }
    drawPath(path = nose, color = palette.nose, colorFilter = colorFilter)
    val mouth =
        Path().apply {
            moveTo(w * 0.50f, h * 0.462f)
            cubicTo(w * 0.50f, h * 0.475f, w * 0.49f, h * 0.482f, w * 0.475f, h * 0.485f)
            moveTo(w * 0.50f, h * 0.462f)
            cubicTo(w * 0.50f, h * 0.475f, w * 0.51f, h * 0.482f, w * 0.525f, h * 0.485f)
        }
    drawPath(
        path = mouth,
        color = outline,
        style = Stroke(width = w * 0.008f, cap = StrokeCap.Round),
        colorFilter = colorFilter,
    )

    // Fine whiskers, kept short so the icon remains elegant rather than spiky.
    val whiskers =
        Path().apply {
            moveTo(w * 0.36f, h * 0.445f)
            cubicTo(w * 0.28f, h * 0.435f, w * 0.22f, h * 0.425f, w * 0.17f, h * 0.41f)
            moveTo(w * 0.35f, h * 0.465f)
            cubicTo(w * 0.27f, h * 0.468f, w * 0.22f, h * 0.475f, w * 0.17f, h * 0.49f)
            moveTo(w * 0.64f, h * 0.445f)
            cubicTo(w * 0.72f, h * 0.435f, w * 0.78f, h * 0.425f, w * 0.83f, h * 0.41f)
            moveTo(w * 0.65f, h * 0.465f)
            cubicTo(w * 0.73f, h * 0.468f, w * 0.78f, h * 0.475f, w * 0.83f, h * 0.49f)
        }
    drawPath(
        path = whiskers,
        color = palette.whisker.copy(alpha = 0.78f),
        style = Stroke(width = w * 0.007f, cap = StrokeCap.Round),
        colorFilter = colorFilter,
    )

    // A permanent micro bow at the collar integrates the Lolita language into the mascot itself.
    drawMiniBow(
        center = Offset(w * 0.50f, h * 0.585f),
        width = w * 0.18f,
        color = theme.primary.copy(alpha = 0.82f),
        knotColor = GOLD_TRIM.copy(alpha = 0.92f),
        colorFilter = colorFilter,
    )
}

/** Draw the three eye states without changing the surrounding face geometry. */
private fun DrawScope.drawCatEyes(
    mood: CatMood,
    palette: CatVectorPalette,
    colorFilter: ColorFilter?,
) {
    val w = size.width
    val h = size.height
    val eyeColor = palette.iris
    val eyeOutline = palette.point.copy(alpha = 0.82f)
    val centers = listOf(w * 0.405f, w * 0.595f)
    when (mood) {
        CatMood.IDLE -> {
            centers.forEach { centerX ->
                drawOval(
                    color = eyeOutline,
                    topLeft = Offset(centerX - w * 0.055f, h * 0.325f),
                    size = Size(w * 0.11f, h * 0.11f),
                    colorFilter = colorFilter,
                )
                drawOval(
                    color = eyeColor,
                    topLeft = Offset(centerX - w * 0.040f, h * 0.337f),
                    size = Size(w * 0.080f, h * 0.085f),
                    colorFilter = colorFilter,
                )
                drawOval(
                    color = eyeOutline,
                    topLeft = Offset(centerX - w * 0.016f, h * 0.345f),
                    size = Size(w * 0.032f, h * 0.070f),
                    colorFilter = colorFilter,
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.94f),
                    radius = w * 0.012f,
                    center = Offset(centerX - w * 0.018f, h * 0.355f),
                    colorFilter = colorFilter,
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.62f),
                    radius = w * 0.006f,
                    center = Offset(centerX + w * 0.020f, h * 0.389f),
                    colorFilter = colorFilter,
                )
            }
        }

        CatMood.HAPPY -> {
            centers.forEach { centerX ->
                drawArc(
                    color = eyeColor,
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(centerX - w * 0.050f, h * 0.335f),
                    size = Size(w * 0.10f, h * 0.075f),
                    style = Stroke(width = w * 0.015f, cap = StrokeCap.Round),
                    colorFilter = colorFilter,
                )
            }
        }

        CatMood.SLEEPY -> {
            centers.forEach { centerX ->
                drawArc(
                    color = eyeColor.copy(alpha = 0.88f),
                    startAngle = 20f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(centerX - w * 0.050f, h * 0.335f),
                    size = Size(w * 0.10f, h * 0.075f),
                    style = Stroke(width = w * 0.014f, cap = StrokeCap.Round),
                    colorFilter = colorFilter,
                )
            }
        }
    }
}

/** Small fixed bow used as part of the mascot silhouette, not as mutable state. */
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
    drawPath(path = left, color = color, colorFilter = colorFilter)
    drawPath(path = right, color = color, colorFilter = colorFilter)
    drawCircle(color = knotColor, radius = width * 0.12f, center = center, colorFilter = colorFilter)
}
