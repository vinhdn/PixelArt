package vn.zenity.football

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import vn.zenity.football.app.App
import vn.zenity.football.base.BaseActivity
import vn.zenity.football.extensions.Config
import vn.zenity.football.extensions.showConfirm
import vn.zenity.football.extensions.toast
import vn.zenity.football.fragment.HomeFragment
import vn.zenity.football.fragment.MineFragment
import vn.zenity.football.fragment.MoreFragment
import vn.zenity.football.tools.LoadDataAsyn
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.tab_indicator.view.*
import vn.vietsens.elife.erpstore.manager.local.PreferencesHelper
import org.json.JSONException
import android.os.AsyncTask
import android.util.Log
import com.android.billingclient.api.Purchase
import com.google.gson.Gson
import vn.zenity.football.R
import vn.zenity.football.extensions.showAlert
import vn.zenity.football.models.InappAlert
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL


class MainActivity : BaseActivity() {

    override fun isPurchaseUse() = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tabhost.setup(this, supportFragmentManager, android.R.id.tabcontent)

        if (App.get().data == null) {
            LoadDataAsyn(assets) {
                setupTab()
            }.execute()
        } else {
            setupTab()
        }
//        adView.adSize = AdSize.SMART_BANNER
//        adView.adUnitId = Config.adsBanner
//        JsonTask().execute("http://ec2-54-191-153-21.us-west-2.compute.amazonaws.com/php_ubecosystem/Services/pixart_inapp_alert.json")
    }

    private fun setupTab() {
        val tab1 = layoutInflater.inflate(R.layout.tab_indicator, null)
        tab1.text.text = "Library"
        tab1.img.setImageResource(R.drawable.selector_home)
        tabhost.addTab(tabhost.newTabSpec("Library").setIndicator(tab1), HomeFragment::class.java, null)
        val tab2 = layoutInflater.inflate(R.layout.tab_indicator, null)
        tab2.text.text = "My Work"
        tabhost.addTab(tabhost.newTabSpec("My Work").setIndicator(tab2), MineFragment::class.java, null)
        val tab3 = layoutInflater.inflate(R.layout.tab_indicator, null)
        tab3.img.setImageResource(R.drawable.selector_more)
        tab3.text.text = "More"
        tabhost.addTab(tabhost.newTabSpec("More").setIndicator(tab3), MoreFragment::class.java, null)
        tabhost.setOnTabChangedListener {
            if (Config.isShowInsterAdsIfSwitchScreen && isInterstitialLoaded()) {
                showInterstitalAds()
            }
        }
    }

    override fun onBackPressed() {
        if (PreferencesHelper.shared.getBooleanValue("isShowRate", true) && !PreferencesHelper.shared.getBooleanValue("isRated", false)) {
            showRateApp()
            return
        } else {
            PreferencesHelper.shared.putValue("isShowRate", true)
            if (Config.isShowInsterAdsIfSwitchScreen && isInterstitialLoaded() == true) {
                showInterstitalAds()
            }
        }
        super.onBackPressed()
    }



    @SuppressLint("StaticFieldLeak")
    private inner class JsonTask : AsyncTask<String, String, String?>() {

        override fun doInBackground(vararg params: String): String? {

            var connection: HttpURLConnection? = null
            var reader: BufferedReader? = null

            try {
                val url = URL(params[0])
                connection = url.openConnection() as? HttpURLConnection
                connection?.connect()


                val stream = connection?.getInputStream()

                if (stream != null)
                    reader = BufferedReader(InputStreamReader(stream))

                val buffer = StringBuffer()
                var line = reader?.readLine()
                while (line != null) {
                    buffer.append(line + "\n")
                    Log.d("Response: ", "> $line")   //here u ll get whole response...... :-)
                    line = reader?.readLine()
                }

                return buffer.toString()


            } catch (e: MalformedURLException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                if (connection != null) {
                    connection.disconnect()
                }
                try {
                    if (reader != null) {
                        reader.close()
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
            return null
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            val oldAlertString = PreferencesHelper.shared.getStringValue("inappalert")
            var oldAlert: InappAlert? = null
            if (oldAlertString != null) {
                try {
                    oldAlert = Gson().fromJson(oldAlertString, InappAlert::class.java)
                } catch (e: Exception) {

                }
            }
            var newAlert: InappAlert? = null
            try {


                if (result != null)
                    try {
                        PreferencesHelper.shared.putValue("inappalert", result)
                        try {
                            newAlert = Gson().fromJson(result, InappAlert::class.java)
                        } catch (e: Exception) {

                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                var oldCountShow = PreferencesHelper.shared.getIntValue("numberShowAlert") ?: 0
                var typeShow = -1
                if (oldAlert != null && newAlert == null) {
                    if (oldAlert.show_limit?.toInt() == -1 || oldCountShow < oldAlert.show_limit?.toInt() ?: 0) {
                        typeShow = 1
                    }
                }
                if (newAlert != null && newAlert.version != oldAlert?.version) {
//                PreferencesHelper.shared.putValue("numberShowAlert", 1)
                    oldCountShow = 0
                    typeShow = 2
                } else if (newAlert?.version == oldAlert?.version) {
                    if (oldAlert?.show_limit?.toInt() == -1 || oldCountShow < oldAlert?.show_limit?.toInt() ?: 0) {
                        typeShow = 1
                    }
                }

                if (typeShow == 1 && oldAlert != null) {
                    showAlert(this@MainActivity, oldAlert)
                    PreferencesHelper.shared.putValue("numberShowAlert", oldCountShow + 1)
                } else if (typeShow == 2 && newAlert != null) {
                    showAlert(this@MainActivity, newAlert)
                    PreferencesHelper.shared.putValue("numberShowAlert", oldCountShow + 1)
                }
            } catch (e: Exception) {
            }
        }
    }

    override fun onPurchases(purchases: List<Purchase>?) {
        super.onPurchases(purchases)
        if (!isPlan03Used) {
            val adRequest = AdRequest.Builder().addTestDevice("F7C2DD6ABF615BEECC0532B3657BE9E7")
                    .addTestDevice("ED3729FBE732A334E24AD4911CC2006C").build()
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
