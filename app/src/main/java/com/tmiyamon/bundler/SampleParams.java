package com.tmiyamon.bundler;

@Bundler
public class SampleParams {
    public String userName;
    public int userId;

    public SampleParams(String userName, int userId) {
        this.userName = userName;
        this.userId = userId;
    }
}
