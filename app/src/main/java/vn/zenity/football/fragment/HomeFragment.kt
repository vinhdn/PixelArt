package vn.zenity.football.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import com.google.gson.Gson
import vn.zenity.football.PixelActivity
import vn.zenity.football.R
import vn.zenity.football.app.App
import vn.zenity.football.base.BaseActivity
import vn.zenity.football.extensions.*
import vn.zenity.football.manager.PixelDB
import vn.zenity.football.models.Category
import vn.zenity.football.models.ImagePixel
import vn.zenity.football.models.ImageViewModel
import kotlinx.android.synthetic.main.dialog_pic_locked.positive as lockPositive
import kotlinx.android.synthetic.main.dialog_pic_locked.coins as lockCoins
import kotlinx.android.synthetic.main.dialog_pic_locked.cancel as lockCancel
import kotlinx.android.synthetic.main.dialog_pic_locked.img as lockImg
import kotlinx.android.synthetic.main.dialog_pic_locked.msg as lockMsg
import kotlinx.android.synthetic.main.dialog_pic_locked.price as lockPrice
import kotlinx.android.synthetic.main.dialog_pic_unlocked.*
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.item_header.view.*
import kotlinx.android.synthetic.main.item_pic.view.*
import vn.zenity.football.extensions.*

/**
 * Created by vinhdn on 28-Feb-18.
 */
class HomeFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    private val adsListener: (BaseActivity.TypeStateAds) -> Unit = {
        when (it) {
            BaseActivity.TypeStateAds.RewardAdLoaded, BaseActivity.TypeStateAds.AdLoaded -> {
                watchAds?.visibility = View.VISIBLE
            }
            else -> {
                watchAds?.visibility = View.GONE
            }
        }
    }

    private var adapter: CateAdapter? = null
    private lateinit var viewModel: ImageViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProviders.of(activity!!).get(ImageViewModel::class.java)
        watchAds.setOnClickListener {
            (activity as? BaseActivity)?.openRewardVideoAds()
        }
        if ((activity as? BaseActivity)?.isRewardLoaded() == true || (activity as? BaseActivity)?.isInterstitialLoaded() == true) {
            watchAds?.visibility = View.VISIBLE
        } else {
            watchAds?.visibility = View.GONE
        }
        (activity as? BaseActivity)?.listAdsListener?.add(adsListener)

        recyclerView.post {
            processData()
            recyclerView.layoutManager = LinearLayoutManager(context)
            adapter = CateAdapter((recyclerView.width * 4f / 5f).toInt())
            recyclerView.adapter = adapter
            viewModel.getCates().observe(activity as BaseActivity, Observer { cates ->
                cates?.let {

                    //                    for ((i,cate) in it.withIndex()) {
//                        if (cate.name == cates?.name) {
//                            category = cate
                    activity?.runOnUiThread {
                        adapter?.notifyDataSetChanged()
                    }
//                            return@let
//                        }
//                    }
                }
            })
            viewModel.getDrawing().observe(activity as BaseActivity, Observer { image ->
                if (image == null) return@Observer
                if(App.get().data == null) return@Observer
                for ((i,cate) in App.get().data!!.withIndex()) {
                    cate.images?.forEachIndexed { index, imagePixel ->
                        if (imagePixel.path == image.path) {
                            cate.images?.set(index, image)
                            adapter?.notifyItemChanged(i)
                            return@Observer
                        }
                    }
                }
            })
        }
    }

    var size = 0
    var maxSize = 0
    private var mapCateStart: HashMap<Int, Pair<Int, Int>> = hashMapOf()
    private fun processData() {
        var p = 0
        size = 0
        mapCateStart = hashMapOf()
        for (cate in App.get().data ?: return) {
            cate.prvSize = size
            mapCateStart[p] = Pair(size, cate.images?.size ?: 0)
            if (cate.images?.size ?: 0 > maxSize) {
                maxSize = cate.images?.size ?: 0
            }
            size += (cate.images?.size ?: 0) + 1
            p += 1
        }
        adapter?.notifyDataSetChanged()
    }

    inner class CateAdapter(private var width: Int = 200) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.item_header, parent, false)
            return Holder(view)
        }

        override fun getItemCount(): Int {
            return App.get().data?.size ?: 0
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            holder.itemView.titleTv.text = App.get().data?.get(position)?.name ?: ""
            holder.itemView.itemRecyclerView.layoutManager = GridLayoutManager(context, 2, GridLayoutManager.HORIZONTAL, false)
            holder.itemView.itemRecyclerView.adapter = Adapter(width, position)
        }

    }

    inner class Adapter(private var width: Int = 200, private var pos: Int) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.item_pic, parent, false)
            return Holder(view)
        }

        override fun getItemCount(): Int {
            return App.get().data?.get(pos)?.images?.size ?: 0
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val lp = (holder.itemView.cardView?.layoutParams as? GridLayoutManager.LayoutParams)
            if (width > 400f * (context?.resources?.displayMetrics?.density ?: 1f)) {
                val xWidth = (400f * (context?.resources?.displayMetrics?.density ?: 1f)).toInt()
                if (Math.abs(width - xWidth - 200f * (context?.resources?.displayMetrics?.density ?: 1f).toInt()) < 20f * (context?.resources?.displayMetrics?.density ?: 1f).toInt()) {
                    width = (350f * (context?.resources?.displayMetrics?.density ?: 1f)).toInt()
                }
            }
            lp?.height = (if (width > 100) width else 100) / 2
            lp?.width = (if (width > 100) width else 100) / 2
            holder.itemView.cardView.layoutParams = lp
            holder.itemView.img.setImageResource(0)
            val image = App.get().data?.get(pos)?.images?.get(position)
            if (image != null) {
                image.path.let {
                    holder.itemView.img.loadAsset(it, oldBitmap = image.bitmap) { imp ->
                        image.bitmap = imp.bitmap
                        image.savedData = imp.savedData
                        image.completeCoin = imp.completeCoin
                        image.lastDraw = imp.lastDraw
                        image.isUnlocked = imp.isUnlocked
                        image.unlockCoin = imp.unlockCoin
                        App.get().data?.get(pos)?.images?.set(position, imp)
                        if (image.bitmap != null)
                            notifyItemChanged(position)
                    }
                }
            } else {
                holder.itemView.img.loadAsset("00005_false_0_Fashion.png")
            }
            holder.itemView.setOnClickListener {
                //                viewModel.getDrawing().postValue(image)
                if (image?.unlockCoin ?: 0 > 0 && image?.isUnlocked == true == false) {
                    showFreeUnlock(image, position, pos)
                } else {
                    showFreeImage(image, position, pos)
                }
            }
            if (image?.unlockCoin ?: 0 > 0 && image?.isUnlocked == true == false) {
                holder.itemView.coin.visibility = View.VISIBLE
            } else {
                holder.itemView.coin.visibility = View.GONE
            }

            if (image?.unlockCoin ?: 0 > 0) {
                holder.itemView.free.visibility = View.GONE
            } else {
                holder.itemView.free.visibility = View.VISIBLE
            }

            holder.itemView.new_tag.visibility = View.GONE
        }

    }

    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView)


    @SuppressLint("SetTextI18n")
    private fun showFreeImage(image: ImagePixel?, position: Int, cateID: Int) {
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.dialog_pic_unlocked)
        dialog.img.loadAsset(image?.path
                ?: "", oldBitmap = image?.bitmap, isForceUpdateOldBitmap = true) { imp ->
            image?.bitmap = imp.bitmap
            image?.bitmap?.let {
                dialog.img.loadAsset(image.path, oldBitmap = it)
                viewModel.getDrawing().value = image
//                adapter?.notifyItemChanged(position)
            }
            image?.savedData = imp.savedData
            image?.completeCoin = imp.completeCoin
            image?.lastDraw = imp.lastDraw
        }
        dialog.cancel.setOnClickListener {
            dialog.dismiss()
        }
        dialog.rewardCoin.text = "Task Reward: +${image?.completeCoin ?: 0} diamond"
        if (image?.savedData != null) {
            dialog.pic_delete.visibility = View.VISIBLE
            dialog.pic_delete.setOnClickListener {
                dialog.dismiss()
                image.savedData = null
                image.bitmap = null
                PixelDB.getInstance().imageDao().insert(image)
//                if (listSaved != null) {
//                    listSaved = listSaved?.filterIndexed { index, _ -> position != index }
//                    adapter?.notifyItemRemoved(position)
//                } else if (category != null) {
                App.get().data?.get(cateID)?.images?.set(position, image)
//                    adapter?.notifyItemChanged(position)
//                }
            }
        } else {
            dialog.pic_delete.visibility = View.GONE
        }

        dialog.pic_share.setOnClickListener {
            image?.path?.loadBitmapToShare {
                dialog.dismiss()
                it?.share(this@HomeFragment.context ?: return@loadBitmapToShare)
            }
        }


        dialog.positive.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(App.get(), PixelActivity::class.java)
            intent.putExtra("position", position)
            intent.putExtra("cateId", cateID)
            intent.putExtra("path", Gson().toJson(image))
            startActivityForResult(intent, 10)
            (activity as BaseActivity).showInterstitalAds()
        }
        dialog.show()
    }

    private fun showFreeUnlock(image: ImagePixel?, position: Int, cateID: Int) {
        val dialogUnlock = showDialog(activity!!, R.layout.dialog_pic_locked) { dialog ->
            val coins = (activity as? BaseActivity)?.getCoins() ?: 0
            dialog.lockCoins.text = "$coins"
            dialog.lockImg.loadAsset(image?.path
                    ?: "", oldBitmap = image?.bitmap, isForceUpdateOldBitmap = true) { imp ->
                image?.bitmap = imp.bitmap
                image?.bitmap?.let {
                    dialog.img.loadAsset(image.path, oldBitmap = it)
                    viewModel.getDrawing().value = image
//                    adapter?.notifyItemChanged(position)
                }
                image?.savedData = imp.savedData
                image?.completeCoin = imp.completeCoin
                image?.lastDraw = imp.lastDraw
            }
            dialog.lockPositive.setOnClickListener {
                dialog.dismiss()
                if (coins < image?.unlockCoin ?: 0) {
                    toast("Your CUP is not enough to unlock this image")
                    return@setOnClickListener
                }
                (activity as? BaseActivity)?.setCoins(coins - (image?.unlockCoin
                        ?: 0))
                image?.isUnlocked = true
                image?.let {
                    PixelDB.getInstance().imageDao().insert(it)
//                    if (listSaved != null) {
//                        adapter?.notifyItemChanged(position)
//                    }
//                    if (category != null) {
//                        category?.images?.set(position, it)
//                        adapter?.notifyItemChanged(position)
//                    }
                    App.get().data?.get(cateID)?.images?.set(position, image)
                    adapter?.notifyItemChanged(cateID)
                }
                viewModel.getDrawing().value = image
                val intent = Intent(App.get(), PixelActivity::class.java)
                intent.putExtra("position", position)
                intent.putExtra("cateId", cateID)
                intent.putExtra("path", Gson().toJson(image))
                startActivityForResult(intent, 10)
                (activity as BaseActivity).showInterstitalAds()
            }
            dialog.lockMsg.text = "Unlock with ${image?.unlockCoin ?: 0} CUP"
            dialog.lockPrice.text = "${image?.unlockCoin ?: 0}"
            dialog.lockCancel.setOnClickListener {
                dialog.dismiss()
            }
        }
        dialogUnlock.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? BaseActivity)?.listAdsListener?.remove(adsListener)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (Config.isShowInsterAdsIfSwitchScreen && (activity as? BaseActivity)?.isInterstitialLoaded() == true) {
                (activity as? BaseActivity)?.showInterstitalAds()
            }
            data?.extras?.getInt("position")?.let {
                val cateId = data?.extras?.getInt("cateId") ?: -1
                viewModel.getCurrentPositionDrawing().value = (it)
                if (cateId < 0 || cateId > adapter?.itemCount ?: -1) {
                }else {
                    if (it < 0 || it >= (App.get().data?.get(cateId)?.images?.size ?: -1)) {
                        return
                    }
                    val image = App.get().data?.get(cateId)?.images?.get(it)
                    image ?: return
                    image.bitmap = null
                    App.get().data?.get(cateId)?.images?.set(it, image)
                    activity?.runOnUiThread {
                        adapter?.notifyItemChanged(cateId)
                    }
                    viewModel.getDrawing().value = image
                    viewModel.getWorking().value = ((PixelDB.getInstance().imageDao().getAllOffline()))
                    viewModel.getFinishing().value = ((PixelDB.getInstance().imageDao().getFinished()))
                }
            }
        }
    }
}