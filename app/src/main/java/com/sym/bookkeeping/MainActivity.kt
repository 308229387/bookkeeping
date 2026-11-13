package com.sym.bookkeeping

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sym.bookkeeping.ui.TradeViewModel
import com.sym.bookkeeping.ui.navigation.BookkeepingAppNav
import com.sym.bookkeeping.ui.theme.BookkeepingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as BookkeepingApp
        setContent {
            BookkeepingTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val viewModel: TradeViewModel = viewModel(
                        factory = TradeViewModel.factory(app)
                    )
                    BookkeepingAppNav(viewModel = viewModel)
                }
            }
        }
    }
}
