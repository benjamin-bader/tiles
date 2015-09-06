package com.nerdtracker.tiles

import android.os.Bundle
import android.support.test.runner.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

import org.hamcrest.Matchers.*
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
public class DirTests {
    @Test public fun opposites() {
        assertThat(Dir.UP.opposite(), equalTo(Dir.DOWN))
        assertThat(Dir.DOWN.opposite(), equalTo(Dir.UP))
        assertThat(Dir.LEFT.opposite(), equalTo(Dir.RIGHT))
        assertThat(Dir.RIGHT.opposite(), equalTo(Dir.LEFT))
    }
}

@RunWith(AndroidJUnit4::class)
public class PosTests {
    @Test public fun getNeighborGetsLeftNeighbor() {
        val p = Pos(1, 1)
        val n = p.getNeighbor(Dir.LEFT)
        assertThat(n, equalTo(Pos(0, 1)))
    }

    @Test public fun getNeighborGetsRightNeighbor() {
        val p = Pos(1, 1)
        val n = p.getNeighbor(Dir.RIGHT)
        assertThat(n, equalTo(Pos(2, 1)))
    }

    @Test public fun getNeighborGetsUpperNeighbor() {
        val p = Pos(1, 1)
        val n = p.getNeighbor(Dir.UP)
        assertThat(n, equalTo(Pos(1, 0)))
    }

    @Test public fun getNeighborGetsLowerNeighbor() {
        val p = Pos(1, 1)
        val n = p.getNeighbor(Dir.DOWN)
        assertThat(n, equalTo(Pos(1, 2)))
    }
}

@RunWith(AndroidJUnit4::class)
public class TileTests {
    @Test public fun testTilePosTriggersUpdate() {
        val tile = Tile(1, Pos(0, 0), 2)
        var didUpdate = false
        tile.onPropertyChanged.subscribe() { didUpdate = true }
        tile.pos = Pos(1, 1)
        assertTrue(didUpdate)
    }

    @Test public fun valueChangeTriggersUpdate() {
        val tile = Tile(1, Pos(0, 0), 2)
        var didUpdate = false
        tile.onPropertyChanged.subscribe() { didUpdate = true }
        tile.value = 8
        assertTrue(didUpdate)
    }

    @Test public fun equality() {
        val lhs = Tile(1, Pos(1, 1), 8)
        assertThat(lhs, equalTo(Tile(1, Pos(1, 1), 8)))
        assertThat(lhs, not(equalTo(Tile(2, Pos(1, 1), 8))))
        assertThat(lhs, not(equalTo(Tile(1, Pos(2, 1), 8))))
        assertThat(lhs, not(equalTo(Tile(1, Pos(1, 2), 8))))
        assertThat(lhs, not(equalTo(Tile(1, Pos(1, 1), 16))))
    }
}

public class GridBuilder(len: Int) {
    public val g: Grid = Grid(len)

    public fun tile(x: Int, y: Int, value: Int = 2) {
        val t = Tile(1, Pos(x, y), value)
        g[x, y] = t
    }
}

public fun grid(length: Int, fn: GridBuilder.() -> Unit): Grid {
    val gb = GridBuilder(length)
    gb.fn()
    return gb.g
}

@RunWith(AndroidJUnit4::class)
public class GridTests {
    @Test public fun simpleMoveUp() {
        val initial = grid(4) {
            tile(x=3, y=3)
        }

        val expected = grid(4) {
            tile(x=3, y=0)
        }

        initial.moveTiles(Dir.UP)
        assertThat(initial, equalTo(expected))
    }

    @Test public fun moveUpAgainstAnotherTile() {
        val initial = grid(4) {
            tile(x=3, y=0, value=1)
            tile(x=3, y=3, value=2)
        }

        val expected = grid(4) {
            tile(x=3, y=0, value=1)
            tile(x=3, y=1, value=2)
        }

        initial.moveTiles(Dir.UP)

        assertThat(initial, equalTo(expected))
    }

    @Test public fun twoTilesMovingInAColumn() {
        val initial = grid(4) {
            tile(x=3, y=2, value=1)
            tile(x=3, y=3, value=2)
        }

        val expected = grid(4) {
            tile(x=3, y=0, value=1)
            tile(x=3, y=1, value=2)
        }

        initial.moveTiles(Dir.UP)

        assertThat(initial, equalTo(expected))
    }

    @Test public fun simpleCombination() {
        val initial = grid(4) {
            tile(x=3, y=0, value=2)
            tile(x=3, y=3, value=2)
        }

        val expected = grid(4) {
            tile(x=3, y=0, value=4)
        }

        initial.moveTiles(Dir.UP)

        assertThat(initial, equalTo(expected))
    }

    @Test public fun complexCombination() {
        val initial = grid(4) {
            tile(x=3, y=0, value=2)
            tile(x=3, y=1, value=2)
            tile(x=3, y=2, value=2)
            tile(x=3, y=3, value=2)
        }

        val expected = grid(4) {
            tile(x=3, y=0, value=4)
            tile(x=3, y=1, value=4)
        }

        initial.moveTiles(Dir.UP)

        assertThat(initial, equalTo(expected))
    }

    @Test public fun simpleCombinationMovingRight() {
        val initial = grid(4) {
            tile(x=0, y=1, value=2)
            tile(x=2, y=1, value=2)
        }

        val expected = grid(4) {
            tile(x=3, y=1, value=4)
        }

        initial.moveTiles(Dir.RIGHT)

        assertThat(initial, equalTo(expected))
    }

    @Test public fun complexCombinationMovingRight() {
        val initial = grid(4) {
            tile(x=0, y=1, value=2)
            tile(x=1, y=1, value=2)
            tile(x=2, y=1, value=2)
            tile(x=3, y=1, value=2)
        }

        val expected = grid(4) {
            tile(x=2, y=1, value=4)
            tile(x=3, y=1, value=4)
        }

        initial.moveTiles(Dir.RIGHT)

        assertThat(initial, equalTo(expected))
    }

    @Test public fun fullness() {
        assertFalse(grid(1){}.isFull)
        assertTrue(grid(1) { tile(0, 0) }.isFull)
    }

    @Test public fun randomInsertion() {
        var g = grid(2) {}

        assertFalse(g.isFull)
        assertNotNull(g.placeRandomTile())
        assertNotNull(g.placeRandomTile())
        assertNotNull(g.placeRandomTile())
        assertNotNull(g.placeRandomTile())

        assertNull(g.placeRandomTile())
        assertTrue(g.isFull)

        var updated = false
        g.onBoardUpdated.subscribe() { updated = true }

        assertNotNull(g.placeRandomTile())
        assertTrue(updated)
    }

    @Test public fun persistence() {
        var original = grid(3) {
            tile(x=0, y=0, value=2048)
            tile(x=1, y=1, value=1024)
            tile(x=2, y=2, value=512)
        }

        val bundle = Bundle()
        original.saveInstanceState(bundle)

        assertThat(original, equalTo(Grid(bundle)))
    }
}