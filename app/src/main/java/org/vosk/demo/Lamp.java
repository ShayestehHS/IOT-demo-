package org.vosk.demo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.widget.TextView;

public class Lamp {

    private final TextView lampStatus;

    @SuppressLint("SetTextI18n")
    public Lamp(TextView lampStatus) {
        this.lampStatus = lampStatus;
        this.lampStatus.setText("off");
    }

    @SuppressLint("SetTextI18n")
    public void turn_off() {
        lampStatus.setText("off");
    }

    @SuppressLint("SetTextI18n")
    public void turn_on() {
        lampStatus.setText("on");
    }
}
