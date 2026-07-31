package com.dj.insulink.shared.feature.meals.data.remote

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

actual fun createHttpClientEngine(): HttpClientEngine = Darwin.create()
