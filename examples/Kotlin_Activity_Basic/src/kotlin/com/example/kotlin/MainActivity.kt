package com.example.kotlin

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val text: TextView = findViewById(R.id.text)

        text.text = "Hello, World!"
    }
}