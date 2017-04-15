package com.tmiyamon.bundler;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class SampleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);

        SampleParams params = BundlerSampleParams.parse(getIntent());
//
//        BundlerSampleParams.fromIntent(getIntent());
//        Bundler.parse(getIntent().getExtra());
//        String text = BundlerSampleParams.getSampleText(getIntent().getExtras());
//        ((TextView)findViewById(R.id.tv_result)).setText(text);
    }

    @Bundler
    public static class SampleParams {
        public String userName;
        public int userId;

        public SampleParams(String userName, int userId) {
            this.userName = userName;
            this.userId = userId;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public int getUserId() {
            return userId;
        }

        public void setUserId(int userId) {
            this.userId = userId;
        }
    }
}
