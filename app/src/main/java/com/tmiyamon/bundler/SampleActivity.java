package com.tmiyamon.bundler;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class SampleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);
//
//        BundlerSampleParams.fromIntent(getIntent());
//        Bundler.parse(getIntent().getExtra());
//        String text = BundlerSampleParams.getSampleText(getIntent().getExtras());
//        ((TextView)findViewById(R.id.tv_result)).setText(text);
    }
}
