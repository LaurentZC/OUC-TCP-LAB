package com.ouc.tcp.test;

import com.ouc.tcp.test.windows.SenderWindow;

import java.util.TimerTask;

public class GBN_RetransTask extends TimerTask {
    private final SenderWindow window;

    public GBN_RetransTask(SenderWindow window) {
        this.window = window;
    }

    @Override
    public void run() {
        window.handleTimeout();
    }
}
