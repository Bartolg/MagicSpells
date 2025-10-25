package com.example.fluidsim

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fluidsim.ai.TFLiteEffectGenerator
import com.example.fluidsim.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// MainActivity should be a top-level class like this
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var effectGenerator: TFLiteEffectGenerator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.simulationView.setLifecycle(lifecycle)

        lifecycleScope.launch {
            val generator = withContext(Dispatchers.IO) {
                TFLiteEffectGenerator.create(this@MainActivity)
            }
            effectGenerator = generator
            if (generator != null) {
                binding.statusText.setText(R.string.status_ready)
                binding.simulationView.attachEffectGenerator(generator)
            } else {
                binding.statusText.setText(R.string.status_missing_model)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        effectGenerator?.close()
    }
}