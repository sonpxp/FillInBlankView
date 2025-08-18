package com.sonpxp.blankview

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.sonpxp.blankview.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val sampleWords = listOf("1", "1", "3", "4", "5", "6", "7", "8")
    private val correctOrder = listOf("8", "7", "6", "5", "4", "3", "1", "1")

    private val sampleWords2 = (1..8).map { it.toString() }
    private val correctOrder2 = (8 downTo 1).map { it.toString() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        loadWords()
    }

    private fun setupListeners() = with(binding) {
        btnCheck.setOnClickListener { checkAnswer() }
        btnReset.setOnClickListener { resetGame() }
    }

    private fun loadWords() = with(binding) {
        wordArrangementView.setWords(sampleWords)
        tvResult.text = ""
    }

    private fun checkAnswer() = with(binding) {
        val arrangedWords = wordArrangementView.getArrangedWords()

        if (arrangedWords.isEmpty()) {
            Toast.makeText(this@MainActivity, "Hãy sắp xếp một số từ trước!", Toast.LENGTH_SHORT).show()
            return@with
        }

        val isCorrect = arrangedWords == correctOrder

        tvResult.apply {
            text = if (isCorrect) {
                "🎉 Chính xác! Câu trả lời đúng: ${arrangedWords.joinToString(" ")}"
            } else {
                "❌ Chưa đúng. Bạn sắp xếp: ${arrangedWords.joinToString(" ")}"
            }
            setTextColor(
                getColor(
                    if (isCorrect) android.R.color.holo_green_dark
                    else android.R.color.holo_red_dark
                )
            )
        }
    }

    private fun resetGame() = with(binding) {
        wordArrangementView.clearArrangedWords()
        tvResult.text = ""
        Toast.makeText(this@MainActivity, "Đã reset! Hãy thử lại.", Toast.LENGTH_SHORT).show()
    }
}
