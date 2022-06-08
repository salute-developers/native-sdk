package ru.sberdevices.sdk.demoapp.ui.gestures.controller

import ru.sberdevices.common.logger.Logger
import ru.sberdevices.pub.demoapp.repository.Gesture
import ru.sberdevices.services.pub.demoapp.R

private const val GRID_ROWS_SIZE = 3
private const val GRID_COLS_SIZE = 5

internal class GridController() {

    private val logger = Logger.get("GridController")

    private var currPosition = 0 to 0

    private val tileGrid = MutableList(GRID_ROWS_SIZE) {
        MutableList(GRID_COLS_SIZE) { Tile(tileColor = R.color.sbdv_solid_gray) }
    }

    val gridSize = GRID_COLS_SIZE

    fun moveTile(gridAdapter: GridAdapter, gesture: Gesture) {
        logger.debug { "moveTile, gesture: $gesture" }

        val currPosition = currPosition
        val (currRow, currCol) = currPosition

        val (row, col) = when (gesture) {
            Gesture.SWIPE_UP -> currPosition.copy(first = currRow - 1)
            Gesture.SWIPE_DOWN -> currPosition.copy(first = currRow + 1)
            Gesture.SWIPE_LEFT -> currPosition.copy(second = currCol - 1)
            Gesture.SWIPE_RIGHT -> currPosition.copy(second = currCol + 1)
        }

        colorTile(gridAdapter, row, col)
    }

    fun colorTile(gridAdapter: GridAdapter, newRow: Int, newCol: Int) {
        logger.verbose { "colorTile: $newRow, $newCol" }

        tileGrid[currPosition.first][currPosition.second] = Tile(tileColor = R.color.sbdv_solid_gray)

        val row = when {
            newRow < 0 -> tileGrid.size - 1
            newRow == tileGrid.size -> 0
            else -> newRow
        }

        val col = when {
            newCol < 0 -> tileGrid[0].size - 1
            newCol == tileGrid[0].size -> 0
            else -> newCol
        }

        tileGrid[row][col] = Tile(tileColor = R.color.green)
        gridAdapter.submitGrid(tileGrid)
        currPosition = row to col
    }
}
