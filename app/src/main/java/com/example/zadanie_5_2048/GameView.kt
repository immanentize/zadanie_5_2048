package com.example.zadanie_5_2048

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

class GameView(context: Context, attrs: AttributeSet?) :
    SurfaceView(context, attrs),
    Runnable {

    private val GRID_SIZE = 4
    private var cellSize = 0f
    private var offsetX = 0f
    private var offsetY = 0f

    private val board = IntArray(GRID_SIZE * GRID_SIZE)
    private var score = 0
    private var bestScore = 0
    private var gameOver = false
    private var gameWin = false

    private val fieldPaint = Paint().apply {
        color = Color.parseColor("#BBADA0")
        style = Paint.Style.FILL
    }

    private val emptyCellPaint = Paint().apply {
        color = Color.parseColor("#CDC1B4")
        style = Paint.Style.FILL
    }

    private val tileTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 60f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    private val scorePaint = Paint().apply {
        color = Color.parseColor("#776E65")
        textSize = 60f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }

    private var touchStartX = 0f
    private var touchStartY = 0f

    private var thread: Thread? = null
    @Volatile
    private var isRunning = false

    init {
        post {
            val topMargin = 100f
            val bottomMargin = 100f

            val availableHeight = height - topMargin - bottomMargin
            val availableWidth = width.toFloat()

            val fieldSize = Math.min(availableWidth, availableHeight)
            cellSize = fieldSize / GRID_SIZE

            offsetX = (width - fieldSize) / 2f
            offsetY = topMargin

            loadGame()
        }

        holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                startGame()
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                stopGame()
            }
        })
    }

    private fun startGame() {
        if (thread == null) {
            isRunning = true
            thread = Thread(this)
            thread?.start()
        }
    }

    private fun stopGame() {
        isRunning = false
        thread?.join()
        thread = null
        saveGame()
    }

    override fun run() {
        while (isRunning) {
            draw()
            try {
                Thread.sleep(16)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    private fun addRandomTile() {
        val emptyCells = mutableListOf<Int>()
        for (i in board.indices) {
            if (board[i] == 0) {
                emptyCells.add(i)
            }
        }
        if (emptyCells.isEmpty()) return

        val randomIndex = emptyCells.random()
        board[randomIndex] = if (Math.random() < 0.9) 2 else 4
    }

    @Synchronized
    private fun draw() {
        val canvas = holder.lockCanvas() ?: return

        canvas.drawColor(Color.parseColor("#FAF8EF"))

        val fieldRight = offsetX + GRID_SIZE * cellSize
        val fieldBottom = offsetY + GRID_SIZE * cellSize
        canvas.drawRect(offsetX, offsetY, fieldRight, fieldBottom, fieldPaint)

        for (i in 0 until GRID_SIZE * GRID_SIZE) {
            val row = i / GRID_SIZE
            val col = i % GRID_SIZE

            val left = offsetX + col * cellSize
            val top = offsetY + row * cellSize
            val right = left + cellSize
            val bottom = top + cellSize

            if (board[i] == 0) {
                canvas.drawRect(left + 8f, top + 8f, right - 8f, bottom - 8f, emptyCellPaint)
            } else {
                val tilePaint = Paint().apply {
                    color = getTileColor(board[i])
                    style = Paint.Style.FILL
                }
                canvas.drawRoundRect(
                    left + 8f, top + 8f, right - 8f, bottom - 8f,
                    15f, 15f, tilePaint
                )

                val textX = left + cellSize / 2
                val textY = top + cellSize / 2 - (tileTextPaint.descent() + tileTextPaint.ascent()) / 2
                tileTextPaint.color = if (board[i] <= 4) Color.parseColor("#776E65") else Color.WHITE
                canvas.drawText(board[i].toString(), textX, textY, tileTextPaint)
            }
        }

        canvas.drawText("Счёт: $score", offsetX, offsetY - 30f, scorePaint)
        canvas.drawText("Рекорд: $bestScore", offsetX, fieldBottom + 80f, scorePaint)

        if (gameWin) {
            val winPaint = Paint().apply {
                color = Color.parseColor("#EDC22E")
                textSize = 100f
                isAntiAlias = true
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            val centerX = offsetX + (GRID_SIZE * cellSize) / 2
            val centerY = offsetY + (GRID_SIZE * cellSize) / 2
            canvas.drawText("ПОБЕДА!", centerX, centerY, winPaint)
        }

        if (gameOver) {
            val overPaint = Paint().apply {
                color = Color.parseColor("#776E65")
                textSize = 100f
                isAntiAlias = true
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            val centerX = offsetX + (GRID_SIZE * cellSize) / 2
            val centerY = offsetY + (GRID_SIZE * cellSize) / 2
            canvas.drawText("ИГРА ОКОНЧЕНА", centerX, centerY, overPaint)
        }

        holder.unlockCanvasAndPost(canvas)
    }

    private fun getTileColor(value: Int): Int {
        return when (value) {
            2 -> Color.parseColor("#EEE4DA")
            4 -> Color.parseColor("#EDE0C8")
            8 -> Color.parseColor("#F2B179")
            16 -> Color.parseColor("#F59563")
            32 -> Color.parseColor("#F67C5F")
            64 -> Color.parseColor("#F65E3B")
            128 -> Color.parseColor("#EDCF72")
            256 -> Color.parseColor("#EDCC61")
            512 -> Color.parseColor("#EDC850")
            1024 -> Color.parseColor("#EDC53F")
            2048 -> Color.parseColor("#EDC22E")
            else -> Color.parseColor("#3C3A32")
        }
    }

    @Synchronized
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (gameOver || gameWin) return true
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = event.x
                touchStartY = event.y
            }

            MotionEvent.ACTION_UP -> {
                val dx = event.x - touchStartX
                val dy = event.y - touchStartY
                if (dx == 0f && dy == 0f) return true

                if (Math.abs(dx) > Math.abs(dy)) {
                    if (dx > 0) moveRight() else moveLeft()
                } else {
                    if (dy > 0) moveDown() else moveUp()
                }
            }
        }
        return true
    }

    private fun moveLeft() {
        var moved = false

        for (row in 0 until GRID_SIZE) {
            val line = IntArray(GRID_SIZE)
            for (col in 0 until GRID_SIZE) {
                line[col] = board[row * GRID_SIZE + col]
            }

            val newLine = mergeLine(line)

            for (col in 0 until GRID_SIZE) {
                if (board[row * GRID_SIZE + col] != newLine[col]) {
                    moved = true
                }
                board[row * GRID_SIZE + col] = newLine[col]
            }
        }

        if (moved) {
            addRandomTile()
            checkGameState()
            saveGame()
        }
    }

    private fun moveRight() {
        var moved = false

        for (row in 0 until GRID_SIZE) {
            val line = IntArray(GRID_SIZE)
            for (col in 0 until GRID_SIZE) {
                line[col] = board[row * GRID_SIZE + (GRID_SIZE - 1 - col)]
            }

            val newLine = mergeLine(line)

            for (col in 0 until GRID_SIZE) {
                if (board[row * GRID_SIZE + (GRID_SIZE - 1 - col)] != newLine[col]) {
                    moved = true
                }
                board[row * GRID_SIZE + (GRID_SIZE - 1 - col)] = newLine[col]
            }
        }

        if (moved) {
            addRandomTile()
            checkGameState()
            saveGame()
        }
    }

    private fun moveUp() {
        var moved = false

        for (col in 0 until GRID_SIZE) {
            val line = IntArray(GRID_SIZE)
            for (row in 0 until GRID_SIZE) {
                line[row] = board[row * GRID_SIZE + col]
            }

            val newLine = mergeLine(line)

            for (row in 0 until GRID_SIZE) {
                if (board[row * GRID_SIZE + col] != newLine[row]) {
                    moved = true
                }
                board[row * GRID_SIZE + col] = newLine[row]
            }
        }

        if (moved) {
            addRandomTile()
            checkGameState()
            saveGame()
        }
    }

    private fun moveDown() {
        var moved = false

        for (col in 0 until GRID_SIZE) {
            val line = IntArray(GRID_SIZE)
            for (row in 0 until GRID_SIZE) {
                line[row] = board[(GRID_SIZE - 1 - row) * GRID_SIZE + col]
            }

            val newLine = mergeLine(line)

            for (row in 0 until GRID_SIZE) {
                if (board[(GRID_SIZE - 1 - row) * GRID_SIZE + col] != newLine[row]) {
                    moved = true
                }
                board[(GRID_SIZE - 1 - row) * GRID_SIZE + col] = newLine[row]
            }
        }

        if (moved) {
            addRandomTile()
            checkGameState()
            saveGame()
        }
    }

    private fun mergeLine(line: IntArray): IntArray {
        val nonZero = line.filter { it != 0 }.toMutableList()

        val merged = mutableListOf<Int>()
        var i = 0
        while (i < nonZero.size) {
            if (i + 1 < nonZero.size && nonZero[i] == nonZero[i + 1]) {
                val newValue = nonZero[i] * 2
                merged.add(newValue)
                score += newValue
                if (score > bestScore) bestScore = score
                if (newValue == 2048) gameWin = true
                i += 2
            } else {
                merged.add(nonZero[i])
                i += 1
            }
        }

        while (merged.size < GRID_SIZE) {
            merged.add(0)
        }

        return merged.toIntArray()
    }

    private fun checkGameState() {
        if (board.contains(2048)) {
            gameWin = true
        }

        if (isBoardFull() && !canMove()) {
            gameOver = true
        }
    }

    private fun isBoardFull(): Boolean {
        return !board.contains(0)
    }

    private fun canMove(): Boolean {
        for (row in 0 until GRID_SIZE) {
            for (col in 0 until GRID_SIZE - 1) {
                if (board[row * GRID_SIZE + col] == board[row * GRID_SIZE + col + 1]) {
                    return true
                }
            }
        }

        for (col in 0 until GRID_SIZE) {
            for (row in 0 until GRID_SIZE - 1) {
                if (board[row * GRID_SIZE + col] == board[(row + 1) * GRID_SIZE + col]) {
                    return true
                }
            }
        }

        return false
    }

    @Synchronized
    fun resetGame() {
        for (i in board.indices) {
            board[i] = 0
        }

        score = 0
        gameOver = false
        gameWin = false

        addRandomTile()
        addRandomTile()
        saveGame()
    }

    @Synchronized
    private fun saveGame() {
        val prefs = context.getSharedPreferences("game2048", Context.MODE_PRIVATE)
        val editor = prefs.edit()

        for (i in board.indices) {
            editor.putInt("cell_$i", board[i])
        }

        if (score > bestScore) bestScore = score
        editor.putInt("score", score)
        editor.putInt("best", bestScore)
        editor.putBoolean("gameOver", gameOver)
        editor.putBoolean("gameWin", gameWin)
        editor.apply()
    }

    @Synchronized
    private fun loadGame() {
        val prefs = context.getSharedPreferences("game2048", Context.MODE_PRIVATE)
        bestScore = prefs.getInt("best", 0)

        var hasSaved = false
        for (i in board.indices) {
            board[i] = prefs.getInt("cell_$i", 0)
            if (board[i] != 0) hasSaved = true
        }

        if (hasSaved) {
            score = prefs.getInt("score", 0)
            if (score > bestScore) {
                bestScore = score
                prefs.edit().putInt("best", bestScore).apply()
            }
            gameOver = prefs.getBoolean("gameOver", false)
            gameWin = prefs.getBoolean("gameWin", false)
        } else {
            addRandomTile()
            addRandomTile()
        }
    }
}
