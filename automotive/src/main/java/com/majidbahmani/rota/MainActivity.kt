package com.majidbahmani.rota

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.majidbahmani.rota.ui.map.NearbyMapRoute
import com.majidbahmani.rota.ui.theme.RotaTheme
import dagger.hilt.android.AndroidEntryPoint

/** AAOS entry point: a native app that draws its own map (no Car App Library templates). */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RotaTheme {
                NearbyMapRoute()
            }
        }
    }
}
