package dev.schlaubi.passconverter.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

@Composable
fun rememberHttpClient(block: HttpClientConfig<*>.() -> Unit = {}): HttpClient {
    val client = RememberedHttpClient(HttpClient(block))

    return client.httpClient
}

private class RememberedHttpClient(val httpClient: HttpClient) : RememberObserver {
    override fun onAbandoned() = httpClient.close()

    override fun onForgotten() = httpClient.close()

    override fun onRemembered() = Unit

}
