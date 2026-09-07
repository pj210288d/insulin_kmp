package com.dj.insulink.shared.core.dispatcher

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

// Dispatchers.Default (deljeni thread pool) - dovoljno za lokalne Room/SQLite operacije koje
// ovaj sloj radi na iOS-u (nema cloud sync-a za sada - vidi NotImplemented*RemoteDataSource).
actual val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
