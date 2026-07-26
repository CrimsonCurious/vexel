package com.jar.plugin;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        TextView tv = new TextView(this);

        String text = StringUtils.capitalize("hello from vexel");

        tv.setText(text);

        setContentView(tv);
    }
}