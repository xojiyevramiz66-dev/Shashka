
package com.example.shashka

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CheckersViewModel : ViewModel() {

    var aiDifficulty by mutableStateOf("Сильный")

    var boardState by mutableStateOf(createInitialBoard())
        private set

    var selectedPosition by mutableStateOf<Position?>(null)
        private set

    var currentPlayer by mutableStateOf(PlayerColor.WHITE)
        private set

    var gameMessage by mutableStateOf<String?>(null)
        private set

    var isAiThinking by mutableStateOf(false)
        private set

    private var mandatoryCapturingPiece: Position? = null
    private var turnSnapshotSaved = false

    private data class Move(
        val from: Position,
        val to: Position,
        val captured: Position? = null
    ) {
        val isCapture: Boolean get() = captured != null
    }

    private data class Snapshot(
        val board: Array<Array<Cell>>,
        val player: PlayerColor,
        val message: String?
    )

    private val moveHistory = mutableListOf<Snapshot>()

    private fun createInitialBoard(): Array<Array<Cell>> =
        Array(8) { row ->
            Array(8) { col ->
                val pos = Position(row, col)
                val dark = (row + col) % 2 != 0

                val piece = when {
                    dark && row < 3 -> CheckerPiece(PlayerColor.BLACK)
                    dark && row > 4 -> CheckerPiece(PlayerColor.WHITE)
                    else -> null
                }

                Cell(pos, piece, isHighlighted = false)
            }
        }

    private fun copyBoard(
        board: Array<Array<Cell>>
    ): Array<Array<Cell>> =
        Array(8) { r ->
            Array(8) { c -> board[r][c].copy() }
        }

    private fun pieceAt(
        board: Array<Array<Cell>>,
        pos: Position
    ) = board[pos.row][pos.col].piece

    private fun inside(row: Int, col: Int) =
        row in 0..7 && col in 0..7

    private fun same(a: Position, b: Position) =
        a.row == b.row && a.col == b.col

    private fun saveTurnSnapshot() {
        if (turnSnapshotSaved) return

        moveHistory.add(
            Snapshot(
                board = copyBoard(boardState),
                player = currentPlayer,
                message = gameMessage
            )
        )
        turnSnapshotSaved = true
    }

    fun undo() {
        if (isAiThinking || moveHistory.isEmpty()) return

        val snapshot = moveHistory.removeAt(moveHistory.lastIndex)
        boardState = copyBoard(snapshot.board)
        currentPlayer = snapshot.player
        gameMessage = snapshot.message
        selectedPosition = null
        mandatoryCapturingPiece = null
        turnSnapshotSaved = false
        clearHighlights()
    }

    fun restartGame() {
        boardState = createInitialBoard()
        currentPlayer = PlayerColor.WHITE
        selectedPosition = null
        mandatoryCapturingPiece = null
        gameMessage = null
        isAiThinking = false
        turnSnapshotSaved = false
        moveHistory.clear()
    }

    // ---------------------------------------------------------
    // Правила русских шашек
    // ---------------------------------------------------------

    private fun rawMovesForPiece(
        board: Array<Array<Cell>>,
        from: Position
    ): List<Move> {
        val piece = pieceAt(board, from) ?: return emptyList()
        val moves = mutableListOf<Move>()

        val directions = listOf(
            -1 to -1, -1 to 1,
             1 to -1,  1 to 1
        )

        for ((dr, dc) in directions) {
            if (piece.type == PieceType.PAWN) {
                // Обычная шашка ходит вперёд на одну клетку.
                val forward = if (piece.color == PlayerColor.WHITE) -1 else 1

                val nr = from.row + forward
                val nc1 = from.col + dc

                if (inside(nr, nc1) &&
                    board[nr][nc1].piece == null
                ) {
                    moves.add(
                        Move(from, Position(nr, nc1))
                    )
                }

                // Но бьёт обычная шашка во всех четырёх направлениях.
                val cr = from.row + dr
                val cc = from.col + dc
                val lr = from.row + dr * 2
                val lc = from.col + dc * 2

                if (inside(cr, cc) && inside(lr, lc)) {
                    val victim = board[cr][cc].piece

                    if (victim != null &&
                        victim.color != piece.color &&
                        board[lr][lc].piece == null
                    ) {
                        moves.add(
                            Move(
                                from,
                                Position(lr, lc),
                                Position(cr, cc)
                            )
                        )
                    }
                }
            } else {
                // Дамка: дальний тихий ход по диагонали.
                var r = from.row + dr
                var c = from.col + dc

                while (inside(r, c) &&
                    board[r][c].piece == null
                ) {
                    moves.add(
                        Move(from, Position(r, c))
                    )
                    r += dr
                    c += dc
                }

                // Дамка может перепрыгнуть одну чужую шашку
                // и приземлиться на любую свободную клетку за ней.
                if (inside(r, c)) {
                    val victim = board[r][c].piece

                    if (victim != null &&
                        victim.color != piece.color
                    ) {
                        val captured = Position(r, c)
                        r += dr
                        c += dc

                        while (inside(r, c) &&
                            board[r][c].piece == null
                        ) {
                            moves.add(
                                Move(
                                    from,
                                    Position(r, c),
                                    captured
                                )
                            )
                            r += dr
                            c += dc
                        }
                    }
                }
            }
        }

        return moves
    }

    private fun legalMoves(
        board: Array<Array<Cell>>,
        color: PlayerColor
    ): List<Move> {
        val all = mutableListOf<Move>()

        for (r in 0..7) {
            for (c in 0..7) {
                val pos = Position(r, c)
                if (pieceAt(board, pos)?.color == color) {
                    all.addAll(rawMovesForPiece(board, pos))
                }
            }
        }

        val captures = all.filter { it.isCapture }
        return if (captures.isNotEmpty()) captures else all
    }

    private fun legalMovesForPiece(
        board: Array<Array<Cell>>,
        pos: Position,
        color: PlayerColor
    ): List<Move> =
        legalMoves(board, color).filter { same(it.from, pos) }

    private fun applyMove(
        board: Array<Array<Cell>>,
        move: Move
    ): Array<Array<Cell>> {
        val next = copyBoard(board)
        var piece = next[move.from.row][move.from.col].piece

        next[move.from.row][move.from.col] =
            next[move.from.row][move.from.col].copy(piece = null)

        if (move.captured != null) {
            val p = move.captured
            next[p.row][p.col] =
                next[p.row][p.col].copy(piece = null)
        }

        if (piece != null) {
            if (piece.color == PlayerColor.WHITE &&
                move.to.row == 0
            ) {
                piece = piece.copy(type = PieceType.KING)
            } else if (piece.color == PlayerColor.BLACK &&
                move.to.row == 7
            ) {
                piece = piece.copy(type = PieceType.KING)
            }
        }

        next[move.to.row][move.to.col] =
            next[move.to.row][move.to.col].copy(piece = piece)

        return next
    }

    private fun captureMovesForPiece(
        board: Array<Array<Cell>>,
        pos: Position
    ): List<Move> =
        rawMovesForPiece(board, pos).filter { it.isCapture }

    // Генерирует полный ход: один обычный ход либо всю серию взятий.
    private fun completeTurns(
        board: Array<Array<Cell>>,
        color: PlayerColor
    ): List<Array<Array<Cell>>> {
        val results = mutableListOf<Array<Array<Cell>>>()
        val moves = legalMoves(board, color)

        for (move in moves) {
            val next = applyMove(board, move)

            if (!move.isCapture) {
                results.add(next)
                continue
            }

            collectCaptureContinuations(
                next,
                move.to,
                color,
                results
            )
        }

        return results
    }

    private fun collectCaptureContinuations(
        board: Array<Array<Cell>>,
        pos: Position,
        color: PlayerColor,
        results: MutableList<Array<Array<Cell>>>
    ) {
        val piece = pieceAt(board, pos)

        if (piece == null || piece.color != color) {
            results.add(board)
            return
        }

        val captures = captureMovesForPiece(board, pos)

        if (captures.isEmpty()) {
            results.add(board)
            return
        }

        for (move in captures) {
            val next = applyMove(board, move)

            // В русских шашках после превращения в дамку
            // серия взятий может продолжаться уже как дамкой.
            collectCaptureContinuations(
                next,
                move.to,
                color,
                results
            )
        }
    }

    // ---------------------------------------------------------
    // Обработка касаний на игровой доске
    // ---------------------------------------------------------

    fun onCellClicked(pos: Position) {
        if (currentPlayer != PlayerColor.WHITE ||
            gameMessage != null ||
            isAiThinking ||
            !inside(pos.row, pos.col)
        ) return

        val cell = boardState[pos.row][pos.col]
        val selected = selectedPosition

        if (selected == null) {
            if (mandatoryCapturingPiece != null &&
                !same(mandatoryCapturingPiece!!, pos)
            ) return

            if (cell.piece?.color != PlayerColor.WHITE) return

            val moves = legalMovesForPiece(
                boardState, pos, PlayerColor.WHITE
            )
            if (moves.isEmpty()) return

            selectedPosition = pos
            updateHighlights(pos)
            return
        }

        if (same(selected, pos)) {
            if (mandatoryCapturingPiece == null) {
                selectedPosition = null
                clearHighlights()
            }
            return
        }

        val move = legalMovesForPiece(
            boardState, selected, PlayerColor.WHITE
        ).firstOrNull { same(it.to, pos) }

        if (move != null) {
            saveTurnSnapshot()

            boardState = applyMove(boardState, move)

            if (move.isCapture &&
                captureMovesForPiece(boardState, pos).isNotEmpty()
            ) {
                mandatoryCapturingPiece = pos
                selectedPosition = pos
                updateHighlights(pos)
                return
            }

            selectedPosition = null
            mandatoryCapturingPiece = null
            clearHighlights()

            finishHumanTurn()
            return
        }

        if (mandatoryCapturingPiece == null &&
            cell.piece?.color == PlayerColor.WHITE
        ) {
            val newMoves = legalMovesForPiece(
                boardState, pos, PlayerColor.WHITE
            )

            if (newMoves.isNotEmpty()) {
                selectedPosition = pos
                updateHighlights(pos)
            }
        }
    }

    private fun finishHumanTurn() {
        if (legalMoves(boardState, PlayerColor.BLACK).isEmpty()) {
            gameMessage = "Поздравляем, вы выиграли!"
            return
        }

        currentPlayer = PlayerColor.BLACK
        makeAIMove()
    }

    private fun updateHighlights(pos: Position) {
        val destinations = legalMovesForPiece(
            boardState, pos, currentPlayer
        ).map { it.to }

        boardState = Array(8) { r ->
            Array(8) { c ->
                val p = Position(r, c)
                boardState[r][c].copy(
                    isHighlighted = destinations.any { same(it, p) }
                )
            }
        }
    }

    private fun clearHighlights() {
        boardState = Array(8) { r ->
            Array(8) { c ->
                boardState[r][c].copy(isHighlighted = false)
            }
        }
    }

    // Совместимость с исходными функциями проекта.
    private fun isValidMoveOnBoard(
        board: Array<Array<Cell>>,
        from: Position,
        to: Position
    ): Boolean =
        legalMovesForPiece(
            board,
            from,
            pieceAt(board, from)?.color ?: return false
        ).any { same(it.to, to) }

    private fun findCapturedPiece(
        board: Array<Array<Cell>>,
        from: Position,
        to: Position,
        pieceColor: PlayerColor
    ): Position? =
        rawMovesForPiece(board, from)
            .firstOrNull {
                same(it.to, to) && it.isCapture
            }?.captured

    private fun isCaptureMove(
        from: Position,
        to: Position
    ): Boolean =
        findCapturedPiece(
            boardState,
            from,
            to,
            pieceAt(boardState, from)?.color ?: return false
        ) != null

    private fun hasCaptureMovesForPiece(
        board: Array<Array<Cell>>,
        pos: Position
    ): Boolean =
        captureMovesForPiece(board, pos).isNotEmpty()

    private fun hasAnyCapturesOnBoard(
        color: PlayerColor
    ): Boolean =
        legalMoves(boardState, color).any { it.isCapture }

    private fun executeMoveWithCapture(
        from: Position,
        to: Position
    ) {
        val piece = pieceAt(boardState, from) ?: return
        val move = rawMovesForPiece(boardState, from)
            .firstOrNull { same(it.to, to) } ?: return
        boardState = applyMove(boardState, move)
    }

    private fun getAllPossibleMovesForBoard(
        board: Array<Array<Cell>>,
        color: PlayerColor
    ): List<Pair<Position, Position>> =
        legalMoves(board, color).map { it.from to it.to }

    private fun simulateMove(
        board: Array<Array<Cell>>,
        from: Position,
        to: Position
    ): Array<Array<Cell>> {
        val color = pieceAt(board, from)?.color
            ?: return copyBoard(board)

        val move = rawMovesForPiece(board, from)
            .firstOrNull { same(it.to, to) }
            ?: return copyBoard(board)

        return applyMove(board, move)
    }

    // ---------------------------------------------------------
    // ИИ: Minimax + alpha-beta + оценка позиции
    // ---------------------------------------------------------

    fun makeAIMove() {
        if (currentPlayer != PlayerColor.BLACK ||
            gameMessage != null ||
            isAiThinking
        ) return

        isAiThinking = true
        val boardCopy = copyBoard(boardState)
        val difficulty = aiDifficulty

        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                val depth = when (difficulty) {
                    "Лёгкий" -> 2
                    "Средний" -> 3
                    "Сильный" -> 4
                    "Непобедимый" -> 5
                    else -> 4
                }

                findBestTurn(boardCopy, depth)
            }

            if (gameMessage == null &&
                currentPlayer == PlayerColor.BLACK
            ) {
                if (result == null) {
                    gameMessage = "Вы выиграли! У противника нет ходов."
                } else {
                    boardState = result
                    clearHighlights()

                    if (legalMoves(
                            boardState,
                            PlayerColor.WHITE
                        ).isEmpty()
                    ) {
                        gameMessage =
                            "Вы проиграли! У вас не осталось ходов."
                    } else {
                        currentPlayer = PlayerColor.WHITE
                    }
                }
            }

            isAiThinking = false
        }
    }

    private fun findBestTurn(
        board: Array<Array<Cell>>,
        depth: Int
    ): Array<Array<Cell>>? {
        val turns = completeTurns(board, PlayerColor.BLACK)
        if (turns.isEmpty()) return null

        var bestBoard: Array<Array<Cell>>? = null
        var bestScore = Int.MIN_VALUE
        var alpha = Int.MIN_VALUE + 1
        val beta = Int.MAX_VALUE

        for (next in turns) {
            val score = minimax(
                next,
                depth - 1,
                alpha,
                beta,
                PlayerColor.WHITE
            )

            if (score > bestScore) {
                bestScore = score
                bestBoard = next
            }

            alpha = maxOf(alpha, bestScore)
        }

        return bestBoard
    }

    private fun minimax(
        board: Array<Array<Cell>>,
        depth: Int,
        alpha: Int,
        beta: Int,
        player: PlayerColor
    ): Int {
        val moves = completeTurns(board, player)

        if (moves.isEmpty()) {
            return if (player == PlayerColor.BLACK) {
                -100_000 - depth
            } else {
                100_000 + depth
            }
        }

        if (depth <= 0) return evaluateBoard(board)

        var a = alpha
        var b = beta

        if (player == PlayerColor.BLACK) {
            var best = Int.MIN_VALUE + 1

            for (next in moves) {
                best = maxOf(
                    best,
                    minimax(next, depth - 1, a, b, PlayerColor.WHITE)
                )
                a = maxOf(a, best)
                if (a >= b) break
            }

            return best
        } else {
            var best = Int.MAX_VALUE

            for (next in moves) {
                best = minOf(
                    best,
                    minimax(next, depth - 1, a, b, PlayerColor.BLACK)
                )
                b = minOf(b, best)
                if (a >= b) break
            }

            return best
        }
    }

    private fun evaluateBoard(
        board: Array<Array<Cell>>
    ): Int {
        var score = 0
        var blackCount = 0
        var whiteCount = 0

        for (r in 0..7) {
            for (c in 0..7) {
                val piece = board[r][c].piece ?: continue

                var value = if (piece.type == PieceType.KING) {
                    320
                } else {
                    100
                }

                if (piece.type == PieceType.PAWN) {
                    val progress = if (piece.color == PlayerColor.BLACK) {
                        r
                    } else {
                        7 - r
                    }
                    value += progress * 5
                }

                if (r in 2..5 && c in 2..5) value += 5

                val mobility = rawMovesForPiece(
                    board, Position(r, c)
                ).size

                value += minOf(mobility, 4) * 2

                if (piece.color == PlayerColor.BLACK) {
                    score += value
                    blackCount++
                } else {
                    score -= value
                    whiteCount++
                }
            }
        }

        if (whiteCount == 0) return 100_000
        if (blackCount == 0) return -100_000

        return score
    }
}
