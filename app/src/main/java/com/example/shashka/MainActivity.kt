package com.example.shashka

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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

@Composable
fun CheckersScreen(viewModel: CheckersViewModel = viewModel()) {
    val board = viewModel.boardState
    val selected = viewModel.selectedPosition
    val currentPlayer = viewModel.currentPlayer

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
                    .border(1.dp, Color(0xFF3D3D3D), RoundedCornerShape(12.dp)),
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
                Text(text = "Противник", color = Color(0xFF888888), fontSize = 15.sp)
            }
            Text(text = "09:45", color = Color(0xFF666666), fontSize = 15.sp)
        }

        // Игровая доска
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .shadow(20.dp, RoundedCornerShape(12.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF4E3621), Color(0xFF2B1A0E))
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                .border(12.dp, Color(0xFF3B2414), RoundedCornerShape(12.dp))
                .padding(2.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                for (row in 0..7) {
                    Row(modifier = Modifier.weight(1f)) {
                        for (col in 0..7) {
                            val cell = board[row][col]
                            val isSelected = selected?.row == row && selected?.col == col
                            
                            val isDarkCell = (row + col) % 2 != 0
                            val cellBackground = when {
                                isSelected -> Brush.linearGradient(listOf(Color(0xFFF4C430), Color(0xFFD4A017)))
                                isDarkCell -> Brush.verticalGradient(listOf(Color(0xFF4A2F1B), Color(0xFF362010)))
                                else -> Brush.verticalGradient(listOf(Color(0xFFEADBCE), Color(0xFFCBB6A4)))
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(cellBackground)
                                    .clickable { viewModel.onCellClicked(cell.position) },
                                contentAlignment = Alignment.Center
                            ) {
                                cell.piece?.let { piece ->
                                    Realistic3DPieceView(piece = piece)
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

        // Нижние кнопки
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BottomIconButton(icon = "↩")
            BottomIconButton(icon = "💡")
            BottomIconButton(icon = "☰")
        }
    }
}

@Composable
fun BottomIconButton(icon: String) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .background(Color(0xFF242424), RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF383838), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = icon, color = Color(0xFFDDDDDD), fontSize = 20.sp)
    }
}

@Composable
fun Realistic3DPieceView(piece: CheckerPiece) {
    val isWhite = piece.color == PlayerColor.WHITE

    Canvas(
        modifier = Modifier
            .fillMaxSize(0.76f) // Сбалансированный размер, чтобы края фишек отчетливо выделялись внутри клеток
    ) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // 1. Падающая тень под шашкой
        drawCircle(
            color = Color.Black.copy(alpha = 0.5f),
            radius = radius * 0.96f,
            center = center + Offset(radius * 0.06f, radius * 0.1f)
        )

        // 2. Внешнее тело шашки
        val bodyBrush = if (isWhite) {
            Brush.radialGradient(
                colors = listOf(Color(0xFFFFFFFF), Color(0xFFFAF3E6), Color(0xFFE2CEB1), Color(0xFF9E8462)),
                center = center - Offset(radius * 0.25f, radius * 0.3f),
                radius = radius * 1.3f
            )
        } else {
            Brush.radialGradient(
                colors = listOf(Color(0xFF666666), Color(0xFF333333), Color(0xFF161616), Color(0xFF050505)),
                center = center - Offset(radius * 0.25f, radius * 0.3f),
                radius = radius * 1.3f
            )
        }

        drawCircle(
            brush = bodyBrush,
            radius = radius * 0.94f,
            center = center
        )

        // ЯРКИЙ КОНТУР КРАЕВ: Светло-серебристый для черных шашек, чтобы они четко сияли на темных клетках!
        drawCircle(
            color = if (isWhite) Color(0xFFB59A74) else Color(0xFFAAAAAA),
            radius = radius * 0.94f,
            center = center,
            style = Stroke(width = radius * 0.08f)
        )

        // 3. Первое концентрическое кольцо (фаска)
        val outerRingRadius = radius * 0.7f
        drawCircle(
            color = if (isWhite) Color(0xFFFFFFFF).copy(alpha = 0.9f) else Color(0xFF666666),
            radius = outerRingRadius,
            center = center,
            style = Stroke(width = radius * 0.05f)
        )
        drawCircle(
            color = if (isWhite) Color(0xFFB59A74) else Color(0xFF1E1E1E),
            radius = outerRingRadius - radius * 0.04f,
            center = center,
            style = Stroke(width = radius * 0.04f)
        )

        // 4. Внутренняя чаша
        val innerBrush = if (isWhite) {
            Brush.radialGradient(
                colors = listOf(Color(0xFFFFFFFF), Color(0xFFE4D2B8), Color(0xFFBCA17D)),
                center = center - Offset(radius * 0.15f, radius * 0.2f),
                radius = radius * 0.8f
            )
        } else {
            Brush.radialGradient(
                colors = listOf(Color(0xFF444444), Color(0xFF222222), Color(0xFF0B0B0B)),
                center = center - Offset(radius * 0.15f, radius * 0.2f),
                radius = radius * 0.8f
            )
        }

        val innerRadius = radius * 0.5f
        drawCircle(
            brush = innerBrush,
            radius = innerRadius,
            center = center
        )

        drawCircle(
            color = if (isWhite) Color(0xFF9E8360) else Color(0xFF0A0A0A),
            radius = innerRadius,
            center = center,
            style = Stroke(width = radius * 0.04f)
        )

        // 5. Корона или центр
        if (piece.type == PieceType.KING) {
            val crownColor = Color(0xFFF4C430)
            val crownPath = Path().apply {
                val w = radius * 0.45f
                val h = radius * 0.35f
                val left = center.x - w / 2f
                val top = center.y - h / 2f

                moveTo(left, top + h)
                lineTo(left, top)
                lineTo(left + w * 0.28f, top + h * 0.5f)
                lineTo(left + w * 0.5f, top - h * 0.2f)
                lineTo(left + w * 0.72f, top + h * 0.5f)
                lineTo(left + w, top)
                lineTo(left + w, top + h)
                close()
            }
            drawPath(path = crownPath, color = crownColor)
            drawPath(path = crownPath, color = Color(0xFFB8860B), style = Stroke(width = 2f))
        } else {
            drawCircle(
                color = if (isWhite) Color(0xFFFFFFFF).copy(alpha = 0.7f) else Color(0xFF777777),
                radius = radius * 0.24f,
                center = center,
                style = Stroke(width = radius * 0.03f)
            )
        }
    }
}
