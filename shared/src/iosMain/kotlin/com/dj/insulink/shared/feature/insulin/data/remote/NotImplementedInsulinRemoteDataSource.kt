package com.dj.insulink.shared.feature.insulin.data.remote

import com.dj.insulink.shared.feature.insulin.domain.model.InsulinType

class NotImplementedInsulinRemoteDataSource : InsulinRemoteDataSource {
    override suspend fun pushInsulinType(userId: String, insulinType: InsulinType): Unit =
        throw NotImplementedError("Firestore sync nije implementiran za iOS (faza 4)")

    override suspend fun deleteInsulinType(userId: String, insulinType: InsulinType): Unit =
        throw NotImplementedError("Firestore sync nije implementiran za iOS (faza 4)")

    override suspend fun fetchAllInsulinTypes(userId: String): List<InsulinType> =
        throw NotImplementedError("Firestore sync nije implementiran za iOS (faza 4)")
}
