
package com.example.shashka

import kotlin.math.pow
import kotlin.math.roundToInt

/** Результат завершённой партии. */
enum class GameResult {
    WIN,
    LOSS,
    DRAW
}

/** Звание игрока. */
data class RankTier(
    val name: String,
    val minRating: Int,
    val description: String
)

/** Все звания, от начального до высшего. */
val rankTiers = listOf(
    RankTier("Новичок", 0, "Начало пути"),
    RankTier("Любитель", 1100, "Первые успехи"),
    RankTier("Тактик", 1300, "Уверенные комбинации"),
    RankTier("Эксперт", 1500, "Сильная игра"),
    RankTier("Мастер", 1700, "Высокий уровень"),
    RankTier("Легенда", 2000, "Элита Shashka")
)

/** Возвращает звание по рейтингу. */
fun rankForRating(rating: Int): RankTier =
    rankTiers.lastOrNull { rating >= it.minRating }
        ?: rankTiers.first()

/** Условный рейтинг соперника для каждого уровня ИИ. */
fun opponentRatingForDifficulty(difficulty: String): Int =
    when (difficulty) {
        "Лёгкий" -> 800
        "Средний" -> 1000
        "Сильный" -> 1200
        "Непобедимый" -> 1400
        else -> 1000
    }

/**
 * Изменение ELO.
 * K = 32 — коэффициент изменения рейтинга.
 */
fun calculateEloChange(
    playerRating: Int,
    opponentRating: Int,
    result: GameResult,
    kFactor: Int = 32
): Int {
    val expectedScore = 1.0 / (
        1.0 + 10.0.pow(
            (opponentRating - playerRating) / 400.0
        )
    )

    val actualScore = when (result) {
        GameResult.WIN -> 1.0
        GameResult.DRAW -> 0.5
        GameResult.LOSS -> 0.0
    }

    return (
        kFactor * (actualScore - expectedScore)
    ).roundToInt()
}

/**
 * Определяет результат по сообщениям,
 * которые используются в текущей игре.
 *
 * Если сообщение не означает завершение партии,
 * возвращает null.
 */
fun resultFromGameMessage(
    message: String
): GameResult? {
    val text = message.lowercase()

    return when {
        "не удалось рассчитать ход ии" in text -> null

        "поздравляем, вы выиграли" in text -> GameResult.WIN
        "вы выиграли" in text -> GameResult.WIN
        "ваша победа" in text -> GameResult.WIN

        "вы проиграли" in text -> GameResult.LOSS
        "вы проиграли!" in text -> GameResult.LOSS
        "победил ии" in text -> GameResult.LOSS

        "ничья" in text -> GameResult.DRAW
        "draw" in text -> GameResult.DRAW

        else -> null
    }
}
