package vn.zenity.football.models

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import vn.zenity.football.manager.PixelDB

/**
 * Created by vinh on 3/15/18.
 */
class ImageViewModel(app: Application): AndroidViewModel(app) {
    private var catesLiveData: MutableLiveData<ArrayList<Category>> = MutableLiveData()
    private var workingLiveData: MutableLiveData<List<ImagePixel>> = MutableLiveData()
    private var finishingLiveData: MutableLiveData<List<FinishedImage>> = MutableLiveData()
    private var currentDrawingLiveData: MutableLiveData<ImagePixel> = MutableLiveData()
    private var currentPositionDrawing: MutableLiveData<Int> = MutableLiveData()
    init {
        workingLiveData.postValue(PixelDB.getInstance().imageDao().getAllOffline())
        finishingLiveData.postValue(PixelDB.getInstance().imageDao().getFinished())
    }
    fun getCates() = catesLiveData
    fun getWorking() = workingLiveData
    fun getFinishing() = finishingLiveData
    fun getDrawing() = currentDrawingLiveData
    fun getCurrentPositionDrawing() = currentPositionDrawing
}