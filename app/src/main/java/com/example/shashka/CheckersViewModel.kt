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
        if (currentPlayer != PlayerColor.WHITE) return // Ход ИИ

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
        val colDiff = abs(to.col - from.col)
        val movingPiece = boardState[from.row][from.col].piece ?: return false

        // 1. Обычный ход на 1 клетку по диагонали
        val isForward = if (movingPiece.color == PlayerColor.WHITE) rowDiff == -1 else rowDiff == 1
        if (colDiff == 1 && isForward && boardState[to.row][to.col].piece == null) {
            executeMove(from, to)
            return true
        }

        // 2. Взятие (перепрыгивание) через 2 клетки
        if (abs(rowDiff) == 2 && colDiff == 2) {
            val midRow = (from.row + to.row) / 2
            val midCol = (from.col + to.col) / 2
            val enemyPiece = boardState[midRow][midCol].piece

            if (enemyPiece != null && enemyPiece.color != movingPiece.color) {
                executeMove(from, to, removePosition = BoardPosition(midRow, midCol))
                return true
            }
        }

        return false
    }

    private fun executeMove(from: BoardPosition, to: BoardPosition, removePosition: BoardPosition? = null) {
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

        // Если было взятие — удаляем срубленную шашку
        if (removePosition != null) {
            newBoard[removePosition.row][removePosition.col] = newBoard[removePosition.row][removePosition.col].copy(piece = null)
        }

        boardState = newBoard
        selectedPosition = null
    }

    private fun makeAiMove() {
        if (currentPlayer != PlayerColor.BLACK) return

        val captureMoves = mutableListOf<Triple<BoardPosition, BoardPosition, BoardPosition>>()
        val regularMoves = mutableListOf<Pair<BoardPosition, BoardPosition>>()

        for (r in 0..7) {
            for (c in 0..7) {
                val piece = boardState[r][c].piece
                if (piece != null && piece.color == PlayerColor.BLACK) {
                    val from = BoardPosition(r, c)
                    val dirs = listOf(Pair(1, 1), Pair(1, -1), Pair(-1, 1), Pair(-1, -1))

                    for (d in dirs) {
                        // Проверка взятия
                        val midR = r + d.first
                        val midC = c + d.second
                        val toR = r + d.first * 2
                        val toC = c + d.second * 2

                        if (toR in 0..7 && toC in 0..7) {
                            val midPiece = boardState[midR][midC].piece
                            val targetCell = boardState[toR][toC].piece
                            if (midPiece != null && midPiece.color == PlayerColor.WHITE && targetCell == null) {
                                captureMoves.add(Triple(from, BoardPosition(toR, toC), BoardPosition(midR, midC)))
                            }
                        }

                        // Проверка обычного хода (только вперед для черных)
                        if (d.first == 1 && midR in 0..7 && midC in 0..7) {
                            if (boardState[midR][midC].piece == null) {
                                regularMoves.add(Pair(from, BoardPosition(midR, midC)))
                            }
                        }
                    }
                }
            }
        }

        // Приоритет: сначала срубаем, если есть возможность, иначе обычный ход
        if (captureMoves.isNotEmpty()) {
            val move = captureMoves.random()
            executeMove(move.first, move.second, move.third)
        } else if (regularMoves.isNotEmpty()) {
            val move = regularMoves.maxByOrNull { it.second.row } ?: regularMoves.random()
            executeMove(move.first, move.second)
        }

        currentPlayer = PlayerColor.WHITE
    }
}
