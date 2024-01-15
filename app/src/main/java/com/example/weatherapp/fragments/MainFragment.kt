package com.example.weatherapp.fragments

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.weatherapp.Adapters.VpAdapter
import com.example.weatherapp.Adapters.WeatherModel
import com.example.weatherapp.DialogManager
import com.example.weatherapp.MainViewModel
import com.example.weatherapp.databinding.FragmentMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.tabs.TabLayoutMediator
import com.squareup.picasso.Picasso
import org.json.JSONObject

const val API_KEY = "aadc41a523b744b483c154258230510"
class MainFragment : Fragment() {
    private lateinit var fLocationClient: FusedLocationProviderClient
    private lateinit var pLauncher: ActivityResultLauncher <String>
    private val fList = listOf(
        hoursFragment.newInstance(),
        daysFragment.newInstance()
    )
    private val tList = listOf(
        "Hours",
        "Days"
    )
    private lateinit var binding: FragmentMainBinding
    private val model: MainViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkPermission()
        init()
        updateCurrentCard()
    }

    override fun onResume() {
        super.onResume()
        checkLocation()
    }
    private fun init() = with(binding){
        fLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        val adapter = VpAdapter(activity as FragmentActivity, fList)
        vp.adapter = adapter
        TabLayoutMediator(tabLayout, vp){
            tab, pos -> tab.text = tList[pos]
        }.attach()
        buttonSync.setOnClickListener{
            tabLayout.selectTab(tabLayout.getTabAt(0))
            checkLocation()
        }
        butSearch.setOnClickListener{
            DialogManager.searchByNameDialog(requireContext(), object: DialogManager.Listener{
                override fun onClick(name: String?) {
                    name?.let{it1 -> requestWeatherData(it1)}
                }

            })
        }
    }
    private fun checkLocation(){
        if(isLocationEnabled()){
            getLocation()
        }else {
            DialogManager.locationSettingsDialog(requireContext(), object : DialogManager.Listener{
                override fun onClick(name: String?) {
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }

            })
        }
    }
    private fun isLocationEnabled(): Boolean{
        val lm = activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }
    private fun getLocation(){
        if(!isLocationEnabled()){
            Toast.makeText(requireContext(), "Location disabled", Toast.LENGTH_SHORT).show()
            return
        }
        val cancellationToken = CancellationTokenSource()
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationToken.token).addOnCompleteListener{
            requestWeatherData("${it.result.latitude}, ${it.result.longitude}")
        }
    }
    private fun updateCurrentCard() = with(binding){
        model.liveDataCurrent.observe(viewLifecycleOwner){
            val maxMinTemp = "${it.maxTemperature}°C / ${it.minTemperature}°C"
            city.text = it.city
            currentTemperature.text = it.currentTemperature.ifEmpty {maxMinTemp }
            tvCondition.text = it.condition
            maxMinTemperature.text = if(it.currentTemperature.isEmpty()) "" else maxMinTemp
            speedWind.text = it.speedWind
            Picasso.get().load("https:"+it.imageUrl).into(logoWeatherMainScreen)
        }
    }
    private fun permissionListener(){
        pLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission() ){
            Toast.makeText(activity, "Permission is $it", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkPermission(){
        if(!isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)){
            permissionListener()
            pLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    private fun requestWeatherData(city: String){
        val url = "https://api.weatherapi.com/v1/forecast.json?key="+
                API_KEY+
                "&q="+
                city+
                "&days=7"+
                "&aqi=no&alerts=no"
        val queue = Volley.newRequestQueue(context)
        val request = StringRequest(
            Request.Method.GET,
            url,
            {
                result -> parseWeatherData(result)
            },
            {
                error -> Log.d("MyLog", "Ошибка: $error")
            }
        )
        queue.add(request)
    }
    private fun parseWeatherData(result: String) {
        val mainObject = JSONObject(result)
        val list = parseDays(mainObject)
        parseCurrentData(mainObject, list[0])
    }
    private fun parseCurrentData(mainObject: JSONObject, weatherItem: WeatherModel){
        val item = WeatherModel(
            mainObject.getJSONObject("location").getString("name"),
            "",
            mainObject.getJSONObject("current").getJSONObject("condition").getString("text"),
            mainObject.getJSONObject("current").getString("temp_c").toFloat().toInt().toString()+"°C",
            weatherItem.maxTemperature,
            weatherItem.minTemperature,
            mainObject.getJSONObject("current").getJSONObject("condition").getString("icon"),
            weatherItem.hours,
            "Wind speed: "+mainObject.getJSONObject("current").getString("wind_kph")+" km/h"
        )
        model.liveDataCurrent.value = item
        Log.d("MyLog", "City: ${item.city}")
        Log.d("MyLog", "Time: ${item.time}")
        Log.d("MyLog", "MaxTemperature: ${item.maxTemperature}")
        Log.d("MyLog", "MinTemperature: ${item.minTemperature}")
        Log.d("MyLog", "Condition: ${item.condition}")
        Log.d("MyLog", "Temperature now: ${item.currentTemperature}")
        Log.d("MyLog", "URL Image: ${item.imageUrl}")
        Log.d("Mylog", "Wind speed: ${item.speedWind}")
    }
    private fun parseDays(mainObject: JSONObject): List<WeatherModel>{
        val list = ArrayList<WeatherModel>()
        val daysArray = mainObject.getJSONObject("forecast").getJSONArray("forecastday")
        val name = mainObject.getJSONObject("location").getString("name")
        for(i in 0 until daysArray.length()){
            val day = daysArray[i] as JSONObject
            val item = WeatherModel(
                name,
                day.getString("date"),
                day.getJSONObject("day").getJSONObject("condition").getString("text"),
                "",
                day.getJSONObject("day").getString("maxtemp_c").toFloat().toInt().toString(),
                day.getJSONObject("day").getString("mintemp_c").toFloat().toInt().toString(),
                day.getJSONObject("day").getJSONObject("condition").getString("icon"),
                day.getJSONArray("hour").toString(),
                ""
            )
            list.add(item)
        }
        model.liveDataList.value = list
        return list
    }
    companion object {
        @JvmStatic
        fun newInstance() = MainFragment()

    }
}