
package com.example.shashka

import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val RankGold = Color(0xFFF4C430)
private val RankPanel = Color(0xFF242424)
private val RankText = Color(0xFFE8DCC6)
private val RankMuted = Color(0xFFBDBDBD)

@Composable
fun RankSection(
    gameMessage: String?,
    difficulty: String,
    prefs: SharedPreferences
) {
    var rating by remember {
        mutableIntStateOf(
            prefs.getInt("player_rating", 1000)
        )
    }

    var bestRating by remember {
        mutableIntStateOf(
            prefs.getInt("best_rating", 1000)
        )
    }

    var lastChange by remember {
        mutableIntStateOf(
            prefs.getInt("last_rating_change", 0)
        )
    }

    var showDetails by remember {
        mutableStateOf(false)
    }

    var showPromotion by remember {
        mutableStateOf(false)
    }

    var ratingRecorded by remember {
        mutableStateOf(false)
    }

    var promotionName by remember {
        mutableStateOf("")
    }

    /*
     * Регистрируем результат только один раз.
     * При новой партии gameMessage снова становится null,
     * после чего можно записывать следующий результат.
     */
    LaunchedEffect(gameMessage) {
        if (gameMessage == null) {
            ratingRecorded = false
            return@LaunchedEffect
        }

        if (ratingRecorded) return@LaunchedEffect

        val result = resultFromGameMessage(gameMessage)
            ?: return@LaunchedEffect

        ratingRecorded = true

        val oldRating = rating
        val oldRank = rankForRating(oldRating)

        val change = calculateEloChange(
            playerRating = oldRating,
            opponentRating = opponentRatingForDifficulty(
                difficulty
            ),
            result = result
        )

        val newRating = (oldRating + change).coerceAtLeast(100)
        val newBest = maxOf(bestRating, newRating)
        val newRank = rankForRating(newRating)

        rating = newRating
        bestRating = newBest
        lastChange = newRating - oldRating

        prefs.edit()
            .putInt("player_rating", newRating)
            .putInt("best_rating", newBest)
            .putInt("last_rating_change", lastChange)
            .apply()

        if (
            newRank.name != oldRank.name &&
            newRating > oldRating
        ) {
            promotionName = newRank.name
            showPromotion = true
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDetails = true },
        color = RankPanel,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 14.dp,
                vertical = 10.dp
            ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "🏆 ${rankForRating(rating).name}",
                    color = RankGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )

                Text(
                    text = "Рекорд: $bestRating ELO",
                    color = RankMuted,
                    fontSize = 11.sp
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "$rating ELO",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                val changeText = when {
                    lastChange > 0 -> "+$lastChange ELO"
                    lastChange < 0 -> "$lastChange ELO"
                    else -> "Рейтинг"
                }

                Text(
                    text = changeText,
                    color = when {
                        lastChange > 0 -> Color(0xFF81C784)
                        lastChange < 0 -> Color(0xFFEF9A9A)
                        else -> RankMuted
                    },
                    fontSize = 11.sp
                )
            }
        }
    }

    if (showDetails) {
        val currentRank = rankForRating(rating)

        AlertDialog(
            onDismissRequest = { showDetails = false },
            containerColor = Color(0xFF1E1E1E),
            titleContentColor = RankGold,
            textContentColor = RankText,
            title = {
                Text(
                    "🏆 Звания Shashka",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Text(
                        "Текущее звание: ${currentRank.name}",
                        color = RankGold,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "Рейтинг: $rating ELO",
                        color = Color.White
                    )

                    Text(
                        "Лучший рейтинг: $bestRating ELO",
                        color = RankMuted
                    )

                    HorizontalDivider(
                        color = Color(0xFF424242)
                    )

                    rankTiers.forEachIndexed { index, tier ->
                        val upperBound = rankTiers
                            .getOrNull(index + 1)
                            ?.minRating
                            ?.minus(1)

                        val range = if (upperBound == null) {
                            "${tier.minRating}+"
                        } else {
                            "${tier.minRating}–$upperBound"
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = if (
                                        tier.name == currentRank.name
                                    ) {
                                        Color(0xFF39301B)
                                    } else {
                                        Color.Transparent
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp),
                            horizontalArrangement =
                                Arrangement.SpaceBetween,
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = tier.name,
                                    color = if (
                                        tier.name == currentRank.name
                                    ) RankGold else Color.White,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = tier.description,
                                    color = RankMuted,
                                    fontSize = 11.sp
                                )
                            }

                            Text(
                                text = "$range ELO",
                                color = RankMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showDetails = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RankGold,
                        contentColor = Color(0xFF141414)
                    )
                ) {
                    Text("Закрыть")
                }
            }
        )
    }

    if (showPromotion) {
        AlertDialog(
            onDismissRequest = { showPromotion = false },
            containerColor = Color(0xFF1E1E1E),
            titleContentColor = RankGold,
            textContentColor = Color.White,
            title = {
                Text("🎉 Новое звание!")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Поздравляем! Ты получил звание:"
                    )

                    Text(
                        promotionName,
                        color = RankGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )

                    Text("Твой рейтинг: $rating ELO")
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPromotion = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RankGold,
                        contentColor = Color(0xFF141414)
                    )
                ) {
                    Text("Продолжить")
                }
            }
        )
    }
}
