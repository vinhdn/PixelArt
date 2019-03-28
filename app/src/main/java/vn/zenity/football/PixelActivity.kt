package vn.zenity.football

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.support.v7.widget.*
import android.util.Log
import android.view.*
import com.android.billingclient.api.Purchase
import com.google.android.gms.ads.*
import com.google.android.gms.ads.reward.RewardItem
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.Gson
import vn.zenity.football.R
import vn.zenity.football.app.App
import vn.zenity.football.base.BaseActivity
import vn.zenity.football.extensions.*
import vn.zenity.football.fragment.ShowResultDrawingFragment
import vn.zenity.football.manager.PixelDB
import vn.zenity.football.models.FinishedImage
import vn.zenity.football.models.ImageColor
import vn.zenity.football.models.ImagePixel
import vn.zenity.football.models.ImageViewModel
import vn.zenity.football.widget.ColorPicker
import vn.zenity.football.widget.PixelView
import vn.zenity.football.widget.SatValView
import kotlinx.android.synthetic.main.activity_pixel.*
import kotlinx.android.synthetic.main.dialog_color_drawing_percent.*
import kotlinx.android.synthetic.main.item_color.*
import kotlinx.android.synthetic.main.item_color.view.*
import org.jetbrains.anko.activityUiThread
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import vn.vietsens.elife.erpstore.manager.local.PreferencesHelper
import vn.zenity.football.extensions.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.ref.WeakReference
import kotlin.collections.ArrayList

class PixelActivity : BaseActivity(), vn.zenity.football.widget.PixelView.OnDropperCallBack, vn.zenity.football.widget.PixelView.OnDrawChangeBox {
    override fun onChanged() {
        runOnUiThread {
            adapter?.notifyDataSetChanged()
        }
    }

    companion object {
        val UNTITLED = "Untitled"
        var currentProjectPath: String? = null
        var coinFillColor = 100
    }

    private lateinit var cp: vn.zenity.football.widget.ColorPicker

    private var onlyShowSelected: Boolean = false
    private var image: ImagePixel? = null

    private lateinit var previousMode: vn.zenity.football.widget.PixelView.Mode
    private var adapter: ColorAdapter? = null
    private var listColors = arrayListOf<ImageColor?>()
    private var isFinished = false
    private var position = -1
    private var cateId = -1
    private var isFirstShowInterstitialAd = true

    private val adsListener: (BaseActivity.TypeStateAds) -> Unit = { type ->
        when (type) {
            BaseActivity.TypeStateAds.RewardAdLoaded, BaseActivity.TypeStateAds.AdLoaded -> {
                weakSelf.get()?.watchAds?.visibility = View.VISIBLE
                if (!isFirstShowInterstitialAd && type == BaseActivity.TypeStateAds.AdLoaded) {
                    isFirstShowInterstitialAd = true
                    weakSelf.get()?.showInterstitalAds()
                }
            }
            else -> {
                weakSelf.get()?.watchAds?.visibility = View.GONE
            }
        }
    }

    private lateinit var weakSelf: WeakReference<BaseActivity>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pixel)
        weakSelf = WeakReference(this)
        listAdsListener.add(adsListener)
        progressView.visibility = View.VISIBLE
        btBack.setOnClickListener { finish() }
        btDone.setOnClickListener {
            pxerView.createResultDraw { cBm, grayBitmap, pixSize ->
                //                showDialogFinishDrawing(this, it, pxerView.imagePixel)
                showFinishDrawing(cBm, grayBitmap, pixSize)
            }
        }
        fab_help.setOnClickListener {
            val intent = Intent(this, TutorialActivity::class.java)
            startActivity(intent)
        }

        btnHandMove.setOnClickListener {
            pxerView.typeTouch = !pxerView.typeTouch
            if (pxerView.typeTouch) {
                btnHandMove.setImageResource(R.drawable.ic_hand_move)
            } else {
                btnHandMove.setImageResource(R.drawable.ic_hand_move_blue)
            }
        }

        if (Config.isShowAdsInDrawScreen) {
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
        }

        if (isRewardLoaded() || isInterstitialLoaded()) {
            watchAds?.visibility = View.VISIBLE
        } else {
            watchAds?.visibility = View.GONE
        }

        val pxerPref = getSharedPreferences("pxerPref", Context.MODE_PRIVATE)
        pxerView.selectedColor = pxerPref.getInt("lastUsedColor", Color.YELLOW)
        pxerView.setDropperCallBack(this)

        pxerView.setFinishedDrawing {
            pxerView.createResultDraw { it, grayBitmap, pixSize ->
                showFinishDrawing(it, grayBitmap, pixSize, true)
//                showDialogFinishDrawing(this@PixelActivity, it, pxerView.imagePixel)
                image?.let {
                    val coinIncrease = it.completeCoin ?: 0
                    if (coinIncrease > 0) {
                        var coin = getCoins()
                        coin += coinIncrease
                        setCoins(coin)
                        toast("Reward: +$coinIncrease CUP")
                    }
                    //Bỏ kiểm tra nếu nhận thưởng rồi sẽ không được nhận thưởng những lần tiếp theo nữa
//                    it.completeCoin = 0
                    it.savedData = pxerView.dataColorDrawing
                    it.lastDraw = System.currentTimeMillis()
                    val _bitmap = pxerView.bitmapDrawing
                    _bitmap?.let { bm ->
                        val stream = ByteArrayOutputStream()
                        bm.compress(Bitmap.CompressFormat.PNG, 0, stream)
                        it.bitmap = stream.toByteArray()
                    }
                    val finishedImage = FinishedImage(it)
                    PixelDB.getInstance().imageDao().insertFinished(finishedImage)
                    it.savedData = null
                    it.bitmap = null
                    it.lastDraw = null
                    PixelDB.getInstance().imageDao().insert(it)
                    ImageViewModel(App.get()).getFinishing().value = (PixelDB.getInstance().imageDao().getFinished())
                    isFinished = true
                }
            }
            val bundle = Bundle()
            bundle.putString("FinishDraw", image?.path)
            FirebaseAnalytics.getInstance(this).logEvent("countryVPN", bundle)
        }

        setUpLayersView()
        setupControl()
        recycler_view.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

//        currentProjectPath = pxerPref.getString("lastOpenedProject", null)
//        if (!currentProjectPath.isNullOrEmpty()) {
//            val file = File(currentProjectPath!!)
//            if (file.exists()) {
//                pxerView.loadProject(file)
//                setTitle(Tool.stripExtension(file.name), false)
//            }
//        } else {
        val drawable = intent?.extras?.getInt("drawable") ?: R.drawable.im56
        position = intent?.extras?.getInt("position", -1) ?: -1
        cateId = intent?.extras?.getInt("cateId", -1) ?: -1
        val path = Gson().fromJson(intent?.extras?.getString("path") ?: "", ImagePixel::class.java)
        if (path != null) {
            image = path
            Log.d("time00", "" + System.currentTimeMillis())
            pxerView.createBlankProject("", 130, 130, path)
            doAsync {
                image?.path?.loadBitmapAsset(true, 1f) { bitmap ->
                    if (bitmap == null) return@loadBitmapAsset
                    val mapNumber = Array(bitmap.width) { IntArray(bitmap.height) }
                    Log.d("time01", "" + System.currentTimeMillis())
                    var countColor = 0
                    val hashmap: HashMap<Int, ImageColor?> = hashMapOf()
                    for (i in 0 until bitmap.width) {
                        for (j in 0 until bitmap.height) {
                            val pixel = bitmap.getPixel(i, j)
                            if (pixel != Color.WHITE && pixel < 0) {
                                if (hashmap[pixel] == null) {
                                    hashmap[pixel] = ImageColor(countColor, pixel, 1, 0)
                                    countColor++
                                    mapNumber[i][j] = countColor
                                } else {
                                    mapNumber[i][j] = (hashmap[pixel]?.id ?: -1) + 1
                                    hashmap[pixel]?.count = (hashmap[pixel]?.count ?: -1) + 1
                                }
                            } else {
                                mapNumber[i][j] = 0
                            }

                        }
                    }
                    val listPairColor = hashmap.values.sortedBy { it?.id }
                    listColors = ArrayList(listPairColor)
                    Log.d("time02", "" + System.currentTimeMillis())

                    activityUiThread {
                        Log.d("time03", "" + System.currentTimeMillis())
                        pxerView.setMapNumber(mapNumber)
                        Log.d("time04", "" + System.currentTimeMillis())
                        pxerView.setListColors(ArrayList(listPairColor))
                        Log.d("time05", "" + System.currentTimeMillis())
                        image?.savedData?.let {
                            pxerView.setDataOffline(it)
                            Log.d("time06", "" + System.currentTimeMillis())
                        }
                        adapter = ColorAdapter(pxerView.selectedColor) { p, c ->
                            pxerView.selectedColor = c
                            fab_color.setColor(c)
                            fab_color.setNumber(p)
                        }
                        pxerView.setOnDrawChangeBox(this@PixelActivity)
                        recycler_view.adapter = adapter
                        if (listColors.size > 0) {
                            val color = listColors[0] ?: return@activityUiThread
                            pxerView.selectedColor = color.color
                            fab_color.setColor(color.color)
                            fab_color.setNumber(color.color)
                        }

                        Log.d("time07", "" + System.currentTimeMillis())
                    }
                    activityUiThread {
                        progressView.visibility = View.GONE
                    }
                }
            }
        } else
            pxerView.createBlankProject("", 130, 130, drawable)
//        }
        watchAds.setOnClickListener {
            openRewardVideoAds()
        }
        System.gc()
        if (isInterstitialLoaded()) {
            isFirstShowInterstitialAd = true
            showInterstitalAds()
        }
        val bundle = Bundle()
        bundle.putString("Image", path.path)
        FirebaseAnalytics.getInstance(this).logEvent("countryVPN", bundle)
    }

    private fun showFinishDrawing(bmC: Bitmap, bmG: Bitmap, pixSize: Float = 10f, isFinished: Boolean = false) {
        val frag = ShowResultDrawingFragment.getInstance(bmC, bmG, pixSize, isFinished)
        supportFragmentManager.beginTransaction().replace(R.id.frame, frag).addToBackStack(null).commitAllowingStateLoss()
        showInterstitalAds()
    }

    override fun onColorDropped(newColor: Int) {
        fab_color.setColor(newColor)
        cp.setColor(newColor)

        fab_dropper.callOnClick()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        super.onPostCreate(savedInstanceState)
    }


    fun onToggleToolsPanel(view: View) {
    }

    private fun setupControl() {

        fab_color.setColor(pxerView.selectedColor)
//        fab_color.colorNormal = pxerView.selectedColor
//        fab_color.colorPressed = pxerView.selectedColor
        cp = vn.zenity.football.widget.ColorPicker(this, pxerView.selectedColor, vn.zenity.football.widget.SatValView.OnColorChangeListener { newColor ->
            pxerView.selectedColor = newColor
            fab_color.setColor(newColor)
        })
        fab_color.setOnClickListener { view -> cp.show(view) }
        fab_dropper.setOnClickListener {
            //            if (pxerView.mode == PixelView.Mode.Dropper) {
//                pxerView.mode = previousMode
//                fab_dropper.setImageResource(R.drawable.ic_colorize_24dp)
//            } else {
//
//                previousMode = pxerView.mode
//                pxerView.mode = PixelView.Mode.Dropper
//                fab_dropper.setImageResource(R.drawable.ic_close_24dp)
//            }
            pxerView.selectedColor = Color.TRANSPARENT
            fab_color.setColor(Color.TRANSPARENT)
            fab_color.setNumber(0)
            fab_dropper.setColorNormalResId(R.color.colorAccent)
            val oldP = adapter?.selectedIndex ?: 0
            adapter?.selectedIndex = -1
            adapter?.notifyItemChanged(oldP)
        }
    }

    private fun setUpLayersView() {

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == 659 && data != null) {
            val path = data.getStringExtra("selectedProjectPath")
            if (path != null && !path.isEmpty()) {
                currentProjectPath = path
                val file = File(path)
                if (file.exists()) {
                    pxerView.loadProject(file)
                }
            } else if (data.getBooleanExtra("fileNameChanged", false)) {
                currentProjectPath = ""
                recreate()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }


    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        cp.onConfigChanges()
    }

    inner class ColorAdapter(var selected: Int, var delegate: (Int, Int) -> Unit) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var selectedIndex = 0

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_color, parent, false)
            return ColorHolder(view)
        }

        override fun getItemCount(): Int = listColors.size

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val color = listColors[position]
            holder.itemView.bg.setTint(color?.color ?: Color.WHITE)
            holder.itemView.text.text = "${position + 1}"
            holder.itemView.text.setTextColor(if ((color?.color
                            ?: Color.WHITE).colorIsDark()) Color.WHITE else Color.BLACK)
//            holder.itemView.color.colorNormal = color
//            holder.itemView.color.colorPressed = color
            if (position == selectedIndex) {
                holder.itemView.border.visibility = View.VISIBLE
            } else {
                holder.itemView.border.visibility = View.GONE
            }

            if (color?.count ?: 0 > 0 && color?.count == color?.correctCount) {
                holder.itemView.check.visibility = View.VISIBLE
                holder.itemView.text.visibility = View.GONE
            } else {
                holder.itemView.check.visibility = View.GONE
                holder.itemView.text.visibility = View.VISIBLE
            }

            holder.itemView.color.setOnClickListener {
                rewardFillColorId = -1
                if (selectedIndex == position) {
//                    pxerView.fillColor(position)
                    val dialg = showDialog(this@PixelActivity, R.layout.dialog_color_drawing_percent) { fillDialog ->
                        val percent = pxerView.getPercentCompleteOfColor(position)
                        if (percent >= 100f) {
                            fillDialog.btnWatchAds.visibility = View.GONE
                            fillDialog.btnUseCup.visibility = View.GONE
                            fillDialog.tvNotice.visibility = View.GONE
                        } else {
                            if (isRewardLoaded()) {
                                fillDialog.btnWatchAds.visibility = View.VISIBLE
                                fillDialog.btnWatchAds.setOnClickListener {
                                    openRewardVideoAds()
                                    rewardFillColorId = position
                                }
                            } else {
                                fillDialog.btnWatchAds.visibility = View.GONE
                            }
                            fillDialog.btnUseCup.visibility = View.VISIBLE
                            fillDialog.tvNotice.visibility = View.VISIBLE
                        }

                        fillDialog.successPercentTv.text = String.format("%.0f", pxerView.getPercentCompleteOfColor(position)) + "%"
                        fillDialog.progressBar.progress = pxerView.getPercentCompleteOfColor(position)
                        fillDialog.bg.setTint(color?.color ?: Color.WHITE)
                        fillDialog.text.text = "${position + 1}"
                        fillDialog.text.setTextColor(if ((color?.color
                                        ?: Color.WHITE).colorIsDark()) Color.WHITE else Color.BLACK)
                        fillDialog.btnUseCup.setOnClickListener {
                            val coin = PreferencesHelper.shared.getIntValue("Coins", 1000) ?: 0
                            if (coin >= coinFillColor) {
                                setCoins(coin - coinFillColor)
                                pxerView.fillColor(position)
                                fillDialog.dismiss()
                            } else {
                                showConfirm(this@PixelActivity, "Your cup is not enough", null, "Ok", {
                                    it.dismiss()
                                })
                            }
                        }
                    }
                    dialg.show()
                    return@setOnClickListener
                }
                delegate.invoke(position + 1, color?.color ?: Color.TRANSPARENT)
                val oldP = selectedIndex
                selectedIndex = position
                if (oldP > -1)
                    notifyItemChanged(oldP)
                if (selectedIndex > -1) {
                    notifyItemChanged(selectedIndex)
                    fab_dropper.setColorNormalResId(R.color.colorPrimary)
                }
            }
        }

        inner class ColorHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    }

    private var rewardFillColorId = -1
    override fun onRewarded(reward: RewardItem) {
        if (rewardFillColorId >= 0) {
            pxerView.fillColor(rewardFillColorId)
            rewardFillColorId = -1
        } else
            super.onRewarded(reward)
    }

    override fun onRewardedVideoAdClosed() {
        super.onRewardedVideoAdClosed()
        rewardFillColorId = -1
    }

    override fun finish() {
        if (position >= 0)
            ImageViewModel(App.get()).getCurrentPositionDrawing().value = (position)
        if (!isFinished) {
            image?.let {
                it.savedData = pxerView.dataColorDrawing
                it.lastDraw = System.currentTimeMillis()
                val bitmap = pxerView.bitmapDrawing
                bitmap?.let { bm ->
                    val stream = ByteArrayOutputStream()
                    bm.compress(Bitmap.CompressFormat.PNG, 0, stream)
                    it.bitmap = stream.toByteArray()
                }
                PixelDB.getInstance().imageDao().insert(it)
                ImageViewModel(App.get()).getWorking().postValue((PixelDB.getInstance().imageDao().getAllOffline()))
                ImageViewModel(App.get()).getCates().value?.let {
                    ImageViewModel(App.get()).getCates().postValue(it)
                }
            }
        }
        val intent = Intent()
        intent.putExtra("position", position)
        intent.putExtra("cateId", cateId)
        setResult(Activity.RESULT_OK, intent)
        super.finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        weakSelf.clear()
        listAdsListener.remove(adsListener)
        vn.zenity.football.extensions.Tool.freeMemory()
    }

    override fun onPurchases(purchases: List<Purchase>?) {
        super.onPurchases(purchases)
        if (!isPlan03Used) {
            if (Config.isShowAdsInDrawScreen) {
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
            }
        } else {
            adView.visibility = View.GONE
        }
    }
}