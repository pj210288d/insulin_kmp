package com.dj.insulink.shared.core.dispatcher

import kotlinx.coroutines.CoroutineDispatcher

// Dispatchers.IO nije deo commonMain API površine kotlinx.coroutines-a koju :shared/commonMain
// vidi kada se prevodi za sve ciljne platforme uključujući iOS (otkriveno preko
// :shared:compileIosMainKotlinMetadata - "Unresolved reference 'IO'" na svakom mestu gde ga je
// repozitorijum sloj ranije direktno referencirao). Repozitorijumi zato koriste ovaj
// expect/actual umesto Dispatchers.IO direktno.
expect val ioDispatcher: CoroutineDispatcher
