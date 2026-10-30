package com.example.nativec.kt

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class MainActivity : Activity() {

    companion object {
        init {
            System.loadLibrary("native")
        }
    }

    external fun getMessage(): String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val text: TextView = findViewById(R.id.text)

        text.text = getMessage()
    }
}