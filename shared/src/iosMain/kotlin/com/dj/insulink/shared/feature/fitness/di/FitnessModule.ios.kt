package com.dj.insulink.shared.feature.fitness.di

import com.dj.insulink.shared.feature.fitness.data.local.DatabaseFactory
import com.dj.insulink.shared.feature.fitness.data.remote.ExerciseRemoteDataSource
import com.dj.insulink.shared.feature.fitness.data.remote.NotImplementedExerciseRemoteDataSource
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformFitnessModule(): Module = module {
    single { DatabaseFactory() }
    single<ExerciseRemoteDataSource> { NotImplementedExerciseRemoteDataSource() }
}
