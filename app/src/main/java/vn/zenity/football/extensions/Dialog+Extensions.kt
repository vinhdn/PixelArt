package vn.zenity.football.extensions

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.support.v4.view.PagerAdapter
import android.support.v7.widget.CardView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import com.bumptech.glide.Glide
import vn.zenity.football.R
import vn.zenity.football.app.App
import vn.zenity.football.models.InappAlert
import kotlinx.android.synthetic.main.dialog_alert.*
import kotlinx.android.synthetic.main.layout_tutorial_zoom.view.*

/**
 * Created by vinh on 3/2/18.
 */

fun showDialog(context: Context, layout: Int, setupView: (Dialog) -> Unit): Dialog {
    val dialog = Dialog(context)
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
    dialog.setCancelable(true)
    dialog.setContentView(layout)
    setupView.invoke(dialog)
    return dialog
}

class HelpPager : PagerAdapter() {

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view = LayoutInflater.from(container.context).inflate(R.layout.layout_tutorial_zoom, container, false)
        view.animation_view.imageAssetsFolder = "lottie/images"
        if (position == 0) {
            view.animation_view.setAnimation("lottie/hand_zoom.json")
        } else if (position == 1) {
            view.animation_view.setAnimation("lottie/touchToFill.json")
            view.tvTitle.text = "Fill Color"
            view.tvContent.text = "Touch to Fill color"
        } else {
            view.tvTitle.text = "Fill Color"
            view.tvContent.text = "Hold and Move to Fill color"
            view.animation_view.setAnimation("lottie/HoldToFill.json")
        }
        view.animation_view.playAnimation()
        container.addView(view)
        container.addView(view)
        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as CardView)
    }

    override fun getCount(): Int = 3

}

fun showLoading(context: Context): Dialog {
    return showDialog(context, R.layout.dialog_loading) {

    }
}


fun showConfirm(context: Context, content: String, title: String? = null, rightButtonTitle: String = "Yes",
                handlerRight: ((DialogInterface) -> Unit)? = null,
                leftButtonTitle: String? = null,
                handlerLeft: ((DialogInterface) -> Unit)? = null,
                natureButton: String? = null,
                handlerNature: ((DialogInterface) -> Unit)? = null) {
    val dialog = AlertDialog.Builder(context)
    dialog.setCancelable(false)
    dialog.setPositiveButton(rightButtonTitle, null)
    title?.let {
        dialog.setTitle(title)
    }
    leftButtonTitle?.let {
        dialog.setNegativeButton(it, null)
    }
    natureButton?.let {
        dialog.setNeutralButton(it, null)
    }
    dialog.setMessage(content)
    val diaInterface = dialog.create()
    diaInterface.setOnShowListener { diai ->
        val positive = diaInterface.getButton(AlertDialog.BUTTON_POSITIVE)
        positive?.setOnClickListener {
            handlerRight?.invoke(diai)
        }

        val negative = diaInterface.getButton(AlertDialog.BUTTON_NEGATIVE)
        negative?.setOnClickListener {
            handlerLeft?.invoke(diai)
        }

        val neutral = diaInterface.getButton(AlertDialog.BUTTON_NEUTRAL)
        neutral?.setOnClickListener {
            handlerNature?.invoke(diai)
        }
    }
    diaInterface.show()
}

fun showAlert(context: Context, alert: InappAlert) {
    val dialog = showDialog(context, R.layout.dialog_alert, { da ->
        da.btnClose.setOnClickListener {
            da.dismiss()
        }
        da.btnOk.setOnClickListener {
            if (alert.url?.isNotEmpty() == true) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(alert.url))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                App.get().startActivity(intent)
            }
            da.dismiss()
        }
        da.tvContent.text = alert.message
        alert.img_banner?.let {
            da.ivImage.visibility = View.VISIBLE
            Glide.with(context).load(it).into(da.ivImage)
        }
    })
    dialog.show()
}