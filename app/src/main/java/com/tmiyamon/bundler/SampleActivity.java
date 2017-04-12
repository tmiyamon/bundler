package com.tmiyamon.bundler;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class SampleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);

        String text = BundlerSampleParams.getSampleText(getIntent().getExtras());
        ((TextView)findViewById(R.id.tv_result)).setText(text);
    }
}
