package com.sonpxp.blankview

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.sonpxp.blankview.databinding.ActivityMainBinding
import com.sonpxp.blankview.word.ReviewMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding


    private val correctOrder2 = listOf("8", "7", "6", "5", "4", "3", "1", "1")
    private val sampleWords2 = listOf("1", "1", "3", "4", "5", "6", "7", "8")

    val sampleWords3 = listOf("The", "weather", "is", "getting", "quite", "cold")
    val sampleWords = listOf("天气", "越来越", "冷", "我们", "多穿", "衣服")

    val correctOrder = listOf("天气", "越来越", "冷", "我们", "多穿", "衣服")
    val userAnswers = listOf("天气", "越来越", "冷", "我们", "多穿", "衣服")

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
        btnReview.setOnClickListener { review() }
    }

    private fun loadWords() = with(binding) {
        wordArrangementView.setWords(sampleWords.shuffled())
        tvResult.text = ""
        tvRightAnswer.text = correctOrder.joinToString(" -> ")
    }

    private fun review() {
        binding.apply {
            binding.wordArrangementView.setWords(sampleWords)

            lifecycleScope.launch {
                delay(1000)
                binding.wordArrangementView.reviewResults(userAnswers.shuffled(), correctOrder, ReviewMode.ALL_OR_NOTHING)
            }
        }
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
                "🎉 Chính xác! \n\nCâu trả lời đúng: ${arrangedWords.joinToString(" ")}"
            } else {
                "❌ Chưa đúng. \n\nBạn sắp xếp: ${arrangedWords.joinToString(" ")}"
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
        wordArrangementView.exitReviewMode()
        tvResult.text = ""
        Toast.makeText(this@MainActivity, "Đã reset! Hãy thử lại.", Toast.LENGTH_SHORT).show()
    }
}
