package com.tempodroid.ui

object WidgetDimensions {
    const val REFERENCE_WIDTH_DP = 320
    const val REFERENCE_HEIGHT_DP = 160
    const val CORNER_RADIUS_DP = 24
    const val PADDING_DP = 16
    const val SPACING_DP = 8
    const val MICRO_GAP_DP = 4
    const val GLASS_RADIUS_DP = 16
    const val TIMELINE_HEIGHT_DP = 52
    const val COMPACT_WIDTH_DP = 180
    const val COMPACT_HEIGHT_DP = 110
    const val EXPANDED_WIDTH_DP = 400
    const val EXPANDED_HEIGHT_DP = 200
    const val MAX_BITMAP_WIDTH_PX = 960
    const val MAX_BITMAP_HEIGHT_PX = 480

    fun clampBitmapSize(widthPx: Int, heightPx: Int): Pair<Int, Int> {
        val w = widthPx.coerceIn(160, MAX_BITMAP_WIDTH_PX)
        val h = heightPx.coerceIn(80, MAX_BITMAP_HEIGHT_PX)
        return w to h
    }
}

enum class WidgetLayoutMode {
    COMPACT,
    NORMAL,
    EXPANDED,
}

object WidgetLayout {
    fun mode(widthDp: Float, heightDp: Float): WidgetLayoutMode = when {
        widthDp < 250f || heightDp < 140f -> WidgetLayoutMode.COMPACT
        widthDp >= 380f -> WidgetLayoutMode.EXPANDED
        else -> WidgetLayoutMode.NORMAL
    }
}