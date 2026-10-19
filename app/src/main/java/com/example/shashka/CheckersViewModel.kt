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

    private var mustCaptureFrom: BoardPosition? = null

    fun onCellClicked(position: BoardPosition) {
        if (currentPlayer != PlayerColor.WHITE) return

        val clickedCell = boardState[position.row][position.col]
        val currentSelected = selectedPosition

        if (mustCaptureFrom != null) {
            if (currentSelected == null) {
                if (position == mustCaptureFrom) {
                    selectedPosition = position
                }
            } else {
                val moveResult = executeMoveIfValid(boardState, currentSelected, position)
                if (moveResult != null && moveResult.second != null) {
                    boardState = moveResult.first
                    val newPos = position
                    if (getPossibleCapturesForPiece(boardState, newPos).isNotEmpty()) {
                        selectedPosition = newPos
                        mustCaptureFrom = newPos
                    } else {
                        selectedPosition = null
                        mustCaptureFrom = null
                        currentPlayer = PlayerColor.BLACK
                        viewModelScope.launch {
                            delay(400)
                            makeSuperAiMove()
                        }
                    }
                } else {
                    selectedPosition = null
                }
            }
            return
        }

        if (currentSelected == null) {
            if (clickedCell.piece != null && clickedCell.piece.color == currentPlayer) {
                selectedPosition = position
            }
        } else {
            if (currentSelected == position) {
                selectedPosition = null
            } else {
                if (clickedCell.piece == null) {
                    val moveResult = executeMoveIfValid(boardState, currentSelected, position)
                    if (moveResult != null) {
                        boardState = moveResult.first
                        val isCapture = moveResult.second != null
                        
                        if (isCapture && getPossibleCapturesForPiece(boardState, position).isNotEmpty()) {
                            selectedPosition = position
                            mustCaptureFrom = position
                        } else {
                            selectedPosition = null
                            currentPlayer = PlayerColor.BLACK
                            viewModelScope.launch {
                                delay(400)
                                makeSuperAiMove()
                            }
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

    private fun executeMoveIfValid(board: Array<Array<Cell>>, from: BoardPosition, to: BoardPosition): Pair<Array<Array<Cell>>, BoardPosition?>? {
        val rowDiff = to.row - from.row
        val colDiff = to.col - from.col
        val movingPiece = board[from.row][from.col].piece ?: return null

        // Ход должен быть строго по диагонали
        if (abs(rowDiff) != abs(colDiff)) return null
        if (board[to.row][to.col].piece != null) return null // Клетка назначения занята

        val stepRow = if (rowDiff > 0) 1 else -1
        val stepCol = if (colDiff > 0) 1 else -1
        val steps = abs(rowDiff)

        var enemyPos: BoardPosition? = null
        var r = from.row + stepRow
        var c = from.col + stepCol

        // Проверяем путь между начальной и конечной клеткой
        for (i in 1 until steps) {
            val piece = board[r][c].piece
            if (piece != null) {
                if (piece.color == movingPiece.color) return null // Своя шашка на пути
                if (enemyPos != null) return null // Вторая вражеская шашка подряд (нельзя перепрыгнуть две)
                enemyPos = BoardPosition(r, c)
            }
            r += stepRow
            c += stepCol
        }

        if (movingPiece.type == PieceType.KING) {
            // Дамка может ходить на любое расстояние и рубить, если на пути ровно одна вражеская шашка
            if (steps < 1) return null
        } else {
            // Обычная шашка
            if (steps > 2) return null
            if (steps == 2 && enemyPos == null) return null
            if (steps == 1 && enemyPos != null) return null
            val isForward = if (movingPiece.color == PlayerColor.WHITE) rowDiff == -1 else rowDiff == 1
            if (steps == 1 && !isForward) return null
        }

        val newBoard = board.map { it.clone() }.toTypedArray()
        var finalPiece = movingPiece

        // Превращение в дамку при достижении края
        if (finalPiece.color == PlayerColor.WHITE && to.row == 0) finalPiece = finalPiece.copy(type = PieceType.KING)
        if (finalPiece.color == PlayerColor.BLACK && to.row == 7) finalPiece = finalPiece.copy(type = PieceType.KING)

        newBoard[to.row][to.col] = newBoard[to.row][to.col].copy(piece = finalPiece)
        newBoard[from.row][from.col] = newBoard[from.row][from.col].copy(piece = null)

        if (enemyPos != null) {
            newBoard[enemyPos.row][enemyPos.col] = newBoard[enemyPos.row][enemyPos.col].copy(piece = null)
        }

        return Pair(newBoard, enemyPos)
    }

    private fun getPossibleCapturesForPiece(board: Array<Array<Cell>>, pos: BoardPosition): List<BoardPosition> {
        val captures = mutableListOf<BoardPosition>()
        for (tr in 0..7) {
            for (tc in 0..7) {
                val to = BoardPosition(tr, tc)
                val res = executeMoveIfValid(board, pos, to)
                if (res != null && res.second != null) {
                    captures.add(to)
                }
            }
        }
        return captures
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
                            val res = executeMoveIfValid(board, from, to)
                            if (res != null) {
                                if (res.second != null) {
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
        return if (captures.isNotEmpty()) captures else moves
    }

    private fun makeSuperAiMove() {
        if (currentPlayer != PlayerColor.BLACK) return

        var currentBoard = boardState
        var aiMovesCount = 0

        while (aiMovesCount < 3) {
            val allMoves = getAllValidMoves(currentBoard, PlayerColor.BLACK)
            val captureMoves = allMoves.filter { move ->
                val res = executeMoveIfValid(currentBoard, move.first, move.second)
                res?.second != null
            }

            val movesToConsider = if (captureMoves.isNotEmpty()) captureMoves else allMoves
            if (movesToConsider.isEmpty()) break

            var bestScore = -999999
            var bestMove = movesToConsider.random()

            for (move in movesToConsider) {
                val nextBoard = executeMoveIfValid(currentBoard, move.first, move.second)?.first ?: continue
                val score = minimax(nextBoard, 2, false, -1000000, 1000000)
                if (score > bestScore) {
                    bestScore = score
                    bestMove = move
                }
            }

            val result = executeMoveIfValid(currentBoard, bestMove.first, bestMove.second) ?: break
            currentBoard = result.first
            boardState = currentBoard

            val landingPos = bestMove.second
            val nextCaptures = getPossibleCapturesForPiece(currentBoard, landingPos)
            if (result.second != null && nextCaptures.isNotEmpty()) {
                aiMovesCount++
                try { Thread.sleep(300) } catch (e: Exception) {}
            } else {
                break
            }
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
                val nextBoard = executeMoveIfValid(board, move.first, move.second)?.first ?: continue
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
                val nextBoard = executeMoveIfValid(board, move.first, move.second)?.first ?: continue
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
                    val value = if (piece.type == PieceType.KING) 30 else 10
                    if (piece.color == PlayerColor.BLACK) {
                        score += value + r
                    } else {
                        score -= value + (7 - r)
                    }
                }
            }
        }
        return score
    }
}
