package com.hyper.weatherapp

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import com.google.gson.Gson
import com.hyper.weatherapp.databinding.ActivityMainBinding
import com.hyper.weatherapp.models.WeatherResponse
import com.hyper.weatherapp.network.WeatherService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone


class MainActivity : AppCompatActivity() {
    private var binding: ActivityMainBinding? = null


    private var mProgressDialog : Dialog ? = null

    private lateinit var mSharePreferences: SharedPreferences



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        mSharePreferences = getSharedPreferences(Constants.PREFERENCE_NAME,Context.MODE_PRIVATE)


        setupUI()

    }

    private fun getCityWeatherDetails(q : String){
        if(Constants.isNetworkAvailable(this)){
            val retrofit : Retrofit = Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service : WeatherService = retrofit.create(WeatherService::class.java)
            val listCall : Call<WeatherResponse> = service.getWeatherByCity(q,Constants.METRIC_UNIT,Constants.APP_ID)

            showCustomProgressDialog()

            listCall.enqueue(object : Callback<WeatherResponse>{

                override fun onResponse(
                    call: Call<WeatherResponse>,
                    response: Response<WeatherResponse>
                ) {
                    if(!response.isSuccessful){
                        Log.e("Error", "Server Response : ${response.code()} : ${response.message()}")
                    }
                    hideProgressDialog()
                    val weatherList : WeatherResponse? = response.body()
                    val weatherResponseJsonString = Gson().toJson(weatherList)
                    val editor = mSharePreferences.edit()
                    editor.putString(Constants.WEATHER_RESPONSE_DATA,weatherResponseJsonString)
                    editor.apply()
                    if (weatherList != null) {
                        setupUI()
                    }
                    Log.i("Response Result" , "$weatherList")
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    Log.e("error" , t.message.toString())
                    hideProgressDialog()
                }

            })

        }else{
            Toast.makeText(this@MainActivity,"No internet available",Toast.LENGTH_SHORT).show()
        }
    }



    private fun showCustomProgressDialog() {
        mProgressDialog = Dialog(this)
        mProgressDialog!!.setContentView(R.layout.dialog_custom_progress)
        mProgressDialog!!.show()
    }

    private fun hideProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog!!.dismiss()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main,menu)

        val search = menu?.findItem(R.id.action_search)
        val searchView = search?.actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {

                if (query != null) {
                    getCityWeatherDetails(query)
                }

                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        return super.onCreateOptionsMenu(menu)
    }


    private fun setupUI(){
        val weatherResponseJsonString = mSharePreferences.getString(Constants.WEATHER_RESPONSE_DATA ,"")

        if(!weatherResponseJsonString.isNullOrEmpty()){
            binding?.llWeatherDetails?.visibility = View.VISIBLE
            binding?.llNoResults?.visibility = View.GONE
            val weatherList = Gson().fromJson(weatherResponseJsonString,WeatherResponse::class.java)
            for(i in weatherList.weather.indices){
                Log.i("weather Name" , weatherList.weather.toString())
                binding?.tvMain?.text = weatherList.weather[i].main
                binding?.tvMainDescription?.text = weatherList.weather[i].description
                binding?.tvTemp?.text = weatherList.main.temp.toString() + getUnit(application.resources.configuration.locales.toString())

                binding?.tvHumidity?.text = weatherList.main.humidity.toString() + " per cent"
                binding?.tvMin?.text = weatherList.main.temp_min.toString() + " min"
                binding?.tvMax?.text = weatherList.main.temp_max.toString() + " max"
                binding?.tvSpeed?.text = weatherList.wind.speed.toString()
                binding?.tvName?.text = weatherList.name
                binding?.tvCountry?.text = weatherList.sys.country

                binding?.tvSunriseTime?.text = unixTime(weatherList.sys.sunrise)
                binding?.tvSunsetTime?.text = unixTime(weatherList.sys.sunset)

                when(weatherList.weather[i].icon){
                    "01d" -> binding?.ivMain?.setImageResource(R.drawable.sunny)
                    "02d" -> binding?.ivMain?.setImageResource(R.drawable.cloud)
                    "03d" -> binding?.ivMain?.setImageResource(R.drawable.cloud)
                    "04d" -> binding?.ivMain?.setImageResource(R.drawable.cloud)
                    "04n" -> binding?.ivMain?.setImageResource(R.drawable.cloud)
                    "10d" -> binding?.ivMain?.setImageResource(R.drawable.rain)
                    "11d" -> binding?.ivMain?.setImageResource(R.drawable.storm)
                    "13d" -> binding?.ivMain?.setImageResource(R.drawable.snowflake)
                    "01n" -> binding?.ivMain?.setImageResource(R.drawable.cloud)
                    "02n" -> binding?.ivMain?.setImageResource(R.drawable.cloud)
                    "03n" -> binding?.ivMain?.setImageResource(R.drawable.cloud)
                    "10n" -> binding?.ivMain?.setImageResource(R.drawable.cloud)
                    "11n" -> binding?.ivMain?.setImageResource(R.drawable.rain)
                    "13n" -> binding?.ivMain?.setImageResource(R.drawable.snowflake)
                    "50d" -> binding?.ivMain?.setImageResource(R.drawable.mist)
                    "50n" -> binding?.ivMain?.setImageResource(R.drawable.mist)
                    "09d" -> binding?.ivMain?.setImageResource(R.drawable.shower_rain)
                    "09n" -> binding?.ivMain?.setImageResource(R.drawable.shower_rain)
                }
            }
        }else{
            binding?.llWeatherDetails?.visibility = View.GONE
            binding?.llNoResults?.visibility = View.VISIBLE
        }
    }

    private fun getUnit(value: String): String? {
        var value = "°C"
        if ("US" == value || "LR" == value || "MM" == value) {
            value = "°F"
        }
        return value
    }

    private fun unixTime(timex : Long) : String?{
        val date = Date(timex *1000L)
        val sdf = SimpleDateFormat("HH:mm" , Locale.UK)
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(date)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}