package com.example.shashka

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Точный цвет фона из палитры референса (#1A1A1A)
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF1A1A1A)
            ) {
                CheckersScreen()
            }
        }
    }
}

@Composable
fun CheckersScreen(viewModel: CheckersViewModel = viewModel()) {
    val board = viewModel.boardState
    val selected = viewModel.selectedPosition
    val currentPlayer = viewModel.currentPlayer

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Верхняя панель (Заголовок / Индикатор хода)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Кнопка возврата назад (стилизованная)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .shadow(4.dp, RoundedCornerShape(12.dp))
                    .background(Color(0xFF2C2C2C), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF444444), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "←", color = Color(0xFF555555), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                text = if (currentPlayer == PlayerColor.WHITE) "Ваш ход" else "Ход соперника",
                color = Color(0xFFE8DCC6),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            // Кнопка настроек
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .shadow(4.dp, RoundedCornerShape(12.dp))
                    .background(Color(0xFF2C2C2C), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF444444), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "⚙️", fontSize = 18.sp)
            }
        }

        // Профиль ИИ / Соперника сверху
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF111111), CircleShape)
                        .border(1.5.dp, Color(0xFF444444), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // Миниатюра черной фишки
                    Box(modifier = Modifier.size(20.dp).background(Color(0xFF1A1A1A), CircleShape).border(1.dp, Color(0xFF444444), CircleShape))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = "Противник", color = Color(0xFF555555), fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
            Text(text = "09:45", color = Color(0xFF555555), fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }

        // Игровая доска в стиле макета (Дерево #3E2F1F с текстурной окантовкой)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .shadow(24.dp, RoundedCornerShape(16.dp))
                .border(8.dp, Color(0xFF3E2F1F), RoundedCornerShape(16.dp))
                .background(Color(0xFF3E2F1F))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                for (row in 0..7) {
                    Row(modifier = Modifier.weight(1f)) {
                        for (col in 0..7) {
                            val cell = board[row][col]
                            val isSelected = selected?.row == row && selected?.col == col
                            
                            // Проверяем, является ли клетка возможным ходом для подсветки
                            // (в будущем для расширения, пока подсвечиваем выбранную желтым)
                            val cellColor = when {
                                isSelected -> Color(0xFFF4C430) // Золотой цвет выбора из палитры
                                (row + col) % 2 != 0 -> Color(0xFF3E2F1F) // Темное дерево
                                else -> Color(0xFFE8DCC6) // Светлые клетки (слоновая кость / бежевый)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(cellColor)
                                    .clickable { viewModel.onCellClicked(cell.position) },
                                contentAlignment = Alignment.Center
                            ) {
                                cell.piece?.let { piece ->
                                    ReferenceCheckerPieceView(piece = piece)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Профиль игрока снизу
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFE8DCC6), CircleShape)
                        .border(1.5.dp, Color(0xFFF4C430), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "👑", fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = "Вы", color = Color(0xFFE8DCC6), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Text(text = "09:52", color = Color(0xFF555555), fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }

        // Нижняя панель с кнопками управления (Подсказка, Назад, Меню)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ActionButton(icon = "↩")
            ActionButton(icon = "💡")
            ActionButton(icon = "☰")
        }
    }
}

@Composable
fun ActionButton(icon: String) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .background(Color(0xFF252525), RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF383838), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = icon, color = Color(0xFFE8DCC6), fontSize = 22.sp)
    }
}

@Composable
fun ReferenceCheckerPieceView(piece: CheckerPiece) {
    val isWhite = piece.color == PlayerColor.WHITE

    // Градиенты с фасками точь-в-точь как на вашем концепте
    val outerBrush = if (isWhite) {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0xFFF5EEDC),
                Color(0xFFE8DCC6),
                Color(0xFFB5A48B)
            )
        )
    } else {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF333333),
                Color(0xFF222222),
                Color(0xFF111111),
                Color(0xFF000000)
            )
        )
    }

    val rimColor = if (isWhite) Color(0xFFC4B299) else Color(0xFF0A0A0A)

    Box(
        modifier = Modifier
            .fillMaxSize(0.82f)
            .shadow(8.dp, CircleShape)
            .background(outerBrush, shape = CircleShape)
            .border(2.5.dp, rimColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Концентрическая фаска (первое кольцо)
        Box(
            modifier = Modifier
                .fillMaxSize(0.72f)
                .border(
                    width = 2.dp,
                    color = if (isWhite) Color(0xFFFFFFFF) else Color(0xFF444444),
                    shape = CircleShape
                )
                .border(
                    width = 3.dp,
                    color = if (isWhite) Color(0xFFD4C2A8) else Color(0xFF1F1F1F),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Внутренний круг/углубление фишки
            Box(
                modifier = Modifier
                    .fillMaxSize(0.52f)
                    .background(
                        brush = if (isWhite) {
                            Brush.radialGradient(listOf(Color(0xFFFFFDF9), Color(0xFFD6C5AC)))
                        } else {
                            Brush.radialGradient(listOf(Color(0xFF262626), Color(0xFF0D0D0D)))
                        },
                        shape = CircleShape
                    )
                    .border(
                        width = 1.5.dp,
                        color = if (isWhite) Color(0xFFB8A68C) else Color(0xFF050505),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (piece.type == PieceType.KING) {
                    // Золотая корона для дамки (точно как на макете #F4C430)
                    Text(
                        text = "👑",
                        fontSize = 18.sp
                    )
                } else {
                    // Центральная выемка
                    Box(
                        modifier = Modifier
                            .fillMaxSize(0.4f)
                            .border(
                                width = 1.dp,
                                color = if (isWhite) Color(0xFFFFFFFF) else Color(0xFF555555),
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}
