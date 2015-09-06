package com.nerdtracker.tiles

import android.os.Bundle

public class Game {
    private var grid: Grid = Grid(0)

    public var score: Int = 0
        private set

    public var largestTile: Int = 0
        private set

    public val canMove: Boolean
        get() = !grid.isFull

    public constructor() {
        startNewGame()
    }

    public constructor(state: Bundle) {
        score = state.getInt("score")
        largestTile = state.getInt("largestTile")
        grid = Grid(state.getBundle("grid"))
    }

    public fun saveInstanceState(state: Bundle) {
        state.putInt("score", score)
        state.putInt("largestTile", largestTile)

        val gridBundle = Bundle()
        grid.saveInstanceState(gridBundle)
        state.putBundle("grid", gridBundle)
    }

    public fun startNewGame() {
        grid = Grid(4)
        score = 0
        largestTile = 0

        // New games always begin with two tiles
        placeNextTile()
        placeNextTile()
    }

    public fun swipe(dir: Dir) {
        if (grid.moveTiles(dir)) {
            if (!grid.isFull) {
                placeNextTile()
            } else {

            }
        }
    }

    private fun placeNextTile() {
        val tile = grid.placeRandomTile()
        if (tile != null && tile.value > largestTile) {
            largestTile = tile.value
        }
    }
}