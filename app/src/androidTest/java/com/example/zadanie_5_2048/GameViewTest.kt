package com.example.zadanie_5_2048

import android.content.Context
import android.view.MotionEvent
import android.widget.Button
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameViewTest {
    private lateinit var view: GameView

    private fun createView() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            view = GameView(instrumentation.targetContext, null)
        }
    }

    @Before
    fun prepareGame() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences("game2048", Context.MODE_PRIVATE).edit().clear().commit()
        createView()
    }

    private fun read(name: String): Any? {
        val field = GameView::class.java.getDeclaredField(name)
        field.isAccessible = true
        return field.get(view)
    }

    private fun write(name: String, value: Any) {
        val field = GameView::class.java.getDeclaredField(name)
        field.isAccessible = true
        field.set(view, value)
    }

    private fun board(): IntArray = read("board") as IntArray

    private fun call(name: String) {
        val method = GameView::class.java.getDeclaredMethod(name)
        method.isAccessible = true
        method.invoke(view)
    }

    private fun merge(line: IntArray): IntArray {
        val method = GameView::class.java.getDeclaredMethod("mergeLine", IntArray::class.java)
        method.isAccessible = true
        return method.invoke(view, line) as IntArray
    }

    private fun swipe(dx: Float, dy: Float) {
        val down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 500f, 500f, 0)
        val up = MotionEvent.obtain(0, 100, MotionEvent.ACTION_UP, 500f + dx, 500f + dy, 0)
        view.onTouchEvent(down)
        view.onTouchEvent(up)
        down.recycle()
        up.recycle()
    }

    private fun checkSwipe(first: Int, second: Int, target: Int, dx: Float, dy: Float) {
        board()[first] = 2
        board()[second] = 2
        swipe(dx, dy)
        assertEquals(4, board()[target])
        assertEquals(4, read("score"))
        assertEquals(2, board().count { it != 0 })
    }

    @Test
    fun newGameHasTwoTilesAndZeroScore() {
        view.resetGame()
        assertEquals(2, board().count { it != 0 })
        assertTrue(board().all { it == 0 || it == 2 || it == 4 })
        assertEquals(0, read("score"))
        assertEquals(false, read("gameWin"))
        assertEquals(false, read("gameOver"))
    }

    @Test
    fun mergesOnlyOncePerTile() {
        assertArrayEquals(intArrayOf(4, 4, 0, 0), merge(intArrayOf(2, 2, 2, 2)))
        assertEquals(8, read("score"))
        write("score", 0)
        assertArrayEquals(intArrayOf(4, 4, 0, 0), merge(intArrayOf(2, 0, 2, 4)))
        assertEquals(4, read("score"))
        write("score", 0)
        assertArrayEquals(intArrayOf(4, 2, 0, 0), merge(intArrayOf(2, 2, 2, 0)))
        assertEquals(4, read("score"))
    }

    @Test
    fun swipeLeft() = checkSwipe(0, 1, 0, -200f, 0f)

    @Test
    fun swipeRight() = checkSwipe(0, 1, 3, 200f, 0f)

    @Test
    fun swipeUp() = checkSwipe(0, 4, 0, 0f, -200f)

    @Test
    fun swipeDown() = checkSwipe(0, 4, 12, 0f, 200f)

    @Test
    fun unchangedMoveAndTapDoNotAddTiles() {
        board()[0] = 2
        val original = board().copyOf()
        swipe(-200f, 0f)
        assertArrayEquals(original, board())
        swipe(0f, 0f)
        assertArrayEquals(original, board())
        assertEquals(0, read("score"))
    }

    @Test
    fun reaching2048Wins() {
        board()[0] = 1024
        board()[1] = 1024
        swipe(-200f, 0f)
        assertEquals(2048, board()[0])
        assertEquals(2048, read("score"))
        assertEquals(true, read("gameWin"))
        val won = board().copyOf()
        swipe(200f, 0f)
        assertArrayEquals(won, board())
    }

    @Test
    fun fullBoardWithoutPairsLoses() {
        intArrayOf(2, 4, 2, 4, 4, 2, 4, 2, 2, 4, 2, 4, 4, 2, 4, 2).copyInto(board())
        call("checkGameState")
        assertEquals(true, read("gameOver"))
        val lost = board().copyOf()
        swipe(-200f, 0f)
        assertArrayEquals(lost, board())
    }

    @Test
    fun fullBoardWithHorizontalOrVerticalPairContinues() {
        val values = intArrayOf(2, 4, 2, 4, 4, 2, 4, 2, 2, 4, 2, 4, 4, 2, 4, 2)
        values.copyInto(board())
        board()[1] = 2
        call("checkGameState")
        assertEquals(false, read("gameOver"))
        values.copyInto(board())
        board()[4] = 2
        call("checkGameState")
        assertEquals(false, read("gameOver"))
    }

    @Test
    fun stateRestoresInNewView() {
        val values = intArrayOf(2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 0, 0, 0, 0, 0)
        values.copyInto(board())
        write("score", 4096)
        write("gameWin", true)
        call("saveGame")
        createView()
        call("loadGame")
        assertArrayEquals(values, board())
        assertEquals(4096, read("score"))
        assertEquals(true, read("gameWin"))
        assertEquals(false, read("gameOver"))
    }

    @Test
    fun resetSavesFreshGame() {
        write("score", 100)
        write("gameWin", true)
        write("gameOver", true)
        view.resetGame()
        val fresh = board().copyOf()
        createView()
        call("loadGame")
        assertArrayEquals(fresh, board())
        assertEquals(2, board().count { it != 0 })
        assertEquals(0, read("score"))
        assertEquals(false, read("gameWin"))
        assertEquals(false, read("gameOver"))
    }

    @Test
    fun newGameButtonResetsBoard() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                view = activity.findViewById(R.id.gameView)
                write("score", 100)
                write("gameOver", true)
                activity.findViewById<Button>(R.id.btnNewGame).performClick()
                assertEquals(2, board().count { it != 0 })
                assertEquals(0, read("score"))
                assertEquals(false, read("gameOver"))
            }
        }
    }
    @Test
    fun mergesUpdateRecordImmediately() {
        board()[0] = 2
        board()[1] = 2
        swipe(-200f, 0f)
        assertEquals(4, read("bestScore"))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals(4, context.getSharedPreferences("game2048", Context.MODE_PRIVATE).getInt("best", 0))
    }

    @Test
    fun smallerScoreDoesNotReplaceRecord() {
        board()[0] = 8
        board()[1] = 8
        swipe(-200f, 0f)
        assertEquals(16, read("bestScore"))
        view.resetGame()
        board().fill(0)
        board()[0] = 2
        board()[1] = 2
        swipe(-200f, 0f)
        assertEquals(4, read("score"))
        assertEquals(16, read("bestScore"))
    }

    @Test
    fun recordSurvivesNewGameAndReload() {
        board()[0] = 16
        board()[1] = 16
        swipe(-200f, 0f)
        view.resetGame()
        createView()
        call("loadGame")
        assertEquals(0, read("score"))
        assertEquals(32, read("bestScore"))
    }

    @Test
    fun existingSavedScoreBecomesFirstRecord() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("game2048", Context.MODE_PRIVATE)
        prefs.edit().putInt("cell_0", 64).putInt("score", 100).commit()
        call("loadGame")
        assertEquals(100, read("bestScore"))
        assertEquals(100, prefs.getInt("best", 0))
    }

}
