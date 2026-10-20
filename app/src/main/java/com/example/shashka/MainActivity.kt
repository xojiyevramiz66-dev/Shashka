
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

private val BackgroundColor = Color(0xFF141414)
private val PanelColor = Color(0xFF242424)
private val GoldColor = Color(0xFFF4C430)
private val TextColor = Color(0xFFE8DCC6)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
    }

    LaunchedEffect(
        currentPlayer,
        gameMessage,
        whiteSeconds,
        blackSeconds
    ) {
        if (gameMessage != null) return@LaunchedEffect

        delay(1000)

        if (gameMessage != null) return@LaunchedEffect

        if (currentPlayer == PlayerColor.WHITE) {
            if (whiteSeconds <= 1) {
                whiteSeconds = 0
                viewModel.onTimeExpired(PlayerColor.WHITE)
            } else {
                whiteSeconds -= 1
            }
        } else {
            if (blackSeconds <= 1) {
                blackSeconds = 0
                viewModel.onTimeExpired(PlayerColor.BLACK)
            } else {
                blackSeconds -= 1
            }
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
                    color = Color(0xFFBDBDBD),
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
                        listOf(
                            Color(0xFF67472B),
                            Color(0xFF25170D)
                        )
                    ),
                    RoundedCornerShape(12.dp)
                )
                .border(
                    4.dp,
                    Color(0xFF76512E),
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
                                        Color(0xFFFFE082),
                                        Color(0xFFD69B16)
                                    )
                                )

                                cell.isHighlighted -> Brush.linearGradient(
                                    listOf(
                                        Color(0xFF80DEEA),
                                        Color(0xFF0097A7)
                                    )
                                )

                                isDarkCell -> Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF593820),
                                        Color(0xFF321D10)
                                    )
                                )

                                else -> Brush.verticalGradient(
                                    listOf(
                                        Color(0xFFF2E5D5),
                                        Color(0xFFD4BDA2)
                                    )
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

                                        if (soundEnabled) {
                                            playGameSound(true)
                                        }

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
                        color = Color(0xEE1E1E1E),
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
            containerColor = Color(0xFF1E1E1E),
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
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
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
                                            Color(0xFF303030)
                                        },
                                        contentColor = if (isSelected) {
                                            Color(0xFF141414)
                                        } else {
                                            Color.White
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

                    HorizontalDivider(color = Color(0xFF3A3A3A))

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
                        color = Color(0xFF999999),
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
            containerColor = Color(0xFF1E1E1E),
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
                Color(0xFF414141),
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isActive) Color(0xFF29251B)
                else Color(0xFF1C1C1C),
                RoundedCornerShape(12.dp)
            )
            .border(
                width = if (isActive) 1.dp else 0.5.dp,
                color = if (isActive) GoldColor
                else Color(0xFF333333),
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
                        if (isWhite) {
                            Brush.radialGradient(
                                listOf(
                                    Color.White,
                                    Color(0xFFE2D0B6),
                                    Color(0xFF8D704C)
                                )
                            )
                        } else {
                            Brush.radialGradient(
                                listOf(
                                    Color(0xFF777777),
                                    Color(0xFF262626),
                                    Color.Black
                                )
                            )
                        },
                        CircleShape
                    )
                    .border(
                        1.dp,
                        if (isWhite) Color(0xFFE8D3AD)
                        else Color(0xFF777777),
                        CircleShape
                    )
            )

            Column {
                Text(
                    text = title,
                    color = if (isActive) GoldColor else Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = subtitle,
                    color = Color(0xFF999999),
                    fontSize = 10.sp
                )
            }
        }

        Text(
            text = clock,
            color = if (isActive) GoldColor else Color(0xFFB0B0B0),
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
                checkedTrackColor = Color(0xFF78601B)
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
                    Color(0xFF414141),
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
                color = Color(0xFF999999),
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
 * Объёмные шашки: затенённая боковая стенка,
 * полированная верхняя поверхность и золотая корона.
 */
@Composable
fun UltraHD3DPieceView(
    piece: CheckerPiece,
    scale: Float = 1f
) {
    val isWhite = piece.color == PlayerColor.WHITE

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

            // Мягкая тень под фишкой
            drawOval(
                color = Color.Black.copy(alpha = 0.52f),
                topLeft = Offset(
                    center.x - radius * 0.91f,
                    center.y - radius * 0.22f
                ),
                size = Size(
                    radius * 1.82f,
                    radius * 1.42f
                )
            )

            // Толстая боковая стенка
            val sideBrush = if (isWhite) {
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFEAD8B9),
                        Color(0xFFB18A58),
                        Color(0xFF79542E),
                        Color(0xFF4D341D)
                    ),
                    startY = center.y - radius * 0.1f,
                    endY = center.y + radius * 0.62f
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF777777),
                        Color(0xFF393939),
                        Color(0xFF151515),
                        Color(0xFF030303)
                    ),
                    startY = center.y - radius * 0.1f,
                    endY = center.y + radius * 0.62f
                )
            }

            drawOval(
                brush = sideBrush,
                topLeft = Offset(
                    center.x - radius * 0.94f,
                    center.y - radius * 0.02f
                ),
                size = Size(
                    radius * 1.88f,
                    radius * 0.65f
                )
            )

            // Нижний металлический кант
            drawCircle(
                brush = Brush.verticalGradient(
                    colors = if (isWhite) {
                        listOf(
                            Color(0xFFFFF1D7),
                            Color(0xFFC9A878),
                            Color(0xFF8C6538)
                        )
                    } else {
                        listOf(
                            Color(0xFF969696),
                            Color(0xFF454545),
                            Color(0xFF101010)
                        )
                    }
                ),
                radius = radius * 0.98f,
                center = center - Offset(
                    0f,
                    radius * 0.10f
                )
            )

            // Верхняя поверхность приподнята над боковой стенкой
            val faceCenter = center - Offset(
                0f,
                radius * 0.20f
            )

            val faceBrush = if (isWhite) {
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFFFF4DF),
                        Color(0xFFE5CDA7),
                        Color(0xFFB18B59)
                    ),
                    center = faceCenter - Offset(
                        radius * 0.32f,
                        radius * 0.40f
                    ),
                    radius = radius * 1.65f
                )
            } else {
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF858585),
                        Color(0xFF4A4A4A),
                        Color(0xFF242424),
                        Color(0xFF080808),
                        Color(0xFF000000)
                    ),
                    center = faceCenter - Offset(
                        radius * 0.32f,
                        radius * 0.40f
                    ),
                    radius = radius * 1.65f
                )
            }

            drawCircle(
                brush = faceBrush,
                radius = radius * 0.94f,
                center = faceCenter
            )

            // Полированный внешний ободок
            drawCircle(
                brush = Brush.linearGradient(
                    colors = if (isWhite) {
                        listOf(
                            Color(0xFFFFF8E9),
                            Color(0xFFB99A6D),
                            Color(0xFFFFF1D2)
                        )
                    } else {
                        listOf(
                            Color(0xFFAAAAAA),
                            Color(0xFF444444),
                            Color(0xFF777777)
                        )
                    },
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

            // Световой блик
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

            // Внутренние кольца
            drawCircle(
                color = if (isWhite) {
                    Color(0xFFC6A579)
                } else {
                    Color(0xFF111111)
                },
                radius = radius * 0.57f,
                center = faceCenter,
                style = Stroke(width = radius * 0.075f)
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = if (isWhite) {
                        listOf(
                            Color(0xFFFFFFFF),
                            Color(0xFFE5D1AD),
                            Color(0xFFB99A6C)
                        )
                    } else {
                        listOf(
                            Color(0xFF505050),
                            Color(0xFF202020),
                            Color(0xFF080808)
                        )
                    },
                    center = faceCenter - Offset(
                        radius * 0.15f,
                        radius * 0.18f
                    ),
                    radius = radius * 0.75f
                ),
                radius = radius * 0.48f,
                center = faceCenter
            )

            // Отражение света на поверхности
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

            if (piece.type == PieceType.KING) {
                val crownCenter = faceCenter - Offset(
                    0f,
                    radius * 0.015f
                )

                val crownWidth = radius * 0.95f
                val crownHeight = radius * 0.65f

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

                // Золотая поверхность короны
                drawPath(
                    path = crownPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFFFE0),
                            Color(0xFFFFD54F),
                            Color(0xFFD49A18),
                            Color(0xFF80520A)
                        ),
                        startY = crownCenter.y - crownHeight,
                        endY = crownCenter.y + crownHeight
                    )
                )

                drawPath(
                    path = crownPath,
                    color = Color(0xFF70450B),
                    style = Stroke(width = radius * 0.045f)
                )

                // Три сияющих камня на короне
                listOf(
                    crownCenter.x - crownWidth * 0.42f,
                    crownCenter.x,
                    crownCenter.x + crownWidth * 0.42f
                ).forEach { jewelX ->
                    val jewelY =
                        crownCenter.y - crownHeight * 0.22f

                    drawCircle(
                        color = Color(0xFF8D5E00),
                        radius = radius * 0.075f,
                        center = Offset(
                            jewelX,
                            jewelY + radius * 0.025f
                        )
                    )

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White,
                                Color(0xFFFFF1A8),
                                Color(0xFFD99A10)
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
