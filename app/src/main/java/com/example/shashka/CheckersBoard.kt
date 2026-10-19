package com.example.shashka

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Цвета доски
val BoardLightColor = Color(0xFFEEEED2)
val BoardDarkColor = Color(0xFF769656)
val HighlightColor = Color(0x88BBDEFB)

// Цвета шашек
val PieceWhiteColor = Color(0xFFFAFAFA)
val PieceBlackColor = Color(0xFF212121)

@Composable
fun CheckersBoard(
    boardState: Array<Array<Cell>>,
    selectedPosition: BoardPosition?,
    onCellClick: (BoardPosition) -> Unit,
    modifier: Modifier = Modifier
) {
    // Делаем доску квадратной
    Column(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        for (row in 0..7) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                for (col in 0..7) {
                    val isDarkCell = (row + col) % 2 != 0
                    val cell = boardState[row][col]
                    val isSelected = selectedPosition == BoardPosition(row, col)

                    CellView(
                        cell = cell,
                        isDark = isDarkCell,
                        isSelected = isSelected,
                        onClick = { onCellClick(BoardPosition(row, col)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun CellView(
    cell: Cell,
    isDark: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isSelected -> Color.Yellow.copy(alpha = 0.6f)
        cell.isHighlighted -> HighlightColor
        isDark -> BoardDarkColor
        else -> BoardLightColor
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Отрисовка шашки
        cell.piece?.let { piece ->
            PieceView(piece = piece)
        }
    }
}

@Composable
fun PieceView(piece: CheckerPiece) {
    val pieceColor = if (piece.color == PlayerColor.WHITE) PieceWhiteColor else PieceBlackColor
    val borderColor = if (piece.color == PlayerColor.WHITE) Color.Gray else Color.LightGray

    Box(
        modifier = Modifier
            .fillMaxSize(0.8f) // Шашка занимает 80% размера клетки
            .background(pieceColor, shape = CircleShape)
            .background(borderColor.copy(alpha = 0.3f), shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Если это дамка — рисуем корону
        if (piece.type == PieceType.KING) {
            Text(
                text = "👑",
                fontSize = 18.sp
            )
        }
    }
}
