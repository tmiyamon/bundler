package com.tmiyamon.bundler;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onSubmit(View view) {
//        startActivity(BundlerSampleParams.apply(new Intent(this, SampleActivity.class), "name", 1));
    }
}
