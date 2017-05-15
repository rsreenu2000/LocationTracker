package com.sreeni.locationtracker;

import android.app.Application;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by sreenirayanki on 5/14/17.
 */

public class LocationTrackerApp extends Application {
    public BlockingQueue<String> waitList = new LinkedBlockingQueue<>();
    public BlockingQueue<String> tagList = new LinkedBlockingQueue<>(5);
}
