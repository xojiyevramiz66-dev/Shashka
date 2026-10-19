import androidx.compose.ui.graphics.Color

// Цвет/Игрок
enum class PlayerColor {
    WHITE, BLACK;

    fun opposite(): PlayerColor = if (this == WHITE) BLACK else WHITE
}

// Тип шашки
enum class PieceType {
    REGULAR, KING
}

// Модель самой шашки
data class CheckerPiece(
    val color: PlayerColor,
    val type: PieceType = PieceType.REGULAR
)

// Координаты на доске (0..7, 0..7)
data class BoardPosition(val row: Int, val col: Int) {
    fun isValid(): Boolean = row in 0..7 && col in 0..7
}

// Модель клетки доски
data class Cell(
    val position: BoardPosition,
    val piece: CheckerPiece? = null,
    val isHighlighted: Boolean = false // Подсветка доступного хода
)
