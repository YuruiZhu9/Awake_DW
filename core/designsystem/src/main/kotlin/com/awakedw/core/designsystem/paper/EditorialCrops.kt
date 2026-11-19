package com.awakedw.core.designsystem.paper

import com.awakedw.core.designsystem.lolita.ArtworkCrop
import com.awakedw.core.model.ThemeId

/**
 * 一页里的两处主题角花：窗环取细节，水印取角花。
 *
 * 两个焦点尽量取不同角，让同一张素材在一页里出现两次而不重复；
 * 八主题的取值按素材实拍校准，换图时只改这一张表。
 */
data class EditorialCrops(
    val lens: ArtworkCrop,
    val ghost: ArtworkCrop,
)

fun editorialCropsOf(id: ThemeId): EditorialCrops =
    when (id) {
        // 晨雾蓝瓷：窗环看左下花瓶与玫瑰，水印印右上鸟笼。
        ThemeId.EMERALD ->
            EditorialCrops(
                lens = ArtworkCrop(focusX = 0.10f, focusY = 0.90f, zoom = 2.3f),
                ghost = ArtworkCrop(focusX = 0.86f, focusY = 0.07f, zoom = 1.55f),
            )
        // 午后藕荷：窗环看右下木马与花，水印印左上玫瑰。
        ThemeId.STRAWBERRY ->
            EditorialCrops(
                lens = ArtworkCrop(focusX = 0.88f, focusY = 0.90f, zoom = 2.3f),
                ghost = ArtworkCrop(focusX = 0.10f, focusY = 0.07f, zoom = 1.55f),
            )
        // 黄昏奶茶：窗环看左下茶具与书，水印印右上吊灯。
        ThemeId.CARAMEL ->
            EditorialCrops(
                lens = ArtworkCrop(focusX = 0.12f, focusY = 0.86f, zoom = 2.3f),
                ghost = ArtworkCrop(focusX = 0.80f, focusY = 0.08f, zoom = 1.55f),
            )
        // 深夜青黛：窗环看右下玫瑰与蕾丝，水印印左上珍珠。
        ThemeId.NIGHT ->
            EditorialCrops(
                lens = ArtworkCrop(focusX = 0.88f, focusY = 0.88f, zoom = 2.2f),
                ghost = ArtworkCrop(focusX = 0.10f, focusY = 0.06f, zoom = 1.55f),
            )
        // 雾紫玫瑰：窗环看右下玫瑰，水印印左上花束。
        ThemeId.LAVENDER ->
            EditorialCrops(
                lens = ArtworkCrop(focusX = 0.88f, focusY = 0.88f, zoom = 2.3f),
                ghost = ArtworkCrop(focusX = 0.10f, focusY = 0.07f, zoom = 1.55f),
            )
        // 黑色哥特：窗环看左下尖窗与烛台，水印印右上红宝石十字。
        ThemeId.GOTHIC ->
            EditorialCrops(
                lens = ArtworkCrop(focusX = 0.16f, focusY = 0.80f, zoom = 2.1f),
                ghost = ArtworkCrop(focusX = 0.86f, focusY = 0.07f, zoom = 1.60f),
            )
        // 白色圣职：窗环看右下烛台与头骨，水印印左上十字架。
        ThemeId.CLERIC ->
            EditorialCrops(
                lens = ArtworkCrop(focusX = 0.80f, focusY = 0.86f, zoom = 2.2f),
                ghost = ArtworkCrop(focusX = 0.08f, focusY = 0.06f, zoom = 1.55f),
            )
        // 薄荷巧克力：窗环看右上甜点架，水印印左下茶杯。
        ThemeId.THIN_MINT ->
            EditorialCrops(
                lens = ArtworkCrop(focusX = 0.88f, focusY = 0.08f, zoom = 2.2f),
                ghost = ArtworkCrop(focusX = 0.10f, focusY = 0.88f, zoom = 1.55f),
            )
    }
