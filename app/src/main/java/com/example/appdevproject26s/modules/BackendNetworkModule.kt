package com.example.appdevproject26s.modules

import com.example.appdevproject26s.gamification.achievements.AchievementApi
import com.example.appdevproject26s.gamification.challenges.ChallengeApi
import com.example.appdevproject26s.gamification.leaderboard.LeaderboardApi
import com.example.appdevproject26s.auth.AuthApiService
import com.example.appdevproject26s.auth.AuthInterceptor
import com.example.appdevproject26s.network.BACKEND_REST_URL
import com.example.appdevproject26s.pr.PersonalBestApi
import com.example.appdevproject26s.social.friends.FriendApiService
import com.example.appdevproject26s.social.messaging.ChatApiService
import com.example.appdevproject26s.social.sharing.PinApiService
import com.example.appdevproject26s.social.sharing.SharingApiService
import com.example.appdevproject26s.steps.StepsApi
import com.example.appdevproject26s.profile.UserApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BackendNetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BACKEND_REST_URL)
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }


    @Provides
    @Singleton
    fun provideUserApiService(retrofit: Retrofit): UserApiService {
        return retrofit.create(UserApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideFriendApiService(retrofit: Retrofit): FriendApiService {
        return retrofit.create(FriendApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideChatApiService(retrofit: Retrofit): ChatApiService {
        return retrofit.create(ChatApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideLeaderboardApi(retrofit: Retrofit): LeaderboardApi {
        return retrofit.create(LeaderboardApi::class.java)
    }

    @Provides
    @Singleton
    fun provideChallengeApi(retrofit: Retrofit): ChallengeApi {
        return retrofit.create(ChallengeApi::class.java)
    }

    @Provides
    @Singleton
    fun provideStepsApi(retrofit: Retrofit): StepsApi {
        return retrofit.create(StepsApi::class.java)
    }

    @Provides
    @Singleton
    fun provideAchievementApi(retrofit: Retrofit): AchievementApi {
        return retrofit.create(AchievementApi::class.java)
    }

    @Provides
    @Singleton
    fun providePersonalBestApi(retrofit: Retrofit): PersonalBestApi {
        return retrofit.create(PersonalBestApi::class.java)
    }

    @Provides
    @Singleton
    fun providePinApiService(retrofit: Retrofit): PinApiService {
        return retrofit.create(PinApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideSharingApiService(retrofit: Retrofit): SharingApiService {
        return retrofit.create(SharingApiService::class.java)
    }
}