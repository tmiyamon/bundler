package com.tmiyamon.bundler

import android.support.v7.app.AppCompatActivity
import android.os.Bundle

class SampleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample)

        val (userId, userName) = BundlerSampleParams.parse(intent)
    }
}
