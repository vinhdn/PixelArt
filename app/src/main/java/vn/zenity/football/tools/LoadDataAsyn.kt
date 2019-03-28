package vn.zenity.football.tools

import android.content.res.AssetManager
import android.os.AsyncTask
import com.google.gson.Gson
import vn.zenity.football.app.App
import vn.zenity.football.extensions.Config
import vn.zenity.football.models.Category
import vn.zenity.football.models.DataCollection
import vn.zenity.football.models.ImageViewModel
import java.io.IOException
import java.nio.charset.Charset

/**
 * Created by vinh on 3/2/18.
 */

class LoadDataAsyn(private var assets: AssetManager, private var doneListener: (() -> Unit)? = null) : AsyncTask<Void, Void, ArrayList<Category>?>() {

    private fun loadJSONFromAsset(index: Int = 1): String? {
        var json: String? = null
        try {
            val `is` = assets.open("collection_$index.json")
            val size = `is`.available()
            val buffer = ByteArray(size)
            `is`.read(buffer)
            `is`.close()
            json = String(buffer, Charset.forName("UTF-8"))
        } catch (ex: IOException) {
            ex.printStackTrace()
            return null
        }
        return json
    }

    override fun doInBackground(vararg p0: Void?): ArrayList<Category>? {
        val data: ArrayList<Category> = arrayListOf()
        val cateAll = Category(name = "All", tag = -1)
        cateAll.images = arrayListOf()
        for (i in 1..Config.versionResource) {
            val json = loadJSONFromAsset(i)
            json?.let {
                val dataCollection = Gson().fromJson(json, DataCollection::class.java)
                if (dataCollection.data != null && dataCollection.data?.size ?: 0 > 0) {
                    for (cate in dataCollection.data!!) {
                        if (cate.images != null && cate.images?.size ?: 0 > 0) {
                            cateAll.images?.addAll(0, cate.images!!)
                        }
                        if (data.contains(cate)) {
                            if (cate.images != null && cate.images?.size ?: 0 > 0) {
                                data[data.indexOf(cate)].images?.addAll(0, cate.images!!)
                            }
                        } else {
                            if (cate.images == null) {
                                cate.images = arrayListOf()
                            }
                            data.add(cate)
                        }
                    }
                }
            }
        }

        //TODO remove cate all
//        if (cateAll.images != null && cateAll.images?.size ?: 0 > 0) {
//            cateAll.images = cateAll.images?.distinctBy { it.path } as? ArrayList<ImagePixel>
//            data.add(0, cateAll)
//        }
        ImageViewModel(App.get()).getCates().postValue(data)
        return data
    }

    override fun onPostExecute(data: ArrayList<Category>?) {
        super.onPostExecute(data)
        App.get().data = data
        doneListener?.invoke()
    }

}