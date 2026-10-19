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
                color = Color(0xFF1E1E1E) // Стильный темный фон приложения
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
        // Индикатор текущего хода
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C)),
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Text(
                text = if (currentPlayer == PlayerColor.WHITE) "Ваш ход (Белые)" else "Ход ИИ (Черные)...",
                color = if (currentPlayer == PlayerColor.WHITE) Color(0xFF81C784) else Color(0xFFE57373),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
        }

        // Игровая доска с красивыми тенями и рамкой
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .shadow(12.dp, shape = MaterialTheme.shapes.medium)
                .border(4.dp, Color(0xFF4E342E), shape = MaterialTheme.shapes.medium)
                .background(Color(0xFFD7CCC8))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                for (row in 0..7) {
                    Row(modifier = Modifier.weight(1f)) {
                        for (col in 0..7) {
                            val cell = board[row][col]
                            val isSelected = selected?.row == row && selected?.col == col
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(
                                        when {
                                            isSelected -> Color(0xFFFFF176) // Яркая подсветка выбора
                                            (row + col) % 2 != 0 -> Color(0xFF388E3C) // Насыщенный темно-зеленый
                                            else -> Color(0xFFE8F5E9) // Светлая клетка
                                        }
                                    )
                                    .clickable { viewModel.onCellClicked(cell.position) },
                                alignment = Alignment.Center
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
    
    // Красивые градиенты для объемных шашек
    val brush = if (isWhite) {
        Brush.radialGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFB0BEC5))
        )
    } else {
        Brush.radialGradient(
            colors = listOf(Color(0xFF616161), Color(0xFF212121))
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize(0.82f)
            .shadow(6.dp, CircleShape)
            .background(brush, shape = CircleShape)
            .border(
                width = 2.dp,
                color = if (isWhite) Color(0xFF90A4AE) else Color(0xFF000000),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        // Если это дамка — рисуем красивую корону
        if (piece.type == PieceType.KING) {
            Text(
                text = "👑",
                fontSize = 22.sp
            )
        } else {
            // Внутренний круг для объема обычной шашки
            Box(
                modifier = Modifier
                    .fillMaxSize(0.6f)
                    .border(
                        width = 1.5.dp,
                        color = if (isWhite) Color(0xFFCFD8DC) else Color(0xFF424242),
                        shape = CircleShape
                    )
            )
        }
    }
}
