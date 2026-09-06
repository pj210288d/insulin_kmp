package com.dj.insulink.shared.core.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

actual fun createCoreHttpClientEngine(): HttpClientEngine = OkHttp.create()
