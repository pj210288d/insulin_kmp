package com.dj.insulink.shared.feature.fitness.di

import com.dj.insulink.shared.feature.fitness.data.local.DatabaseFactory
import com.dj.insulink.shared.feature.fitness.data.local.ExerciseDatabase
import com.dj.insulink.shared.feature.fitness.data.local.buildExerciseDatabase
import com.dj.insulink.shared.feature.fitness.data.repository.ExerciseRepository
import org.koin.core.module.Module
import org.koin.dsl.module

expect fun platformFitnessModule(): Module

val fitnessModule = module {
    includes(platformFitnessModule())
    single<ExerciseDatabase> { buildExerciseDatabase(get<DatabaseFactory>().create()) }
    single { get<ExerciseDatabase>().exerciseDao() }
    single { ExerciseRepository(get(), get()) }
}
