package com.nerdtracker.tiles

import android.os.Bundle
import android.support.v4.util.ArrayMap
import android.util.Log
import rx.Observable
import rx.Subscriber
import rx.Subscription
import rx.lang.kotlin.BehaviourSubject
import rx.lang.kotlin.PublishSubject
import java.util.*
import kotlin.platform.platformStatic
import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty

public enum class Dir {
    UP, DOWN, LEFT, RIGHT;

    public fun opposite(): Dir = when (this) {
        UP -> DOWN
        DOWN -> UP
        LEFT -> RIGHT
        RIGHT -> LEFT
    }
}

public data class Pos(public val x: Int, public val y: Int) {
    public fun getNeighbor(dir: Dir): Pos {
        return when (dir) {
            Dir.UP -> Pos(x, y - 1)
            Dir.DOWN -> Pos(x, y + 1)
            Dir.LEFT -> Pos(x - 1, y)
            Dir.RIGHT -> Pos(x + 1, y)
        }
    }
}

public abstract class PropertyChangedBase<T : PropertyChangedBase<T>> {
    private val propertyChangedSubject = PublishSubject<T>()

    public val onPropertyChanged: Observable<T>
        get() = propertyChangedSubject

    protected fun notifyPropertyChanged() {
        propertyChangedSubject.onNext(this as T)
    }

    protected fun readWriteProperty<P>(initialValue: P): ReadWriteProperty<T, P> {
        return Delegates.observable(initialValue, { meta, old, new ->
            notifyPropertyChanged()
        })
    }
}

public class Tile(id: Int, pos: Pos, value: Int) : PropertyChangedBase<Tile>() {
    public val id: Int = id
    public var value: Int by readWriteProperty(value)
    public var pos: Pos by readWriteProperty(pos)

    /**
     * When [isCombining] is true, then it has already been combined during
     * this move; no other tiles may combine with this one for the remainder of
     * the move.
     */
    public var isCombining: Boolean = false

    /**
     * Combines the given [other] tile into this one, adding its value to ours.
     *
     * Fails if [other] has a different value from this tile, or if
     * [isCombining] is set due to another combination during this move.
     */
    public fun combineWith(other: Tile) {
        check(other.value == value, {"Cannot combine with mismatched values (this=$value, that=${other.value}"})
        check(isCombining == false, "Already combined once during this move.")

        isCombining = true
        value += other.value
    }

    public fun reset() {
        isCombining = false
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (null === other) return false
        return other is Tile
                && pos == other.pos
                && value == other.value
    }

    override fun hashCode(): Int {
        var hash = pos.hashCode()
        return hash * 31 + value
    }
}

/**
 *
 * @param length the length of one side of the square grid, in tiles.
 */
public class Grid {
    private val tileCombinedSubject = PublishSubject<Tile>()
    private val boardUpdatedSubject = PublishSubject<Grid>()
    private val rand = Random()

    private val length: Int
    private val board: Array<Tile?>

    private var lastTileId = 1

    public val onBoardUpdated: Observable<Grid>
        get() = boardUpdatedSubject

    public val onTileCombined: Observable<Tile>
        get() = tileCombinedSubject

    public var count: Int = 0
        private set

    public val isFull: Boolean
        get() = count == board.size()

    public fun get(x: Int, y: Int): Tile? {
        return board[posToIndex(x, y)]
    }

    public fun set(x: Int, y: Int, tile: Tile?) {
        board[posToIndex(x, y)] = tile
    }

    public constructor(length: Int) {
        this.length = length
        this.board = arrayOfNulls(length * length)
    }

    public constructor(bundle: Bundle) {
        this.length = bundle.getInt("length")
        this.board = arrayOfNulls(length * length)

        bundle.getIntArray("board").forEachIndexed { index, state ->
            val id = (state shr 16)

            // The lower 16 bits are the base-2 log of the value.
            val valueLog = (state and 0xFFFF)
            val value = Math.pow(2.0, valueLog.toDouble()).toInt()

            if (id > 0) {
                ++count
                board[index] = Tile(id, indexToPos(index), value)
                if (id > lastTileId) {
                    lastTileId = id
                }
            }
        }
    }

    /**
     * Packs the grid into a bundle for persistence.
     *
     * Each tile is packed into a 32-bit int.
     *
     * From most-significant-byte to least, the scheme is:
     * |76543210|76543210|76543210|76543210|
     *        tile ID          value
     *
     * The tile ID occupies the first two octets.  IDs are transient, existing
     * only to facilitate use of Adapters higher up the stack; it matters only
     * that they are distinct from one another, so we mask off the lowest 16
     * bits of existing tile IDs.  This of course means that IDs are not stable
     * across saves/restores.
     *
     * The final two bytes contain the value of the tile.  The value of a tile
     * is stored as the base-2 log, as values are all powers of two.
     *
     * The position of the tile in the board is represented as the position
     * of the packed value in the state array.
     */
    public fun saveInstanceState(bundle: Bundle) {
        val stateArray = IntArray(board.size())
        board.forEachIndexed { index, tile ->
            if (tile != null) {
                // HS math time: logX(V) / logX(N) == logN(V)
                var v = tile.value.toDouble()
                var log = (Math.log10(v) / Math.log10(2.0)).toInt()
                var state = ((tile.id and 0xFFFF).toInt() shl 16) or (log and 0xFFFF)
                stateArray[index] = state
            } else {
                stateArray[index] = 0
            }
        }

        bundle.putInt("length", length)
        bundle.putIntArray("board", stateArray)
    }

    public fun placeRandomTile(): Tile? {
        if (!isFull) {
            val empties = board.indices.filter { board[it] == null }
            val index = empties[rand.nextInt(empties.size())]
            val value = if (rand.nextBoolean()) 2 else 4
            val tile = Tile(lastTileId++, indexToPos(index), value)
            board[index] = tile
            ++count

            boardUpdatedSubject.onNext(this)

            return tile
        }
        return null
    }

    public fun moveTiles(dir: Dir): Boolean {
        var moved = false
        traverse(dir) { tile ->
            if (tile != null) {
                if (moveTile(tile, dir)) {
                    moved = true
                }
            }
        }
        board.forEach { it?.reset() }
        boardUpdatedSubject.onNext(this)
        return moved
    }

    /**
     * Moves a tile in the given direction, if possible.
     *
     * @return [true] if the tile moves (or is combined), otherwise false.
     */
    private fun moveTile(tile: Tile, dir: Dir): Boolean {
        check(tile === board[posToIndex(tile.pos)])

        val neighbor = nextNeighborOf(tile.pos, dir)
        if (neighbor == null) {
            // If there is no neighbor, this tile is potentially being moved to
            // the edge of the board -- if it isn't already there!
            var (x, y) = tile.pos
            when (dir) {
                Dir.UP -> y = 0
                Dir.DOWN -> y = length - 1
                Dir.LEFT -> x = 0
                Dir.RIGHT -> x = length - 1
            }

            if (tile.pos.x != x || tile.pos.y != y) {
                this[tile.pos] = null
                tile.pos = Pos(x, y)
                this[tile.pos] = tile
                return true
            } else {
                error("We should not be moving a tile already at the edge")
            }
        } else if (neighbor.value == tile.value && !neighbor.isCombining) {
            // If there is a neighbor with an equal value, that hasn't already
            // combined, then the two can combine.
            tile.combineWith(neighbor)
            this[neighbor.pos] = tile
            this[tile.pos] = null
            --count

            tileCombinedSubject.onNext(tile)
            return true
        } else {
            // Is this neighbor directly adjacent to the tile?
            // If not, then we can still move
            var nn = neighbor.pos.getNeighbor(dir.opposite())
            if (nn != tile.pos) {
                this[tile.pos] = null
                this[nn] = tile
                tile.pos = nn
                return true
            }

            // Nothing to do.
        }

        return false
    }

    tailRecursive
    private fun nextNeighborOf(pos: Pos, dir: Dir): Tile? {
        val np = pos.getNeighbor(dir)
        if (!inRange(np.x, np.y)) {
            return null
        }

        return get(np) ?: nextNeighborOf(np, dir)
    }

    private fun inRange(x: Int, y: Int): Boolean {
        return x >= 0
                && x < length
                && y >= 0
                && y < length
    }

    private fun get(pos: Pos): Tile? {
        return board[posToIndex(pos)]
    }

    private fun set(pos: Pos, tile: Tile?) {
        board[posToIndex(pos)] = tile
    }

    private fun indexToPos(index: Int): Pos {
        val x = index % length
        val y = index / length
        return Pos(x, y)
    }

    private fun posToIndex(pos: Pos): Int {
        return posToIndex(pos.x, pos.y)
    }

    private fun posToIndex(x: Int, y: Int): Int {
        return y * length + x
    }

    /**
     * Iterates over the tiles on the board in an order appropriate for moving
     * the tiles in a given direction.
     *
     * For example, if the tiles are being moved {@link Dir#UP}, then tiles are
     * traversed from top row to bottom.  If tiles are being moved
     * {@link Dir#LEFT}, then tiles are traversed column-wise from left to
     * right.
     *
     * The principle is that any tile move should terminate either at the edge
     * of the board or adjacent to a tile that has already finished its move.
     *
     * @param dir the direction the tiles are being moved.
     * @param fn a function that will move a single tile.
     */
    private inline fun traverse(dir: Dir, fn: (tile: Tile?) -> Unit) {
        when (dir) {
            // The first [length] indices aren't moving or combining,
            // so we don't have to iterate over them.
            Dir.UP -> {
                board.drop(length).forEach(fn)
            }

            Dir.DOWN -> {
                board.reverse().drop(length).forEach(fn)
            }

            Dir.LEFT -> {
                for (x in 0..(length - 1)) {
                    for (y in 1..(length - 1)) {
                        val ix = posToIndex(x, y)
                        fn(board[ix])
                    }
                }
            }

            Dir.RIGHT -> {
                for (x in (length - 1) downTo 0) {
                    for (y in (length - 2) downTo 0) {
                        val ix = posToIndex(x, y)
                        fn(board[ix])
                    }
                }
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other === null) return false
        return other is Grid
                && length == other.length
                && Arrays.equals(board, other.board)
    }

    override fun hashCode(): Int {
        var hash = length
        hash *= 31 + Arrays.hashCode(board)
        return hash
    }

    override fun toString(): String {
        val spacesPerRow = 5 * length + (length - 1) + 2
        val newlines = length - 1
        val rows = length
        val totalSpaces = spacesPerRow * rows + newlines
        val sb = StringBuilder(totalSpaces)

        sb.append("\n")
        for (i in board.indices) {
            val isRowStart = i % length == 0;
            if (isRowStart) {
                if (i > 0) {
                    sb.setLength(sb.length() - 1)
                    sb.append("]\n")
                }
                sb.append("[")
            }

            val tile = board[i]
            val repr = if (tile != null) {
                "%-5d".format(tile.value)
            } else {
                "     "
            }

            sb.append(repr)
            sb.append("|")
        }

        return sb.append("]").toString()
    }
}