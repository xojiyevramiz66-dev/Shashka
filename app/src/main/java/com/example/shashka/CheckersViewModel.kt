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
        val clickedCell = boardState[position.row][position.col]
        
        val currentSelected = selectedPosition
        if (currentSelected == null) {
            // Выбираем шашку текущего игрока
            if (clickedCell.piece != null && clickedCell.piece.color == currentPlayer) {
                selectedPosition = position
            }
        } else {
            if (currentSelected == position) {
                // Снимаем выделение при повторном клике
                selectedPosition = null
            } else {
                // Пытаемся сделать ход на пустую клетку
                if (clickedCell.piece == null) {
                    val rowDiff = position.row - currentSelected.row
                    val colDiff = kotlin.math.abs(position.col - currentSelected.col)
                    
                    // Проверяем движение на 1 клетку по диагонали
                    val isForward = if (currentPlayer == PlayerColor.WHITE) rowDiff == -1 else rowDiff == 1
                    
                    if (colDiff == 1 && isForward) {
                        val newBoard = boardState.map { it.clone() }.toTypedArray()
                        val movingPiece = newBoard[currentSelected.row][currentSelected.col].piece
                        
                        newBoard[position.row][position.col] = newBoard[position.row][position.col].copy(piece = movingPiece)
                        newBoard[currentSelected.row][currentSelected.col] = newBoard[currentSelected.row][currentSelected.col].copy(piece = null)
                        
                        boardState = newBoard
                        selectedPosition = null
                        currentPlayer = if (currentPlayer == PlayerColor.WHITE) PlayerColor.BLACK else PlayerColor.WHITE
                    } else {
                        selectedPosition = null
                    }
                } else {
                    if (clickedCell.piece.color == currentPlayer) {
                        selectedPosition = position
                    } else {
                        selectedPosition = null
                    }
                }
            }
        }
    }
}
