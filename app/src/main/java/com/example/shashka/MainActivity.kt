package com.example.shashka

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
                color = Color(0xFF0D0907) // Насыщенный темный фон игрового пространства
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
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Панель статуса хода высокого разрешения
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1F140F)),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            modifier = Modifier
                .padding(bottom = 24.dp)
                .border(2.dp, Color(0xFF4E342E), MaterialTheme.shapes.medium)
        ) {
            Text(
                text = if (currentPlayer == PlayerColor.WHITE) "Ваш ход (Белые)" else "Ход ИИ...",
                color = if (currentPlayer == PlayerColor.WHITE) Color(0xFFC8E6C9) else Color(0xFFFFCDD2),
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 36.dp, vertical = 14.dp)
            )
        }

        // Четкая игровая доска с качественной рамкой
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .shadow(32.dp, shape = MaterialTheme.shapes.medium)
                .border(10.dp, Color(0xFF2C1810), shape = MaterialTheme.shapes.medium)
                .background(Color(0xFF795548))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                for (row in 0..7) {
                    Row(modifier = Modifier.weight(1f)) {
                        for (col in 0..7) {
                            val cell = board[row][col]
                            val isSelected = selected?.row == row && selected?.col == col
                            
                            val cellColor = when {
                                isSelected -> Color(0xFFFFC107) // Золотисто-янтарная подсветка
                                (row + col) % 2 != 0 -> Color(0xFF234E30) // Глубокий зеленый цвет сукна
                                else -> Color(0xFFEFEBE9) // Текстурный светлый дуб
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(cellColor)
                                    .clickable { viewModel.onCellClicked(cell.position) },
                                contentAlignment = Alignment.Center
                            ) {
                                cell.piece?.let { piece ->
                                    HDCheckerPieceView(piece = piece)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HDCheckerPieceView(piece: CheckerPiece) {
    val isWhite = piece.color == PlayerColor.WHITE
    
    // Плавные радиальные градиенты высокой четкости для шашек
    val brush = if (isWhite) {
        Brush.radialGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFE0E0E0), Color(0xFF9E9E9E), Color(0xFF616161))
        )
    } else {
        Brush.radialGradient(
            colors = listOf(Color(0xFF505050), Color(0xFF2E2E2E), Color(0xFF141414), Color(0xFF000000))
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize(0.82f)
            .shadow(12.dp, CircleShape)
            .background(brush, shape = CircleShape)
            .border(
                width = 3.dp,
                color = if (isWhite) Color(0xFFB0BEC5) else Color(0xFF111111),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (piece.type == PieceType.KING) {
            // Корона дамки с объемным золотым сиянием
            Box(
                modifier = Modifier
                    .fillMaxSize(0.7f)
                    .background(
                        Brush.radialGradient(listOf(Color(0xFFFFEE58), Color(0xFFFF8F00))),
                        shape = CircleShape
                    )
                    .border(1.5.dp, Color(0xFFFF6F00), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "👑",
                    fontSize = 22.sp
                )
            }
        } else {
            // Аккуратная внутренняя фаска обычной шашки
            Box(
                modifier = Modifier
                    .fillMaxSize(0.58f)
                    .border(
                        width = 1.5.dp,
                        color = if (isWhite) Color(0xFF90A4AE) else Color(0xFF424242),
                        shape = CircleShape
                    )
            )
        }
    }
}
