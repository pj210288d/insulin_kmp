package com.dj.insulink.util

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope

fun TestScope.collectInBackground(vararg flows: StateFlow<*>) {
    flows.forEach { flow ->
        backgroundScope.launch { flow.collect {} }
    }
}
