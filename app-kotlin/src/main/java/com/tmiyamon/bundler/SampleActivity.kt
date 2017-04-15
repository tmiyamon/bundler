package com.tmiyamon.bundler

import android.os.Bundle
import android.support.v7.app.AppCompatActivity

class SampleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample)

        val (userName, userId) = BundlerSampleActivityParams.parse(intent)
    }

    @Bundler
    data class Params(val userName: String, val userId: Int)
}
