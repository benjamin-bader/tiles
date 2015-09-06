package com.nerdtracker.tiles

public inline fun emptyArray<reified T>(size: Int): Array<T> {
    return Array(size, { null as T })
}