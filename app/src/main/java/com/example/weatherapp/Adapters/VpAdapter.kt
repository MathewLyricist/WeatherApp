package com.example.weatherapp.Adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class VpAdapter(fAct : FragmentActivity, private val list : List<Fragment>) : FragmentStateAdapter(fAct) {
    override fun getItemCount(): Int {
        return list.size
    }

    override fun createFragment(p0: Int): Fragment {
        return list[p0]
    }

}