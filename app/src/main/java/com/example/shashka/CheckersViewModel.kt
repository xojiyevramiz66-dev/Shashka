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
                Cell(pos, piece, isHighlighted = false)
            }
        }
    }

    fun onCellClicked(pos: Position) {
        if (currentPlayer != PlayerColor.WHITE) return // Ход игрока только за белых

        val currentSelected = selectedPosition
        val cell = boardState[pos.row][pos.col]

        if (currentSelected == null) {
            if (cell.piece?.color == currentPlayer) {
                selectedPosition = pos
                updateHighlights(pos)
            }
        } else {
            if (currentSelected == pos) {
                selectedPosition = null
                clearHighlights()
            } else if (isValidMove(currentSelected, pos)) {
                saveStateToHistory()
                executeMoveWithCapture(currentSelected, pos)
                selectedPosition = null
                clearHighlights()

                // Смена хода на ИИ (черные)
                currentPlayer = PlayerColor.BLACK
                makeAIMove()
            } else if (cell.piece?.color == currentPlayer) {
                selectedPosition = pos
                updateHighlights(pos)
            }
        }
    }

    fun undo() {
        if (moveHistory.isNotEmpty()) {
            boardState = moveHistory.removeAt(moveHistory.lastIndex)
            currentPlayer = PlayerColor.WHITE
            selectedPosition = null
            clearHighlights()
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

    private fun updateHighlights(pos: Position) {
        val newBoard = Array(8) { r ->
            Array(8) { c ->
                val fromCell = boardState[r][c]
                val isValid = isValidMove(pos, Position(r, c))
                fromCell.copy(isHighlighted = isValid)
            }
        }
        boardState = newBoard
    }

    private fun clearHighlights() {
        val newBoard = Array(8) { r ->
            Array(8) { c ->
                boardState[r][c].copy(isHighlighted = false)
            }
        }
        boardState = newBoard
    }

    private fun isValidMove(from: Position, to: Position): Boolean {
        val targetCell = boardState[to.row][to.col]
        if (targetCell.piece != null) return false

        val piece = boardState[from.row][from.col].piece ?: return false
        val rowDiff = to.row - from.row
        val colDiff = kotlin.math.abs(to.col - from.col)

        if (piece.type == PieceType.PAWN) {
            val forwardDir = if (piece.color == PlayerColor.WHITE) -1 else 1
            // Обычный ход на 1 клетку вперед по диагонали
            if (rowDiff == forwardDir && colDiff == 1) return true

            // Рубка через шашку
            if (rowDiff == forwardDir * 2 && colDiff == 2) {
                val middleRow = (from.row + to.row) / 2
                val middleCol = (from.col + to.col) / 2
                val middlePiece = boardState[middleRow][middleCol].piece
                if (middlePiece != null && middlePiece.color != piece.color) return true
            }
            // Шашка назад при рубке
            if (rowDiff == -forwardDir * 2 && colDiff == 2) {
                val middleRow = (from.row + to.row) / 2
                val middleCol = (from.col + to.col) / 2
                val middlePiece = boardState[middleRow][middleCol].piece
                if (middlePiece != null && middlePiece.color != piece.color) return true
            }
        } else if (piece.type == PieceType.KING) {
            // Ходы и рубка для дамки
            if (kotlin.math.abs(rowDiff) == kotlin.math.abs(colDiff)) {
                val rowStep = if (to.row > from.row) 1 else -1
                val colStep = if (to.col > from.col) 1 else -1
                var r = from.row + rowStep
                var c = from.col + colStep
                var opponentCount = 0

                while (r != to.row && c != to.col) {
                    val p = boardState[r][c].piece
                    if (p != null) {
                        if (p.color == piece.color) return false
                        opponentCount++
                    }
                    r += rowStep
                    c += colStep
                }
                return opponentCount <= 1
            }
        }
        return false
    }

    private fun executeMoveWithCapture(from: Position, to: Position) {
        val newBoard = Array(8) { r ->
            Array(8) { c ->
                boardState[r][c].copy(isHighlighted = false)
            }
        }

        var piece = newBoard[from.row][from.col].piece
        newBoard[from.row][from.col] = newBoard[from.row][from.col].copy(piece = null)

        // Если это была рубка, удаляем сбитую фигуру
        if (kotlin.math.abs(to.row - from.row) == 2 && kotlin.math.abs(to.col - from.col) == 2) {
            val middleRow = (from.row + to.row) / 2
            val middleCol = (from.col + to.col) / 2
            newBoard[middleRow][middleCol] = newBoard[middleRow][middleCol].copy(piece = null)
        }

        // Превращение в дамку
        if (piece != null) {
            if (piece.color == PlayerColor.WHITE && to.row == 0) {
                piece = piece.copy(type = PieceType.KING)
            } else if (piece.color == PlayerColor.BLACK && to.row == 7) {
                piece = piece.copy(type = PieceType.KING)
            }
        }

        newBoard[to.row][to.col] = newBoard[to.row][to.col].copy(piece = piece)
        boardState = newBoard
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
                    executeMoveWithCapture(from, to)
                }
                currentPlayer = PlayerColor.WHITE
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
                var pieceValue = if (piece.type == PieceType.KING) 350 else 100

                if (row in 2..5 && col in 2..5) pieceValue += 20

                if (piece.color == PlayerColor.BLACK) {
                    score += pieceValue + (row * 5)
                } else {
                    score -= pieceValue + ((7 - row) * 5)
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
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = board[r][c].piece
                if (piece?.color == color) {
                    val from = Position(r, c)
                    // Проверяем все возможные целевые клетки на доске
                    for (tr in 0..7) {
                        for (tc in 0..7) {
                            val to = Position(tr, tc)
                            if (isValidMoveForSimulatedBoard(board, from, to)) {
                                list.add(Pair(from, to))
                            }
                        }
                    }
                }
            }
        }
        return list
    }

    private fun isValidMoveForSimulatedBoard(
        board: Array<Array<Cell>>,
        from: Position,
        to: Position
    ): Boolean {
        val targetCell = board[to.row][to.col]
        if (targetCell.piece != null) return false

        val piece = board[from.row][from.col].piece ?: return false
        val rowDiff = to.row - from.row
        val colDiff = kotlin.math.abs(to.col - from.col)

        if (piece.type == PieceType.PAWN) {
            val forwardDir = if (piece.color == PlayerColor.WHITE) -1 else 1
            if (rowDiff == forwardDir && colDiff == 1) return true
            if (kotlin.math.abs(rowDiff) == 2 && colDiff == 2) {
                val middleRow = (from.row + to.row) / 2
                val middleCol = (from.col + to.col) / 2
                val middlePiece = board[middleRow][middleCol].piece
                if (middlePiece != null && middlePiece.color != piece.color) return true
            }
        } else if (piece.type == PieceType.KING) {
            if (kotlin.math.abs(rowDiff) == kotlin.math.abs(colDiff)) return true
        }
        return false
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
        newBoard[from.row][from.col] = newBoard[from.row][from.col].copy(piece = null)

        if (kotlin.math.abs(to.row - from.row) == 2 && kotlin.math.abs(to.col - from.col) == 2) {
            val middleRow = (from.row + to.row) / 2
            val middleCol = (from.col + to.col) / 2
            newBoard[middleRow][middleCol] = newBoard[middleRow][middleCol].copy(piece = null)
        }

        if (piece != null) {
            if (piece.color == PlayerColor.WHITE && to.row == 0) {
                piece = piece.copy(type = PieceType.KING)
            } else if (piece.color == PlayerColor.BLACK && to.row == 7) {
                piece = piece.copy(type = PieceType.KING)
            }
        }
        newBoard[to.row][to.col] = newBoard[to.row][to.col].copy(piece = piece)
        return newBoard
    }
}
