package com.google.cloud.android.speech.retrofit;

import android.util.Log;

import com.google.cloud.android.speech.retrofit.LongRunning.LongrunningResponse;
import com.google.cloud.android.speech.retrofit.LongRunning.Words;
import com.google.cloud.android.speech.view.interfaces.StreamObserverRetrofit;
import com.google.gson.Gson;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by USER on 2017-11-08.
 */

public class GoogleClientRetrofit {


    public void longrunningRequestRetrofit(String name, int rate, MultipartBody.Part body, final StreamObserverRetrofit<LongrunningResponse> callback) {
        RetrofitService retrofitService = RetrofitService.retrofit.create(RetrofitService.class);
        final Gson gson = new Gson();
        retrofitService.longrunningRequest(name, rate, body).enqueue(new Callback<LongrunningResponse>() {
            @Override
            public void onResponse(Call<LongrunningResponse> call, Response<LongrunningResponse> response) {
                if (response.body() != null)

                    callback.onComplete(response.body());
            }

            @Override
            public void onFailure(Call<LongrunningResponse> call, Throwable t) {
                callback.onError(t);
            }
        });
    }
}
