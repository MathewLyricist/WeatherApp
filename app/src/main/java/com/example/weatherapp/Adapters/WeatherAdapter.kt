package com.example.weatherapp.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.R
import com.example.weatherapp.databinding.ListItemBinding
import com.squareup.picasso.Picasso

class WeatherAdapter(val listener: Listener?) : ListAdapter<WeatherModel, WeatherAdapter.Holder>(Comparator()){
    class Holder(view: View, val listener: Listener?) : RecyclerView.ViewHolder(view){
        val binding = ListItemBinding.bind(view)
        var itemTemp: WeatherModel? = null
        init{
            itemView.setOnClickListener{
                itemTemp?.let{it1 -> listener?.onClick(it1)}
            }
        }
        fun bind(item: WeatherModel) = with(binding){
            itemTemp = item
            tvDate.text = item.time
            tvCondition.text = item.condition
            tvTemperature.text = item.currentTemperature.ifEmpty { "${item.maxTemperature}°C / ${item.minTemperature}°C" }
            Picasso.get().load("https:"+item.imageUrl).into(imageView)
        }
    }
    class Comparator : DiffUtil.ItemCallback<WeatherModel>(){
        override fun areItemsTheSame(p0: WeatherModel, p1: WeatherModel): Boolean {
            return p0 == p1
        }

        override fun areContentsTheSame(p0: WeatherModel, p1: WeatherModel): Boolean {
            return p0 == p1
        }

    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): Holder {
        val view = LayoutInflater.from(p0.context).inflate(R.layout.list_item, p0, false)
        return Holder(view, listener)
    }

    override fun onBindViewHolder(holder: Holder, p1: Int) {
        holder.bind(getItem(p1))
    }
    interface Listener{
        fun onClick(item:WeatherModel)
    }
}