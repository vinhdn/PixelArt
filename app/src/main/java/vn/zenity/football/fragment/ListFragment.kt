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
import kotlinx.android.synthetic.main.fragment_list.*
import kotlinx.android.synthetic.main.item_pic.view.*
import vn.zenity.football.extensions.*

/**
 * Created by vinhdn on 28-Feb-18.
 */
class ListFragment : Fragment() {

    companion object {
        fun getInstance(type: Int): ListFragment {
            val fragment = ListFragment()
            fragment.type = type
            return fragment
        }

        fun getInstance(category: Category?): ListFragment {
            val fragment = ListFragment()
            fragment.category = category
            return fragment
        }
    }

    var type: Int = 0
    var category: Category? = null
    var listSaved: List<ImagePixel>? = null
    var adapter: Adapter? = null
    private lateinit var viewModel: ImageViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recycler_view.layoutManager = GridLayoutManager(context, 2)
        recycler_view.setHasFixedSize(true)

        viewModel = ViewModelProviders.of(activity!!).get(ImageViewModel::class.java)
        recycler_view.post {
            adapter = Adapter(recycler_view.width)
            recycler_view.adapter = adapter
            if (category != null) {
                viewModel.getCates().observe(activity as BaseActivity, Observer { cates ->
                    cates?.let {
                        for (cate in it) {
                            if (cate.name == category?.name) {
                                category = cate
                                activity?.runOnUiThread {
                                    adapter?.notifyDataSetChanged()
                                }
                                return@let
                            }
                        }
                    }
                })
            } else {
                viewModel.getWorking().observe(activity as BaseActivity, Observer {
                    listSaved = it
                    activity?.runOnUiThread {
                        adapter?.notifyDataSetChanged()
                    }
                })
            }

            viewModel.getDrawing().observe(activity as BaseActivity, Observer { image ->
                if (image == null) return@Observer
                category?.images?.forEachIndexed { index, imagePixel ->
                    if (imagePixel.path == image?.path) {
                        category?.images?.set(index, image)
                        activity?.runOnUiThread {
                            adapter?.notifyItemChanged(index)
                        }
                        return@Observer
                    }
                }

                listSaved?.forEachIndexed { index, imagePixel ->
                    if (imagePixel.path == image?.path) {
                        activity?.runOnUiThread {
                            adapter?.notifyItemChanged(index)
                        }
                        return@Observer
                    }
                }

            })
        }

    }

    inner class Adapter(private var width: Int = 200) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.item_pic, parent, false)
            return Holder(view)
        }

        override fun getItemCount(): Int {
            if (category != null) {
//                return App.get().data?.get(category!!)?.size ?: 0
                return category?.images?.size ?: 0
            } else {
                return listSaved?.size ?: 0
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val lp = (holder.itemView.cardView?.layoutParams as? GridLayoutManager.LayoutParams)
            lp?.height = (if (width > 100) width else 100) / 2
            holder.itemView.cardView.layoutParams = lp
            holder.itemView.img.setImageResource(0)

            var image = listSaved?.get(position)
            if (category != null) {
                image = category?.images?.get(position)
            }
            if (image != null) {
                image.path.let {
                    holder.itemView.img.loadAsset(it, oldBitmap = image.bitmap) { imp ->
                        image.bitmap = imp.bitmap
                        image.savedData = imp.savedData
                        image.completeCoin = imp.completeCoin
                        image.lastDraw = imp.lastDraw
                        image.isUnlocked = imp.isUnlocked
                        image.unlockCoin = imp.unlockCoin
                        category?.images?.set(position, imp)
                        if (image.bitmap != null)
                            notifyItemChanged(position)
                    }
                }
            } else {
                holder.itemView.img.loadAsset("00005_false_0_Fashion.png")
            }
            holder.itemView.setOnClickListener {
                viewModel.getDrawing().postValue(image)
                if(image?.unlockCoin ?: 0 > 0 && image?.isUnlocked == true == false) {
                    showFreeUnlock(image, position)
                } else {
                    showFreeImage(image, position)
                }
            }
            if (image?.unlockCoin ?: 0 > 0 && image?.isUnlocked == true == false) {
                holder.itemView.coin.visibility = View.VISIBLE
            } else {
                holder.itemView.coin.visibility = View.GONE
            }

            if (image?.unlockCoin ?: 0 > 0 || listSaved != null) {
                holder.itemView.free.visibility = View.GONE
            } else {
                holder.itemView.free.visibility = View.VISIBLE
            }

            holder.itemView.new_tag.visibility = View.GONE
        }

        inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView)
    }

    @SuppressLint("SetTextI18n")
    private fun showFreeImage(image: ImagePixel?, position: Int) {
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.dialog_pic_unlocked)
        dialog.img.loadAsset(image?.path ?: "", oldBitmap = image?.bitmap, isForceUpdateOldBitmap = true) { imp ->
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
        dialog.rewardCoin.text = "Task Reward: +${image?.completeCoin ?: 0} CUP"
        if (image?.savedData != null) {
            dialog.pic_delete.visibility = View.VISIBLE
            dialog.pic_delete.setOnClickListener {
                dialog.dismiss()
                image.savedData = null
                image.bitmap = null
                PixelDB.getInstance().imageDao().insert(image)
                if (listSaved != null) {
                    listSaved = listSaved?.filterIndexed { index, _ -> position != index }
                    adapter?.notifyItemRemoved(position)
                } else if (category != null) {
                    category?.images?.set(position, image)
                    adapter?.notifyItemChanged(position)
                }
            }
        } else {
            dialog.pic_delete.visibility = View.GONE
        }

        dialog.pic_share.setOnClickListener {
            image?.path?.loadBitmapToShare {
                dialog.dismiss()
                it?.share(this@ListFragment.context ?: return@loadBitmapToShare)
            }
        }


        dialog.positive.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(App.get(), PixelActivity::class.java)
            intent.putExtra("position", position)
            intent.putExtra("path", Gson().toJson(image))
            startActivityForResult(intent, 10)
            (activity as BaseActivity).showInterstitalAds()
        }
        dialog.show()
    }

    private fun showFreeUnlock(image: ImagePixel?, position: Int) {
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
                    if (category != null) {
                        category?.images?.set(position, it)
//                        adapter?.notifyItemChanged(position)
                    }
                }
                viewModel.getDrawing().value = image
                val intent = Intent(App.get(), PixelActivity::class.java)
                intent.putExtra("position", position)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (Config.isShowInsterAdsIfSwitchScreen && (activity as? BaseActivity)?.isInterstitialLoaded() == true) {
                (activity as? BaseActivity)?.showInterstitalAds()
            }
            data?.extras?.getInt("position")?.let {
                viewModel.getCurrentPositionDrawing().value = (it)
                if (it < 0 || it > adapter?.itemCount ?: -1) {
                }else {
                    var image = listSaved?.get(it)
                    if (category != null) {
                        image = category?.images?.get(it)
                    }
                    image?.bitmap = null
                    if (category != null) {
                        image ?: return
                        category?.images?.set(it, image)
                    }
                    activity?.runOnUiThread {
                        adapter?.notifyItemChanged(it)
                    }
                    viewModel.getDrawing().value = image
                }
            }
        }
    }
}