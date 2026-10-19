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
                color = Color(0xFF0F0F0F) // Ультра-темный премиум-фон
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
        // Статусная строка хода
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            modifier = Modifier
                .padding(bottom = 24.dp)
                .border(1.5.dp, Color(0xFF444444), MaterialTheme.shapes.medium)
        ) {
            Text(
                text = if (currentPlayer == PlayerColor.WHITE) "Ваш ход (Белые)" else "ИИ думает...",
                color = if (currentPlayer == PlayerColor.WHITE) Color(0xFF81C784) else Color(0xFFE57373),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 14.dp)
            )
        }

        // Премиальная игровая доска
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .shadow(24.dp, shape = MaterialTheme.shapes.medium)
                .border(8.dp, Color(0xFF2C1E18), shape = MaterialTheme.shapes.medium) // Эффект багета из темного дерева
                .background(Color(0xFFBCAAA4))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                for (row in 0..7) {
                    Row(modifier = Modifier.weight(1f)) {
                        for (col in 0..7) {
                            val cell = board[row][col]
                            val isSelected = selected?.row == row && selected?.col == col
                            
                            // Палитра деревянных клеток высокой четкости
                            val cellColor = when {
                                isSelected -> Color(0xFFFFEE58) // Золотая подсветка активной шашки
                                (row + col) % 2 != 0 -> Color(0xFF1B5E20) // Глубокий лесной зеленый
                                else -> Color(0xFFF5F5F5) // Светлый полированный дуб
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
    
    // Эффект многослойного глянцевого покрытия
    val brush = if (isWhite) {
        Brush.radialGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFE0E0E0), Color(0xFF9E9E9E))
        )
    } else {
        Brush.radialGradient(
            colors = listOf(Color(0xFF424242), Color(0xFF212121), Color(0xFF000000))
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize(0.82f)
            .shadow(10.dp, CircleShape)
            .background(brush, shape = CircleShape)
            .border(
                width = 3.dp,
                color = if (isWhite) Color(0xFFB0BEC5) else Color(0xFF272727),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (piece.type == PieceType.KING) {
            // Корона с золотым сиянием для дамок
            Box(
                modifier = Modifier
                    .fillMaxSize(0.72f)
                    .background(
                        Brush.radialGradient(listOf(Color(0xFFFFEE58), Color(0xFFFFA000))),
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
            // Внутреннее углубление (гравировка) на шашке
            Box(
                modifier = Modifier
                    .fillMaxSize(0.55f)
                    .border(
                        width = 1.5.dp,
                        color = if (isWhite) Color(0xFF90A4AE) else Color(0xFF616161),
                        shape = CircleShape
                    )
            )
        }
    }
}
