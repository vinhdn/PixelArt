package vn.zenity.football.fragment

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.RectF
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.res.ResourcesCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.widget.DrawableUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.load.resource.bitmap.BitmapResource
import vn.zenity.football.R
import vn.zenity.football.extensions.saveImage
import vn.zenity.football.extensions.share
import vn.zenity.football.extensions.showLoading
import kotlinx.android.synthetic.main.fragment_show_result_drawing.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import vn.vietsens.elife.erpstore.manager.local.PreferencesHelper
import vn.zenity.football.base.BaseActivity
import vn.zenity.football.extensions.toast

/**
 * Created by vinhdn on 28-Feb-18.
 */
class ShowResultDrawingFragment : Fragment() {

    companion object {
        fun getInstance(colorBitmap: Bitmap, grayBitmap: Bitmap, pixSize: Float = 10f, isFinished: Boolean = false): ShowResultDrawingFragment {
            val fragment = ShowResultDrawingFragment()
            fragment.colorBitmap = colorBitmap
            fragment.grayBitmap = grayBitmap
            fragment.pixSize = pixSize
            fragment.isFinished = isFinished
            return fragment
        }
    }

    private lateinit var colorBitmap: Bitmap
    private lateinit var grayBitmap: Bitmap
    private var fullColorBitmap: Bitmap? = null
    private var pixSize: Float = 10f
    private var isFinished: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_show_result_drawing, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (isFinished) {
            underlay.visibility = View.GONE
            switch_button.visibility = View.GONE
            switch_button.isChecked = false
        }
        back.setOnClickListener {
            activity?.onBackPressed()
        }
        home.setOnClickListener {
            activity?.finish()
        }
        switch_button.setOnCheckedChangeListener { _, isChecked ->
            showImage(isChecked)
        }
        btnDownload.setOnClickListener {
            saveImage()
        }

        btnShare.setOnClickListener {
            saveImage(true)
        }
        showImage(!isFinished)
    }

    private fun saveImage(isShare: Boolean = false) {
        activity?.let {
            val dialog = showLoading(it)
            dialog.show()
            doAsync {
                if (switch_button.isChecked) {
                    if (fullColorBitmap == null) {
                        createFullColorBitmap()
                    }
                    if (isShare) {
                        fullColorBitmap?.share(this@ShowResultDrawingFragment.context
                                ?: return@doAsync)
                    } else
                        fullColorBitmap?.saveImage({
                            if (it != null) {
                                toast("Saved successfully")
                            } else {
                                toast("Can not save this drawing")
                            }
                            if (PreferencesHelper.shared.getBooleanValue("isShowRate", true) && !PreferencesHelper.shared.getBooleanValue("isRated", false)) {
                                (activity as? BaseActivity)?.showRateApp()
                                PreferencesHelper.shared.putValue("isRated", PreferencesHelper.shared.getBooleanValue("isRated", false))
                                return@saveImage
                            }
                        })
                } else {
                    if (isShare) {
                        Bitmap.createScaledBitmap(colorBitmap, (colorBitmap.width * pixSize).toInt(), (colorBitmap.height * pixSize).toInt(), false).share(this@ShowResultDrawingFragment.context
                                ?: return@doAsync)
                    } else
                        Bitmap.createScaledBitmap(colorBitmap, (colorBitmap.width * pixSize).toInt(), (colorBitmap.height * pixSize).toInt(), false).saveImage()
                }
                uiThread {
                    dialog.dismiss()
                }
            }
        }
    }

    private fun showImage(isWithGray: Boolean) {
        doAsync {
            if (isWithGray) {
                if (fullColorBitmap == null) {
                    createFullColorBitmap()
                }
                image.setImageBitmap(fullColorBitmap)
            } else {
                image.setImageBitmap(Bitmap.createScaledBitmap(colorBitmap, (colorBitmap.width * pixSize).toInt(), (colorBitmap.height * pixSize).toInt(), false))
            }
        }
    }

    private fun createFullColorBitmap() {
        val fullBitmap = Bitmap.createBitmap((colorBitmap.width * pixSize).toInt(), (colorBitmap.height * pixSize).toInt(), Bitmap.Config.ARGB_8888)
        val c = Canvas(fullBitmap)
        c.drawBitmap(grayBitmap, null, RectF(0f, 0f, fullBitmap.width.toFloat(), fullBitmap.height.toFloat()), null)
        c.drawBitmap(colorBitmap, null, RectF(0f, 0f, fullBitmap.width.toFloat(), fullBitmap.height.toFloat()), null)
        context?.let {
            val bitmapTag = BitmapFactory.decodeResource(it.resources, R.drawable.tag2)
            if (bitmapTag != null && bitmapTag.width > 0) {
                val scaled = (fullBitmap.width.toFloat() * 0.4f) / bitmapTag.width.toFloat()
                val scaleTag = Bitmap.createScaledBitmap(bitmapTag, (bitmapTag.width * scaled).toInt(), (bitmapTag.height * scaled).toInt(), false)
                c.drawBitmap(scaleTag, (fullBitmap.width - scaleTag.width).toFloat(), (fullBitmap.height - scaleTag.height).toFloat(), null)
            }
        }
//        fullColorBitmap = (Bitmap.createScaledBitmap(fullBitmap, (fullBitmap.width * pixSize).toInt(), (fullBitmap.height * pixSize).toInt(), false))
        fullColorBitmap = fullBitmap
    }
}