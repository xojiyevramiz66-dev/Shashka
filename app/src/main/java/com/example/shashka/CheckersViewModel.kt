    fun onCellClicked(position: BoardPosition) {
        val clickedCell = boardState[position.row][position.col]
        
        val currentSelected = selectedPosition
        if (currentSelected == null) {
            // Выбираем шашку текущего игрока
            if (clickedCell.piece != null && clickedCell.piece.color == currentPlayer) {
                selectedPosition = position
            }
        } else {
            if (currentSelected == position) {
                // Снимаем выделение при повторном клике
                selectedPosition = null
            } else {
                // Пытаемся сделать ход на пустую клетку
                if (clickedCell.piece == null) {
                    val rowDiff = position.row - currentSelected.row
                    val colDiff = kotlin.math.abs(position.col - currentSelected.col)
                    
                    // Проверяем простое движение на 1 клетку по диагонали
                    val isForward = if (currentPlayer == PlayerColor.WHITE) rowDiff == -1 else rowDiff == 1
                    
                    if (colDiff == 1 && isForward) {
                        // Создаем новый массив доски для обновления состояния Compose
                        val newBoard = boardState.map { it.clone() }.toTypedArray()
                        val movingPiece = newBoard[currentSelected.row][currentSelected.col].piece
                        
                        // Переносим шашку на новую клетку
                        newBoard[position.row][position.col] = newBoard[position.row][position.col].copy(piece = movingPiece)
                        newBoard[currentSelected.row][currentSelected.col] = newBoard[currentSelected.row][currentSelected.col].copy(piece = null)
                        
                        boardState = newBoard
                        selectedPosition = null
                        // Переход хода к другому игроку
                        currentPlayer = if (currentPlayer == PlayerColor.WHITE) PlayerColor.BLACK else PlayerColor.WHITE
                    } else {
                        selectedPosition = null
                    }
                } else {
                    // Если кликнули на другую свою шашку — переключаем выбор
                    if (clickedCell.piece.color == currentPlayer) {
                        selectedPosition = position
                    } else {
                        selectedPosition = null
                    }
                }
            }
        }
    }
