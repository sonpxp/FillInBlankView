package com.sonpxp.blankview

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    private lateinit var wordArrangementView: WordArrangementView
    private lateinit var btnCheck: Button
    private lateinit var btnReset: Button
    private lateinit var tvResult: TextView

    // Sample data - có thể thay đổi theo nhu cầu
    private val sampleWords1 = listOf("老", "这", "是", "师", "书", "的", "书", "的")
    private val correctOrder1 = listOf("这", "是", "老", "师", "的", "书", "书", "的") // Đáp án đúng

    private val sampleWords = listOf("1", "1", "3", "4", "5", "6", "7", "8")
    private val correctOrder = listOf("8", "7", "6", "5", "4", "3", "1", "1")

    private val sampleWords2 = (1..8).map { it.toString() }
    private val correctOrder2 = (8 downTo 1).map { it.toString() }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()
        loadWords()
    }

    private fun initViews() {
        wordArrangementView = findViewById(R.id.wordArrangementView)
        btnCheck = findViewById(R.id.btnCheck)
        btnReset = findViewById(R.id.btnReset)
        tvResult = findViewById(R.id.tvResult)
    }

    private fun setupListeners() {
        btnCheck.setOnClickListener {
            checkAnswer()
        }

        btnReset.setOnClickListener {
            resetGame()
        }
    }

    private fun loadWords() {
        wordArrangementView.setWords(sampleWords)
        tvResult.text = ""
    }

    private fun checkAnswer() {
        val arrangedWords = wordArrangementView.getArrangedWords()

        if (arrangedWords.isEmpty()) {
            Toast.makeText(this, "Hãy sắp xếp một số từ trước!", Toast.LENGTH_SHORT).show()
            return
        }

        // Kiểm tra đáp án
        val isCorrect = arrangedWords == correctOrder

        if (isCorrect) {
            tvResult.text = "🎉 Chính xác! Câu trả lời đúng: ${arrangedWords.joinToString(" ")}"
            tvResult.setTextColor(getColor(android.R.color.holo_green_dark))
            Toast.makeText(this, "Chúc mừng! Bạn đã làm đúng!", Toast.LENGTH_LONG).show()
        } else {
            tvResult.text = "❌ Chưa đúng. Bạn sắp xếp: ${arrangedWords.joinToString(" ")}"
            tvResult.setTextColor(getColor(android.R.color.holo_red_dark))
            Toast.makeText(this, "Thử lại nhé!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetGame() {
        wordArrangementView.clearArrangedWords()
        tvResult.text = ""
        Toast.makeText(this, "Đã reset! Hãy thử lại.", Toast.LENGTH_SHORT).show()
    }
}