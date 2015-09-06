package com.nerdtracker.tiles

import android.annotation.TargetApi
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.widget.TextView

public class TileView : TextView {
    public constructor(context: Context) : super(context) {
        init(context, null, 0, 0)
    }

    public constructor(
            context: Context,
            attrSet: AttributeSet) : super(context, attrSet) {
        init(context, attrSet, 0, 0)
    }

    public constructor(
            context: Context,
            attrSet: AttributeSet,
            defStyleAttr: Int) : super(context, attrSet, defStyleAttr) {
        init(context, attrSet, defStyleAttr, 0)
    }

    @TargetApi(21)
    public constructor(
            context: Context,
            attrSet: AttributeSet,
            defStyleAttr: Int,
            defStyleRes: Int) : super(context, attrSet, defStyleAttr, defStyleRes) {
        init(context, attrSet, defStyleAttr, defStyleRes)
    }

    private fun init(context: Context, attrSet: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {

    }
}