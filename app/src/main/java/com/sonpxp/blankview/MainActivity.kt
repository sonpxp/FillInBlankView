package com.sonpxp.blankview

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.sonpxp.blankview.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Nên lấy từ ViewModel/data source; tạm để cố định cho ví dụ
    private val sampleWords = listOf("1", "1", "3", "4", "5", "6", "7", "8")
    private val correctOrder = listOf("8", "7", "6", "5", "4", "3", "1", "1")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        setupWordViewAppearance()
        loadWords()
    }

    private fun setupWordViewAppearance() = with(binding.wordArrangementView) {
        // Nền cho words ở khu vực available (dưới) - xám
        setAvailableWordBackground(R.drawable.img_box_grey)
        // Nền cho words ở khu vực arranged (trên) - cyan
        setArrangedWordBackground(R.drawable.img_box_cyan)
        // Placeholder (tuỳ chọn)
        setPlaceholderBackground(R.drawable.img_box_grey)
        // Màu chữ theo màu nền
        setAvailableTextColorRes(R.color.grey_text_color)
        setArrangedTextColorRes(R.color.cyan_text_color)

        setCorrectBackground(R.drawable.img_box_cyan)
        setIncorrectBackground(R.drawable.img_box_pink)
    }

    private fun setupListeners() = with(binding) {
        btnCheck.setOnClickListener { onCheckAnswer() }
        btnReset.setOnClickListener { onReset() }
        btnReview.setOnClickListener {
            val userAnswers = sampleWords // Từ server
            val correctAnswers = correctOrder

            wordArrangementView.reviewResults(
                userAnswers,
                correctAnswers,
                WordArrangementView.ReviewMode.ALL_OR_NOTHING
            )
        }
    }

    private fun loadWords() = with(binding) {
        wordArrangementView.setWords(sampleWords)
        tvResult.text = ""
    }

    private fun onCheckAnswer() = with(binding) {
        val arranged = wordArrangementView.getArrangedWords()
        if (arranged.isEmpty()) {
            showSnack("Hãy sắp xếp một số từ trước!")
            return
        }

        val isCorrect = arranged == correctOrder
        if (isCorrect) {
            tvResult.text = "🎉 Chính xác! Câu trả lời đúng: ${arranged.joinToString(" ")}"
            tvResult.setTextColor(
                ContextCompat.getColor(
                    this@MainActivity,
                    android.R.color.holo_green_dark
                )
            )
        } else {
            tvResult.text = "❌ Chưa đúng. Bạn sắp xếp: ${arranged.joinToString(" ")}"
            tvResult.setTextColor(
                ContextCompat.getColor(
                    this@MainActivity,
                    android.R.color.holo_red_dark
                )
            )
        }
    }

    private fun onReset() = with(binding) {
        wordArrangementView.clearArrangedWords()
        tvResult.text = ""
        //showSnack("Đã reset! Hãy thử lại.")
    }

    private fun showSnack(message: String, long: Boolean = false) {
        Snackbar.make(
            binding.root,
            message,
            if (long) Snackbar.LENGTH_LONG else Snackbar.LENGTH_SHORT
        ).show()
    }
}
