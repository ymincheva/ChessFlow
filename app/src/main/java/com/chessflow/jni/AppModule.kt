package com.chessflow.jni

import com.chessflow.jni.repositories.PuzzleRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun providePuzzleRepository(): PuzzleRepository = PuzzleRepository()

}