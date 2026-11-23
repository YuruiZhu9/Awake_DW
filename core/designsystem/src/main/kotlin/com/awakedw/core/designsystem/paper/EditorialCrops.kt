package com.awakedw.core.designsystem.paper

import com.awakedw.core.designsystem.lolita.ArtworkCrop
import com.awakedw.core.model.ThemeId

/**
 * 一页里的三处主题裁切：窗环取细节，水印取角花，统计卷首取横裁。
 *
 * 三个焦点尽量取不同角，让同一张素材在一页里出现多次而不重复；
 * 八主题的取值按素材实拍校准，换图时只改这一张表。
 */
data class EditorialCrops(
    val lens: ArtworkCrop,
    val ghost: ArtworkCrop,
    val plate: ArtworkCrop,
)

fun editorialCropsOf(id: ThemeId): EditorialCrops =
    when (id) {
        // 晨雾蓝瓷：窗环看左下花瓶与玫瑰，水印印右上鸟笼，卷首取左上蝴蝶结与水晶。
        ThemeId.EMERALD ->
            EditorialCrops(
                lens = ArtworkCrop(focusX = 0.10f, focusY = 0.90f, zoom = 2.3f),
                ghost = ArtworkCrop(focusX = 0.86f, focusY = 0.07f, zoom = 1.55f),
                plate = ArtworkCrop(focusX = 0.20f, focusY = 0.09f, zoom = 1.50f),
            )
        // 午后藕荷：窗环看右下木马与花，水印印左上玫瑰，卷首取左上玫瑰与蕾丝。
        ThemeId.STRAWBERRY ->
            EditorialCrops(
                lens = ArtworkCrop(focusX = 0.88f, focusY = 0.90f, zoom = 2.3f),
                ghost = ArtworkCrop(focusX = 0.10f, focusY = 0.07f, zoom = 1.55f),
                plate = ArtworkCrop(focusX = 0.18f, focusY = 0.09f, zoom = 1.50f),
            )
        // 黄昏奶茶：窗环看左下茶具与书，水印印右上吊灯，卷首取右上吊灯与帷幕。
        ThemeId.CARAMEL ->
            EditorialCrops(
                lens = ArtworkCrop(focusX = 0.12f, focusY = 0.86f, zoom = 2.3f),
                ghost = ArtworkCrop(focusX = 0.80f, focusY = 0.08f, zoom = 1.55f),
                plate = ArtworkCrop(focusX = 0.68f, focusY = 0.06f, zoom = 1.50f),
            )
        // 深夜青黛：窗环看右下玫瑰与蕾丝，水印印左上珍珠，卷首取左上玫瑰与珍珠。
        ThemeId.NIGHT ->
            EditorialCrops(
                lens = ArtworkCrop(focusX = 0.88f, focusY = 0.88f, zoom = 2.2f),
                ghost = ArtworkCrop(focusX = 0.10f, focusY = 0.06f, zoom = 1.55f),
                plate = ArtworkCrop(focusX = 0.15f, focusY = 0.10f, zoom = 1.45f),
            )
        // 雾紫玫瑰：窗环看右下玫瑰，水印印左上花束，卷首取左上玫瑰与薰衣草。
        ThemeId.LAVENDER ->
            EditorialCrops(
                lens = ArtworkCrop(focusX = 0.88f, focusY = 0.88f, zoom = 2.3f),
                ghost = ArtworkCrop(focusX = 0.10f, focusY = 0.07f, zoom = 1.55f),
                plate = ArtworkCrop(focusX = 0.18f, focusY = 0.08f, zoom = 1.50f),
            )
        // 黑色哥特：窗环看左下尖窗与烛台，水印印右上红宝石十字，卷首取左上黑玫瑰与银十字。
        ThemeId.GOTHIC ->
            EditorialCrops(
                lens = ArtworkCrop(focusX = 0.16f, focusY = 0.80f, zoom = 2.1f),
                ghost = ArtworkCrop(focusX = 0.86f, focusY = 0.07f, zoom = 1.60f),
                plate = ArtworkCrop(focusX = 0.14f, focusY = 0.08f, zoom = 1.40f),
            )
        // 白色圣职：窗环看右下烛台与头骨，水印印左上十字架，卷首取左上银十字与玫瑰。
        ThemeId.CLERIC ->
            EditorialCrops(
                lens = ArtworkCrop(focusX = 0.80f, focusY = 0.86f, zoom = 2.2f),
                ghost = ArtworkCrop(focusX = 0.08f, focusY = 0.06f, zoom = 1.55f),
                plate = ArtworkCrop(focusX = 0.18f, focusY = 0.10f, zoom = 1.50f),
            )
        // 薄荷巧克力：窗环看右上甜点架，水印印左下茶杯，卷首取右上甜点架与巧克力。
        ThemeId.THIN_MINT ->
            EditorialCrops(
                lens = ArtworkCrop(focusX = 0.88f, focusY = 0.08f, zoom = 2.2f),
                ghost = ArtworkCrop(focusX = 0.10f, focusY = 0.88f, zoom = 1.55f),
                plate = ArtworkCrop(focusX = 0.82f, focusY = 0.10f, zoom = 1.50f),
            )
    }
