package com.dj.insulink.shared.core.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

actual fun createCoreHttpClientEngine(): HttpClientEngine = Darwin.create()
