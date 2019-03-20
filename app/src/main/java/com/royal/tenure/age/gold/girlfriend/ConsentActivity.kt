package com.royal.tenure.age.gold.girlfriend

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button

class ConsentActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_consent)

        findViewById<Button>(R.id.consent_button).setOnClickListener {
            getSharedPreferences(Commons.PRIVACY_INFO, Context.MODE_PRIVATE)
                .edit().putBoolean(Commons.CONSENT_CHOICE, true).apply()
            Log.e(Commons.TAG, "you clicked consent_button")
            finish()
        }

        findViewById<Button>(R.id.decline_button).setOnClickListener {
            getSharedPreferences(Commons.PRIVACY_INFO, Context.MODE_PRIVATE)
                .edit().putBoolean(Commons.CONSENT_CHOICE, false).apply()
            Log.e(Commons.TAG, "you clicked deny_button")
            finish()
        }
    }
}
