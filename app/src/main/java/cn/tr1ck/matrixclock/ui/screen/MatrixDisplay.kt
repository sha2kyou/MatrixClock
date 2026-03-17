package cn.tr1ck.matrixclock.ui.screen

import cn.tr1ck.matrixclock.data.model.DisplayMode
import cn.tr1ck.matrixclock.ui.display.MatrixFont
import cn.tr1ck.matrixclock.ui.display.MatrixIcons
import cn.tr1ck.matrixclock.ui.display.MATRIX_ROWS
import cn.tr1ck.matrixclock.ui.display.MINI_GLYPH_ROWS
import cn.tr1ck.matrixclock.ui.display.LARGE_FONT_ROWS
import cn.tr1ck.matrixclock.ui.display.LARGE_CHAR_WIDTH
import cn.tr1ck.matrixclock.ui.display.LARGE_GLYPH_COLS
import cn.tr1ck.matrixclock.ui.display.MEDIUM_CHAR_WIDTH
import cn.tr1ck.matrixclock.ui.display.MEDIUM_GLYPH_COLS
import cn.tr1ck.matrixclock.ui.display.MEDIUM_GLYPH_ROWS
import cn.tr1ck.matrixclock.ui.display.MINI_CHAR_WIDTH
import cn.tr1ck.matrixclock.ui.display.MINI_GLYPH_COLS
import cn.tr1ck.matrixclock.ui.display.CLOCK_DATE_MARGIN
import cn.tr1ck.matrixclock.ui.display.CLOCK_DATE_ROW_START
import cn.tr1ck.matrixclock.ui.display.CLOCK_TIME_ROW_START
import cn.tr1ck.matrixclock.ui.display.TEXT_CONTENT_ROW_START
import cn.tr1ck.matrixclock.ui.display.TEXT_COUNTDOWN_ROW_START
import cn.tr1ck.matrixclock.ui.display.TEXT_PROGRESS_ROW_TOP
import cn.tr1ck.matrixclock.ui.display.TEXT_PROGRESS_ROW_BOTTOM
import cn.tr1ck.matrixclock.ui.display.STAR_SIZE
import cn.tr1ck.matrixclock.ui.display.OFF_DOT_COLOR
import cn.tr1ck.matrixclock.ui.display.PROGRESS_TRACK_COLOR
import cn.tr1ck.matrixclock.ui.display.UNBOUND_IP_COLOR
import cn.tr1ck.matrixclock.ui.display.NO_WIFI_COLOR
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.coroutines.delay
import java.util.Calendar

@Composable
fun MatrixClockApp(
    text: String,
    dateInfo: String,
    amPm: String,
    color: Color,
    mode: DisplayMode,
    clockTemplate: Int,
    clockT3TimeMillis: Long,
    progress: Float,
    remainingSeconds: Int,
    countdownStyle: Int = 1,
    isBound: Boolean,
    ipAddress: String?,
    cycleCompleted: Int = 0,
    cycleTotal: Int = 0,
    statusIconKey: String? = null
) {
    var scrollOffset by remember { mutableFloatStateOf(0f) }
    var matrixWidthForScroll by remember { mutableIntStateOf(64) }
    val unboundText = remember(ipAddress) { 
        if (ipAddress == null) "NO WIFI" else "$ipAddress:6574" 
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { size ->
                if (size.height > 0) matrixWidthForScroll = (size.width / (size.height / MATRIX_ROWS)).toInt()
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (h > 0 && w > 0) {
                val dotSize = h / MATRIX_ROWS
                val matrixWidth = (w / dotSize).toInt()
                val startX = (w - matrixWidth * dotSize) / 2f

                val displayContent: String
                val contentStartCol: Float
                val showDate: Boolean
                val showProgress: Boolean
                val contentColor: Color
                val rowInMainFontBase: Int

                val drawUnboundWithMiniFont: Boolean
                if (!isBound) {
                    displayContent = ""
                    contentStartCol = 0f
                    showDate = false
                    showProgress = false
                    contentColor = if (ipAddress == null) NO_WIFI_COLOR else UNBOUND_IP_COLOR
                    rowInMainFontBase = -1
                    drawUnboundWithMiniFont = true
                } else {
                    drawUnboundWithMiniFont = false
                    displayContent = text
                    val isStyle3Area = isBound && mode == DisplayMode.TEXT && countdownStyle.coerceIn(1, 3) == 3
                    val leftWidthStyle3 = if (isStyle3Area) (matrixWidth - 33).coerceIn(0, matrixWidth) else matrixWidth
                    val textLengthLarge = displayContent.length * LARGE_CHAR_WIDTH
                    val textLengthMedium = displayContent.length * MEDIUM_CHAR_WIDTH
                    contentStartCol = if (mode == DisplayMode.CLOCK) {
                        (matrixWidth - textLengthLarge) / 2f
                    } else if (mode == DisplayMode.TEXT && isStyle3Area) {
                        if (textLengthMedium <= leftWidthStyle3) (leftWidthStyle3 - textLengthMedium) / 2f
                        else leftWidthStyle3 - (scrollOffset % (leftWidthStyle3 + textLengthMedium + 8f))
                    } else if (mode == DisplayMode.TEXT && textLengthLarge <= leftWidthStyle3) {
                        (leftWidthStyle3 - textLengthLarge) / 2f
                    } else {
                        matrixWidth - scrollOffset
                    }
                    showDate = (mode == DisplayMode.CLOCK && (clockTemplate == 1 || clockTemplate == 3))
                    showProgress = (mode == DisplayMode.TEXT)
                    contentColor = when {
                        mode != DisplayMode.CLOCK -> color
                        clockTemplate == 2 -> Color(0xFF00D4FF)
                        clockTemplate == 3 -> Color(0xFFFFD54F)
                        else -> Color.Red
                    }
                    rowInMainFontBase = when (mode) {
                        DisplayMode.CLOCK -> CLOCK_TIME_ROW_START
                        DisplayMode.TEXT -> TEXT_CONTENT_ROW_START
                    }
                }

                val unboundRowStart = (MATRIX_ROWS - MINI_GLYPH_ROWS) / 2
                val dateTextLength = dateInfo.length * MINI_CHAR_WIDTH
                val dateStartCol = if (showDate) CLOCK_DATE_MARGIN.toFloat() else (matrixWidth - dateTextLength) / 2f
                val showAmPm = isBound && mode == DisplayMode.CLOCK && amPm.isNotBlank()
                val amPmTextLength = amPm.length * MINI_CHAR_WIDTH
                val amPmStartCol = (matrixWidth - amPmTextLength - CLOCK_DATE_MARGIN).toFloat().coerceAtLeast(CLOCK_DATE_MARGIN.toFloat())

                val hasCountdown = showProgress && remainingSeconds >= 0
                val countdownStr = if (hasCountdown) {
                    val m = remainingSeconds / 60
                    val s = remainingSeconds % 60
                    "%d:%02d".format(m, s)
                } else ""
                val alarmIconColStart = 1
                val alarmIconRowStart = TEXT_COUNTDOWN_ROW_START
                val countdownStartCol = if (hasCountdown) (alarmIconColStart + MINI_GLYPH_ROWS + 1).toFloat() else CLOCK_DATE_MARGIN.toFloat()

                val statusIconRows: IntArray? = if (showProgress && !hasCountdown) {
                    when ((statusIconKey ?: "").trim().lowercase()) {
                        "star" -> MatrixIcons.ICON_MINI_STAR_ROWS
                        "moon" -> MatrixIcons.ICON_MINI_MOON_ROWS
                        "phone" -> MatrixIcons.ICON_MINI_PHONE_ROWS
                        "alarm" -> MatrixIcons.ICON_MINI_ALARM_ROWS
                        "clock" -> MatrixIcons.ICON_MINI_CLOCK_ROWS
                        "stopwatch" -> MatrixIcons.ICON_MINI_STOPWATCH_ROWS
                        else -> null
                    }
                } else null

                val showCycle = isBound && showProgress && cycleTotal > 0
                val cycleShowStar = showCycle && cycleCompleted >= cycleTotal
                val cycleStr = if (showCycle && !cycleShowStar) "${minOf(cycleCompleted, cycleTotal)}/$cycleTotal" else ""
                val cycleRowStartBottom = MATRIX_ROWS - MINI_GLYPH_ROWS
                val cycleRowStartTop = TEXT_COUNTDOWN_ROW_START
                val cycleStrLength = cycleStr.length * MINI_CHAR_WIDTH
                val cycleStarColEnd = matrixWidth - 1
                val cycleStarColStart = cycleStarColEnd - STAR_SIZE + 1
                val cycleStrColStart = (matrixWidth - cycleStrLength).toFloat()

                val clockT2Str = if (isBound && mode == DisplayMode.CLOCK && clockTemplate == 2) text + " " + dateInfo else ""
                val clockT2RowStart = (MATRIX_ROWS - MINI_GLYPH_ROWS) / 2
                val clockT2StartCol = if (clockT2Str.isNotEmpty()) (matrixWidth - clockT2Str.length * MINI_CHAR_WIDTH) / 2f else 0f

                val clockT3Active = isBound && mode == DisplayMode.CLOCK && clockTemplate == 3
                val hourStrT3 = if (clockT3Active) {
                    val parts = text.split(":", " ")
                    if (parts.isNotEmpty()) parts[0].take(2).padStart(2, '0') else "00"
                } else ""
                val hourStartColT3 = (CLOCK_DATE_MARGIN + 5).toFloat()
                val hourWidthT3 = hourStrT3.length * LARGE_CHAR_WIDTH
                val clockT3HourRowStart = CLOCK_TIME_ROW_START
                val clockT3MiniRowStart = (MATRIX_ROWS - MINI_GLYPH_ROWS) / 2
                val minuteAreaStartColT3 = (hourStartColT3 + hourWidthT3 + 4).toInt()
                val lineCenterColT3 = (minuteAreaStartColT3 + matrixWidth) / 2
                val lineRowStartT3 = clockT3MiniRowStart - 6
                val lineRowEndT3 = clockT3MiniRowStart + MINI_GLYPH_ROWS + 6
                val lineGapStartT3 = clockT3MiniRowStart - 3
                val lineGapEndT3 = clockT3MiniRowStart + 7
                val calT3 = if (clockT3Active && clockT3TimeMillis > 0) {
                    Calendar.getInstance().apply { timeInMillis = clockT3TimeMillis }
                } else null
                val minuteT3 = calT3?.get(Calendar.MINUTE) ?: 0
                val secondT3 = calT3?.get(Calendar.SECOND) ?: 0
                val colsPerMinuteNumber = 2 * MINI_CHAR_WIDTH + 1
                val stripColAtCurrent = (20.0 + secondT3 / 60.0) * colsPerMinuteNumber
                val scrollOffsetT3 = (stripColAtCurrent - lineCenterColT3).toFloat()

                val style = countdownStyle.coerceIn(1, 3)
                val progressColStyle2 = if (isBound && showProgress && style == 2) {
                    (matrixWidth * (1f - progress)).toInt().coerceIn(0, matrixWidth)
                } else -1
                val matrixStartColStyle3 = if (isBound && showProgress && style == 3) (matrixWidth - 33).coerceIn(0, matrixWidth) else -1
                val matrixStartRowStyle3 = if (matrixStartColStyle3 >= 0) (MATRIX_ROWS - 32) / 2 else 0
                val filledCountStyle3 = if (matrixStartColStyle3 >= 0) (1024 * (1f - progress)).toInt().coerceIn(0, 1024) else 0
                val mediumRowStartStyle3 = if (matrixStartColStyle3 >= 0) (MATRIX_ROWS - MEDIUM_GLYPH_ROWS) / 2 else 0
                val cycleStrColStartStyle3 = if (matrixStartColStyle3 >= 0) (CLOCK_DATE_MARGIN + 1).toFloat() else cycleStrColStart
                val cycleStarColStartStyle3 = if (matrixStartColStyle3 >= 0) CLOCK_DATE_MARGIN + 1 else cycleStarColStart
                val cycleRowStartStyle3 = cycleRowStartBottom - 1
                val cycleRowStart = if (style == 3 && matrixStartColStyle3 >= 0) cycleRowStartStyle3 else cycleRowStartTop
                // Style 3: when static/centered, keep left padding >= 1; when scrolling, do not clamp to 1 to avoid stalling.
                val contentStartColStyle3 = if (style == 3 && matrixStartColStyle3 >= 0) {
                    if (contentStartCol >= 0f) (contentStartCol + 1f).coerceAtLeast(1f) else contentStartCol + 1f
                } else contentStartCol

                for (r in 0 until MATRIX_ROWS) {
                    val rowInMainFont = r - rowInMainFontBase
                    val rowInMiniFont = r - CLOCK_DATE_ROW_START
                    val rowInCountdown = r - TEXT_COUNTDOWN_ROW_START

                    for (c in 0 until matrixWidth) {
                        var isOn = false
                        var isProgressBar = false
                        var isProgressTrack = false
                        var isStyle3MatrixBlack = false
                        var isStyle3MatrixTheme = false
                        var t3Alpha = 1f
                        var t3MinuteOrLine = false
                        var t3IsLine = false

                        if (drawUnboundWithMiniFont) {
                            val unboundStartCol = (matrixWidth - unboundText.length * MINI_CHAR_WIDTH) / 2f
                            if (r in unboundRowStart until unboundRowStart + MINI_GLYPH_ROWS) {
                                val rowInMini = r - unboundRowStart
                                val charColFloat = c - unboundStartCol
                                if (charColFloat >= 0f) {
                                    val charCol = charColFloat.toInt()
                                    val charIdx = charCol / MINI_CHAR_WIDTH
                                    val colInChar = charCol % MINI_CHAR_WIDTH
                                    if (charIdx in unboundText.indices && colInChar in 0 until MINI_GLYPH_COLS) {
                                        val ch = unboundText[charIdx]
                                        val font = MatrixFont.miniFontData[ch]
                                            ?: MatrixFont.miniFontData[ch.uppercaseChar()]
                                            ?: MatrixFont.miniFontData[' ']!!
                                        if (rowInMini < font.size && ((font[rowInMini] shr (2 - colInChar)) and 1) == 1) isOn = true
                                    }
                                }
                            }
                        }

                        if (hasCountdown && !isOn && r in alarmIconRowStart until alarmIconRowStart + MINI_GLYPH_ROWS && c in alarmIconColStart until alarmIconColStart + MINI_GLYPH_ROWS) {
                            val localRow = r - alarmIconRowStart
                            val localCol = c - alarmIconColStart
                            if (MatrixIcons.isMiniOn(MatrixIcons.ICON_MINI_ALARM_ROWS, localRow, localCol)) isOn = true
                        }

                        if (!hasCountdown && statusIconRows != null && !isOn && r in alarmIconRowStart until alarmIconRowStart + MINI_GLYPH_ROWS && c in alarmIconColStart until alarmIconColStart + MINI_GLYPH_ROWS) {
                            val localRow = r - alarmIconRowStart
                            val localCol = c - alarmIconColStart
                            if (MatrixIcons.isMiniOn(statusIconRows, localRow, localCol)) isOn = true
                        }

                        val inStyle3Left = matrixStartColStyle3 < 0 || c < matrixStartColStyle3
                        if (style == 3 && matrixStartColStyle3 >= 0 && inStyle3Left && r in mediumRowStartStyle3 until mediumRowStartStyle3 + MEDIUM_GLYPH_ROWS) {
                            val rowInMedium = r - mediumRowStartStyle3
                            val charColFloat = c - contentStartColStyle3
                            if (charColFloat >= 0f) {
                                val charCol = charColFloat.toInt()
                                val charIdx = charCol / MEDIUM_CHAR_WIDTH
                                val colInChar = charCol % MEDIUM_CHAR_WIDTH
                                if (charIdx in displayContent.indices && colInChar in 0 until MEDIUM_GLYPH_COLS) {
                                    val ch = displayContent[charIdx]
                                    val font = MatrixFont.mediumFontData[ch] ?: MatrixFont.mediumFontData[ch.uppercaseChar()] ?: MatrixFont.mediumFontData[' ']!!
                                    if (rowInMedium < font.size && (((font[rowInMedium] and 0x1F) shr (MEDIUM_GLYPH_COLS - 1 - colInChar)) and 1) == 1) isOn = true
                                }
                            }
                        } else {
                            val drawMainContent = !drawUnboundWithMiniFont && (mode != DisplayMode.CLOCK || clockTemplate == 1) && rowInMainFont in 0 until LARGE_FONT_ROWS && inStyle3Left && (style != 3 || matrixStartColStyle3 < 0)
                            if (drawMainContent) {
                                val charColFloat = c - contentStartCol
                                if (charColFloat >= 0f) {
                                    val charCol = charColFloat.toInt()
                                    val charIdx = charCol / LARGE_CHAR_WIDTH
                                    val colInChar = charCol % LARGE_CHAR_WIDTH
                                    if (charIdx in displayContent.indices && colInChar in 0 until LARGE_GLYPH_COLS) {
                                        val ch = displayContent[charIdx]
                                        val font = MatrixFont.largeFontData[ch] ?: MatrixFont.largeFontData[ch.uppercaseChar()] ?: MatrixFont.largeFontData[' ']!!
                                        if (rowInMainFont < font.size && (((font[rowInMainFont] and 0xFF) shr (7 - colInChar)) and 1) == 1) isOn = true
                                    }
                                }
                            }
                        }

                        if (clockT2Str.isNotEmpty() && !isOn && r in clockT2RowStart until clockT2RowStart + MINI_GLYPH_ROWS) {
                            val rowInT2 = r - clockT2RowStart
                            val charColFloat = c - clockT2StartCol
                            if (charColFloat >= 0f) {
                                val charCol = charColFloat.toInt()
                                val charIdx = charCol / MINI_CHAR_WIDTH
                                val colInChar = charCol % MINI_CHAR_WIDTH
                                if (charIdx in clockT2Str.indices && colInChar in 0 until MINI_GLYPH_COLS) {
                                    val ch = clockT2Str[charIdx]
                                    val font = MatrixFont.miniFontData[ch]
                                        ?: MatrixFont.miniFontData[ch.uppercaseChar()]
                                        ?: MatrixFont.miniFontData[' ']!!
                                    if (rowInT2 < font.size && ((font[rowInT2] shr (2 - colInChar)) and 1) == 1) isOn = true
                                }
                            }
                        }

                        if (clockT3Active && !isOn) {
                            if (r in clockT3HourRowStart until clockT3HourRowStart + LARGE_FONT_ROWS) {
                                val rowInMain = r - clockT3HourRowStart
                                val charColFloat = c - hourStartColT3
                                if (charColFloat >= 0f) {
                                    val charCol = charColFloat.toInt()
                                    val charIdx = charCol / LARGE_CHAR_WIDTH
                                    val colInChar = charCol % LARGE_CHAR_WIDTH
                                    if (charIdx in hourStrT3.indices && colInChar in 0 until LARGE_GLYPH_COLS) {
                                        val ch = hourStrT3[charIdx]
                                        val font = MatrixFont.largeFontData[ch] ?: MatrixFont.largeFontData[' ']!!
                                        if (rowInMain < font.size && (((font[rowInMain] and 0xFF) shr (7 - colInChar)) and 1) == 1) {
                                            isOn = true
                                        }
                                    }
                                }
                            }
                            if (!isOn && c >= minuteAreaStartColT3 && r in clockT3MiniRowStart until clockT3MiniRowStart + MINI_GLYPH_ROWS) {
                                val rowInMini = r - clockT3MiniRowStart
                                val stripCol = c + scrollOffsetT3
                                val numIdx = (stripCol / colsPerMinuteNumber).toInt()
                                val colInNum = ((stripCol.toInt() % colsPerMinuteNumber) + colsPerMinuteNumber) % colsPerMinuteNumber
                                val (digitIdx, colInDigit) = when {
                                    colInNum in 0..2 -> Pair(0, colInNum)
                                    colInNum in 4..6 -> Pair(1, colInNum - 4)
                                    else -> Pair(-1, -1)
                                }
                                if (digitIdx >= 0 && colInDigit in 0 until MINI_GLYPH_COLS) {
                                    val minuteValue = ((minuteT3 - 20 + numIdx) % 60 + 60) % 60
                                    val numStr = "%02d".format(minuteValue)
                                    val ch = numStr.getOrElse(digitIdx) { ' ' }
                                    val font = MatrixFont.miniFontData[ch]
                                        ?: MatrixFont.miniFontData[ch.uppercaseChar()]
                                        ?: MatrixFont.miniFontData[' ']!!
                                    if (rowInMini < font.size && ((font[rowInMini] shr (2 - colInDigit)) and 1) == 1) {
                                        isOn = true
                                        t3MinuteOrLine = true
                                        val dist = kotlin.math.abs(c - lineCenterColT3).toFloat()
                                        t3Alpha = (1f - dist / 22f).coerceIn(0.04f, 1f)
                                    }
                                }
                            }
                            if (!isOn && c >= minuteAreaStartColT3 && (r == clockT3MiniRowStart - 2 || r == clockT3MiniRowStart + MINI_GLYPH_ROWS + 1)) {
                                val dist = kotlin.math.abs(c - lineCenterColT3).toFloat()
                                val trackAlpha = (1f - dist / 22f).coerceIn(0.04f, 1f)
                                if (trackAlpha > 0.02f) {
                                    isOn = true
                                    t3MinuteOrLine = true
                                    t3Alpha = trackAlpha
                                }
                            }
                            if (!isOn && c == lineCenterColT3 && r in lineRowStartT3 until lineRowEndT3 && r !in lineGapStartT3..lineGapEndT3) {
                                isOn = true
                                t3MinuteOrLine = true
                                t3IsLine = true
                            }
                        }

                        if (isBound && showDate && !isOn && rowInMiniFont in 0 until MINI_GLYPH_ROWS) {
                            val charColDate = c - dateStartCol
                            if (charColDate >= 0) {
                                val charIdx = (charColDate / MINI_CHAR_WIDTH).toInt()
                                val colInChar = (charColDate % MINI_CHAR_WIDTH).toInt()
                                if (charIdx in dateInfo.indices && colInChar in 0 until MINI_GLYPH_COLS) {
                                    val ch = dateInfo[charIdx]
                                    val font = MatrixFont.miniFontData[ch]
                                        ?: MatrixFont.miniFontData[ch.uppercaseChar()]
                                        ?: MatrixFont.miniFontData[' ']!!
                                    if (rowInMiniFont < font.size && ((font[rowInMiniFont] shr (2 - colInChar)) and 1) == 1) isOn = true
                                }
                            }
                        }
                        if (showAmPm && !isOn && rowInMiniFont in 0 until MINI_GLYPH_ROWS) {
                            val charColAmPm = c - amPmStartCol
                            if (charColAmPm >= 0) {
                                val charIdx = (charColAmPm / MINI_CHAR_WIDTH).toInt()
                                val colInChar = (charColAmPm % MINI_CHAR_WIDTH).toInt()
                                if (charIdx in amPm.indices && colInChar in 0 until MINI_GLYPH_COLS) {
                                    val ch = amPm[charIdx]
                                    val font = MatrixFont.miniFontData[ch]
                                        ?: MatrixFont.miniFontData[ch.uppercaseChar()]
                                        ?: MatrixFont.miniFontData[' ']!!
                                    if (rowInMiniFont < font.size && ((font[rowInMiniFont] shr (2 - colInChar)) and 1) == 1) isOn = true
                                }
                            }
                        }

                        if (isBound && showProgress) {
                            if (style == 3 && matrixStartColStyle3 >= 0 && r in matrixStartRowStyle3 until matrixStartRowStyle3 + 32 && c in matrixStartColStyle3 until matrixStartColStyle3 + 32) {
                                val localRow = r - matrixStartRowStyle3
                                val localCol = c - matrixStartColStyle3
                                val localIndex = localRow * 32 + localCol
                                if (localIndex < filledCountStyle3) isStyle3MatrixBlack = true else isStyle3MatrixTheme = true
                            } else if (style == 2) {
                                // Background: theme color; black spreads left-to-right (elapsed)
                                val progressCol = (matrixWidth * (1f - progress)).toInt().coerceIn(0, matrixWidth)
                                val blackSpread = c < progressCol
                                if (blackSpread) isProgressTrack = true else isProgressBar = true
                            } else if (style == 1) {
                                if (r == TEXT_PROGRESS_ROW_TOP || r == TEXT_PROGRESS_ROW_BOTTOM) {
                                    val progressCol = (matrixWidth * progress).toInt()
                                    val filled = when (r) {
                                        TEXT_PROGRESS_ROW_TOP -> c >= matrixWidth - progressCol
                                        else -> c < progressCol
                                    }
                                    if (filled) {
                                        isOn = true
                                        isProgressBar = true
                                    } else {
                                        isProgressTrack = true
                                    }
                                }
                            }
                        }
                        
                        if (isBound && showProgress && !isOn && inStyle3Left && rowInCountdown in 0 until MINI_GLYPH_ROWS) {
                            val charColCountdown = c - countdownStartCol
                            if (charColCountdown >= 0) {
                                val charIdx = (charColCountdown / MINI_CHAR_WIDTH).toInt()
                                val colInChar = (charColCountdown % MINI_CHAR_WIDTH).toInt()
                                if (charIdx in countdownStr.indices && colInChar in 0 until MINI_GLYPH_COLS) {
                                    val ch = countdownStr[charIdx]
                                    val font = MatrixFont.miniFontData[ch]
                                        ?: MatrixFont.miniFontData[ch.uppercaseChar()]
                                        ?: MatrixFont.miniFontData[' ']!!
                                    if (rowInCountdown < font.size && ((font[rowInCountdown] shr (2 - colInChar)) and 1) == 1) isOn = true
                                }
                            }
                        }

                        if (showCycle && !isOn && r in cycleRowStart until cycleRowStart + MINI_GLYPH_ROWS) {
                            val cycleStrStartCol = if (style == 3 && matrixStartColStyle3 >= 0) cycleStrColStartStyle3 else cycleStrColStart
                            val cycleStarStartCol = if (style == 3 && matrixStartColStyle3 >= 0) cycleStarColStartStyle3 else cycleStarColStart
                            if (cycleShowStar) {
                                if (c in cycleStarStartCol until cycleStarStartCol + STAR_SIZE) {
                                    val localRow = r - cycleRowStart
                                    val localCol = c - cycleStarStartCol
                                    if (MatrixIcons.isMiniOn(MatrixIcons.ICON_MINI_STAR_ROWS, localRow, localCol)) isOn = true
                                }
                            } else if (cycleStr.isNotEmpty()) {
                                val rowInCycle = r - cycleRowStart
                                val charColFloat = c - cycleStrStartCol
                                if (charColFloat >= 0f) {
                                    val charCol = charColFloat.toInt()
                                    val charIdx = charCol / MINI_CHAR_WIDTH
                                    val colInChar = charCol % MINI_CHAR_WIDTH
                                    if (charIdx in cycleStr.indices && colInChar in 0 until MINI_GLYPH_COLS) {
                                        val ch = cycleStr[charIdx]
                                        val font = MatrixFont.miniFontData[ch]
                                            ?: MatrixFont.miniFontData[ch.uppercaseChar()]
                                            ?: MatrixFont.miniFontData[' ']!!
                                        if (rowInCycle < font.size && ((font[rowInCycle] shr (2 - colInChar)) and 1) == 1) isOn = true
                                    }
                                }
                            }
                        }

                        val finalColor = when {
                            isOn -> when {
                                progressColStyle2 >= 0 -> if (c < progressColStyle2) color else Color.White
                                clockT3Active && t3MinuteOrLine && t3IsLine -> Color.White
                                clockT3Active && t3MinuteOrLine -> Color.White.copy(alpha = t3Alpha)
                                clockT3Active && t3Alpha < 1f -> contentColor.copy(alpha = t3Alpha)
                                drawUnboundWithMiniFont -> contentColor
                                isBound && (rowInMiniFont in 0 until MINI_GLYPH_ROWS || rowInCountdown in 0 until MINI_GLYPH_ROWS) -> Color(0xFF6B7280)
                                else -> contentColor
                            }
                            isStyle3MatrixBlack -> OFF_DOT_COLOR
                            isStyle3MatrixTheme -> color
                            progressColStyle2 >= 0 -> if (c < progressColStyle2) OFF_DOT_COLOR else color
                            isProgressBar -> color
                            isProgressTrack -> PROGRESS_TRACK_COLOR
                            else -> OFF_DOT_COLOR
                        }

                        drawCircle(
                            color = finalColor,
                            radius = (dotSize / 2f) * 0.82f,
                            center = Offset(startX + c * dotSize + dotSize / 2f, r * dotSize + dotSize / 2f)
                        )
                    }
                }
            }
        }

        LaunchedEffect(text, isBound, mode, countdownStyle, matrixWidthForScroll) {
            if (isBound && mode == DisplayMode.TEXT) {
                scrollOffset = 0f
                val leftWidthS3 = (matrixWidthForScroll - 33).coerceAtLeast(0)
                val scrollPeriod = when (countdownStyle.coerceIn(1, 3)) {
                    3 -> leftWidthS3 + text.length * MEDIUM_CHAR_WIDTH + 8
                    else -> matrixWidthForScroll + text.length * LARGE_CHAR_WIDTH + 8
                }
                while (true) {
                    delay(120)
                    scrollOffset += 1f
                    if (scrollOffset > scrollPeriod) scrollOffset = 0f
                }
            }
        }
    }
}
