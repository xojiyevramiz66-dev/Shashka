package com.example.shashka

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

fun createInitialBoard(): Array<Array<Cell>> {
    return Array(8) { row ->
        Array(8) { col ->
            val position = BoardPosition(row, col)
            val piece = if ((row + col) % 2 != 0) {
                when {
                    row < 3 -> CheckerPiece(PlayerColor.BLACK)
                    row > 4 -> CheckerPiece(PlayerColor.WHITE)
                    else -> null
                }
            } else null
            Cell(position = position, piece = piece)
        }
    }
}

class CheckersViewModel : ViewModel() {
    var boardState by mutableStateOf(createInitialBoard())
        private set

    var selectedPosition by mutableStateOf<BoardPosition?>(null)
        private set

    var currentPlayer by mutableStateOf(PlayerColor.WHITE)
        private set

    fun onCellClicked(position: BoardPosition) {
        val cell = boardState[position.row][position.col]
        if (selectedPosition == null) {
            if (cell.piece != null && cell.piece.color == currentPlayer) {
                selectedPosition = position
            }
        } else {
            if (selectedPosition == position) {
                selectedPosition = null
            } else {
                // Простая логика хода (можно расширять по желанию)
                selectedPosition = null
            }
        }
    }
}
