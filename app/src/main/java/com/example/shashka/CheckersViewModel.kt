package com.example.shashka

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

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
        if (currentPlayer != PlayerColor.WHITE) return

        val clickedCell = boardState[position.row][position.col]
        val currentSelected = selectedPosition

        if (currentSelected == null) {
            if (clickedCell.piece != null && clickedCell.piece.color == currentPlayer) {
                selectedPosition = position
            }
        } else {
            if (currentSelected == position) {
                selectedPosition = null
            } else {
                if (clickedCell.piece == null) {
                    if (tryMakeMoveOrCapture(currentSelected, position)) {
                        currentPlayer = PlayerColor.BLACK
                        viewModelScope.launch {
                            delay(500)
                            makeAiMove()
                        }
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

    private fun tryMakeMoveOrCapture(from: BoardPosition, to: BoardPosition): Boolean {
        val rowDiff = to.row - from.row
        val colDiff = to.col - from.col
        val movingPiece = boardState[from.row][from.col].piece ?: return false

        // Ход должен быть строго по диагонали
        if (abs(rowDiff) != abs(colDiff)) return false

        val stepRow = if (rowDiff > 0) 1 else -1
        val stepCol = if (colDiff > 0) 1 else -1
        val steps = abs(rowDiff)

        if (movingPiece.type == PieceType.KING) {
            // Логика для Дамки (может ходить на много клеток и перепрыгивать через одну вражескую)
            var enemyFound: BoardPosition? = null
            var r = from.row + stepRow
            var c = from.col + stepCol

            for (i in 1 until steps) {
                val piece = boardState[r][c].piece
                if (piece != null) {
                    if (piece.color == movingPiece.color) return false // Своя шашка на пути
                    if (enemyFound != null) return false // Две вражеские шашки подряд
                    enemyFound = BoardPosition(r, c)
                }
                r += stepRow
                c += stepCol
            }

            if (boardState[to.row][to.col].piece != null) return false
            executeMove(from, to, enemyFound)
            return true

        } else {
            // Логика для обычной шашки
            if (steps == 1) {
                val isForward = if (movingPiece.color == PlayerColor.WHITE) rowDiff == -1 else rowDiff == 1
                if (isForward && boardState[to.row][to.col].piece == null) {
                    executeMove(from, to, null)
                    return true
                }
            } else if (steps == 2) {
                val midRow = from.row + stepRow
                val midCol = from.col + stepCol
                val enemyPiece = boardState[midRow][midCol].piece
                if (enemyPiece != null && enemyPiece.color != movingPiece.color && boardState[to.row][to.col].piece == null) {
                    executeMove(from, to, BoardPosition(midRow, midCol))
                    return true
                }
            }
        }
        return false
    }

    private fun executeMove(from: BoardPosition, to: BoardPosition, removePosition: BoardPosition?) {
        val newBoard = boardState.map { it.clone() }.toTypedArray()
        var movingPiece = newBoard[from.row][from.col].piece

        if (movingPiece != null) {
            if (movingPiece.color == PlayerColor.WHITE && to.row == 0) {
                movingPiece = movingPiece.copy(type = PieceType.KING)
            } else if (movingPiece.color == PlayerColor.BLACK && to.row == 7) {
                movingPiece = movingPiece.copy(type = PieceType.KING)
            }
        }

        newBoard[to.row][to.col] = newBoard[to.row][to.col].copy(piece = movingPiece)
        newBoard[from.row][from.col] = newBoard[from.row][from.col].copy(piece = null)

        if (removePosition != null) {
            newBoard[removePosition.row][removePosition.col] = newBoard[removePosition.row][removePosition.col].copy(piece = null)
        }

        boardState = newBoard
        selectedPosition = null
    }

    private fun makeAiMove() {
        if (currentPlayer != PlayerColor.BLACK) return

        // Простой ИИ для примера делает базовый ход черных
        val allMoves = mutableListOf<Triple<BoardPosition, BoardPosition, BoardPosition?>>()
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = boardState[r][c].piece
                if (piece != null && piece.color == PlayerColor.BLACK) {
                    val from = BoardPosition(r, c)
                    val dirs = listOf(Pair(1, 1), Pair(1, -1), Pair(-1, 1), Pair(-1, -1))
                    for (d in dirs) {
                        val toR = r + d.first
                        val toC = c + d.second
                        if (toR in 0..7 && toC in 0..7 && boardState[toR][toC].piece == null) {
                            if (piece.type == PieceType.KING || d.first == 1) {
                                allMoves.add(Triple(from, BoardPosition(toR, toC), null))
                            }
                        }
                    }
                }
            }
        }

        if (allMoves.isNotEmpty()) {
            val move = allMoves.random()
            executeMove(move.first, move.second, move.third)
        }

        currentPlayer = PlayerColor.WHITE
    }
}
