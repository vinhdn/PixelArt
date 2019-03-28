package vn.zenity.football

import android.app.Dialog
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import com.android.billingclient.api.Purchase
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import vn.zenity.football.R
import vn.zenity.football.base.BaseActivity
import vn.zenity.football.extensions.loadBitmapAsset
import vn.zenity.football.extensions.showLoading
import vn.zenity.football.fragment.ShowResultDrawingFragment
import vn.zenity.football.manager.PixelDB
import kotlinx.android.synthetic.main.activity_show_finished.*
import org.jetbrains.anko.doAsync
import java.lang.ref.WeakReference

class ShowFinishedActivity : BaseActivity() {

    private lateinit var dialogLoading : Dialog
    private var isFirstShowInterstitialAd = false

    private val adsListener : (BaseActivity.TypeStateAds) -> Unit = { type ->
        when (type) {
            BaseActivity.TypeStateAds.AdLoaded -> {
                if(!isFirstShowInterstitialAd) {
                    isFirstShowInterstitialAd = true
                    weakSelf.get()?.showInterstitalAds()
                }
            }
            else -> {
            }
        }
    }

    private lateinit var weakSelf: WeakReference<ShowFinishedActivity>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_finished)
        weakSelf = WeakReference(this)
        listAdsListener.add(adsListener)
        val pathFinished = intent.getStringExtra("path")
//        adView.adSize = AdSize.SMART_BANNER
//        adView.adUnitId = Config.adsBanner
        dialogLoading = showLoading(this)
        dialogLoading.show()
        pathFinished?.loadBitmapAsset(alpha = 0.6f) { bitmap ->
            bitmap?.let {
                doAsync {
                    val imp = PixelDB.getInstance().imageDao().getFinishImageByName(pathFinished)
                    imp?.bitmap?.let { impLet ->
                        var w = Resources.getSystem().displayMetrics.widthPixels
                        if (w <= 0) w = 1024
                        val bmFinished = BitmapFactory.decodeByteArray(impLet, 0, impLet.size)
                        if (bitmap.width > 0)
                            weakSelf.get()?.showFinishDrawing(bmFinished, bitmap, (w / bitmap.width).toFloat())
                        weakSelf.get()?.dialogLoading?.dismiss()
                    }
                }
            }
        }
    }

    private fun showFinishDrawing(bmC: Bitmap, bmG: Bitmap, pixSize: Float = 10f) {
        val frag = ShowResultDrawingFragment.getInstance(bmC, bmG, pixSize, true)
        supportFragmentManager.beginTransaction().replace(R.id.frame, frag).commit()
    }

    override fun onDestroy() {
        super.onDestroy()
        weakSelf.clear()
        listAdsListener.remove(adsListener)
    }

    override fun onPurchases(purchases: List<Purchase>?) {
        super.onPurchases(purchases)
        if (!isPlan03Used) {
            val adRequest = AdRequest.Builder().addTestDevice("F7C2DD6ABF615BEECC0532B3657BE9E7").build()
            adView.loadAd(adRequest)
            adView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    adView.visibility = View.VISIBLE
                    super.onAdLoaded()
                }

                override fun onAdClosed() {
                    adView.visibility = View.GONE
                    super.onAdClosed()
                }
            }
        } else {
            adView.visibility = View.GONE
        }
    }
}
