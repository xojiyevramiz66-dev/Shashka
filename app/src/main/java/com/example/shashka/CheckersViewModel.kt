
package com.example.shashka

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
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
    private var gameGeneration = 0

    private data class Move(
        val from: Position,
        val to: Position,
        val captured: Position? = null
    ) {
        val isCapture: Boolean
            get() = captured != null
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
                val position = Position(row, col)
                val dark = (row + col) % 2 != 0

                val piece = when {
                    dark && row < 3 ->
                        CheckerPiece(PlayerColor.BLACK)

                    dark && row > 4 ->
                        CheckerPiece(PlayerColor.WHITE)

                    else -> null
                }

                Cell(
                    position,
                    piece,
                    isHighlighted = false
                )
            }
        }

    private fun copyBoard(
        board: Array<Array<Cell>>
    ): Array<Array<Cell>> =
        Array(8) { row ->
            Array(8) { col ->
                board[row][col].copy()
            }
        }

    private fun pieceAt(
        board: Array<Array<Cell>>,
        position: Position
    ): CheckerPiece? =
        board[position.row][position.col].piece

    private fun inside(row: Int, col: Int): Boolean =
        row in 0..7 && col in 0..7

    private fun same(a: Position, b: Position): Boolean =
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

        gameGeneration++
        boardState = copyBoard(snapshot.board)
        currentPlayer = snapshot.player
        gameMessage = snapshot.message
        selectedPosition = null
        mandatoryCapturingPiece = null
        isAiThinking = false
        turnSnapshotSaved = false

        clearHighlights()
    }

    fun restartGame() {
        gameGeneration++

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
    // Подсказка
    // ---------------------------------------------------------

    fun showHint(): Boolean {
        if (
            currentPlayer != PlayerColor.WHITE ||
            gameMessage != null ||
            isAiThinking
        ) {
            return false
        }

        val forcedPiece = mandatoryCapturingPiece

        val move = if (forcedPiece != null) {
            legalMovesForPiece(
                boardState,
                forcedPiece,
                PlayerColor.WHITE
            ).firstOrNull()
        } else {
            legalMoves(boardState, PlayerColor.WHITE).firstOrNull()
        } ?: return false

        selectedPosition = move.from
        updateHighlights(move.from)

        return true
    }

    // ---------------------------------------------------------
    // Таймер
    // ---------------------------------------------------------

    fun onTimeExpired(loser: PlayerColor) {
        if (gameMessage != null) return

        gameGeneration++
        isAiThinking = false
        selectedPosition = null
        mandatoryCapturingPiece = null
        clearHighlights()

        gameMessage = if (loser == PlayerColor.WHITE) {
            "Время вышло. Победил ИИ."
        } else {
            "Время ИИ вышло. Вы выиграли!"
        }
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
            -1 to -1,
            -1 to 1,
            1 to -1,
            1 to 1
        )

        for ((dr, dc) in directions) {
            if (piece.type == PieceType.PAWN) {
                // Обычная шашка ходит вперёд.
                val forward =
                    if (piece.color == PlayerColor.WHITE) -1 else 1

                val nextRow = from.row + forward
                val nextCol = from.col + dc

                if (
                    inside(nextRow, nextCol) &&
                    board[nextRow][nextCol].piece == null
                ) {
                    moves.add(
                        Move(
                            from,
                            Position(nextRow, nextCol)
                        )
                    )
                }

                // Обычная шашка может бить во всех четырёх
                // диагональных направлениях.
                val victimRow = from.row + dr
                val victimCol = from.col + dc
                val landingRow = from.row + dr * 2
                val landingCol = from.col + dc * 2

                if (
                    inside(victimRow, victimCol) &&
                    inside(landingRow, landingCol)
                ) {
                    val victim =
                        board[victimRow][victimCol].piece

                    if (
                        victim != null &&
                        victim.color != piece.color &&
                        board[landingRow][landingCol].piece == null
                    ) {
                        moves.add(
                            Move(
                                from = from,
                                to = Position(landingRow, landingCol),
                                captured = Position(victimRow, victimCol)
                            )
                        )
                    }
                }
            } else {
                // Дамка может перемещаться на любое число
                // свободных клеток по диагонали.
                var row = from.row + dr
                var col = from.col + dc

                while (
                    inside(row, col) &&
                    board[row][col].piece == null
                ) {
                    moves.add(
                        Move(from, Position(row, col))
                    )

                    row += dr
                    col += dc
                }

                // Дамка может перепрыгнуть одну чужую шашку
                // и приземлиться на любую свободную клетку
                // за ней по той же диагонали.
                if (inside(row, col)) {
                    val victim = board[row][col].piece

                    if (
                        victim != null &&
                        victim.color != piece.color
                    ) {
                        val captured = Position(row, col)

                        row += dr
                        col += dc

                        while (
                            inside(row, col) &&
                            board[row][col].piece == null
                        ) {
                            moves.add(
                                Move(
                                    from = from,
                                    to = Position(row, col),
                                    captured = captured
                                )
                            )

                            row += dr
                            col += dc
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
        val allMoves = mutableListOf<Move>()

        for (row in 0..7) {
            for (col in 0..7) {
                val position = Position(row, col)

                if (pieceAt(board, position)?.color == color) {
                    allMoves.addAll(
                        rawMovesForPiece(board, position)
                    )
                }
            }
        }

        // Если возможно хотя бы одно взятие, тихие ходы запрещены.
        val captures = allMoves.filter { it.isCapture }

        return if (captures.isNotEmpty()) captures else allMoves
    }

    private fun legalMovesForPiece(
        board: Array<Array<Cell>>,
        position: Position,
        color: PlayerColor
    ): List<Move> =
        legalMoves(board, color).filter {
            same(it.from, position)
        }

    private fun applyMove(
        board: Array<Array<Cell>>,
        move: Move
    ): Array<Array<Cell>> {
        val next = copyBoard(board)
        var piece = next[move.from.row][move.from.col].piece

        next[move.from.row][move.from.col] =
            next[move.from.row][move.from.col].copy(piece = null)

        move.captured?.let { captured ->
            next[captured.row][captured.col] =
                next[captured.row][captured.col].copy(piece = null)
        }

        // Превращение в дамку.
        if (piece != null) {
            if (
                piece.color == PlayerColor.WHITE &&
                move.to.row == 0
            ) {
                piece = piece.copy(type = PieceType.KING)
            } else if (
                piece.color == PlayerColor.BLACK &&
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
        position: Position
    ): List<Move> =
        rawMovesForPiece(board, position).filter {
            it.isCapture
        }

    // Полный ход ИИ включает всю последовательность взятий.
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
            } else {
                collectCaptureContinuations(
                    next,
                    move.to,
                    color,
                    results
                )
            }
        }

        return results
    }

    private fun collectCaptureContinuations(
        board: Array<Array<Cell>>,
        position: Position,
        color: PlayerColor,
        results: MutableList<Array<Array<Cell>>>
    ) {
        val piece = pieceAt(board, position)

        if (piece == null || piece.color != color) {
            results.add(board)
            return
        }

        // После превращения в дамку rawMovesForPiece уже
        // генерирует ходы дамки, включая продолжение взятия.
        val captures = captureMovesForPiece(board, position)

        if (captures.isEmpty()) {
            results.add(board)
            return
        }

        for (move in captures) {
            val next = applyMove(board, move)

            collectCaptureContinuations(
                next,
                move.to,
                color,
                results
            )
        }
    }

    // ---------------------------------------------------------
    // Обработка нажатий
    // ---------------------------------------------------------

    fun onCellClicked(position: Position) {
        if (
            currentPlayer != PlayerColor.WHITE ||
            gameMessage != null ||
            isAiThinking ||
            !inside(position.row, position.col)
        ) {
            return
        }

        val cell = boardState[position.row][position.col]
        val selected = selectedPosition

        if (selected == null) {
            val forcedPiece = mandatoryCapturingPiece

            if (
                forcedPiece != null &&
                !same(forcedPiece, position)
            ) {
                return
            }

            if (cell.piece?.color != PlayerColor.WHITE) return

            val moves = legalMovesForPiece(
                boardState,
                position,
                PlayerColor.WHITE
            )

            if (moves.isEmpty()) return

            selectedPosition = position
            updateHighlights(position)
            return
        }

        if (same(selected, position)) {
            if (mandatoryCapturingPiece == null) {
                selectedPosition = null
                clearHighlights()
            }
            return
        }

        val move = legalMovesForPiece(
            boardState,
            selected,
            PlayerColor.WHITE
        ).firstOrNull {
            same(it.to, position)
        }

        if (move != null) {
            saveTurnSnapshot()

            boardState = applyMove(boardState, move)

            // Продолжение обязательной серии взятий.
            if (
                move.isCapture &&
                captureMovesForPiece(
                    boardState,
                    position
                ).isNotEmpty()
            ) {
                mandatoryCapturingPiece = position
                selectedPosition = position
                updateHighlights(position)
                return
            }

            selectedPosition = null
            mandatoryCapturingPiece = null
            clearHighlights()

            finishHumanTurn()
            return
        }

        // Позволяем выбрать другую шашку, только если
        // серия взятий ещё не началась.
        if (
            mandatoryCapturingPiece == null &&
            cell.piece?.color == PlayerColor.WHITE
        ) {
            val newMoves = legalMovesForPiece(
                boardState,
                position,
                PlayerColor.WHITE
            )

            if (newMoves.isNotEmpty()) {
                selectedPosition = position
                updateHighlights(position)
            }
        }
    }

    private fun finishHumanTurn() {
        if (legalMoves(boardState, PlayerColor.BLACK).isEmpty()) {
            gameMessage = "Поздравляем, вы выиграли!"
            turnSnapshotSaved = false
            return
        }

        currentPlayer = PlayerColor.BLACK
        makeAIMove()
    }

    private fun updateHighlights(position: Position) {
        val destinations = legalMovesForPiece(
            boardState,
            position,
            currentPlayer
        ).map { it.to }

        boardState = Array(8) { row ->
            Array(8) { col ->
                val cellPosition = Position(row, col)

                boardState[row][col].copy(
                    isHighlighted = destinations.any {
                        same(it, cellPosition)
                    }
                )
            }
        }
    }

    private fun clearHighlights() {
        boardState = Array(8) { row ->
            Array(8) { col ->
                boardState[row][col].copy(
                    isHighlighted = false
                )
            }
        }
    }

    // ---------------------------------------------------------
    // Совместимость со вспомогательными функциями
    // ---------------------------------------------------------

    private fun isValidMoveOnBoard(
        board: Array<Array<Cell>>,
        from: Position,
        to: Position
    ): Boolean {
        val piece = pieceAt(board, from) ?: return false

        return legalMovesForPiece(
            board,
            from,
            piece.color
        ).any {
            same(it.to, to)
        }
    }

    private fun findCapturedPiece(
        board: Array<Array<Cell>>,
        from: Position,
        to: Position,
        pieceColor: PlayerColor
    ): Position? {
        val piece = pieceAt(board, from) ?: return null
        if (piece.color != pieceColor) return null

        return rawMovesForPiece(board, from)
            .firstOrNull {
                same(it.to, to) && it.isCapture
            }?.captured
    }

    private fun isCaptureMove(
        from: Position,
        to: Position
    ): Boolean {
        val piece = pieceAt(boardState, from) ?: return false

        return findCapturedPiece(
            boardState,
            from,
            to,
            piece.color
        ) != null
    }

    private fun hasCaptureMovesForPiece(
        board: Array<Array<Cell>>,
        position: Position
    ): Boolean =
        captureMovesForPiece(board, position).isNotEmpty()

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
            .firstOrNull {
                same(it.to, to)
            } ?: return

        if (piece.color != currentPlayer) return

        boardState = applyMove(boardState, move)
    }

    private fun getAllPossibleMovesForBoard(
        board: Array<Array<Cell>>,
        color: PlayerColor
    ): List<Pair<Position, Position>> =
        legalMoves(board, color).map {
            it.from to it.to
        }

    private fun simulateMove(
        board: Array<Array<Cell>>,
        from: Position,
        to: Position
    ): Array<Array<Cell>> {
        val piece = pieceAt(board, from) ?: return copyBoard(board)

        val move = rawMovesForPiece(board, from)
            .firstOrNull {
                same(it.to, to)
            } ?: return copyBoard(board)

        if (piece.color != currentPlayer) {
            // Для поиска ИИ нельзя проверять currentPlayer:
            // ИИ анализирует позиции обоих цветов.
            // Здесь достаточно проверить корректность исходной шашки.
        }

        return applyMove(board, move)
    }

    // ---------------------------------------------------------
    // ИИ: Minimax + alpha-beta
    // ---------------------------------------------------------

    fun makeAIMove() {
        if (
            currentPlayer != PlayerColor.BLACK ||
            gameMessage != null ||
            isAiThinking
        ) {
            return
        }

        isAiThinking = true

        val boardCopy = copyBoard(boardState)
        val difficulty = aiDifficulty
        val generation = gameGeneration

        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.Default) {
                    val depth = when (difficulty) {
                        "Лёгкий" -> 2
                        "Средний" -> 3
                        "Сильный" -> 4
                        "Непобедимый" -> 5
                        else -> 3
                    }

                    findBestTurn(boardCopy, depth)
                }

                // Игнорируем результат устаревшей партии.
                if (generation != gameGeneration) return@launch

                if (
                    gameMessage == null &&
                    currentPlayer == PlayerColor.BLACK
                ) {
                    if (result == null) {
                        gameMessage =
                            "Вы выиграли! У противника нет доступных ходов."
                    } else {
                        boardState = result
                        clearHighlights()

                        if (
                            legalMoves(
                                boardState,
                                PlayerColor.WHITE
                            ).isEmpty()
                        ) {
                            gameMessage =
                                "Вы проиграли! У вас нет доступных ходов."
                        } else {
                            currentPlayer = PlayerColor.WHITE
                        }
                    }

                    turnSnapshotSaved = false
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (generation == gameGeneration) {
                    gameMessage =
                        "Не удалось рассчитать ход ИИ. Начните новую игру."
                }
            } finally {
                if (generation == gameGeneration) {
                    isAiThinking = false
                }
            }
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
        val turns = completeTurns(board, player)

        if (turns.isEmpty()) {
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

            for (next in turns) {
                best = maxOf(
                    best,
                    minimax(
                        next,
                        depth - 1,
                        a,
                        b,
                        PlayerColor.WHITE
                    )
                )

                a = maxOf(a, best)
                if (a >= b) break
            }

            return best
        } else {
            var best = Int.MAX_VALUE

            for (next in turns) {
                best = minOf(
                    best,
                    minimax(
                        next,
                        depth - 1,
                        a,
                        b,
                        PlayerColor.BLACK
                    )
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

        for (row in 0..7) {
            for (col in 0..7) {
                val piece = board[row][col].piece ?: continue

                var value = if (piece.type == PieceType.KING) {
                    320
                } else {
                    100
                }

                if (piece.type == PieceType.PAWN) {
                    val progress =
                        if (piece.color == PlayerColor.BLACK) {
                            row
                        } else {
                            7 - row
                        }

                    value += progress * 5
                }

                // Небольшой бонус за контроль центра.
                if (row in 2..5 && col in 2..5) {
                    value += 5
                }

                val mobility = rawMovesForPiece(
                    board,
                    Position(row, col)
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
