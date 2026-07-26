package co.ke.maawebhost.invest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import co.ke.maawebhost.invest.nav.MaaNavGraph
import co.ke.maawebhost.invest.ui.theme.MaaInvestTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaaInvestTheme {
                MaaNavGraph()
            }
        }
    }
}
