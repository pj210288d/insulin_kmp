package com.dj.insulink.shared.feature.glucose.data.remote

import com.dj.insulink.shared.feature.glucose.domain.model.GlucoseReading

// Firestore sync za iOS je planiran za fazu 4 migracije (vidi CLAUDE.md), kada bude
// dostupan Mac za testiranje. Do tada svaki poziv eksplicitno puca umesto da se tiho
// no-op-uje, da se odmah primeti ako neki commonMain kod pozove sync na iOS-u.
class NotImplementedGlucoseRemoteDataSource : GlucoseRemoteDataSource {
    override suspend fun pushReading(userId: String, reading: GlucoseReading): Unit =
        throw NotImplementedError("Firestore sync nije implementiran za iOS (faza 4)")

    override suspend fun deleteReading(userId: String, reading: GlucoseReading): Unit =
        throw NotImplementedError("Firestore sync nije implementiran za iOS (faza 4)")

    override suspend fun fetchAllReadings(userId: String): List<GlucoseReading> =
        throw NotImplementedError("Firestore sync nije implementiran za iOS (faza 4)")
}
