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
import androidx.compose.ui.draw.drawBehind
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
                color = Color(0xFF140C08) // Глубокий фон деревянного стола
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
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Статусная плашка в стиле резного дерева
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1810)),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            modifier = Modifier
                .padding(bottom = 20.dp)
                .border(2.dp, Color(0xFF5D3A24), MaterialTheme.shapes.medium)
        ) {
            Text(
                text = if (currentPlayer == PlayerColor.WHITE) "Ваш ход (Белые)" else "Ход ИИ...",
                color = if (currentPlayer == PlayerColor.WHITE) Color(0xFFFFF8E7) else Color(0xFFFFCC80),
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 36.dp, vertical = 12.dp)
            )
        }

        // Премиальная деревянная доска
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .shadow(28.dp, shape = MaterialTheme.shapes.medium)
                .border(10.dp, Color(0xFF3D2314), shape = MaterialTheme.shapes.medium) // Деревянная рамка
                .background(Color(0xFF8D5B3A))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                for (row in 0..7) {
                    Row(modifier = Modifier.weight(1f)) {
                        for (col in 0..7) {
                            val cell = board[row][col]
                            val isSelected = selected?.row == row && selected?.col == col
                            
                            // Классическая цветовая гамма резной деревянной доски
                            val cellColor = when {
                                isSelected -> Color(0xFFFFB300) // Янтарный подсвечник для хода
                                (row + col) % 2 != 0 -> Color(0xFFA04B28) // Насыщенное красное дерево
                                else -> Color(0xFFE8D3B0) // Светлая береза / сосна
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
                                    WoodCheckerPieceView(piece = piece)
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
fun WoodCheckerPieceView(piece: CheckerPiece) {
    val isWhite = piece.color == PlayerColor.WHITE

    // Градиенты объёма для светлых и тёмно-коричневых деревянных шашек
    val outerGradient = if (isWhite) {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0xFFF7EBD2),
                Color(0xFFDCC196),
                Color(0xFFA88457)
            )
        )
    } else {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFFD66D3B),
                Color(0xFFA8451E),
                Color(0xFF6B260C),
                Color(0xFF3B1103)
            )
        )
    }

    val borderColor = if (isWhite) Color(0xFF8C6B3F) else Color(0xFF260A02)

    // Внешняя фишка с объёмной тенью
    Box(
        modifier = Modifier
            .fillMaxSize(0.84f)
            .shadow(6.dp, CircleShape)
            .background(outerGradient, shape = CircleShape)
            .border(2.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Первый резной концентрический круг (фаска)
        Box(
            modifier = Modifier
                .fillMaxSize(0.72f)
                .border(
                    width = 2.dp,
                    color = if (isWhite) Color(0xFFFFF3DB) else Color(0xFFE58250),
                    shape = CircleShape
                )
                .border(
                    width = 3.5.dp,
                    color = if (isWhite) Color(0xFFB8966B) else Color(0xFF521C0B),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Второй внутренний кольцевой узор
            Box(
                modifier = Modifier
                    .fillMaxSize(0.52f)
                    .background(
                        brush = if (isWhite) {
                            Brush.radialGradient(listOf(Color(0xFFFFFBF2), Color(0xFFD1B288)))
                        } else {
                            Brush.radialGradient(listOf(Color(0xFFE87E48), Color(0xFF591E0A)))
                        },
                        shape = CircleShape
                    )
                    .border(
                        width = 1.5.dp,
                        color = if (isWhite) Color(0xFF9E7A4C) else Color(0xFF330F04),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (piece.type == PieceType.KING) {
                    // Золотая роспись короны для дамок
                    Text(
                        text = "👑",
                        fontSize = 18.sp
                    )
                } else {
                    // Центр резной фишки (самый внутренний кружок)
                    Box(
                        modifier = Modifier
                            .fillMaxSize(0.45f)
                            .border(
                                width = 1.dp,
                                color = if (isWhite) Color(0xFFFFF8EA) else Color(0xFFFFA270),
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}
