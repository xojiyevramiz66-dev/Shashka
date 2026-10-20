
package com.example.shashka

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

// ============================================================
// SHASHKA ULTRA HD
// Темы:
// gold = Obsidian Gold
// neon = Cyber Neon
// ruby = Ruby Diamond
// ============================================================

private var activeTheme by mutableStateOf("gold")

private val BackgroundColor: Color
    get() = when (activeTheme) {
        "neon" -> Color(0xFF080D1D)
        "ruby" -> Color(0xFF18080D)
        else -> Color(0xFF141414)
    }

private val PanelColor: Color
    get() = when (activeTheme) {
        "neon" -> Color(0xFF111D35)
        "ruby" -> Color(0xFF301019)
        else -> Color(0xFF242424)
    }

private val GoldColor: Color
    get() = when (activeTheme) {
        "neon" -> Color(0xFF00E5FF)
        "ruby" -> Color(0xFFFF3155)
        else -> Color(0xFFF4C430)
    }

private val TextColor: Color
    get() = when (activeTheme) {
        "neon" -> Color(0xFFD8F7FF)
        "ruby" -> Color(0xFFFFE4EA)
        else -> Color(0xFFE8DCC6)
    }

private val ActivePanelColor: Color
    get() = when (activeTheme) {
        "neon" -> Color(0xFF122A43)
        "ruby" -> Color(0xFF421523)
        else -> Color(0xFF29251B)
    }

private val BorderColor: Color
    get() = when (activeTheme) {
        "neon" -> Color(0xFF087C9B)
        "ruby" -> Color(0xFF8C2940)
        else -> Color(0xFF414141)
    }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(
            "game_prefs",
            Context.MODE_PRIVATE
        )
        activeTheme = prefs.getString("board_theme", "gold") ?: "gold"

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundColor
                ) {
                    CheckersScreen()
                }
            }
        }
    }
}

fun triggerVibration(context: Context, enabled: Boolean) {
    if (!enabled) return

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(
                Context.VIBRATOR_MANAGER_SERVICE
            ) as VibratorManager

            manager.defaultVibrator.vibrate(
                VibrationEffect.createOneShot(
                    30L,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(
                Context.VIBRATOR_SERVICE
            ) as Vibrator

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        30L,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(30L)
            }
        }
    } catch (_: Exception) {
        // Игра работает и без вибрации.
    }
}

private fun playGameSound(success: Boolean) {
    var tone: ToneGenerator? = null

    try {
        tone = ToneGenerator(AudioManager.STREAM_MUSIC, 55)

        val soundType = if (success) {
            ToneGenerator.TONE_PROP_BEEP2
        } else {
            ToneGenerator.TONE_PROP_NACK
        }

        tone.startTone(soundType, if (success) 70 else 100)

        val activeTone = tone

        Handler(Looper.getMainLooper()).postDelayed({
            try {
                activeTone.release()
            } catch (_: Exception) {
                // Ресурс уже освобождён.
            }
        }, 300L)
    } catch (_: Exception) {
        try {
            tone?.release()
        } catch (_: Exception) {
            // Ошибка звука не должна останавливать игру.
        }
    }
}

private fun formatClock(seconds: Int): String {
    val safeSeconds = seconds.coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val remainingSeconds = safeSeconds % 60

    return "%02d:%02d".format(minutes, remainingSeconds)
}

@Composable
fun CheckersScreen(
    viewModel: CheckersViewModel = viewModel()
) {
    val context = LocalContext.current

    val board = viewModel.boardState
    val selected = viewModel.selectedPosition
    val currentPlayer = viewModel.currentPlayer
    val isAiThinking = viewModel.isAiThinking
    val gameMessage = viewModel.gameMessage

    val prefs = remember {
        context.getSharedPreferences(
            "game_prefs",
            Context.MODE_PRIVATE
        )
    }

    var currentTheme by remember {
        mutableStateOf(
            prefs.getString("board_theme", "gold") ?: "gold"
        )
    }

    LaunchedEffect(currentTheme) {
        activeTheme = currentTheme

        prefs.edit()
            .putString("board_theme", currentTheme)
            .apply()
    }

    var showSettingsDialog by remember {
        mutableStateOf(false)
    }

    var soundEnabled by remember {
        mutableStateOf(prefs.getBoolean("sound_enabled", true))
    }

    var vibrationEnabled by remember {
        mutableStateOf(prefs.getBoolean("vibration_enabled", true))
    }

    var whiteSeconds by remember {
        mutableIntStateOf(prefs.getInt("white_seconds", 600))
    }

    var blackSeconds by remember {
        mutableIntStateOf(prefs.getInt("black_seconds", 600))
    }

    LaunchedEffect(Unit) {
        activeTheme = currentTheme

        val savedDifficulty = prefs.getString(
            "ai_difficulty",
            "Непобедимый"
        )

        if (savedDifficulty != null) {
            viewModel.aiDifficulty = savedDifficulty
        }
    }

    fun restartWithClocks() {
        viewModel.restartGame()
        whiteSeconds = 600
        blackSeconds = 600

        prefs.edit()
            .putInt("white_seconds", whiteSeconds)
            .putInt("black_seconds", blackSeconds)
            .apply()
    }

    LaunchedEffect(currentPlayer, gameMessage) {
        while (gameMessage == null) {
            delay(1000)

            if (currentPlayer == PlayerColor.WHITE) {
                if (whiteSeconds <= 1) {
                    whiteSeconds = 0
                    viewModel.onTimeExpired(PlayerColor.WHITE)
                    break
                } else {
                    whiteSeconds -= 1
                }
            } else {
                if (blackSeconds <= 1) {
                    blackSeconds = 0
                    viewModel.onTimeExpired(PlayerColor.BLACK)
                    break
                } else {
                    blackSeconds -= 1
                }
            }

            prefs.edit()
                .putInt("white_seconds", whiteSeconds)
                .putInt("black_seconds", blackSeconds)
                .apply()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PanelButton(
                text = "←",
                onClick = {
                    triggerVibration(context, vibrationEnabled)
                    (context as? Activity)?.finish()
                }
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ШАШКИ",
                    color = GoldColor,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp
                )

                Text(
                    text = when {
                        gameMessage != null -> "Игра окончена"
                        isAiThinking -> "ИИ думает…"
                        currentPlayer == PlayerColor.WHITE -> "Ваш ход"
                        else -> "Ход противника"
                    },
                    color = TextColor.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }

            PanelButton(
                text = "⚙",
                onClick = {
                    triggerVibration(context, vibrationEnabled)
                    showSettingsDialog = true
                }
            )
        }

        PlayerPanel(
            title = "ПРОТИВНИК",
            subtitle = "ИИ • ${viewModel.aiDifficulty}",
            clock = formatClock(blackSeconds),
            isActive = currentPlayer == PlayerColor.BLACK &&
                gameMessage == null,
            isWhite = false
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true)
                .aspectRatio(1f)
                .shadow(18.dp, RoundedCornerShape(12.dp))
                .background(
                    Brush.verticalGradient(
                        when (activeTheme) {
                            "neon" -> listOf(
                                Color(0xFF00E5FF),
                                Color(0xFF102A50),
                                Color(0xFF080D1D)
                            )

                            "ruby" -> listOf(
                                Color(0xFFFF3155),
                                Color(0xFF601528),
                                Color(0xFF18080D)
                            )

                            else -> listOf(
                                Color(0xFF67472B),
                                Color(0xFF25170D)
                            )
                        }
                    ),
                    RoundedCornerShape(12.dp)
                )
                .border(
                    4.dp,
                    GoldColor,
                    RoundedCornerShape(12.dp)
                )
                .padding(3.dp)
        ) {
            Column(Modifier.fillMaxSize()) {
                for (row in 0..7) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        for (col in 0..7) {
                            val cell = board[row][col]

                            val isSelected =
                                selected?.row == row &&
                                    selected.col == col

                            val isDarkCell = (row + col) % 2 != 0

                            val squareBrush = when {
                                isSelected -> Brush.linearGradient(
                                    listOf(
                                        GoldColor.copy(alpha = 0.85f),
                                        GoldColor
                                    )
                                )

                                cell.isHighlighted -> Brush.linearGradient(
                                    listOf(
                                        Color(0xFF80DEEA),
                                        Color(0xFF0097A7)
                                    )
                                )

                                isDarkCell -> Brush.verticalGradient(
                                    when (activeTheme) {
                                        "neon" -> listOf(
                                            Color(0xFF172E62),
                                            Color(0xFF090F27)
                                        )

                                        "ruby" -> listOf(
                                            Color(0xFF8B1733),
                                            Color(0xFF350B19)
                                        )

                                        else -> listOf(
                                            Color(0xFF593820),
                                            Color(0xFF321D10)
                                        )
                                    }
                                )

                                else -> Brush.verticalGradient(
                                    when (activeTheme) {
                                        "neon" -> listOf(
                                            Color(0xFFD8F7FF),
                                            Color(0xFF7899C8)
                                        )

                                        "ruby" -> listOf(
                                            Color(0xFFFFE5EA),
                                            Color(0xFFD7A4B0)
                                        )

                                        else -> listOf(
                                            Color(0xFFF2E5D5),
                                            Color(0xFFD4BDA2)
                                        )
                                    }
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(squareBrush)
                                    .clickable(
                                        enabled = gameMessage == null &&
                                            !isAiThinking
                                    ) {
                                        triggerVibration(
                                            context,
                                            vibrationEnabled
                                        )

                                        viewModel.onCellClicked(
                                            cell.position
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                cell.piece?.let { piece ->
                                    AnimatedPieceWrapper(piece = piece)
                                }

                                if (
                                    cell.isHighlighted &&
                                    cell.piece == null
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize(0.23f)
                                            .background(
                                                Color.White.copy(alpha = 0.65f),
                                                CircleShape
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isAiThinking) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = PanelColor.copy(alpha = 0.95f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "ИИ думает…",
                            color = GoldColor,
                            modifier = Modifier.padding(
                                horizontal = 18.dp,
                                vertical = 12.dp
                            ),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        PlayerPanel(
            title = "ВЫ",
            subtitle = "Белые шашки",
            clock = formatClock(whiteSeconds),
            isActive = currentPlayer == PlayerColor.WHITE &&
                gameMessage == null,
            isWhite = true
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomIconButton(
                icon = "↩",
                caption = "Назад",
                onClick = {
                    triggerVibration(context, vibrationEnabled)
                    viewModel.undo()
                }
            )

            BottomIconButton(
                icon = "⟳",
                caption = "Заново",
                onClick = {
                    triggerVibration(context, vibrationEnabled)
                    restartWithClocks()
                }
            )

            BottomIconButton(
                icon = "💡",
                caption = "Подсказка",
                onClick = {
                    triggerVibration(context, vibrationEnabled)
                    val found = viewModel.showHint()

                    if (soundEnabled) {
                        playGameSound(found)
                    }
                }
            )

            BottomIconButton(
                icon = "☰",
                caption = "Меню",
                onClick = {
                    triggerVibration(context, vibrationEnabled)
                    showSettingsDialog = true
                }
            )
        }
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = {
                showSettingsDialog = false
            },
            containerColor = PanelColor,
            titleContentColor = GoldColor,
            textContentColor = TextColor,
            title = {
                Text(
                    text = "⚙ Настройки",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "ТЕМА ОФОРМЛЕНИЯ",
                        color = GoldColor,
                        fontWeight = FontWeight.Bold
                    )

                    listOf(
                        "gold" to "Obsidian Gold",
                        "neon" to "Cyber Neon",
                        "ruby" to "Ruby Diamond"
                    ).forEach { (themeId, themeName) ->
                        val selectedTheme = currentTheme == themeId

                        val themeAccent = when (themeId) {
                            "neon" -> Color(0xFF00E5FF)
                            "ruby" -> Color(0xFFFF3155)
                            else -> Color(0xFFF4C430)
                        }

                        val themeButtonColor = when (themeId) {
                            "neon" -> Color(0xFF12385A)
                            "ruby" -> Color(0xFF67182C)
                            else -> Color(0xFF49371B)
                        }

                        Button(
                            onClick = {
                                currentTheme = themeId
                                activeTheme = themeId

                                prefs.edit()
                                    .putString("board_theme", themeId)
                                    .apply()

                                triggerVibration(
                                    context,
                                    vibrationEnabled
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = themeButtonColor,
                                contentColor = Color.White
                            ),
                            border = if (selectedTheme) {
                                androidx.compose.foundation.BorderStroke(
                                    2.dp,
                                    themeAccent
                                )
                            } else {
                                null
                            }
                        ) {
                            Text(
                                text = if (selectedTheme) {
                                    "✓ $themeName"
                                } else {
                                    themeName
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider(color = BorderColor)

                    Text(
                        text = "Сложность ИИ",
                        color = GoldColor,
                        fontWeight = FontWeight.SemiBold
                    )

                    listOf(
                        listOf("Лёгкий", "Средний"),
                        listOf("Сильный", "Непобедимый")
                    ).forEach { rowLevels ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowLevels.forEach { level ->
                                val isSelected =
                                    viewModel.aiDifficulty == level

                                Button(
                                    onClick = {
                                        viewModel.aiDifficulty = level

                                        prefs.edit()
                                            .putString("ai_difficulty", level)
                                            .apply()

                                        triggerVibration(
                                            context,
                                            vibrationEnabled
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) {
                                            GoldColor
                                        } else {
                                            PanelColor
                                        },
                                        contentColor = if (isSelected) {
                                            Color(0xFF141414)
                                        } else {
                                            TextColor
                                        }
                                    ),
                                    contentPadding = PaddingValues(
                                        horizontal = 4.dp,
                                        vertical = 8.dp
                                    )
                                ) {
                                    Text(
                                        text = level,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = BorderColor)

                    SettingSwitch(
                        title = "Звуки",
                        checked = soundEnabled,
                        onCheckedChange = {
                            soundEnabled = it

                            prefs.edit()
                                .putBoolean("sound_enabled", it)
                                .apply()

                            if (it) playGameSound(true)
                        }
                    )

                    SettingSwitch(
                        title = "Вибрация",
                        checked = vibrationEnabled,
                        onCheckedChange = {
                            vibrationEnabled = it

                            prefs.edit()
                                .putBoolean("vibration_enabled", it)
                                .apply()

                            triggerVibration(context, it)
                        }
                    )

                    Text(
                        text = "Контроль времени: 10 минут на каждого игрока.",
                        color = TextColor.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )

                    OutlinedButton(
                        onClick = {
                            whiteSeconds = 600
                            blackSeconds = 600

                            prefs.edit()
                                .putInt("white_seconds", 600)
                                .putInt("black_seconds", 600)
                                .apply()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Сбросить часы")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSettingsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldColor,
                        contentColor = Color(0xFF141414)
                    )
                ) {
                    Text("Готово", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    gameMessage?.let { message ->
        AlertDialog(
            onDismissRequest = {},
            containerColor = PanelColor,
            titleContentColor = GoldColor,
            textContentColor = TextColor,
            title = {
                Text(
                    text = "🏁 Игра окончена",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(message)
            },
            confirmButton = {
                Button(
                    onClick = {
                        restartWithClocks()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldColor,
                        contentColor = Color(0xFF141414)
                    )
                ) {
                    Text("Новая игра", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun PanelButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .shadow(5.dp, RoundedCornerShape(12.dp))
            .background(PanelColor, RoundedCornerShape(12.dp))
            .border(
                1.dp,
                BorderColor,
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = GoldColor,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PlayerPanel(
    title: String,
    subtitle: String,
    clock: String,
    isActive: Boolean,
    isWhite: Boolean
) {
    val whitePanelBrush = when (activeTheme) {
        "neon" -> Brush.radialGradient(
            listOf(
                Color(0xFFFFFFFF),
                Color(0xFFB7FFFF),
                Color(0xFF43C6D5)
            )
        )
        "ruby" -> Brush.radialGradient(
            listOf(
                Color.White,
                Color(0xFFFFDCE3),
                Color(0xFFDF8B9B)
            )
        )
        else -> Brush.radialGradient(
            listOf(
                Color.White,
                Color(0xFFFFF0C9),
                Color(0xFFB18A58)
            )
        )
    }

    val blackPanelBrush = when (activeTheme) {
        "neon" -> Brush.radialGradient(
            listOf(
                Color(0xFF00E5FF),
                Color(0xFF163C62),
                Color(0xFF030918)
            )
        )
        "ruby" -> Brush.radialGradient(
            listOf(
                Color(0xFFFF3155),
                Color(0xFF67182C),
                Color(0xFF16040A)
            )
        )
        else -> Brush.radialGradient(
            listOf(
                Color(0xFF777777),
                Color(0xFF262626),
                Color.Black
            )
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isActive) ActivePanelColor
                else PanelColor.copy(alpha = 0.75f),
                RoundedCornerShape(12.dp)
            )
            .border(
                width = if (isActive) 1.dp else 0.5.dp,
                color = if (isActive) GoldColor else BorderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .shadow(4.dp, CircleShape)
                    .background(
                        if (isWhite) whitePanelBrush else blackPanelBrush,
                        CircleShape
                    )
                    .border(1.dp, GoldColor, CircleShape)
            )

            Column {
                Text(
                    text = title,
                    color = if (isActive) GoldColor else TextColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = subtitle,
                    color = TextColor.copy(alpha = 0.65f),
                    fontSize = 10.sp
                )
            }
        }

        Text(
            text = clock,
            color = if (isActive) GoldColor else TextColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = TextColor,
            fontSize = 15.sp
        )

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = GoldColor,
                checkedTrackColor = ActivePanelColor
            )
        )
    }
}

@Composable
fun BottomIconButton(
    icon: String,
    caption: String = "",
    onClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .shadow(6.dp, RoundedCornerShape(15.dp))
                .background(PanelColor, RoundedCornerShape(15.dp))
                .border(
                    1.dp,
                    BorderColor,
                    RoundedCornerShape(15.dp)
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon,
                color = GoldColor,
                fontSize = 22.sp
            )
        }

        if (caption.isNotEmpty()) {
            Text(
                text = caption,
                color = TextColor.copy(alpha = 0.65f),
                fontSize = 9.sp
            )
        }
    }
}

@Composable
fun AnimatedPieceWrapper(piece: CheckerPiece) {
    val scale = remember { Animatable(0.65f) }

    LaunchedEffect(piece) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 180,
                easing = FastOutSlowInEasing
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        UltraHD3DPieceView(
            piece = piece,
            scale = scale.value
        )
    }
}

/**
 * Трёхмерная шашка с цветовыми схемами для всех тем.
 *
 * GOLD: белое золото и чёрный металл.
 * NEON: ледяной голубой и тёмно-синий неон.
 * RUBY: жемчужно-розовый и рубиново-чёрный.
 *
 * Меняются боковая стенка, лицевая поверхность,
 * металлические кольца, блики и корона дамки.
 */
@Composable
fun UltraHD3DPieceView(
    piece: CheckerPiece,
    scale: Float = 1f
) {
    val isWhite = piece.color == PlayerColor.WHITE

    val whiteSideColors = when (activeTheme) {
        "neon" -> listOf(
            Color(0xFFDAFFFF),
            Color(0xFF65DDEB),
            Color(0xFF2588A5),
            Color(0xFF12425D)
        )
        "ruby" -> listOf(
            Color(0xFFFFE8ED),
            Color(0xFFEFADBC),
            Color(0xFFBA647D),
            Color(0xFF743448)
        )
        else -> listOf(
            Color(0xFFEAD8B9),
            Color(0xFFB18A58),
            Color(0xFF79542E),
            Color(0xFF4D341D)
        )
    }

    val blackSideColors = when (activeTheme) {
        "neon" -> listOf(
            Color(0xFF39F4FF),
            Color(0xFF164D7B),
            Color(0xFF092644),
            Color(0xFF020713)
        )
        "ruby" -> listOf(
            Color(0xFFFF3155),
            Color(0xFF9A1838),
            Color(0xFF4C0D20),
            Color(0xFF16040A)
        )
        else -> listOf(
            Color(0xFF777777),
            Color(0xFF393939),
            Color(0xFF151515),
            Color(0xFF030303)
        )
    }

    val whiteFaceColors = when (activeTheme) {
        "neon" -> listOf(
            Color.White,
            Color(0xFFD8FFFF),
            Color(0xFF8CEEFF),
            Color(0xFF368FAF)
        )
        "ruby" -> listOf(
            Color.White,
            Color(0xFFFFE7ED),
            Color(0xFFF1B5C4),
            Color(0xFFBF7188)
        )
        else -> listOf(
            Color.White,
            Color(0xFFFFF4DF),
            Color(0xFFE5CDA7),
            Color(0xFFB18B59)
        )
    }

    val blackFaceColors = when (activeTheme) {
        "neon" -> listOf(
            Color(0xFF6CFFFF),
            Color(0xFF245D8D),
            Color(0xFF102B50),
            Color(0xFF020916)
        )
        "ruby" -> listOf(
            Color(0xFFFF718A),
            Color(0xFF8F2440),
            Color(0xFF42101F),
            Color(0xFF080205)
        )
        else -> listOf(
            Color(0xFF858585),
            Color(0xFF4A4A4A),
            Color(0xFF242424),
            Color(0xFF080808)
        )
    }

    val whiteRimColors = when (activeTheme) {
        "neon" -> listOf(
            Color.White,
            Color(0xFF00E5FF),
            Color(0xFF65CFE8)
        )
        "ruby" -> listOf(
            Color.White,
            Color(0xFFFF3155),
            Color(0xFFDA8399)
        )
        else -> listOf(
            Color(0xFFFFF8E9),
            Color(0xFFB99A6D),
            Color(0xFFFFF1D2)
        )
    }

    val blackRimColors = when (activeTheme) {
        "neon" -> listOf(
            Color(0xFF80FFFF),
            Color(0xFF00A9D4),
            Color(0xFF164A76)
        )
        "ruby" -> listOf(
            Color(0xFFFF879B),
            Color(0xFFE12248),
            Color(0xFF71162E)
        )
        else -> listOf(
            Color(0xFFAAAAAA),
            Color(0xFF444444),
            Color(0xFF777777)
        )
    }

    val pieceAccent = when (activeTheme) {
        "neon" -> Color(0xFF00E5FF)
        "ruby" -> Color(0xFFFF3155)
        else -> Color(0xFFFFD166)
    }

    val sideColors = if (isWhite) whiteSideColors else blackSideColors
    val faceColors = if (isWhite) whiteFaceColors else blackFaceColors
    val rimColors = if (isWhite) whiteRimColors else blackRimColors

    Box(
        modifier = Modifier.fillMaxSize(
            scale.coerceIn(0.5f, 1f)
        ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension * 0.46f
            val center = Offset(
                size.width / 2f,
                size.height / 2f
            )

            // Тень
            drawOval(
                color = Color.Black.copy(alpha = 0.52f),
                topLeft = Offset(
                    center.x - radius * 0.91f,
                    center.y - radius * 0.02f
                ),
                size = Size(
                    radius * 1.82f,
                    radius * 1.42f
                )
            )

            // Боковая стенка с оттенками текущей темы
            drawOval(
                brush = Brush.verticalGradient(
                    colors = sideColors,
                    startY = center.y - radius * 0.1f,
                    endY = center.y + radius * 0.62f
                ),
                topLeft = Offset(
                    center.x - radius * 0.94f,
                    center.y - radius * 0.02f
                ),
                size = Size(
                    radius * 1.88f,
                    radius * 0.65f
                )
            )

            // Металлический нижний ободок
            drawCircle(
                brush = Brush.verticalGradient(
                    colors = rimColors,
                    startY = center.y - radius,
                    endY = center.y + radius
                ),
                radius = radius * 0.98f,
                center = center - Offset(
                    0f,
                    radius * 0.10f
                )
            )

            val faceCenter = center - Offset(
                0f,
                radius * 0.20f
            )

            // Верхняя поверхность
            drawCircle(
                brush = Brush.radialGradient(
                    colors = faceColors,
                    center = faceCenter - Offset(
                        radius * 0.32f,
                        radius * 0.40f
                    ),
                    radius = radius * 1.65f
                ),
                radius = radius * 0.94f,
                center = faceCenter
            )

            // Внешний металлический кант
            drawCircle(
                brush = Brush.linearGradient(
                    colors = rimColors,
                    start = Offset(
                        faceCenter.x - radius,
                        faceCenter.y - radius
                    ),
                    end = Offset(
                        faceCenter.x + radius,
                        faceCenter.y + radius
                    )
                ),
                radius = radius * 0.88f,
                center = faceCenter,
                style = Stroke(width = radius * 0.075f)
            )

            // Световой ободок
            drawCircle(
                color = pieceAccent.copy(
                    alpha = if (isWhite) 0.58f else 0.75f
                ),
                radius = radius * 0.79f,
                center = faceCenter,
                style = Stroke(width = radius * 0.025f)
            )

            // Отражение на поверхности
            drawCircle(
                color = Color.White.copy(
                    alpha = if (isWhite) 0.72f else 0.34f
                ),
                radius = radius * 0.72f,
                center = faceCenter - Offset(
                    0f,
                    radius * 0.025f
                ),
                style = Stroke(width = radius * 0.035f)
            )

            // Внутреннее кольцо
            drawCircle(
                color = if (isWhite) {
                    pieceAccent.copy(alpha = 0.72f)
                } else {
                    pieceAccent.copy(alpha = 0.95f)
                },
                radius = radius * 0.57f,
                center = faceCenter,
                style = Stroke(width = radius * 0.075f)
            )

            // Центральная поверхность
            drawCircle(
                brush = Brush.radialGradient(
                    colors = faceColors,
                    center = faceCenter - Offset(
                        radius * 0.15f,
                        radius * 0.18f
                    ),
                    radius = radius * 0.75f
                ),
                radius = radius * 0.48f,
                center = faceCenter
            )

            // Блик
            drawOval(
                color = Color.White.copy(
                    alpha = if (isWhite) 0.48f else 0.19f
                ),
                topLeft = Offset(
                    faceCenter.x - radius * 0.53f,
                    faceCenter.y - radius * 0.66f
                ),
                size = Size(
                    radius * 0.62f,
                    radius * 0.13f
                )
            )

            // Дополнительный цветной блик для неоновой темы
            if (activeTheme == "neon") {
                drawCircle(
                    color = pieceAccent.copy(alpha = 0.7f),
                    radius = radius * 0.92f,
                    center = faceCenter,
                    style = Stroke(width = radius * 0.025f)
                )
            }

            // Корона дамки
            if (piece.type == PieceType.KING) {
                val crownCenter = faceCenter - Offset(
                    0f,
                    radius * 0.015f
                )

                val crownWidth = radius * 0.95f
                val crownHeight = radius * 0.65f

                val crownColors = when (activeTheme) {
                    "neon" -> listOf(
                        Color.White,
                        Color(0xFF00F5FF),
                        Color(0xFF0077FF),
                        Color(0xFF06365E)
                    )
                    "ruby" -> listOf(
                        Color(0xFFFFF0F3),
                        Color(0xFFFF3155),
                        Color(0xFFB70F38),
                        Color(0xFF58091E)
                    )
                    else -> listOf(
                        Color(0xFFFFFFE0),
                        Color(0xFFFFD54F),
                        Color(0xFFD49A18),
                        Color(0xFF80520A)
                    )
                }

                val crownPath = Path().apply {
                    moveTo(
                        crownCenter.x - crownWidth * 0.50f,
                        crownCenter.y + crownHeight * 0.38f
                    )
                    lineTo(
                        crownCenter.x - crownWidth * 0.43f,
                        crownCenter.y - crownHeight * 0.28f
                    )
                    lineTo(
                        crownCenter.x - crownWidth * 0.16f,
                        crownCenter.y + crownHeight * 0.02f
                    )
                    lineTo(
                        crownCenter.x,
                        crownCenter.y - crownHeight * 0.50f
                    )
                    lineTo(
                        crownCenter.x + crownWidth * 0.16f,
                        crownCenter.y + crownHeight * 0.02f
                    )
                    lineTo(
                        crownCenter.x + crownWidth * 0.43f,
                        crownCenter.y - crownHeight * 0.28f
                    )
                    lineTo(
                        crownCenter.x + crownWidth * 0.50f,
                        crownCenter.y + crownHeight * 0.38f
                    )
                    close()
                }

                // Тень короны
                drawPath(
                    path = crownPath,
                    color = Color.Black.copy(alpha = 0.65f)
                )

                // Цветная металлическая корона
                drawPath(
                    path = crownPath,
                    brush = Brush.verticalGradient(
                        colors = crownColors,
                        startY = crownCenter.y - crownHeight,
                        endY = crownCenter.y + crownHeight
                    )
                )

                drawPath(
                    path = crownPath,
                    color = pieceAccent,
                    style = Stroke(width = radius * 0.045f)
                )

                // Три сияющих камня
                listOf(
                    crownCenter.x - crownWidth * 0.42f,
                    crownCenter.x,
                    crownCenter.x + crownWidth * 0.42f
                ).forEach { jewelX ->
                    val jewelY =
                        crownCenter.y - crownHeight * 0.22f

                    drawCircle(
                        color = pieceAccent.copy(alpha = 0.7f),
                        radius = radius * 0.09f,
                        center = Offset(
                            jewelX,
                            jewelY + radius * 0.025f
                        )
                    )

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White,
                                pieceAccent,
                                crownColors.last()
                            ),
                            center = Offset(
                                jewelX - radius * 0.02f,
                                jewelY - radius * 0.02f
                            ),
                            radius = radius * 0.12f
                        ),
                        radius = radius * 0.055f,
                        center = Offset(jewelX, jewelY)
                    )
                }
            }
        }
    }
}
