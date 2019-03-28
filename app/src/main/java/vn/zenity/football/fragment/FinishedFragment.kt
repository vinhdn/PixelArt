package vn.zenity.football.fragment

import android.app.Dialog
import android.arch.lifecycle.Observer
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import vn.zenity.football.R
import vn.zenity.football.ShowFinishedActivity
import vn.zenity.football.app.App
import vn.zenity.football.base.BaseActivity
import vn.zenity.football.extensions.loadAsset
import vn.zenity.football.extensions.loadBitmapToShare
import vn.zenity.football.extensions.share
import vn.zenity.football.manager.PixelDB
import vn.zenity.football.models.FinishedImage
import vn.zenity.football.models.ImageViewModel
import kotlinx.android.synthetic.main.dialog_pic_unlocked.*
import kotlinx.android.synthetic.main.fragment_list.*
import kotlinx.android.synthetic.main.item_pic.view.*

/**
 * Created by vinhdn on 28-Feb-18.
 */
class FinishedFragment : Fragment() {

    companion object {
        fun getInstance(): FinishedFragment {
            val fragment = FinishedFragment()
            return fragment
        }
    }

    var listFinished: List<FinishedImage>? = null
    var adapter : Adapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recycler_view.layoutManager = GridLayoutManager(context, 2)
        recycler_view.setHasFixedSize(true)
        listFinished = ImageViewModel(App.get()).getFinishing().value
        recycler_view.post {
            adapter = Adapter(recycler_view.width)
            recycler_view.adapter = adapter
            ImageViewModel(App.get()).getFinishing().observe(activity as BaseActivity, Observer {
                listFinished = it
                activity?.runOnUiThread {
                    adapter?.notifyDataSetChanged()
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
            return listFinished?.size ?: 0
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val lp = (holder.itemView.cardView?.layoutParams as? GridLayoutManager.LayoutParams)
            lp?.height = (if (width > 100) width else 100) / 2
            holder.itemView.cardView.layoutParams = lp
            holder.itemView.img.setImageResource(0)

            var image = listFinished?.get(position)
            if (image != null) {
                image.path?.let {
                    holder.itemView.img.loadAsset(it, oldBitmap = image.bitmap) { imp ->
                        image.bitmap = imp.bitmap
                        if (image.bitmap != null)
                            notifyItemChanged(position)
                    }
                }
            } else {
                holder.itemView.img.loadAsset("00005_false_0_Fashion.png")
            }
            holder.itemView.setOnClickListener {
//                showFreeImage(image, position)
                image?.path?.let {
                    openActivityFinished(it)
                }
            }
            holder.itemView.coin.visibility = View.GONE
            holder.itemView.free.visibility = View.GONE

            holder.itemView.new_tag.visibility = View.GONE
        }

        inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView)
    }

    private fun openActivityFinished(path: String) {
        val intent = Intent(activity, ShowFinishedActivity::class.java)
        intent.putExtra("path", path)
        startActivity(intent)
    }

    private fun showFreeImage(image: FinishedImage?, position: Int) {
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.dialog_pic_unlocked)
        dialog.img.loadAsset(image?.path
                ?: "", oldBitmap = image?.bitmap)
        dialog.cancel.setOnClickListener {
            dialog.dismiss()
        }
        dialog.positive.visibility = View.GONE
        dialog.pic_share.setOnClickListener {
            image?.path?.loadBitmapToShare {
                dialog.dismiss()
                it?.share(this@FinishedFragment.context ?: return@loadBitmapToShare)
            }
        }

        dialog.pic_delete.setOnClickListener {
            dialog.dismiss()
            image?.let {
                PixelDB.getInstance().imageDao().deleteFinishedImage(it)
            }
            listFinished = listFinished?.filterIndexed { index, _ -> position != index }
            adapter?.notifyItemRemoved(position)
        }
        dialog.show()
    }
}