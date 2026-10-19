package com.example.shashka

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
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
        if (currentPlayer != PlayerColor.WHITE) return // Ждем ход ИИ

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
                    if (makeMove(currentSelected, position)) {
                        currentPlayer = PlayerColor.BLACK
                        // Ход ИИ после небольшой паузы или сразу
                        makeAiMove()
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

    private fun makeMove(from: BoardPosition, to: BoardPosition): Boolean {
        val rowDiff = to.row - from.row
        val colDiff = abs(to.col - from.col)
        val movingPiece = boardState[from.row][from.col].piece ?: return false

        // Проверка базового хода (на 1 клетку по диагонали)
        val isForward = if (movingPiece.color == PlayerColor.WHITE) rowDiff == -1 else rowDiff == 1
        if (colDiff == 1 && isForward && boardState[to.row][to.col].piece == null) {
            executeMove(from, to)
            return true
        }
        return false
    }

    private fun executeMove(from: BoardPosition, to: BoardPosition) {
        val newBoard = boardState.map { it.clone() }.toTypedArray()
        var movingPiece = newBoard[from.row][from.col].piece

        // Превращение в дамку
        if (movingPiece != null) {
            if (movingPiece.color == PlayerColor.WHITE && to.row == 0) {
                movingPiece = movingPiece.copy(type = PieceType.KING)
            } else if (movingPiece.color == PlayerColor.BLACK && to.row == 7) {
                movingPiece = movingPiece.copy(type = PieceType.KING)
            }
        }

        newBoard[to.row][to.col] = newBoard[to.row][to.col].copy(piece = movingPiece)
        newBoard[from.row][from.col] = newBoard[from.row][from.col].copy(piece = null)
        boardState = newBoard
        selectedPosition = null
    }

    // Интеллект для черных (ИИ)
    private fun makeAiMove() {
        if (currentPlayer != PlayerColor.BLACK) return

        // Ищем все возможные ходы для черных
        val allMoves = mutableListOf<Pair<BoardPosition, BoardPosition>>()
        
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = boardState[r][c].piece
                if (piece != null && piece.color == PlayerColor.BLACK) {
                    val from = BoardPosition(r, c)
                    // Проверяем 4 направления для хода
                    val directions = listOf(Pair(1, 1), Pair(1, -1))
                    for (dir in directions) {
                        val toRow = r + dir.first
                        val toCol = c + dir.second
                        if (toRow in 0..7 && toCol in 0..7) {
                            val to = BoardPosition(toRow, toCol)
                            if (boardState[toRow][toCol].piece == null) {
                                allMoves.add(Pair(from, to))
                            }
                        }
                    }
                }
            }
        }

        if (allMoves.isNotEmpty()) {
            // Выбираем случайный или первый умный ход (можно расширить алгоритмом оценки)
            // ИИ выбирает ход, который продвигает шашку дальше к краю белых
            val bestMove = allMoves.maxByOrNull { it.second.row } ?: allMoves.random()
            executeMove(bestMove.first, bestMove.second)
        }

        currentPlayer = PlayerColor.WHITE
    }
}
