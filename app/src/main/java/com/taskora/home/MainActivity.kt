package com.taskora.home

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.taskora.home.ui.navigation.TaskoraApp
import com.taskora.home.ui.theme.TaskoraHomeTheme
import com.taskora.home.ui.viewmodel.TaskoraViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: TaskoraViewModel by viewModels {
        TaskoraViewModel.factory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Swap from the static splash theme to the main app theme before drawing.
        setTheme(R.style.Theme_TaskoraHome)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            TaskoraHomeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val vm = remember { viewModel }
                    TaskoraApp(vm)
                }
            }
        }
    }
}
