package com.hyper.weatherapp.network

import com.hyper.weatherapp.models.WeatherResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query


interface WeatherService {

    @GET("2.5/weather")
    fun getWeatherByCity(
        @Query("q") q : String?,
        @Query("units") units : String?,
        @Query("appid") appid : String?
    ) : Call<WeatherResponse>
}