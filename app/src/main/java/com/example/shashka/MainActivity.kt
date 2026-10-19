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
                color = Color(0xFF121212) // Глубокий темно-серый фон
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
        // Статусная панель хода
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .padding(bottom = 24.dp)
                .border(1.dp, Color(0xFF333333), MaterialTheme.shapes.medium)
        ) {
            Text(
                text = if (currentPlayer == PlayerColor.WHITE) "Ваш ход (Белые)" else "Думает ИИ...",
                color = if (currentPlayer == PlayerColor.WHITE) Color(0xFFA5D6A7) else Color(0xFFEF9A9A),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 14.dp)
            )
        }

        // Игровая доска с качественной рамкой
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .shadow(16.dp, shape = MaterialTheme.shapes.medium)
                .border(6.dp, Color(0xFF3E2723), shape = MaterialTheme.shapes.medium)
                .background(Color(0xFFD7CCC8))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                for (row in 0..7) {
                    Row(modifier = Modifier.weight(1f)) {
                        for (col in 0..7) {
                            val cell = board[row][col]
                            val isSelected = selected?.row == row && selected?.col == col
                            
                            // Определяем цвет клетки
                            val cellColor = when {
                                isSelected -> Color(0xFFFFEE58) // Золотая подсветка выбранной шашки
                                (row + col) % 2 != 0 -> Color(0xFF2E7D32) // Насыщенный благородный зеленый
                                else -> Color(0xFFEFEBE9) // Светлая клетка дерева
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
                                    CheckerPieceView(piece = piece)
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
fun CheckerPieceView(piece: CheckerPiece) {
    val isWhite = piece.color == PlayerColor.WHITE
    
    // Роскошные радиальные градиенты для максимального объема
    val brush = if (isWhite) {
        Brush.radialGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFCFD8DC), Color(0xFF90A4AE))
        )
    } else {
        Brush.radialGradient(
            colors = listOf(Color(0xFF546E7A), Color(0xFF263238), Color(0xFF000000))
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize(0.82f)
            .shadow(8.dp, CircleShape)
            .background(brush, shape = CircleShape)
            .border(
                width = 2.5.dp,
                color = if (isWhite) Color(0xFFECEFF1) else Color(0xFF111111),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (piece.type == PieceType.KING) {
            // Корона для дамки с эффектом золота
            Box(
                modifier = Modifier
                    .fillMaxSize(0.7f)
                    .background(
                        Brush.radialGradient(listOf(Color(0xFFFFD54F), Color(0xFFFFA000))),
                        shape = CircleShape
                    )
                    .border(1.5.dp, Color(0xFFFF8F00), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "👑",
                    fontSize = 20.sp
                )
            }
        } else {
            // Внутреннее тиснение для обычной шашки
            Box(
                modifier = Modifier
                    .fillMaxSize(0.55f)
                    .border(
                        width = 1.5.dp,
                        color = if (isWhite) Color(0xFFB0BEC5) else Color(0xFF37474F),
                        shape = CircleShape
                    )
            )
        }
    }
}
