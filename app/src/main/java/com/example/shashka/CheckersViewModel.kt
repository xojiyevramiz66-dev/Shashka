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
                            delay(400)
                            makeSuperAiMove()
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
        return executeMoveIfValid(boardState, from, to)?.let { newBoard ->
            boardState = newBoard
            selectedPosition = null
            true
        } ?: false
    }

    private fun executeMoveIfValid(board: Array<Array<Cell>>, from: BoardPosition, to: BoardPosition): Array<Array<Cell>>? {
        val rowDiff = to.row - from.row
        val colDiff = to.col - from.col
        val movingPiece = board[from.row][from.col].piece ?: return null

        if (abs(rowDiff) != abs(colDiff)) return null

        val stepRow = if (rowDiff > 0) 1 else -1
        val stepCol = if (colDiff > 0) 1 else -1
        val steps = abs(rowDiff)

        var enemyPos: BoardPosition? = null
        var r = from.row + stepRow
        var c = from.col + stepCol

        for (i in 1 until steps) {
            val piece = board[r][c].piece
            if (piece != null) {
                if (piece.color == movingPiece.color) return null
                if (enemyPos != null) return null
                enemyPos = BoardPosition(r, c)
            }
            r += stepRow
            c += stepCol
        }

        if (board[to.row][to.col].piece != null) return null

        if (movingPiece.type != PieceType.KING) {
            if (steps > 2) return null
            if (steps == 2 && enemyPos == null) return null
            if (steps == 1 && enemyPos != null) return null
            val isForward = if (movingPiece.color == PlayerColor.WHITE) rowDiff == -1 else rowDiff == 1
            if (steps == 1 && !isForward) return null
        }

        val newBoard = board.map { it.clone() }.toTypedArray()
        var finalPiece = movingPiece

        if (finalPiece.color == PlayerColor.WHITE && to.row == 0) finalPiece = finalPiece.copy(type = PieceType.KING)
        if (finalPiece.color == PlayerColor.BLACK && to.row == 7) finalPiece = finalPiece.copy(type = PieceType.KING)

        newBoard[to.row][to.col] = newBoard[to.row][to.col].copy(piece = finalPiece)
        newBoard[from.row][from.col] = newBoard[from.row][from.col].copy(piece = null)

        if (enemyPos != null) {
            newBoard[enemyPos.row][enemyPos.col] = newBoard[enemyPos.row][enemyPos.col].copy(piece = null)
        }

        return newBoard
    }

    private fun getAllValidMoves(board: Array<Array<Cell>>, color: PlayerColor): List<Pair<BoardPosition, BoardPosition>> {
        val moves = mutableListOf<Pair<BoardPosition, BoardPosition>>()
        val captures = mutableListOf<Pair<BoardPosition, BoardPosition>>()

        for (r in 0..7) {
            for (c in 0..7) {
                val piece = board[r][c].piece
                if (piece != null && piece.color == color) {
                    val from = BoardPosition(r, c)
                    for (tr in 0..7) {
                        for (tc in 0..7) {
                            val to = BoardPosition(tr, tc)
                            if (executeMoveIfValid(board, from, to) != null) {
                                val rowDiff = abs(tr - r)
                                if (rowDiff >= 2) {
                                    captures.add(Pair(from, to))
                                } else {
                                    moves.add(Pair(from, to))
                                }
                            }
                        }
                    }
                }
            }
        }
        // В шашках бой обязателен, если есть взятие
        return if (captures.isNotEmpty()) captures else moves
    }

    // Сверхмощный ИИ с оценкой позиции
    private fun makeSuperAiMove() {
        if (currentPlayer != PlayerColor.BLACK) return

        val allMoves = getAllValidMoves(boardState, PlayerColor.BLACK)
        if (allMoves.isNotEmpty()) {
            // Ищем лучший ход на 3 шага вперед (Minimax)
            var bestScore = -999999
            var bestMove = allMoves.random()

            for (move in allMoves) {
                val nextBoard = executeMoveIfValid(boardState, move.first, move.second) ?: continue
                val score = minimax(nextBoard, 3, false, -1000000, 1000000)
                if (score > bestScore) {
                    bestScore = score
                    bestMove = move
                }
            }

            boardState = executeMoveIfValid(boardState, bestMove.first, bestMove.second) ?: boardState
        }

        currentPlayer = PlayerColor.WHITE
    }

    private fun minimax(board: Array<Array<Cell>>, depth: Int, isMaximizing: Boolean, alpha: Int, beta: Int): Int {
        if (depth == 0) {
            return evaluateBoard(board)
        }

        var currentAlpha = alpha
        var currentBeta = beta

        if (isMaximizing) {
            var maxEval = -999999
            val moves = getAllValidMoves(board, PlayerColor.BLACK)
            if (moves.isEmpty()) return -100000
            for (move in moves) {
                val nextBoard = executeMoveIfValid(board, move.first, move.second) ?: continue
                val eval = minimax(nextBoard, depth - 1, false, currentAlpha, currentBeta)
                maxEval = maxOf(maxEval, eval)
                currentAlpha = maxOf(currentAlpha, eval)
                if (currentBeta <= currentAlpha) break
            }
            return maxEval
        } else {
            var minEval = 999999
            val moves = getAllValidMoves(board, PlayerColor.WHITE)
            if (moves.isEmpty()) return 100000
            for (move in moves) {
                val nextBoard = executeMoveIfValid(board, move.first, move.second) ?: continue
                val eval = minimax(nextBoard, depth - 1, true, currentAlpha, currentBeta)
                minEval = minOf(minEval, eval)
                currentBeta = minOf(currentBeta, eval)
                if (currentBeta <= currentAlpha) break
            }
            return minEval
        }
    }

    private fun evaluateBoard(board: Array<Array<Cell>>): Int {
        var score = 0
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = board[r][c].piece
                if (piece != null) {
                    val value = if (piece.type == PieceType.KING) 20 else 10
                    if (piece.color == PlayerColor.BLACK) {
                        score += value + r // Черным выгодно продвигаться вниз
                    } else {
                        score -= value + (7 - r) // Белым выгодно продвигаться вверх
                    }
                }
            }
        }
        return score
    }
}
