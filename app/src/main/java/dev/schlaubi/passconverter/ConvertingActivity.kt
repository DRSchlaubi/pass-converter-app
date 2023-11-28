package dev.schlaubi.passconverter

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.wallet.button.WalletButton
import dev.schlaubi.passconverter.util.rememberHttpClient
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BasicAuthCredentials
import io.ktor.client.plugins.auth.providers.basic
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headers
import io.ktor.http.path
import java.io.InputStream

private sealed interface State {
    val isLoading: Boolean

    data object Waiting : State {
        override val isLoading: Boolean = true
    }

    data class HasFile(val file: ByteArray) : State {
        override val isLoading: Boolean = true
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

    data class GotToken(val token: String) : State {
        override val isLoading: Boolean = false
    }

    data class Error(val code: HttpStatusCode) : State {
        override val isLoading: Boolean = false
    }
}

class ConvertingActivity : AppCompatActivity() {
    @SuppressLint("Recycle") // we do use use()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val client = rememberHttpClient {
                followRedirects = false
                defaultRequest {
                    url(BuildConfig.API_SERVICE)
                }
                install(Auth) {
                    basic {
                        sendWithoutRequest { true }
                        credentials { BasicAuthCredentials("apikey", BuildConfig.API_KEY) }
                    }
                }
            }
            var state by remember(intent) { mutableStateOf<State>(State.Waiting) }

            Column(
                verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator()
                    Text(
                        stringResource(R.string.converting),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                when (val currentState = state) {
                    is State.Waiting -> {
                        LaunchedEffect(state) {
                            state = State.HasFile(
                                contentResolver.openInputStream(intent.data!!)!!.buffered()
                                    .use(InputStream::readBytes)
                            )
                        }
                    }

                    is State.HasFile -> {
                        LaunchedEffect(state) {
                            val data = formData {
                                append("pass", currentState.file, headers {
                                    append(HttpHeaders.ContentType, "application/vnd.apple.pkpass")
                                    append(HttpHeaders.ContentDisposition, "filename=pass.pkpass")
                                })
                            }

                            val response = try {
                                client.submitFormWithBinaryData(data) {
                                    url { path("convert") }
                                }
                            } catch (e: ResponseException) {
                                state = State.Error(e.response.status)
                                return@LaunchedEffect
                            }

                            state = State.GotToken(response.headers[HttpHeaders.Location]!!)
                        }
                    }

                    is State.Error -> {
                        Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            stringResource(R.string.error, currentState.code.value),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    is State.GotToken -> {
                        Icon(Icons.Default.Wallet, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(stringResource(R.string.conversion_successful))
                        WalletButton(onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(currentState.token),
                            )
                            startActivity(intent)
                        })
                    }
                }
            }
        }
    }
}
