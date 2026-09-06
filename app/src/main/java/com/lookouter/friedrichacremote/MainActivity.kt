package com.lookouter.friedrichacremote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.lookouter.friedrichacremote.ui.RemoteScreen
import com.lookouter.friedrichacremote.ui.theme.FriedrichRemoteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FriedrichRemoteTheme {
                RemoteScreen()
            }
        }
    }
}
