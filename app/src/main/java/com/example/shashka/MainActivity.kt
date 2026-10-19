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
                color = Color(0xFF070504) // Студийный черный фон 4K-уровня
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
        // Статусная панель хода в ультра-высоком качестве
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A100B)),
            elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
            modifier = Modifier
                .padding(bottom = 28.dp)
                .border(2.dp, Color(0xFF5D4037), MaterialTheme.shapes.medium)
        ) {
            Text(
                text = if (currentPlayer == PlayerColor.WHITE) "Ваш ход (Белые)" else "Ход ИИ...",
                color = if (currentPlayer == PlayerColor.WHITE) Color(0xFFA5D6A7) else Color(0xFFEF9A9A),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 40.dp, vertical = 16.dp)
            )
        }

        // Премиальная доска с глубоким багетом под дерево
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .shadow(36.dp, shape = MaterialTheme.shapes.medium)
                .border(12.dp, Color(0xFF211009), shape = MaterialTheme.shapes.medium)
                .background(Color(0xFF6D4C41))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                for (row in 0..7) {
                    Row(modifier = Modifier.weight(1f)) {
                        for (col in 0..7) {
                            val cell = board[row][col]
                            val isSelected = selected?.row == row && selected?.col == col
                            
                            val cellColor = when {
                                isSelected -> Color(0xFFFFD54F) // Насыщенное золото при выборе
                                (row + col) % 2 != 0 -> Color(0xFF1B4D2E) // Благородный изумрудный цвет сукна
                                else -> Color(0xFFF5F1E9) // Текстурный слоновая кость
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
                                    UltraHDCheckerPieceView(piece = piece)
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
fun UltraHDCheckerPieceView(piece: CheckerPiece) {
    val isWhite = piece.color == PlayerColor.WHITE
    
    // Плавные радиальные градиенты для объемного 4K-эффекта
    val brush = if (isWhite) {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFFFFF), 
                Color(0xFFF5F5F5), 
                Color(0xFFCFD8DC), 
                Color(0xFF90A4AE)
            )
        )
    } else {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF4A4A4A), 
                Color(0xFF2C2C2C), 
                Color(0xFF121212), 
                Color(0xFF000000)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize(0.85f) // Крупные и четкие шашки
            .shadow(8.dp, CircleShape)
            .background(brush, shape = CircleShape)
            .border(
                width = 3.dp,
                // Идеально контрастная окантовка, видимая на любых клетках
                color = if (isWhite) Color(0xFF78909C) else Color(0xFFB0BEC5),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (piece.type == PieceType.KING) {
            // Роскошная корона с золотым сиянием для дамок
            Box(
                modifier = Modifier
                    .fillMaxSize(0.7f)
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
            // Двойная внутренняя гравировка высокой четкости
            Box(
                modifier = Modifier
                    .fillMaxSize(0.58f)
                    .border(
                        width = 1.5.dp,
                        color = if (isWhite) Color(0xFFB0BEC5) else Color(0xFF616161),
                        shape = CircleShape
                    )
            )
        }
    }
}
