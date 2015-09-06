package com.nerdtracker.tiles

import android.os.Bundle
import android.support.v7.app.AppCompatActivity

public class GameActivity() : AppCompatActivity() {
    private var game: Game? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState != null) {
            game = Game(savedInstanceState.getBundle("game"))
        } else {
            game = Game()
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        if (outState != null) {
            game!!.saveInstanceState(outState)
        }
    }
}