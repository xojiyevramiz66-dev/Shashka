package com.example.shashka

enum class PlayerColor {
    WHITE, BLACK
}

enum class PieceType {
    PAWN, KING
}

data class Position(val row: Int, val col: Int)

data class CheckerPiece(
    val color: PlayerColor,
    val type: PieceType = PieceType.PAWN
)

data class Cell(
    val position: Position,
    val piece: CheckerPiece? = null,
    val isHighlighted: Boolean = false
)
