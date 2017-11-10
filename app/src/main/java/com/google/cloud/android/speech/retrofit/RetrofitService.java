package com.google.cloud.android.speech.retrofit;

import com.facebook.stetho.okhttp3.StethoInterceptor;
import com.google.cloud.android.speech.longRunning.longRunningDTO.LongrunningResponse;

import java.util.concurrent.TimeUnit;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

/**
 * Created by USER on 2017-11-08.
 */

public interface RetrofitService {

    @Multipart
    @POST("users/upload/{filename}/{samplerate}")
    Call<LongrunningResponse> longrunningRequest(
            @Path("filename") String filename,
            @Path("samplerate") int samplerate,
            @Part MultipartBody.Part file);


    Retrofit retrofit = new Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .client(new OkHttpClient().newBuilder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .addNetworkInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                    .addNetworkInterceptor(new StethoInterceptor()) .build())
            .baseUrl("https://speech-diary-server-express4-dboong.c9users.io/")
            .build();
}
