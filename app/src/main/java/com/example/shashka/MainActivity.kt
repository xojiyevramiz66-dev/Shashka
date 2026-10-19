package com.example.shashka

import android.content.Context
import android.os.Build
import android.os.Bundle
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF141414)
            ) {
                CheckersScreen()
            }
        }
    }
}

fun triggerVibration(context: Context, enabled: Boolean) {
    if (!enabled) return
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator.vibrate(VibrationEffect.createOneShot(30L, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(30L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(30L)
            }
        }
    } catch (e: Exception) {}
}

@Composable
fun CheckersScreen(viewModel: CheckersViewModel = viewModel()) {
    val context = LocalContext.current
    val board = viewModel.boardState
    val selected = viewModel.selectedPosition
    val currentPlayer = viewModel.currentPlayer

    val prefs = remember { context.getSharedPreferences("game_prefs", Context.MODE_PRIVATE) }

    // Загружаем сохраненную сложность при старте
    LaunchedEffect(Unit) {
        val savedDifficulty = prefs.getString("ai_difficulty", "Непобедимый")
        if (savedDifficulty != null) {
            viewModel.aiDifficulty = savedDifficulty
        }
    }

    var showSettingsDialog by remember { mutableStateOf(false) }
    var soundEnabled by remember { mutableStateOf(true) }
    var vibrationEnabled by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Верхняя панель управления
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .shadow(6.dp, RoundedCornerShape(12.dp))
                    .background(Color(0xFF262626), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF3D3D3D), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "←", color = Color(0xFF888888), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                text = if (currentPlayer == PlayerColor.WHITE) "Ваш ход" else "Ход противника",
                color = Color(0xFFF5F5F5),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .shadow(6.dp, RoundedCornerShape(12.dp))
                    .background(Color(0xFF262626), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF3D3D3D), RoundedCornerShape(12.dp))
                    .clickable { 
                        triggerVibration(context, vibrationEnabled)
                        showSettingsDialog = true 
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "⚙️", fontSize = 16.sp)
            }
        }

        // Профиль противника сверху
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color(0xFF1E1E1E), CircleShape)
                        .border(2.dp, Color(0xFF3A3A3A), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(
                                Brush.radialGradient(listOf(Color(0xFF4A4A4A), Color(0xFF111111))),
                                CircleShape
                            )
                            .border(1.dp, Color(0xFF888888), CircleShape)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = "Противник", color = Color(0xFF888888), fontSize = 15.sp)
                    Text(text = "Сложность: ${viewModel.aiDifficulty}", color = Color(0xFFF4C430), fontSize = 11.sp)
                }
            }
            Text(text = "09:45", color = Color(0xFF666666), fontSize = 15.sp)
        }

        // Ультра-качественная игровая доска
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .shadow(24.dp, RoundedCornerShape(14.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF5A3E26), Color(0xFF231409))
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
                .border(5.dp, Color(0xFF352011), RoundedCornerShape(14.dp))
                .padding(3.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                for (row in 0..7) {
                    Row(modifier = Modifier.weight(1f)) {
                        for (col in 0..7) {
                            val cell = board[row][col]
                            val isSelected = selected?.row == row && selected?.col == col
                            
                            val isDarkCell = (row + col) % 2 != 0
                            val cellBackground = when {
                                isSelected -> Brush.linearGradient(listOf(Color(0xFFFFD54F), Color(0xFFE5A912)))
                                cell.isHighlighted -> Brush.linearGradient(listOf(Color(0xFF81D4FA), Color(0xFF29B6F6)))
                                isDarkCell -> Brush.verticalGradient(listOf(Color(0xFF482D1B), Color(0xFF2C190D)))
                                else -> Brush.verticalGradient(listOf(Color(0xFFF2E6D8), Color(0xFFD6C0A8)))
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(cellBackground)
                                    .clickable { 
                                        triggerVibration(context, vibrationEnabled)
                                        viewModel.onCellClicked(cell.position) 
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                cell.piece?.let { piece ->
                                    AnimatedPieceWrapper(piece = piece)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Профиль игрока снизу
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color(0xFFEADBCE), CircleShape)
                        .border(2.dp, Color(0xFFF4C430), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "👑", fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = "Вы", color = Color(0xFFEEEEEE), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Text(text = "09:52", color = Color(0xFF666666), fontSize = 15.sp)
        }

        // Нижние кнопки управления
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .shadow(6.dp, RoundedCornerShape(16.dp))
                    .background(Color(0xFF242424), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFF383838), RoundedCornerShape(16.dp))
                    .clickable { 
                        triggerVibration(context, vibrationEnabled)
                        viewModel.undo()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "↩", color = Color(0xFFDDDDDD), fontSize = 20.sp)
            }

            BottomIconButton(icon = "💡", onClick = { triggerVibration(context, vibrationEnabled) })
            BottomIconButton(icon = "☰", onClick = { triggerVibration(context, vibrationEnabled) })
        }
    }

    // Диалоговое окно настроек
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            containerColor = Color(0xFF1E1E1E),
            titleContentColor = Color(0xFFFFD54F),
            textContentColor = Color(0xFFDDDDDD),
            title = {
                Text(text = "⚙️ Настройки игры", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = "Сложность ИИ", color = Color(0xFFF4C430), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf("Лёгкий", "Средний", "Непобедимый").forEach { level ->
                                val isSelected = viewModel.aiDifficulty == level
                                Button(
                                    onClick = { 
                                        viewModel.aiDifficulty = level
                                        // Сохраняем сложность в память устройства
                                        prefs.edit().putString("ai_difficulty", level).apply()
                                        triggerVibration(context, vibrationEnabled)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) Color(0xFFF4C430) else Color(0xFF2C2C2C),
                                        contentColor = if (isSelected) Color(0xFF141414) else Color(0xFFDDDDDD)
                                    ),
                                    modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                                ) {
                                    Text(text = level, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Звуки", color = Color(0xFFE8DCC6), fontSize = 15.sp)
                        Switch(
                            checked = soundEnabled,
                            onCheckedChange = { 
                                soundEnabled = it
                                triggerVibration(context, vibrationEnabled)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFF4C430))
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Вибрация", color = Color(0xFFE8DCC6), fontSize = 15.sp)
                        Switch(
                            checked = vibrationEnabled,
                            onCheckedChange = { 
                                vibrationEnabled = it
                                triggerVibration(context, it)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFF4C430))
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSettingsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF4C430), contentColor = Color(0xFF141414))
                ) {
                    Text(text = "Применить", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun BottomIconButton(icon: String, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .background(Color(0xFF242424), RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF383838), RoundedCornerShape(16.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = icon, color = Color(0xFFDDDDDD), fontSize = 20.sp)
    }
}

@Composable
fun AnimatedPieceWrapper(piece: CheckerPiece) {
    val scale = remember { Animatable(0.4f) }

    LaunchedEffect(piece) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 220,
                easing = FastOutSlowInEasing
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(all = (0.82f * (1f - scale.value) * 10).dp),
        contentAlignment = Alignment.Center
    ) {
        UltraHD3DPieceView(piece = piece)
    }
}

@Composable
fun UltraHD3DPieceView(piece: CheckerPiece) {
    val isWhite = piece.color == PlayerColor.WHITE

    Canvas(
        modifier = Modifier
            .fillMaxSize(0.82f)
    ) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // 1. Глубокая многослойная мягкая тень (создает эффект высоты фишки над доской)
        drawCircle(
            color = Color.Black.copy(alpha = 0.35f),
            radius = radius * 1.02f,
            center = center + Offset(radius * 0.04f, radius * 0.12f)
        )
        drawCircle(
            color = Color.Black.copy(alpha = 0.25f),
            radius = radius * 0.98f,
            center = center + Offset(radius * 0.02f, radius * 0.06f)
        )

        // 2. Тело шашки с продвинутым 3D-градиентом освещения сверху
        val bodyBrush = if (isWhite) {
            Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFFFFF),
                    Color(0xFFFAF4E8),
                    Color(0xFFE3D0B5),
                    Color(0xFFB39872),
                    Color(0xFF7A613F)
                ),
                center = center - Offset(radius * 0.3f, radius * 0.35f),
                radius = radius * 1.4f
            )
        } else {
            Brush.radialGradient(
                colors = listOf(
                    Color(0xFF6E6E6E),
                    Color(0xFF383838),
                    Color(0xFF1C1C1C),
                    Color(0xFF0A0A0A),
                    Color(0xFF000000)
                ),
                center = center - Offset(radius * 0.3f, radius * 0.35f),
                radius = radius * 1.4f
            )
        }

        drawCircle(
            brush = bodyBrush,
            radius = radius * 0.95f,
            center = center
        )

        // Премиальный глянцевый ободок по краю
        drawCircle(
            color = if (isWhite) Color(0xFFC7B192) else Color(0xFF9EABB5),
            radius = radius * 0.95f,
            center = center,
            style = Stroke(width = radius * 0.07f)
        )

        // Внутренний световой блик по верхней дуге (имитация отражения студийного света)
        drawCircle(
            color = Color.White.copy(alpha = if (isWhite) 0.4f else 0.15f),
            radius = radius * 0.90f,
            center = center - Offset(0f, radius * 0.05f),
            style = Stroke(width = radius * 0.03f)
        )

        // 3. Первая концентрическая ступень (фаска) с теневым углублением
        val outerRingRadius = radius * 0.68f
        drawCircle(
            color = if (isWhite) Color(0xFFFFFFFF) else Color(0xFF555555),
            radius = outerRingRadius,
            center = center,
            style = Stroke(width = radius * 0.05f)
        )
        // Теневой внутренний контур фаски (Ambient Occlusion)
        drawCircle(
            color = if (isWhite) Color(0xFF9E845E) else Color(0xFF0D0D0D),
            radius = outerRingRadius - radius * 0.035f,
            center = center,
            style = Stroke(width = radius * 0.045f)
        )

        // 4. Внутренняя чаша (центральное углубление)
        val innerBrush = if (isWhite) {
            Brush.radialGradient(
                colors = listOf(Color(0xFFFFFFFF), Color(0xFFE8D7BE), Color(0xFFC2A884)),
                center = center - Offset(radius * 0.15f, radius * 0.2f),
                radius = radius * 0.75f
            )
        } else {
            Brush.radialGradient(
                colors = listOf(Color(0xFF4A4A4A), Color(0xFF222222), Color(0xFF080808)),
                center = center - Offset(radius * 0.15f, radius * 0.2f),
                radius = radius * 0.75f
            )
        }

        val innerRadius = radius * 0.48f
        drawCircle(
            brush = innerBrush,
            radius = innerRadius,
            center = center
        )

        drawCircle(
            color = if (isWhite) Color(0xFF8F734E) else Color(0xFF030303),
            radius = innerRadius,
            center = center,
            style = Stroke(width = radius * 0.035f)
        )

        // 5. Корона высшего качества для дамок (KING)
        if (piece.type == PieceType.KING) {
            val crownWidth = radius * 0.68f
            val crownHeight = radius * 0.48f
            val left = center.x - crownWidth / 2f
            val top = center.y - crownHeight / 2f + radius * 0.04f

            // Роскошный золотой градиент с зеркальным переливом
            val crownBrush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFFF8E1),
                    Color(0xFFFFD54F),
                    Color(0xFFF57F17),
                    Color(0xFFB78103)
                ),
                startY = top,
                endY = top + crownHeight
            )

            val crownPath = Path().apply {
                moveTo(left + crownWidth * 0.06f, top + crownHeight)
                lineTo(left + crownWidth * 0.94f, top + crownHeight)
                lineTo(left + crownWidth, top + crownHeight * 0.16f)
                lineTo(left + crownWidth * 0.72f, top + crownHeight * 0.55f)
                lineTo(left + crownWidth * 0.5f, top - crownHeight * 0.08f)
                lineTo(left + crownWidth * 0.28f, top + crownHeight * 0.55f)
                lineTo(left, top + crownHeight * 0.16f)
                close()
            }

            // Тень под короной для максимального объема
            drawPath(
                path = crownPath,
                color = Color.Black.copy(alpha = 0.4f)
            )

            // Основная заливка короны
            drawPath(path = crownPath, brush = crownBrush)
            
            // Четкий контур короны
            drawPath(
                path = crownPath,
                color = Color(0xFF795548),
                style = Stroke(width = radius * 0.03f)
            )

            // Жемчужные шарики на вершинах зубцов с зеркальным бликом
            val ballRadius = radius * 0.06f
            val ballColor = Color(0xFFFFFFFF)
            val ballStroke = Color(0xFF8C6D14)

            // Левый шарик
            val leftBallCenter = Offset(left, top + crownHeight * 0.16f)
            drawCircle(color = ballColor, radius = ballRadius, center = leftBallCenter)
            drawCircle(color = ballStroke, radius = ballRadius, center = leftBallCenter, style = Stroke(width = 1.5f))

            // Центральный шарик (выше остальных)
            val centerBallCenter = Offset(left + crownWidth * 0.5f, top - crownHeight * 0.08f)
            drawCircle(color = ballColor, radius = ballRadius * 1.15f, center = centerBallCenter)
            drawCircle(color = ballStroke, radius = ballRadius * 1.15f, center = centerBallCenter, style = Stroke(width = 1.5f))

            // Правый шарик
            val rightBallCenter = Offset(left + crownWidth, top + crownHeight * 0.16f)
            drawCircle(color = ballColor, radius = ballRadius, center = rightBallCenter)
            drawCircle(color = ballStroke, radius = ballRadius, center = rightBallCenter, style = Stroke(width = 1.5f))

            // Декоративная линия основания короны
            val baseLinePath = Path().apply {
                moveTo(left + crownWidth * 0.14f, top + crownHeight * 0.82f)
                lineTo(left + crownWidth * 0.86f, top + crownHeight * 0.82f)
            }
            drawPath(
                path = baseLinePath,
                color = Color(0xFF5D4037),
                style = Stroke(width = radius * 0.04f)
            )
        } else {
            // Элегантный внутренний круговой акцент для обычной шашки
            drawCircle(
                color = if (isWhite) Color(0xFFFFFFFF).copy(alpha = 0.8f) else Color(0xFF888888),
                radius = radius * 0.22f,
                center = center,
                style = Stroke(width = radius * 0.035f)
            )
        }
    }
}
