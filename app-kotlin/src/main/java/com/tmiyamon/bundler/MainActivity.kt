package com.tmiyamon.bundler

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun onSubmit(view: View) {
        startActivity(BundlerSampleActivityParams.apply(Intent(this, SampleActivity::class.java), "name", 1));
    }
}
