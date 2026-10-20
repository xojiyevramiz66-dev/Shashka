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

    // Сообщение об окончании игры (победа / поражение)
    var gameMessage by mutableStateOf<String?>(null)
        private set

    // Хранит позицию шашки, которая обязана продолжить бить (в режиме серии ударов)
    private var mandatoryCapturingPiece: Position? = null

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
        if (currentPlayer != PlayerColor.WHITE || gameMessage != null) return

        val currentSelected = selectedPosition
        val cell = boardState[pos.row][pos.col]

        if (currentSelected == null) {
            // Если идет серия взятий, можно выбирать только ту же шашку, которая бьет дальше
            if (mandatoryCapturingPiece != null && mandatoryCapturingPiece != pos) return

            if (cell.piece?.color == currentPlayer) {
                // Если включено обязательное взятие, проверим, есть ли у этой шашки доступные удары
                if (mandatoryCapturingPiece == null && hasAnyCapturesOnBoard(currentPlayer) && !hasCaptureMovesForPiece(boardState, pos)) {
                    return // Нельзя ходить простой шашкой, если можно бить
                }
                selectedPosition = pos
                updateHighlights(pos)
            }
        } else {
            if (currentSelected == pos) {
                // Если серия взятий не в процессе, можно отменить выбор
                if (mandatoryCapturingPiece == null) {
                    selectedPosition = null
                    clearHighlights()
                }
            } else if (isValidMoveOnBoard(boardState, currentSelected, pos)) {
                saveStateToHistory()
                val isCapture = isCaptureMove(currentSelected, pos)
                val movedPiece = boardState[currentSelected.row][currentSelected.col].piece

                executeMoveWithCapture(currentSelected, pos)

                // Проверяем, может ли эта же шашка продолжить бить (серия из 2-3 прыжков)
                if (isCapture && movedPiece != null && hasCaptureMovesForPiece(boardState, pos)) {
                    // Продолжаем ход той же шашкой
                    selectedPosition = pos
                    mandatoryCapturingPiece = pos
                    updateHighlights(pos)
                } else {
                    // Серия завершена, проверяем, остались ли ходы у ИИ (Черных)
                    selectedPosition = null
                    mandatoryCapturingPiece = null
                    clearHighlights()

                    if (getAllPossibleMovesForBoard(boardState, PlayerColor.BLACK).isEmpty()) {
                        gameMessage = "Поздравляем, вы выиграли! У противника нет ходов."
                        return
                    }

                    currentPlayer = PlayerColor.BLACK
                    makeAIMove()
                }
            } else if (mandatoryCapturingPiece == null && cell.piece?.color == currentPlayer) {
                selectedPosition = pos
                updateHighlights(pos)
            }
        }
    }

    fun undo() {
        if (mandatoryCapturingPiece != null || gameMessage != null) return
        if (moveHistory.isNotEmpty()) {
            boardState = moveHistory.removeAt(moveHistory.lastIndex)
            currentPlayer = PlayerColor.WHITE
            selectedPosition = null
            mandatoryCapturingPiece = null
            clearHighlights()
            gameMessage = null
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
                val isValid = isValidMoveOnBoard(boardState, pos, Position(r, c))
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

    private fun findCapturedPiece(board: Array<Array<Cell>>, from: Position, to: Position, pieceColor: PlayerColor): Position? {
        val rowDiff = to.row - from.row
        val colDiff = to.col - from.col
        if (kotlin.math.abs(rowDiff) != kotlin.math.abs(colDiff)) return null

        val rowStep = if (rowDiff > 0) 1 else -1
        val colStep = if (colDiff > 0) 1 else -1

        var r = from.row + rowStep
        var c = from.col + colStep
        var capturedPos: Position? = null
        var opponentCount = 0

        while (r != to.row && c != to.col) {
            val p = board[r][c].piece
            if (p != null) {
                if (p.color == pieceColor) return null
                opponentCount++
                capturedPos = Position(r, c)
            }
            r += rowStep
            c += colStep
        }
        return if (opponentCount == 1) capturedPos else null
    }

    private fun isCaptureMove(from: Position, to: Position): Boolean {
        val piece = boardState[from.row][from.col].piece ?: return false
        return findCapturedPiece(boardState, from, to, piece.color) != null
    }

    private fun hasCaptureMovesForPiece(board: Array<Array<Cell>>, pos: Position): Boolean {
        val piece = board[pos.row][pos.col].piece ?: return false
        for (r in 0..7) {
            for (c in 0..7) {
                val to = Position(r, c)
                if (isValidMoveOnBoard(board, pos, to) && findCapturedPiece(board, pos, to, piece.color) != null) {
                    return true
                }
            }
        }
        return false
    }

    private fun hasAnyCapturesOnBoard(color: PlayerColor): Boolean {
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = boardState[r][c].piece
                if (piece?.color == color) {
                    if (hasCaptureMovesForPiece(boardState, Position(r, c))) return true
                }
            }
        }
        return false
    }

    private fun isValidMoveOnBoard(board: Array<Array<Cell>>, from: Position, to: Position): Boolean {
        if (board[to.row][to.col].piece != null) return false
        val piece = board[from.row][from.col].piece ?: return false
        val rowDiff = to.row - from.row
        val colDiff = to.col - from.col
        val steps = kotlin.math.abs(rowDiff)

        if (kotlin.math.abs(rowDiff) != kotlin.math.abs(colDiff)) return false

        if (mandatoryCapturingPiece != null && mandatoryCapturingPiece != from) return false

        if (piece.type == PieceType.PAWN) {
            val forwardDir = if (piece.color == PlayerColor.WHITE) -1 else 1
            if (steps == 1 && mandatoryCapturingPiece == null) {
                return rowDiff == forwardDir
            } else if (steps == 2) {
                val captured = findCapturedPiece(board, from, to, piece.color)
                return captured != null && board[captured.row][captured.col].piece?.color != piece.color
            }
        } else if (piece.type == PieceType.KING) {
            val captured = findCapturedPiece(board, from, to, piece.color)
            val rowStep = if (rowDiff > 0) 1 else -1
            val colStep = if (colDiff > 0) 1 else -1
            var r = from.row + rowStep
            var c = from.col + colStep
            var blocked = false

            while (r != to.row && c != to.col) {
                val p = board[r][c].piece
                if (p != null) {
                    if (captured == null || r != captured.row || c != captured.col) {
                        blocked = true
                        break
                    }
                }
                r += rowStep
                c += colStep
            }
            return !blocked && (captured != null || mandatoryCapturingPiece == null)
        }
        return false
    }

    private fun executeMoveWithCapture(from: Position, to: Position) {
        val newBoard = Array(8) { r ->
            Array(8) { c ->
                boardState[r][c].copy(isHighlighted = false)
            }
        }

        val piece = newBoard[from.row][from.col].piece
        newBoard[from.row][from.col] = newBoard[from.row][from.col].copy(piece = null)

        if (piece != null) {
            val capturedPos = findCapturedPiece(newBoard, from, to, piece.color)
            if (capturedPos != null) {
                newBoard[capturedPos.row][capturedPos.col] = newBoard[capturedPos.row][capturedPos.col].copy(piece = null)
            }
        }

        var updatedPiece = piece
        if (updatedPiece != null) {
            if (updatedPiece.color == PlayerColor.WHITE && to.row == 0) {
                updatedPiece = updatedPiece.copy(type = PieceType.KING)
            } else if (updatedPiece.color == PlayerColor.BLACK && to.row == 7) {
                updatedPiece = updatedPiece.copy(type = PieceType.KING)
            }
        }

        newBoard[to.row][to.col] = newBoard[to.row][to.col].copy(piece = updatedPiece)
        boardState = newBoard
    }

    fun makeAIMove() {
        if (currentPlayer != PlayerColor.BLACK || gameMessage != null) return

        viewModelScope.launch(Dispatchers.Default) {
            val depth = when (aiDifficulty) {
                "Лёгкий" -> 1
                "Средний" -> 3
                else -> 5
            }

            val bestMove = findBestMove(depth)

            launch(Dispatchers.Main) {
                if (bestMove != null) {
                    saveStateToHistory()
                    executeMoveWithCapture(bestMove.first, bestMove.second)
                }

                // Проверяем, остались ли ходы у игрока (Белых) после хода ИИ
                if (getAllPossibleMovesForBoard(boardState, PlayerColor.WHITE).isEmpty()) {
                    gameMessage = "Вы проиграли! У вас не осталось доступных ходов."
                } else {
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
                    for (tr in 0..7) {
                        for (tc in 0..7) {
                            val to = Position(tr, tc)
                            if (isValidMoveOnBoard(board, from, to)) {
                                list.add(Pair(from, to))
                            }
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
        val piece = newBoard[from.row][from.col].piece
        newBoard[from.row][from.col] = newBoard[from.row][from.col].copy(piece = null)

        if (piece != null) {
            val capturedPos = findCapturedPiece(newBoard, from, to, piece.color)
            if (capturedPos != null) {
                newBoard[capturedPos.row][capturedPos.col] = newBoard[capturedPos.row][capturedPos.col].copy(piece = null)
            }
        }

        var updatedPiece = piece
        if (updatedPiece != null) {
            if (updatedPiece.color == PlayerColor.WHITE && to.row == 0) {
                updatedPiece = updatedPiece.copy(type = PieceType.KING)
            } else if (updatedPiece.color == PlayerColor.BLACK && to.row == 7) {
                updatedPiece = updatedPiece.copy(type = PieceType.KING)
            }
        }
        newBoard[to.row][to.col] = newBoard[to.row][to.col].copy(piece = updatedPiece)
        return newBoard
    }
}
