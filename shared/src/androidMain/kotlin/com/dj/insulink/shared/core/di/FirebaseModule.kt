package com.dj.insulink.shared.core.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.koin.core.module.Module
import org.koin.dsl.module

// Shared across feature Koin modules (glucose, meals, ...) so FirebaseFirestore is only
// registered once. Must be included exactly once, at the top level in InsulinkApplication.
// FirebaseAuth added alongside for feature/auth's FirebaseAuthRepository (Faza 1) - same
// singleton instance the app's own Hilt-based auth already uses via
// FirebaseAuth.getInstance(), just also exposed through Koin.
val firebaseModule: Module = module {
    single { FirebaseFirestore.getInstance() }
    single { FirebaseAuth.getInstance() }
}
