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
                color = Color(0xFF0A0A0A) // Глубокий черный фон студии
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
        // Статусная панель хода с неоновым акцентом
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF181818)),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier
                .padding(bottom = 28.dp)
                .border(1.dp, Color(0xFF333333), MaterialTheme.shapes.medium)
        ) {
            Text(
                text = if (currentPlayer == PlayerColor.WHITE) "Ваш ход (Белые)" else "ИИ думает...",
                color = if (currentPlayer == PlayerColor.WHITE) Color(0xFF81C784) else Color(0xFFE57373),
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 36.dp, vertical = 14.dp)
            )
        }

        // Элегантная доска с деревянной рамой
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .shadow(28.dp, shape = MaterialTheme.shapes.medium)
                .border(10.dp, Color(0xFF26140B), shape = MaterialTheme.shapes.medium)
                .background(Color(0xFF8D6E63))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                for (row in 0..7) {
                    Row(modifier = Modifier.weight(1f)) {
                        for (col in 0..7) {
                            val cell = board[row][col]
                            val isSelected = selected?.row == row && selected?.col == col
                            
                            val cellColor = when {
                                isSelected -> Color(0xFFFFD54F) // Теплое золотое выделение
                                (row + col) % 2 != 0 -> Color(0xFF1F4E27) // Глубокий изумрудный цвет темных клеток
                                else -> Color(0xFFF0E6D2) // Текстурный слоновая кость / светлое дерево
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
    
    // Премиальный металлический/глянцевый градиент
    val brush = if (isWhite) {
        Brush.radialGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFDCDCDC), Color(0xFF9E9E9E))
        )
    } else {
        Brush.radialGradient(
            colors = listOf(Color(0xFF484848), Color(0xFF222222), Color(0xFF000000))
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize(0.82f)
            .shadow(12.dp, CircleShape)
            .background(brush, shape = CircleShape)
            .border(
                width = 3.dp,
                color = if (isWhite) Color(0xFFB0BEC5) else Color(0xFF1A1A1A),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (piece.type == PieceType.KING) {
            // Корона с богатым золотым сиянием
            Box(
                modifier = Modifier
                    .fillMaxSize(0.72f)
                    .background(
                        Brush.radialGradient(listOf(Color(0xFFFFEE58), Color(0xFFFF8F00))),
                        shape = CircleShape
                    )
                    .border(2.dp, Color(0xFFFF6F00), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "👑",
                    fontSize = 22.sp
                )
            }
        } else {
            // Внутренняя двойная гравировка для идеальной детализации шашки
            Box(
                modifier = Modifier
                    .fillMaxSize(0.55f)
                    .border(
                        width = 1.5.dp,
                        color = if (isWhite) Color(0xFF80CBC4) else Color(0xFF555555),
                        shape = CircleShape
                    )
            )
        }
    }
}
