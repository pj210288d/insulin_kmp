package com.dj.insulink.shared.feature.meals.data.remote

import com.dj.insulink.shared.feature.meals.domain.model.FoodImageAnalysis

interface FoodImageAnalysisRemoteDataSource {
    suspend fun analyzeFoodImage(imageBytes: ByteArray): FoodImageAnalysis
}
