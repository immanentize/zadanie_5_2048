package com.example.zadanie_5_2048

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val gameView = findViewById<GameView>(R.id.gameView)
        val btnNewGame = findViewById<Button>(R.id.btnNewGame)
        btnNewGame.setOnClickListener {
            gameView.resetGame()
        }
    }
}
