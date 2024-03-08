package com.c2m.storyviewer.customview

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.c2m.storyviewer.data.StoryUser
import com.c2m.storyviewer.screen.StoryDisplayFragment

class StoryPagerAdapter(private val fragment: FragmentActivity, private val storyList: ArrayList<StoryUser>)
    : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = storyList.size

    override fun createFragment(position: Int): Fragment {
        return StoryDisplayFragment.newInstance(position, storyList[position])
    }

    fun findFragmentByPosition(position: Int): Fragment? {
        val tag = "f${position}"
        return fragment.supportFragmentManager.findFragmentByTag(tag)
    }
}