package dev.schlaubi.passconverter

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.schlaubi.passconverter.ui.theme.PassConverterAppTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val pickerLauncher =
                rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
                    val intent = Intent(this, ConvertingActivity::class.java).apply {
                        data = it
                    }

                    startActivity(intent)
                }

            PassConverterAppTheme {
                Column(
                    verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Icon(Icons.Default.Wallet, null)
                    Text(stringResource(R.string.welcome))
                    Button(onClick = { pickerLauncher.launch(arrayOf("application/vnd.apple.pkpass")) }) {
                        Icon(Icons.Default.FileOpen, null)
                        Text(stringResource(R.string.select_file))
                    }
                }
            }
        }
    }
}
