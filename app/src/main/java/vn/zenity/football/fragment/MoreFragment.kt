package vn.zenity.football.fragment

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import vn.zenity.football.R
import kotlinx.android.synthetic.main.fragment_more.*
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import vn.zenity.football.base.BaseActivity
import vn.zenity.football.extensions.toast


/**
 * Created by vinhdn on 28-Feb-18.
 */
class MoreFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_more, container, false)
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharefriend.setOnClickListener {
            shareApp()
        }
        feedback.setOnClickListener {
            composeEmail("Feedback app World Cup Pixel Art")
        }
        rate.setOnClickListener {
            launchMarket()
        }
        watchAds.setOnClickListener {
            (activity as? BaseActivity)?.openRewardVideoAds()
        }
        if((activity as? BaseActivity)?.isRewardLoaded() == true || (activity as? BaseActivity)?.isInterstitialLoaded() == true) {
            watchAds?.visibility = View.VISIBLE
        } else {
            watchAds?.visibility = View.GONE
        }
        (activity as? BaseActivity)?.listAdsListener?.add(adsListener)

        buyPro.setOnClickListener {
            (activity as? BaseActivity)?.showBuyPro()
        }
        buyDiamond.setOnClickListener {
            (activity as? BaseActivity)?.showDialogBuyDiamond()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? BaseActivity)?.listAdsListener?.remove(adsListener)
    }

    private fun launchMarket() {
        val uri = Uri.parse("market://details?id=" + activity!!.packageName)
        val myAppLinkToMarket = Intent(Intent.ACTION_VIEW, uri)
        try {
            startActivity(myAppLinkToMarket)
        } catch (e: ActivityNotFoundException) {
            toast("unable to find market app")
        }
    }

    private fun composeEmail( subject: String) {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("mailto:") // only email apps should handle this
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf("wordcupart68@gmail.com"))
        intent.putExtra(Intent.EXTRA_SUBJECT, subject)
        if (intent.resolveActivity(activity!!.packageManager) != null) {
            startActivity(intent)
        }
    }

    fun shareApp() {
        val i = Intent(Intent.ACTION_SEND)
        i.type = "text/plain"
        i.putExtra(Intent.EXTRA_SUBJECT, "Sharing App")
        i.putExtra(Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=" + activity!!.packageName)
        startActivity(Intent.createChooser(i, "Share URL"))
    }
}