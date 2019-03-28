package vn.zenity.football.fragment

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import vn.zenity.football.R
import vn.zenity.football.base.BaseActivity
import kotlinx.android.synthetic.main.fragment_mine.*
import vn.vietsens.elife.erpstore.manager.local.PreferencesHelper
import java.lang.ref.WeakReference

/**
 * Created by vinhdn on 28-Feb-18.
 */
class MineFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_mine, container, false)
    }

    private val adsListener : (BaseActivity.TypeStateAds) -> Unit = {
        when (it) {
            BaseActivity.TypeStateAds.RewardAdLoaded, BaseActivity.TypeStateAds.AdLoaded -> {
                watchAds?.visibility = View.VISIBLE
            }
            else -> {
                watchAds?.visibility = View.GONE
            }
        }
    }

    private val coinListener: WeakReference<(Int) ->Unit> = WeakReference({ coin ->
        coins?.text = "$coin"
    })

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pager.adapter = PagerAdapter(childFragmentManager)
        tabs.setupWithViewPager(pager)
        watchAds.setOnClickListener {
            (activity as? BaseActivity)?.openRewardVideoAds()
        }
        if((activity as? BaseActivity)?.isRewardLoaded() == true || (activity as? BaseActivity)?.isInterstitialLoaded() == true) {
            watchAds?.visibility = View.VISIBLE
        } else {
            watchAds?.visibility = View.GONE
        }
        (activity as? BaseActivity)?.listAdsListener?.add(adsListener)
        (activity as? BaseActivity)?.coinsListener?.add(coinListener)
    }

    override fun onResume() {
        super.onResume()
        val coin = (activity as? BaseActivity)?.getCoins()
        coins?.text = "$coin"
    }

    inner class PagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
        override fun getItem(position: Int): Fragment {
            if (position == 0)
            return ListFragment.getInstance(-1)
            return FinishedFragment.getInstance()
        }

        override fun getCount(): Int = 2

        override fun getPageTitle(position: Int): CharSequence? {
            if (position == 0)
            return "Work".capitalize()
            return "Finished".capitalize()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? BaseActivity)?.listAdsListener?.remove(adsListener)
        (activity as? BaseActivity)?.coinsListener?.remove(coinListener)
        coinListener.clear()
    }
}