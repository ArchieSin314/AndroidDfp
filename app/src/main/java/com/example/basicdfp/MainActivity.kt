package com.example.basicdfp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button = findViewById<Button>(R.id.dfp_gen)
        val txt = findViewById<TextView>(R.id.id_placeholder)
        val context = applicationContext
        button.setOnClickListener {
            val d = DfpFactory(GsfIdProvider(context.contentResolver!!),AndroidIdProvider(context.contentResolver!!),MediaDrmIdProvider())
            txt.text = d.getHash()
        }
    }

}