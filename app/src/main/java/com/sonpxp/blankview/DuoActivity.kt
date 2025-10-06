package com.sonpxp.blankview

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sonpxp.blankview.databinding.ActivityDuoBinding

class DuoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDuoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDuoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupDuolingoView()
    }

    private fun setupDuolingoView() {

    }

}