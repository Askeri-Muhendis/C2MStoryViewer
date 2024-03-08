package com.c2m.storyviewer.screen

import android.animation.Animator
import android.animation.ValueAnimator
import android.os.Bundle
import android.util.SparseIntArray
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.FileDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.c2m.storyviewer.R
import com.c2m.storyviewer.app.StoryApp
import com.c2m.storyviewer.customview.StoryPagerAdapter
import com.c2m.storyviewer.data.StoryUser
import com.c2m.storyviewer.databinding.ActivityMainBinding
import com.c2m.storyviewer.utils.CubeOutTransformer
import com.c2m.storyviewer.utils.StoryGenerator
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

class MainActivity : AppCompatActivity(),
    PageViewOperator {
    private lateinit var binding : ActivityMainBinding
    private lateinit var pagerAdapter: StoryPagerAdapter
    private var currentPage: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpPager()
    }

    private fun setUpPager() {
        val storyUserList = StoryGenerator.generateStories()
        preLoadStories(storyUserList)

        pagerAdapter = StoryPagerAdapter(
            this,
            storyUserList
        )
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.currentItem = currentPage
        binding.viewPager.setPageTransformer(
            CubeOutTransformer()
        )
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback(){
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentPage = position
            }

            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    currentFragment().resumeCurrentStory()
                }
            }
        })
    }

    private fun currentFragment(): StoryDisplayFragment {
        return pagerAdapter.findFragmentByPosition(currentPage) as StoryDisplayFragment
    }

    private fun preLoadStories(storyUserList: ArrayList<StoryUser>) {
        val imageList = mutableListOf<String>()
        val videoList = mutableListOf<String>()

        storyUserList.forEach { storyUser ->
            storyUser.stories.forEach { story ->
                if (story.isVideo()) {
                    videoList.add(story.url)
                } else {
                    imageList.add(story.url)
                }
            }
        }
        preLoadVideos(videoList)
        preLoadImages(imageList)
    }

    @OptIn(UnstableApi::class) private fun preLoadVideos(videoList: MutableList<String>) {
        videoList.map {
            GlobalScope.async {
                val cacheSink = CacheDataSink.Factory()
                    .setCache(StoryApp.simpleCache as Cache)
                val upstreamFactory = DefaultDataSource.Factory(this@MainActivity, DefaultHttpDataSource.Factory())
                val downStreamFactory = FileDataSource.Factory()
                val cacheDataSourceFactory  =
                    CacheDataSource.Factory()
                        .setCache(StoryApp.simpleCache as Cache)
                        .setCacheWriteDataSinkFactory(cacheSink)
                        .setCacheReadDataSourceFactory(downStreamFactory)
                        .setUpstreamDataSourceFactory(upstreamFactory)
                        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)


                try {
                    cacheDataSourceFactory.setCache(StoryApp.simpleCache as Cache)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun preLoadImages(imageList: MutableList<String>) {
        imageList.forEach { imageStory ->
            Glide.with(this).load(imageStory).preload()
        }
    }

    override fun backPageView() {
        if (binding.viewPager.currentItem > 0) {
            try {
                fakeDrag(false)
            } catch (e: Exception) {
                //NO OP
            }
        }
    }
    private var prevDragPosition = 0

    private fun fakeDrag(forward: Boolean) {
        if (prevDragPosition == 0 && binding.viewPager.beginFakeDrag()) {
            ValueAnimator.ofInt(0, binding.viewPager.width).apply {
                duration = 400L
                interpolator = FastOutSlowInInterpolator()
                addListener(object : Animator.AnimatorListener {
                    override fun onAnimationRepeat(animation: Animator) {}

                    override fun onAnimationEnd(animation: Animator) {
                        removeAllUpdateListeners()
                        if (binding.viewPager.isFakeDragging) {
                            binding.viewPager.endFakeDrag()
                        }
                        prevDragPosition = 0
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        removeAllUpdateListeners()
                        if (binding.viewPager.isFakeDragging) {
                            binding.viewPager.endFakeDrag()
                        }
                        prevDragPosition = 0
                    }

                    override fun onAnimationStart(animation: Animator) {}
                })
                addUpdateListener {
                    if (!binding.viewPager.isFakeDragging) return@addUpdateListener
                    val dragPosition: Int = it.animatedValue as Int
                    val dragOffset: Float =
                        ((dragPosition - prevDragPosition) * if (forward) -1 else 1).toFloat()
                    prevDragPosition = dragPosition
                    binding.viewPager.fakeDragBy(dragOffset)
                }
            }.start()
        }
    }
    override fun nextPageView() {
        if (binding.viewPager.currentItem + 1 < (binding.viewPager.adapter?.itemCount ?: 0)) {
            try {
                fakeDrag(true)
            } catch (e: Exception) {
                //NO OP
            }
        } else {
            //there is no next story
            Toast.makeText(this, "All stories displayed.", Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        val progressState = SparseIntArray()
    }
}
