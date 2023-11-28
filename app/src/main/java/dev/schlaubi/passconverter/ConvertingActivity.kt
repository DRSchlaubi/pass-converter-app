package dev.schlaubi.passconverter

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.wallet.button.WalletButton
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.http.HttpHeaders
import io.ktor.http.headers
import java.io.InputStream

private sealed interface State {
    data object Waiting : State
    data class HasFile(val file: ByteArray) : State {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as HasFile

            if (!file.contentEquals(other.file)) return false

            return true
        }

        override fun hashCode(): Int {
            return file.contentHashCode()
        }
    }

    data class GotToken(val token: String) : State
}

class ConvertingActivity : AppCompatActivity() {
    @SuppressLint("Recycle") // we do use use()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val client = remember { HttpClient { followRedirects = false } }
            var state by remember(intent) { mutableStateOf<State>(State.Waiting) }

            when (val currentState = state) {
                is State.Waiting -> {
                    CircularProgressIndicator()
                    LaunchedEffect(state) {
                        state = State.HasFile(
                            contentResolver.openInputStream(intent.data!!)!!.buffered()
                                .use(InputStream::readBytes)
                        )
                    }
                }

                is State.HasFile -> {
                    CircularProgressIndicator()

                    LaunchedEffect(state) {
                        val data = formData {
                            append("pass", currentState.file, headers {
                                append(HttpHeaders.ContentType, "application/vnd.apple.pkpass")
                                append(HttpHeaders.ContentDisposition, "filename=pass.pkpass")
                            })
                        }

                        val response = client.submitFormWithBinaryData(
                            "https://pass-converter.schlau.bi/convert",
                            data
                        )

                        state = State.GotToken(response.headers[HttpHeaders.Location]!!).also {
                            println("Received token")
                        }
                    }
                }

                is State.GotToken -> {
                    WalletButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentState.token))
                        startActivity(intent)
                    })
                }
            }
        }
    }
}
