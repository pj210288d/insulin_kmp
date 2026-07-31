package com.dj.insulink.shared.core.di

import com.google.firebase.firestore.FirebaseFirestore
import org.koin.core.module.Module
import org.koin.dsl.module

// Shared across feature Koin modules (glucose, meals, ...) so FirebaseFirestore is only
// registered once. Must be included exactly once, at the top level in InsulinkApplication.
val firebaseModule: Module = module {
    single { FirebaseFirestore.getInstance() }
}
