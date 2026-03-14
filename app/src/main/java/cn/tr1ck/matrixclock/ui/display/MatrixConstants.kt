package cn.tr1ck.matrixclock.ui.display

import androidx.compose.ui.graphics.Color

// Layout constants for matrix display (clock vs text)
internal const val MATRIX_ROWS = 36
internal const val LARGE_FONT_ROWS = 16   // 8x16 main font height
internal const val LARGE_GLYPH_COLS = 8
internal const val LARGE_CHAR_WIDTH = LARGE_GLYPH_COLS + 1   // 9, includes 1 column spacing
internal const val MINI_GLYPH_ROWS = 5   // 5x3 mini font height
internal const val MINI_GLYPH_COLS = 3
internal const val MINI_GAP_COLS = 1
internal const val MINI_CHAR_WIDTH = MINI_GLYPH_COLS + MINI_GAP_COLS

internal const val MEDIUM_GLYPH_ROWS = 7   // 5x7 medium font height
internal const val MEDIUM_GLYPH_COLS = 5
internal const val MEDIUM_CHAR_WIDTH = MEDIUM_GLYPH_COLS + 1

internal const val CLOCK_DATE_MARGIN = 2
internal const val CLOCK_DATE_ROW_START = CLOCK_DATE_MARGIN
internal const val CLOCK_TIME_ROW_START = (MATRIX_ROWS - LARGE_FONT_ROWS) / 2   // centered

internal const val TEXT_CONTENT_ROW_START = (MATRIX_ROWS - LARGE_FONT_ROWS) / 2 + 2
internal const val TEXT_COUNTDOWN_ROW_START = CLOCK_DATE_MARGIN
internal const val TEXT_PROGRESS_ROW_TOP = TEXT_CONTENT_ROW_START - 2
internal const val TEXT_PROGRESS_ROW_BOTTOM = TEXT_CONTENT_ROW_START + LARGE_FONT_ROWS + 1

internal const val CYCLE_TOTAL = 12
// Cycle-complete star: same size as mini icons (same height as MINI_GLYPH_ROWS).
internal const val STAR_SIZE: Int = MINI_GLYPH_ROWS

internal val OFF_DOT_COLOR = Color(0xFF0D0D0D)
internal val PROGRESS_TRACK_COLOR = Color(0xFF1F2937)
internal val UNBOUND_IP_COLOR = Color(0xFF22C55E) // Green for unauthorized IP
internal val NO_WIFI_COLOR = Color(0xFFEF4444)    // Red for No WiFi
