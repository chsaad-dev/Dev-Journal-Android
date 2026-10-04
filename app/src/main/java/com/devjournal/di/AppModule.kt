package com.devjournal.di

import android.content.Context
import androidx.room.Room
import com.devjournal.data.model.local.AppDatabase
import com.devjournal.data.model.local.BookmarkedPostDao
import com.devjournal.data.model.local.DraftDao
import com.devjournal.data.remote.CloudinaryUploader
import com.devjournal.data.remote.NotifyWorkerApi
import com.devjournal.data.repository.AuthRepositoryImpl
import com.devjournal.data.repository.BugReportRepositoryImpl
import com.devjournal.data.repository.CommentRepositoryImpl
import com.devjournal.data.repository.DraftRepositoryImpl
import com.devjournal.data.repository.PostRepositoryImpl
import com.devjournal.data.repository.UserRepositoryImpl
import com.devjournal.domain.repository.AuthRepository
import com.devjournal.domain.repository.BugReportRepository
import com.devjournal.domain.repository.CommentRepository
import com.devjournal.domain.repository.DraftRepository
import com.devjournal.domain.repository.PostRepository
import com.devjournal.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindPostRepository(
        impl: PostRepositoryImpl
    ): PostRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        impl: UserRepositoryImpl
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindCommentRepository(
        impl: CommentRepositoryImpl
    ): CommentRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindDraftRepository(
        draftRepositoryImpl: DraftRepositoryImpl
    ): DraftRepository

    @Binds
    @Singleton
    abstract fun bindNetworkMonitor(
        networkMonitorImpl: com.devjournal.util.ConnectivityNetworkMonitor
    ): com.devjournal.util.NetworkMonitor

    @Binds
    @Singleton
    abstract fun bindBugReportRepository(
        impl: BugReportRepositoryImpl
    ): BugReportRepository

    companion object {
        @Provides
        @Singleton
        fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

        @Provides
        @Singleton
        fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

        @Provides
        @Singleton
        fun provideOkHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
        }

        @Provides
        @Singleton
        fun provideCloudinaryUploader(
            @ApplicationContext context: Context,
            client: OkHttpClient
        ): CloudinaryUploader = CloudinaryUploader(context, client)

        @Provides
        @Singleton
        fun provideNotifyWorkerApi(
            client: OkHttpClient
        ): NotifyWorkerApi = NotifyWorkerApi(client)
        @Provides
        @Singleton
        fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "devjournal_database"
            ).fallbackToDestructiveMigration().build()
        }

        @Provides
        @Singleton
        fun provideDraftDao(database: AppDatabase): DraftDao {
            return database.draftDao()
        }

        @Provides
        @Singleton
        fun provideBookmarkedPostDao(database: AppDatabase): BookmarkedPostDao {
            return database.bookmarkedPostDao()
        }
    }
}

