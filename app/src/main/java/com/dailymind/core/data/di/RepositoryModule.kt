package com.dailymind.core.data.di

import com.dailymind.core.data.QuoteRepository
import com.dailymind.core.data.QuoteRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindQuoteRepository(impl: QuoteRepositoryImpl): QuoteRepository
}
