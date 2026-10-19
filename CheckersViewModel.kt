import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class CheckersViewModel : ViewModel() {

    // Состояние доски (массив 8x8)
    var boardState by mutableStateOf(createInitialBoard())
        private set

    // Чей сейчас ход (начинают белые)
    var currentTurn by mutableStateOf(PlayerColor.WHITE)
        private set

    // Выбранная в данный момент позиция шашки
    var selectedPosition by mutableStateOf<BoardPosition?>(null)
        private set

    // Список позиций, куда текущая выбранная шашка может походить
    private var validMoves: List<BoardPosition> = emptyList()

    // Обработка клика по клетке
    fun onCellClicked(position: BoardPosition) {
        val clickedCell = boardState[position.row][position.col]

        // 1. Если кликнули на подсвеченную клетку — делаем ход
        if (position in validMoves) {
            executeMove(selectedPosition!!, position)
            return
        }

        // 2. Если кликнули на свою шашку — выбираем её и ищем ходы
        if (clickedCell.piece != null && clickedCell.piece.color == currentTurn) {
            selectedPosition = position
            validMoves = calculateValidMoves(position, clickedCell.piece)
            updateBoardHighlights()
            return
        }

        // 3. Если кликнули в пустую или чужую клетку (и это не ход) — сбрасываем выбор
        clearSelection()
    }

    // Расчет доступных ходов для шашки
    private fun calculateValidMoves(pos: BoardPosition, piece: CheckerPiece): List<BoardPosition> {
        val moves = mutableListOf<BoardPosition>()
        val row = pos.row
        val col = pos.col

        // Направления движения: обычная белая ходит вверх (row - 1), черная вниз (row + 1)
        // Дамка ходит в любые 4 стороны по диагонали
        val rowDirections = if (piece.type == PieceType.KING) {
            listOf(-1, 1)
        } else {
            when (piece.color) {
                PlayerColor.WHITE -> listOf(-1)
                PlayerColor.BLACK -> listOf(1)
            }
        }

        val colDirections = listOf(-1, 1)

        for (rd in rowDirections) {
            for (cd in colDirections) {
                val nextRow = row + rd
                val nextCol = col + cd
                val nextPos = BoardPosition(nextRow, nextCol)

                if (nextPos.isValid()) {
                    val targetCell = boardState[nextRow][nextCol]
                    if (targetCell.piece == null) {
                        // Простое движение на пустую клетку
                        moves.add(nextPos)
                    } else if (targetCell.piece.color != piece.color) {
                        // Потенциальный сруб (прыжок через чужую шашку)
                        val jumpRow = nextRow + rd
                        val jumpCol = nextCol + cd
                        val jumpPos = BoardPosition(jumpRow, jumpCol)
                        if (jumpPos.isValid() && boardState[jumpRow][jumpCol].piece == null) {
                            moves.add(jumpPos)
                        }
                    }
                }
            }
        }

        return moves
    }

    // Выполнение хода
    private fun executeMove(from: BoardPosition, to: BoardPosition) {
        val newBoard = boardState.map { row -> row.map { it.copy() }.toTypedArray() }.toTypedArray()
        val movingPiece = newBoard[from.row][from.col].piece

        // Убираем шашку со старого места
        newBoard[from.row][from.col] = newBoard[from.row][from.col].copy(piece = null)

        // Проверяем, был ли это прыжок (сруб) — если между ними больше 1 клетки по ряду
        if (kotlin.math.abs(to.row - from.row) == 2) {
            val middleRow = (from.row + to.row) / 2
            val middleCol = (from.col + to.col) / 2
            newBoard[middleRow][middleCol] = newBoard[middleRow][middleCol].copy(piece = null)
        }

        // Превращение в дамку
        var finalPiece = movingPiece
        if (movingPiece != null && movingPiece.type == PieceType.REGULAR) {
            if ((movingPiece.color == PlayerColor.WHITE && to.row == 0) ||
                (movingPiece.color == PlayerColor.BLACK && to.row == 7)
            ) {
                finalPiece = movingPiece.copy(type = PieceType.KING)
            }
        }

        // Ставим шашку на новое место
        newBoard[to.row][to.col] = newBoard[to.row][to.col].copy(piece = finalPiece)

        // Обновляем состояние и меняем ход
        boardState = newBoard
        clearSelection()
        currentTurn = currentTurn.opposite()
    }

    // Сброс выбора
    private fun clearSelection() {
        selectedPosition = null
        validMoves = emptyList()
        updateBoardHighlights()
    }

    // Подсветка клеток на доске
    private fun updateBoardHighlights() {
        boardState = boardState.mapRowIndexed { rowIdx, row ->
            row.mapColIndexed { colIdx, cell ->
                val pos = BoardPosition(rowIdx, colIdx)
                cell.copy(isHighlighted = pos in validMoves)
            }
        }
    }
}

// Утилитарные расширения для удобного маппинга массива 8x8
private inline fun Array<Array<Cell>>.mapRowIndexed(transform: (Int, Array<Cell>) -> Array<Cell>): Array<Array<Cell>> {
    return Array(size) { i -> transform(i, this[i]) }
}

private inline fun Array<Cell>.mapColIndexed(transform: (Int, Cell) -> Cell): Array<Cell> {
    return Array(size) { j -> transform(j, this[j]) }
}
