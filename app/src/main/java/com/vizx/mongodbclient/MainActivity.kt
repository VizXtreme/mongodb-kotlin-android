package com.vizx.mongodbclient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vizx.mongodbclient.ui.MongoApp
import com.vizx.mongodbclient.ui.MongoViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: MongoViewModel = viewModel()
            MongoApp(viewModel = viewModel)
        }
    }
}
