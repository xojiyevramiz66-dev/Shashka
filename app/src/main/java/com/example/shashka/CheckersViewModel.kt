package com.example.shashka

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CheckersViewModel : ViewModel() {

    var aiDifficulty by mutableStateOf("Непобедимый")
    var boardState by mutableStateOf(createInitialBoard())
        private set

    var selectedPosition by mutableStateOf<Position?>(null)
        private set

    var currentPlayer by mutableStateOf(PlayerColor.WHITE)
        private set

    private val moveHistory = mutableListOf<Array<Array<Cell>>>()

    private fun createInitialBoard(): Array<Array<Cell>> {
        return Array(8) { row ->
            Array(8) { col ->
                val pos = Position(row, col)
                val isDark = (row + col) % 2 != 0
                val piece = when {
                    isDark && row < 3 -> CheckerPiece(PlayerColor.BLACK)
                    isDark && row > 4 -> CheckerPiece(PlayerColor.WHITE)
                    else -> null
                }
                Cell(pos, piece)
            }
        }
    }

    fun onCellClicked(pos: Position) {
        val currentSelected = selectedPosition
        val cell = boardState[pos.row][pos.col]

        if (currentSelected == null) {
            if (cell.piece?.color == currentPlayer) {
                selectedPosition = pos
            }
        } else {
            if (currentSelected == pos) {
                selectedPosition = null
            } else if (isValidMove(currentSelected, pos)) {
                saveStateToHistory()
                executeMove(currentSelected, pos)
                selectedPosition = null
                
                currentPlayer = if (currentPlayer == PlayerColor.WHITE) PlayerColor.BLACK else PlayerColor.WHITE

                if (currentPlayer == PlayerColor.BLACK) {
                    makeAIMove()
                }
            } else if (cell.piece?.color == currentPlayer) {
                selectedPosition = pos
            }
        }
    }

    fun undo() {
        if (moveHistory.isNotEmpty()) {
            boardState = moveHistory.removeAt(moveHistory.lastIndex)
            currentPlayer = PlayerColor.WHITE
            selectedPosition = null
        }
    }

    private fun saveStateToHistory() {
        val copy = Array(8) { r ->
            Array(8) { c ->
                boardState[r][c].copy()
            }
        }
        moveHistory.add(copy)
    }

    private fun isValidMove(from: Position, to: Position): Boolean {
        val targetCell = boardState[to.row][to.col]
        if (targetCell.piece != null) return false
        val rowDiff = kotlin.math.abs(to.row - from.row)
        val colDiff = kotlin.math.abs(to.col - from.col)
        return (rowDiff == 1 && colDiff == 1) || (rowDiff == 2 && colDiff == 2)
    }

    fun makeAIMove() {
        viewModelScope.launch(Dispatchers.Default) {
            val depth = when (aiDifficulty) {
                "Лёгкий" -> 1
                "Средний" -> 3
                else -> 5
            }

            val bestMove = findBestMove(depth)
            
            launch(Dispatchers.Main) {
                bestMove?.let { (from, to) ->
                    saveStateToHistory()
                    executeMove(from, to)
                    currentPlayer = PlayerColor.WHITE
                }
            }
        }
    }

    private fun findBestMove(depth: Int): Pair<Position, Position>? {
        var bestScore = Int.MIN_VALUE
        var bestMove: Pair<Position, Position>? = null

        val availableMoves = getAllPossibleMovesForBoard(boardState, PlayerColor.BLACK)

        for (move in availableMoves) {
            val simulatedBoard = simulateMove(boardState, move.first, move.second)
            val score = minimax(simulatedBoard, depth - 1, Int.MIN_VALUE, Int.MAX_VALUE, false)

            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
        }
        return bestMove
    }

    private fun minimax(
        board: Array<Array<Cell>>,
        depth: Int,
        alpha: Int,
        beta: Int,
        isMaximizing: Boolean
    ): Int {
        if (depth == 0) return evaluateBoard(board)

        var currentAlpha = alpha
        var currentBeta = beta

        if (isMaximizing) {
            var maxEval = Int.MIN_VALUE
            val moves = getAllPossibleMovesForBoard(board, PlayerColor.BLACK)
            for (move in moves) {
                val nextBoard = simulateMove(board, move.first, move.second)
                val eval = minimax(nextBoard, depth - 1, currentAlpha, currentBeta, false)
                maxEval = maxOf(maxEval, eval)
                currentAlpha = maxOf(currentAlpha, eval)
                if (currentBeta <= currentAlpha) break
            }
            return maxEval
        } else {
            var minEval = Int.MAX_VALUE
            val moves = getAllPossibleMovesForBoard(board, PlayerColor.WHITE)
            for (move in moves) {
                val nextBoard = simulateMove(board, move.first, move.second)
                val eval = minimax(nextBoard, depth - 1, currentAlpha, currentBeta, true)
                minEval = minOf(minEval, eval)
                currentBeta = minOf(currentBeta, eval)
                if (currentBeta <= currentAlpha) break
            }
            return minEval
        }
    }

    private fun evaluateBoard(board: Array<Array<Cell>>): Int {
        var score = 0
        for (row in 0..7) {
            for (col in 0..7) {
                val piece = board[row][col].piece ?: continue
                var pieceValue = if (piece.type == PieceType.KING) 300 else 100

                if (row in 2..5 && col in 2..5) pieceValue += 15
                if (col == 0 || col == 7) pieceValue += 10

                if (piece.type == PieceType.PAWN) {
                    if (piece.color == PlayerColor.BLACK) {
                        pieceValue += row * 5
                    } else {
                        pieceValue += (7 - row) * 5
                    }
                }

                if (piece.color == PlayerColor.BLACK) {
                    score += pieceValue
                } else {
                    score -= pieceValue
                }
            }
        }
        return score
    }

    private fun getAllPossibleMovesForBoard(
        board: Array<Array<Cell>>,
        color: PlayerColor
    ): List<Pair<Position, Position>> {
        val list = mutableListOf<Pair<Position, Position>>()
        val direction = if (color == PlayerColor.BLACK) 1 else -1

        for (r in 0..7) {
            for (c in 0..7) {
                val piece = board[r][c].piece
                if (piece?.color == color) {
                    val targetRow = r + direction
                    if (targetRow in 0..7) {
                        if (c - 1 >= 0 && board[targetRow][c - 1].piece == null) {
                            list.add(Pair(Position(r, c), Position(targetRow, c - 1)))
                        }
                        if (c + 1 <= 7 && board[targetRow][c + 1].piece == null) {
                            list.add(Pair(Position(r, c), Position(targetRow, c + 1)))
                        }
                    }
                }
            }
        }
        return list
    }

    private fun simulateMove(
        board: Array<Array<Cell>>,
        from: Position,
        to: Position
    ): Array<Array<Cell>> {
        val newBoard = Array(8) { r ->
            Array(8) { c ->
                board[r][c].copy()
            }
        }
        var piece = newBoard[from.row][from.col].piece
        if (piece != null) {
            if (piece.color == PlayerColor.BLACK && to.row == 7) {
                piece = piece.copy(type = PieceType.KING)
            } else if (piece.color == PlayerColor.WHITE && to.row == 0) {
                piece = piece.copy(type = PieceType.KING)
            }
        }
        newBoard[from.row][from.col] = newBoard[from.row][from.col].copy(piece = null)
        newBoard[to.row][to.col] = newBoard[to.row][to.col].copy(piece = piece)
        return newBoard
    }

    private fun executeMove(from: Position, to: Position) {
        boardState = simulateMove(boardState, from, to)
    }
}
