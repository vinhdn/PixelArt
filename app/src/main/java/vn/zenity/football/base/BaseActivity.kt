package vn.zenity.football.base

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.android.billingclient.api.*
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.reward.RewardItem
import com.google.android.gms.ads.reward.RewardedVideoAd
import com.google.android.gms.ads.reward.RewardedVideoAdListener
import vn.zenity.football.R
import vn.zenity.football.extensions.Config
import kotlinx.android.synthetic.main.layout_buy_diamond.*
import org.jetbrains.anko.toast
import vn.vietsens.elife.erpstore.manager.local.PreferencesHelper
import java.util.ArrayList
import com.android.billingclient.api.BillingClient.SkuType
import com.android.billingclient.api.BillingFlowParams
import vn.zenity.football.extensions.showConfirm
import java.lang.ref.WeakReference


/**
 * Created by vinhdn on 07-Mar-18.
 */
abstract class BaseActivity : AppCompatActivity(), RewardedVideoAdListener, PurchasesUpdatedListener {

    companion object {
        val PLAN01 = "1500_diamond"
        val PLAN02 = "500_diamond"
        val PLAN03 = "remove_ads"
    }

    protected var plan01Sku: String = ""
    protected var plan02Sku: String = ""
    protected var plan03Sku: String = ""
    protected var plan01Token: String = ""
    protected var plan02Token: String = ""
    private var listSku: List<SkuDetails> = listOf()

    enum class TypeStateAds {
        RewardAdClosed, RewardAdLeft, RewardAdLoaded, RewardAdOpened, RewardAdStared, RewardAdFailedToLoad, Rewarded,
        AdLoaded, AdOpened, AdClosed, AdFailed
    }

    open val listAdsListener = arrayListOf<(TypeStateAds) -> Unit>()
    open val coinsListener = arrayListOf<WeakReference<(Int) -> Unit>>()
    private var isClickShowInterstitialAd = false

    protected var mBillingClient: BillingClient? = null
    protected var isPlan01Used = false
    protected var isPlan02Used = false
    protected var isPlan03Used = false
    open protected fun isPurchaseUse(): Boolean {
        return false
    }

    override fun onRewardedVideoAdClosed() {
        loadRewardedVideoAd()
        listAdsListener.forEach {
            it.invoke(TypeStateAds.RewardAdClosed)
        }
    }

    override fun onRewardedVideoAdLeftApplication() {
        listAdsListener.forEach {
            it.invoke(TypeStateAds.RewardAdLeft)
        }
    }

    open override fun onRewardedVideoAdLoaded() {
        listAdsListener.forEach {
            it.invoke(TypeStateAds.RewardAdLoaded)
        }
    }

    override fun onRewardedVideoAdOpened() {
        listAdsListener.forEach {
            it.invoke(TypeStateAds.RewardAdOpened)
        }
    }

    override fun onRewarded(reward: RewardItem) {
        rewardSuccess()
    }

    private fun rewardSuccess(coinIncrease: Int = 60) {
        var coin = getCoins()
        coin += coinIncrease
        setCoins(coin)
        listAdsListener.forEach {
            it.invoke(TypeStateAds.Rewarded)
        }
        toast("+60 CUPs")
        loadRewardedVideoAd()
    }

    public fun getCoins(): Int {
        return PreferencesHelper.shared.getIntValue("Coins", 1000) ?: 0
    }

    public fun setCoins(coins: Int) {
        PreferencesHelper.shared.putValue("Coins", coins)
        for (ltn in coinsListener) {
            ltn.get()?.invoke(coins)
        }
    }

    override fun onRewardedVideoStarted() {
        listAdsListener.forEach {
            it.invoke(TypeStateAds.RewardAdStared)
        }
    }

    override fun onRewardedVideoAdFailedToLoad(p0: Int) {
        listAdsListener.forEach {
            it.invoke(TypeStateAds.RewardAdFailedToLoad)
        }
    }

    private lateinit var mRewardedVideoAd: RewardedVideoAd
    private var mInterstitialAd: InterstitialAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this, Config.adsId)
        mRewardedVideoAd = MobileAds.getRewardedVideoAdInstance(this)
        mRewardedVideoAd.userId = Config.adsId
        mRewardedVideoAd.rewardedVideoAdListener = this
        loadRewardedVideoAd()

        mInterstitialAd = InterstitialAd(this)
        mInterstitialAd?.adUnitId = Config.adsInter
        loadIntertitialAd()
        mInterstitialAd?.adListener = object : AdListener() {
            override fun onAdLoaded() {
                super.onAdLoaded()
                isClickShowInterstitialAd = false
                listAdsListener.forEach {
                    it.invoke(TypeStateAds.AdLoaded)
                }
            }

            override fun onAdOpened() {
                super.onAdOpened()
                listAdsListener.forEach {
                    it.invoke(TypeStateAds.AdOpened)
                }
                if (isClickShowInterstitialAd) {
                    onRewarded(object : RewardItem {
                        override fun getType(): String = "Interstitial"

                        override fun getAmount(): Int = 60
                    })
                }
            }

            override fun onAdClosed() {
                super.onAdClosed()
                loadIntertitialAd()
            }

            override fun onAdFailedToLoad(p0: Int) {
                super.onAdFailedToLoad(p0)
                isClickShowInterstitialAd = false
                listAdsListener.forEach {
                    it.invoke(TypeStateAds.AdFailed)
                }
            }

            override fun onAdClicked() {
                super.onAdClicked()
                loadIntertitialAd()
            }
        }

        if (isPurchaseUse()) {
            mBillingClient = BillingClient.newBuilder(this).setListener(this).build()
            mBillingClient?.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(@BillingClient.BillingResponse billingResponseCode: Int) {
                    if (billingResponseCode == BillingClient.BillingResponse.OK) {
                        // The billing client is ready. You can query purchases here.
                        this@BaseActivity.onBillingSetupFinished(true)
                        getListItems()
                        getHistoryPurchases()
                    } else {
                        this@BaseActivity.onBillingSetupFinished(false)
                    }
                    Log.d("billingResponseCode", "" + billingResponseCode)
                }

                override fun onBillingServiceDisconnected() {
                    // Try to restart the connection on the next request to
                    // Google Play by calling the startConnection() method.
                    Log.d("onBillingDisconnected", "onBillingServiceDisconnected")
                    this@BaseActivity.onBillingSetupFinished(false)
                }
            })
        }
    }

    private fun loadRewardedVideoAd() {
        mRewardedVideoAd.loadAd(Config.adsReward,
                AdRequest.Builder().addTestDevice("F7C2DD6ABF615BEECC0532B3657BE9E7").build())
    }

    private fun loadIntertitialAd() {
        mInterstitialAd?.loadAd(AdRequest.Builder().addTestDevice("F7C2DD6ABF615BEECC0532B3657BE9E7").build())
    }

    public fun openRewardVideoAds() {
        isClickShowInterstitialAd = false
        if (mRewardedVideoAd.isLoaded) {
            mRewardedVideoAd.show()
        } else {
            if (mInterstitialAd?.isLoaded == true) {
                isClickShowInterstitialAd = true
                mInterstitialAd?.show()
            } else {
                loadIntertitialAd()
                loadRewardedVideoAd()
                toast("Reward video is loading...")
            }
        }
    }

    public fun showInterstitalAds() {
        if (!isPlan03Used && mInterstitialAd?.isLoaded == true) {
            mInterstitialAd?.show()
        }
    }

    public fun isRewardLoaded(): Boolean {
        return mRewardedVideoAd.isLoaded
    }

    public fun isInterstitialLoaded(): Boolean {
        if (mInterstitialAd?.isLoaded == true) {
            return true
        }
        loadIntertitialAd()
        return false
    }

    override fun onPause() {
        super.onPause()
        mRewardedVideoAd.pause(this)
    }

    override fun onResume() {
        super.onResume()
        mRewardedVideoAd.resume(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        mRewardedVideoAd.destroy(this)
        mInterstitialAd = null
    }

    override fun onPurchasesUpdated(responseCode: Int, purchases: MutableList<Purchase>?) {
        if (responseCode == BillingClient.BillingResponse.OK && purchases != null) {
            onPurchases(purchases)
            for (purchase in purchases) {
                Log.d("Purchase", purchase.toString())
                if (purchase.sku == PLAN03) {
                    isPlan03Used = true
                }

                if (purchase.sku == PLAN02) {
                    plan02Token = purchase.purchaseToken
                    rewardSuccess(1500)
                }

                if (purchase.sku == PLAN01) {
                    plan01Token = purchase.purchaseToken
                    rewardSuccess(500)
                }
            }
        } else if (responseCode == BillingClient.BillingResponse.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
        } else {
            // Handle any other error codes.
        }
    }

    protected fun onBillingSetupFinished(isSuccess: Boolean) {
//        showAds()
    }

    protected open fun onPurchases(purchases: List<Purchase>?) {
        if (purchases != null) {
            for (purchase in purchases) {
                if (purchase.sku == PLAN01) {
                    plan01Token = purchase.purchaseToken
                    isPlan01Used = true
                }
                if (purchase.sku == PLAN02) {
                    plan02Token = purchase.purchaseToken
                    isPlan02Used = true
                }
                if (purchase.sku == PLAN03) {
                    isPlan03Used = true
                }
            }
        }
//        showAds()
    }

    protected fun getHistoryPurchases() {
        mBillingClient?.queryPurchaseHistoryAsync(BillingClient.SkuType.INAPP,
                { responseCode, purchasesList ->
                    if (responseCode == BillingClient.BillingResponse.OK && purchasesList != null) {
                        onPurchases(purchasesList)
                    }
                })

    }

    private fun getListItems() {
        val skuList = ArrayList<String>()
        skuList.add(PLAN01)
        skuList.add(PLAN02)
        skuList.add(PLAN03)
        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP)
        mBillingClient?.querySkuDetailsAsync(params.build(), { _, list ->
            onListItemsGet(list)
            Log.d("Purchase", list.toString())
        })
    }

    open protected fun onListItemsGet(list: List<SkuDetails>) {
//        showAds()
        this.listSku = list
    }

    fun showDialogBuyDiamond() {
//        if (listSku != nu)
        val dialog = vn.zenity.football.extensions.showDialog(this, R.layout.layout_buy_diamond) { dia ->
            dia.btnCancel.setOnClickListener {
                dia.dismiss()
            }
            for (sku in listSku) {
                if (sku.sku == PLAN01) {
                    dia.tvPrice500.text = sku.price
                }

                if (sku.sku == PLAN02) {
                    dia.tvPrice1500.text = sku.price
                }
            }
            dia.btnBuy500.setOnClickListener {
                val flowParams = BillingFlowParams.newBuilder()
                        .setSku(PLAN01)
                        .setType(SkuType.INAPP)
                        .build()
                if (!isPlan01Used) {
                    val responseCode = mBillingClient?.launchBillingFlow(this, flowParams) ?: -1
                }else {
                    mBillingClient?.consumeAsync(plan01Token, { responseCode, i1 ->
//                        if (responseCode == BillingClient.BillingResponse.OK)
                            mBillingClient?.launchBillingFlow(this, flowParams)
                    }) ?: -1
                }
            }
            dia.btnBuy1500.setOnClickListener {
                val flowParams = BillingFlowParams.newBuilder()
                        .setSku(PLAN02)
                        .setType(SkuType.INAPP)
                        .build()
                if (!isPlan02Used) {
                    val responseCode = mBillingClient?.launchBillingFlow(this, flowParams) ?: -1
                }else {
                    mBillingClient?.consumeAsync(plan02Token, { responseCode, i1 ->
//                        if (responseCode == BillingClient.BillingResponse.OK)
                            mBillingClient?.launchBillingFlow(this, flowParams)
                    }) ?: -1
                }
            }
        }
        dialog.show()
    }

    fun showBuyPro() {
        val flowParams = BillingFlowParams.newBuilder()
                .setSku(PLAN03)
                .setType(SkuType.INAPP)
                .build()
        val responseCode = mBillingClient?.launchBillingFlow(this, flowParams) ?: -1
    }

    fun showRateApp() {
        showConfirm(this, "If you love our App, please take a moment to rate it. It won't take more than a minute. Thank for your support!", "Rate our App", "Rate", {
            launchMarket()
            PreferencesHelper.shared.putValue("isRated", true)
            PreferencesHelper.shared.putValue("isShowRate", false)
            it.dismiss()
            super.onBackPressed()
        }, "Close", {
            PreferencesHelper.shared.putValue("isShowRate", false)
            it.dismiss()
            super.onBackPressed()
        }, "Never", {
            PreferencesHelper.shared.putValue("isRated", true)
            PreferencesHelper.shared.putValue("isShowRate", false)
            it.dismiss()
            super.onBackPressed()
        })
    }

    private fun launchMarket() {
        val uri = Uri.parse("market://details?id=$packageName")
        val myAppLinkToMarket = Intent(Intent.ACTION_VIEW, uri)
        try {
            startActivity(myAppLinkToMarket)
        } catch (e: ActivityNotFoundException) {
        }
    }
}