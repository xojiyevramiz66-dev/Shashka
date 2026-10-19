package com.example.shashka

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CheckersViewModel : ViewModel() {

    // Состояние сложности: "Лёгкий", "Средний", "Непобедимый"
    var aiDifficulty by mutableStateOf("Непобедимый")

    // ... Ваши текущие переменные boardState, selectedPosition, currentPlayer ...

    /**
     * Вызов хода ИИ
     */
    fun makeAIMove() {
        viewModelScope.launch(Dispatchers.Default) {
            val depth = when (aiDifficulty) {
                "Лёгкий" -> 1
                "Средний" -> 3
                else -> 6 // "Непобедимый" — глубокий просчет на 6 шагов
            }

            val bestMove = findBestMove(depth)
            
            launch(Dispatchers.Main) {
                bestMove?.let { (from, to) ->
                    // Выполняем лучший найденный ход на доске
                    executeMove(from, to)
                }
            }
        }
    }

    /**
     * Главная функция поиска наилучшего хода через Alpha-Beta Pruning
     */
    private fun findBestMove(depth: Int): Pair<Position, Position>? {
        var bestScore = Int.MIN_VALUE
        var bestMove: Pair<Position, Position>? = null

        val availableMoves = getAllPossibleMoves(PlayerColor.BLACK) // Предполагаем, что ИИ играет черными

        for (move in availableMoves) {
            // Эмулируем ход
            val simulatedBoard = simulateMove(boardState, move.first, move.second)
            
            // Оцениваем позицию алгоритмом Минимакс
            val score = minimax(
                board = simulatedBoard,
                depth = depth - 1,
                alpha = Int.MIN_VALUE,
                beta = Int.MAX_VALUE,
                isMaximizing = false
            )

            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
        }
        return bestMove
    }

    /**
     * Алгоритм Минимакс с Альфа-Бета отсечением
     */
    private fun minimax(
        board: Array<Array<Cell>>,
        depth: Int,
        alpha: Int,
        beta: Int,
        isMaximizing: Boolean
    ): Int {
        if (depth == 0 /* или игра окончена */) {
            return evaluateBoard(board)
        }

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
                if (currentBeta <= currentAlpha) break // Альфа-отсечение
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
                if (currentBeta <= currentAlpha) break // Бета-отсечение
            }
            return minEval
        }
    }

    /**
     * Функция глубокой оценки позиций на доске
     */
    private fun evaluateBoard(board: Array<Array<Cell>>): Int {
        var score = 0

        for (row in 0..7) {
            for (col in 0..7) {
                val piece = board[row][col].piece ?: continue
                
                // Базовая ценность фигуры
                var pieceValue = if (piece.type == PieceType.KING) 300 else 100

                // Бонус за контроль центра (стратегические клетки)
                if (row in 2..5 && col in 2..5) {
                    pieceValue += 15
                }

                // Бонус за безопасные боковые позиции
                if (col == 0 || col == 7) {
                    pieceValue += 10
                }

                // Бонус за продвижение обычных шашек ближе к дамкам
                if (piece.type == PieceType.PAWN) {
                    if (piece.color == PlayerColor.BLACK) {
                        pieceValue += row * 5 // Чем ниже черная шашка, тем лучше
                    } else {
                        pieceValue += (7 - row) * 5 // Чем выше белая шашка, тем лучше
                    }
                }

                // Плюсуем ИИ (черным) и минусуем Игроку (белым)
                if (piece.color == PlayerColor.BLACK) {
                    score += pieceValue
                } else {
                    score -= pieceValue
                }
            }
        }
        return score
    }

    // Вспомогательные функции эмуляции и получения всех возможных ходов
    private fun getAllPossibleMoves(color: PlayerColor): List<Pair<Position, Position>> {
        return getAllPossibleMovesForBoard(boardState, color)
    }

    private fun getAllPossibleMovesForBoard(
        board: Array<Array<Cell>>,
        color: PlayerColor
    ): List<Pair<Position, Position>> {
        val list = mutableListOf<Pair<Position, Position>>()
        // Логика сбора всех валидных ходов для указанного цвета
        return list
    }

    private fun simulateMove(
        board: Array<Array<Cell>>,
        from: Position,
        to: Position
    ): Array<Array<Cell>> {
        // Создание копии состояния доски и выполнение хода для гипотетической оценки
        return board
    }

    private fun executeMove(from: Position, to: Position) {
        // Выполнение хода на реальной доске
    }
}
