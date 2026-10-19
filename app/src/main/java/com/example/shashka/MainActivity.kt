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
                color = Color(0xFF140D0B) // Теплый темно-коричневый фон игрового стола
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
        // Статусная плашка в стиле деревянной инкрустации
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2D1E16)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .padding(bottom = 24.dp)
                .border(2.dp, Color(0xFF5D4037), MaterialTheme.shapes.medium)
        ) {
            Text(
                text = if (currentPlayer == PlayerColor.WHITE) "Ваш ход (Белые)" else "Ход ИИ...",
                color = if (currentPlayer == PlayerColor.WHITE) Color(0xFFC8E6C9) else Color(0xFFFFCDD2),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp)
            )
        }

        // Реалистичная деревянная доска
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .shadow(32.dp, shape = MaterialTheme.shapes.medium)
                .border(12.dp, Color(0xFF3E2723), shape = MaterialTheme.shapes.medium) // Толстый деревянный бортик
                .background(Color(0xFF8D6E63))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                for (row in 0..7) {
                    Row(modifier = Modifier.weight(1f)) {
                        for (col in 0..7) {
                            val cell = board[row][col]
                            val isSelected = selected?.row == row && selected?.col == col
                            
                            // Настоящие цвета классической деревянной доски
                            val cellColor = when {
                                isSelected -> Color(0xFFFFB300) // Яркое янтарное выделение выбранной шашки
                                (row + col) % 2 != 0 -> Color(0xFF2C5E3B) // Глубокий благородный зеленый сукно
                                else -> Color(0xFFE4D5B7) // Полированное светлое дерево
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
                                    RealisticPieceView(piece = piece)
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
fun RealisticPieceView(piece: CheckerPiece) {
    val isWhite = piece.color == PlayerColor.WHITE
    
    // Объемные радиальные градиенты для эффекта настоящей фишки с фаской
    val pieceBrush = if (isWhite) {
        Brush.radialGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFE0E0E0), Color(0xFF9E9E9E), Color(0xFF616161))
        )
    } else {
        Brush.radialGradient(
            colors = listOf(Color(0xFF5A5A5A), Color(0xFF333333), Color(0xFF1A1A1A), Color(0xFF000000))
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize(0.82f)
            .shadow(10.dp, CircleShape)
            .background(pieceBrush, shape = CircleShape)
            .border(
                width = 3.dp,
                color = if (isWhite) Color(0xFFCFD8DC) else Color(0xFF000000),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (piece.type == PieceType.KING) {
            // Корона для дамки с золотым свечением
            Box(
                modifier = Modifier
                    .fillMaxSize(0.68f)
                    .background(
                        Brush.radialGradient(listOf(Color(0xFFFFEE58), Color(0xFFFFA000))),
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
            // Внутренняя резьба (выемка) на шашке, как в настоящих настольных играх
            Box(
                modifier = Modifier
                    .fillMaxSize(0.6f)
                    .border(
                        width = 1.5.dp,
                        color = if (isWhite) Color(0xFFB0BEC5) else Color(0xFF424242),
                        shape = CircleShape
                    )
                    .background(
                        color = Color.Transparent,
                        shape = CircleShape
                    )
            )
        }
    }
}
