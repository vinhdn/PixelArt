package vn.zenity.football.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Vibrator;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.ColorUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import vn.zenity.football.R;
import vn.zenity.football.app.App;

import vn.zenity.football.extensions.Tool;

import vn.zenity.football.extensions.ImageView_ExtensionsKt;
import vn.zenity.football.models.ImageColor;
import vn.zenity.football.models.ImagePixel;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import static vn.zenity.football.extensions.ImageView_ExtensionsKt.freeMemory;

/**
 * Update by vinhdn on 11/3/2018.
 */
public class PixelView extends View implements ScaleGestureDetector.OnScaleGestureListener, GestureDetector.OnGestureListener {

    public interface CreateResultListener {
        void finish(Bitmap bitmapColor, Bitmap bitmapGray, float pixSize);
    }

    public interface FinishedDrawingListener {
        void finished();
    }

    public interface OnDrawChangeBox {
        void onChanged();
    }

    private final static Long pressDelay = 10L;
    private ArrayList<PxerLayer> pxerLayers = new ArrayList<>();

    //Drawing property
    private Paint pxerPaint;
    private int selectedColor = Color.YELLOW;
    private int selectedNumber = 1;
    private Mode mode = Mode.Normal;
    private int currentLayer = 0;
    private boolean showGrid;
    private boolean isUnrecordedChanges;
    private Paint borderPaint;
    private int picWidth;
    private int picHeight;
    private float pxerSize;
    private RectF picBoundary;
    private Rect picRect = new Rect();
    private Bitmap bgbitmap;
    private Canvas previewCanvas = new Canvas();
    private Bitmap preview;
    //Control property
    private Point[] points;
    private int downY, downX;
    private Matrix drawMatrix = new Matrix();
    private ScaleGestureDetector mScaleDetector;
    private GestureDetector mGestureDetector;
    private float mScaleFactor = 1.f;
    private Long prePressedTime = -1L;
    //History property
    private ArrayList<ArrayList<PxerHistory>> history = new ArrayList<>();
    private ArrayList<ArrayList<PxerHistory>> redohistory = new ArrayList<>();
    private ArrayList<Integer> historyIndex = new ArrayList<>();
    private ArrayList<Pxer> currentHistory = new ArrayList<>();
    //Callback
    private OnDropperCallBack dropperCallBack;

    //Config
    int[][] colors;
    float minScaleToDraw = 5.0f;
    private Bitmap numberBitmap;
    private Canvas currentColorCanvas = new Canvas();
    private Canvas numberCanvas = new Canvas();
    private Paint textPaint = new Paint();
    private Bitmap grayBitmap;
    private Bitmap currentColorBitmap;
    public ImagePixel imagePixel;
    private ArrayList<ImageColor> listColors;
    private int[][] mapNumber;
    private int[][] mapCurrentNumber;
    private boolean isShowFinishedDrawing = false;
    private MotionEvent eventDown;
    private Boolean mustRedo = false;
    private Boolean isMoved = false;
    private Boolean isLongClick = false;

    public void setTypeTouch(Boolean typeTouch) {
        this.typeTouch = typeTouch;
    }

    public Boolean getTypeTouch() {
        return typeTouch;
    }

    private Boolean typeTouch = true;
    private final float SCROLL_THRESHOLD = 10;
    private final static long longTimePress = 250;
    private WeakReference<OnDrawChangeBox> onDrawChangeBoxWeakReference;
    private Bitmap borderBitmap;
    private Canvas borderCanvas = new Canvas();
    private float widthStroke;

    private int preX = -1;
    private int preY = -1;

    public void setFinishedDrawing(FinishedDrawingListener finishedDrawingListener) {
        this.finishedDrawingListener = finishedDrawingListener;
    }

    public void setOnDrawChangeBox(OnDrawChangeBox onDrawChangeBox) {
        this.onDrawChangeBoxWeakReference = new WeakReference<>(onDrawChangeBox);
    }

    private FinishedDrawingListener finishedDrawingListener;

    public PixelView(Context context) {
        super(context);

        init();
    }

    public PixelView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    public static ArrayList<Pxer> cloneList(List<Pxer> list) {
        ArrayList<Pxer> clone = new ArrayList<>(list.size());
        for (Pxer item : list) clone.add(item.clone());
        return clone;
    }

    public void setDropperCallBack(OnDropperCallBack dropperCallBack) {
        this.dropperCallBack = dropperCallBack;
    }

    public ArrayList<PxerLayer> getPxerLayers() {
        return pxerLayers;
    }

    public int getSelectedColor() {
        return selectedColor;
    }

    public void setSelectedColor(int selectedColor) {
        this.selectedColor = selectedColor;
        if (listColors != null) {
            selectedNumber = listColors.indexOf(new ImageColor(1, selectedColor, 0, 0)) + 1;
        }
        new DrawCurrentColor().execute();
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }


    public int getCurrentLayer() {
        return currentLayer;
    }

    public void setCurrentLayer(int currentLayer) {
        this.currentLayer = currentLayer;
        invalidate();
    }

    public Canvas getPreviewCanvas() {
        return previewCanvas;
    }

    public Bitmap getPreview() {
        return preview;
    }

    public ArrayList<Pxer> getCurrentHistory() {
        return currentHistory;
    }

    public void copyAndPasteCurrentLayer() {
        Bitmap bitmap = pxerLayers.get(currentLayer).bitmap.copy(Bitmap.Config.ARGB_8888, true);
        pxerLayers.add(Math.max(getCurrentLayer(), 0), new PxerLayer(bitmap));

        history.add(Math.max(getCurrentLayer(), 0), new ArrayList<PxerHistory>());
        redohistory.add(Math.max(getCurrentLayer(), 0), new ArrayList<PxerHistory>());
        historyIndex.add(Math.max(getCurrentLayer(), 0), 0);
    }

    public void addLayer() {
        Bitmap bitmap = Bitmap.createBitmap(picWidth, picHeight, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(Color.TRANSPARENT);
        pxerLayers.add(Math.max(getCurrentLayer(), 0), new PxerLayer(bitmap));

        history.add(Math.max(getCurrentLayer(), 0), new ArrayList<PxerHistory>());
        redohistory.add(Math.max(getCurrentLayer(), 0), new ArrayList<PxerHistory>());
        historyIndex.add(Math.max(getCurrentLayer(), 0), 0);
    }

    public void removeCurrentLayer() {
        getPxerLayers().remove(getCurrentLayer());

        history.remove(getCurrentLayer());
        redohistory.remove(getCurrentLayer());
        historyIndex.remove(getCurrentLayer());

        setCurrentLayer(Math.max(0, getCurrentLayer() - 1));
        invalidate();
    }

    public void moveLayer(int from, int to) {
        Collections.swap(getPxerLayers(), from, to);

        Collections.swap(history, from, to);
        Collections.swap(redohistory, from, to);
        Collections.swap(historyIndex, from, to);
        invalidate();
    }

    public void clearCurrentLayer() {
        getPxerLayers().get(currentLayer).bitmap.eraseColor(Color.TRANSPARENT);
    }

    public void mergeDownLayer() {
        getPreview().eraseColor(Color.TRANSPARENT);
        getPreviewCanvas().setBitmap(getPreview());

        getPreviewCanvas().drawBitmap(pxerLayers.get(getCurrentLayer() + 1).bitmap, 0, 0, null);
        getPreviewCanvas().drawBitmap(pxerLayers.get(getCurrentLayer()).bitmap, 0, 0, null);

        pxerLayers.remove(getCurrentLayer() + 1);
        history.remove(getCurrentLayer() + 1);
        redohistory.remove(getCurrentLayer() + 1);
        historyIndex.remove(getCurrentLayer() + 1);

        pxerLayers.set(getCurrentLayer(), new PxerLayer(Bitmap.createBitmap(getPreview())));
        history.set(getCurrentLayer(), new ArrayList<PxerHistory>());
        redohistory.set(getCurrentLayer(), new ArrayList<PxerHistory>());
        historyIndex.set(getCurrentLayer(), 0);

        invalidate();
    }

    public void visibilityAllLayer(boolean visible) {
        for (int i = 0; i < pxerLayers.size(); i++) {
            pxerLayers.get(i).visible = visible;
        }
        invalidate();
    }

    public void mergeAllLayers(boolean isMergeGrayColor) {
        getPreview().eraseColor(Color.TRANSPARENT);
        getPreviewCanvas().setBitmap(getPreview());
        if (isMergeGrayColor) {
            getPreviewCanvas().drawBitmap(grayBitmap, 0f, 0f, null);
        }
        for (int i = 0; i < pxerLayers.size(); i++) {
            getPreviewCanvas().drawBitmap(pxerLayers.get(pxerLayers.size() - i - 1).bitmap, 0, 0, null);
        }
        pxerLayers.clear();
        history.clear();
        redohistory.clear();
        historyIndex.clear();

        pxerLayers.add(new PxerLayer(Bitmap.createBitmap(getPreview())));
        history.add(new ArrayList<PxerHistory>());
        redohistory.add(new ArrayList<PxerHistory>());
        historyIndex.add(0);

        setCurrentLayer(0);

        invalidate();
    }

    public void createBlankProject(String name, int picWidth, int picHeight) {
        createBlankProject(name, picWidth, picHeight, R.drawable.im56);
    }

    public void createBlankProject(final String name, final int picWidth, final int picHeight, ImagePixel imagePixel) {
        this.imagePixel = imagePixel;
        if (imagePixel != null && imagePixel.getPath() != null) {
            ImageView_ExtensionsKt.loadBitmapAsset(imagePixel.getPath(), false, 0.5f, new Function1<Bitmap, Unit>() {
                @Override
                public Unit invoke(Bitmap bitmap) {
                    createBlankProject(name, picWidth, picHeight, bitmap);
                    return null;
                }
            });
        }
    }

    public void createBlankProject(String name, int picWidth, int picHeight, int drawable) {
        Bitmap bitmap1 = BitmapFactory.decodeResource(getResources(),
                drawable);
        createBlankProject(name, picWidth, picHeight, bitmap1);
    }

    public void createBlankProject(String name, int picWidth, int picHeight, Bitmap bitmap1) {
        grayBitmap = bitmap1;
        colors = new int[bitmap1.getWidth()][bitmap1.getHeight()];
        for (int i = 0; i < bitmap1.getWidth(); i++) {
            for (int j = 0; j < bitmap1.getHeight(); j++) {
                int p = bitmap1.getPixel(i, j);
                if (p == Color.WHITE) {
                    colors[i][j] = p;
                    return;
                }
                colors[i][j] = p;
            }
        }


        this.picWidth = bitmap1.getWidth();
        this.picHeight = bitmap1.getHeight();
        picWidth = bitmap1.getWidth();
        picHeight = bitmap1.getHeight();

        points = new Point[picWidth * picHeight];
        for (int i = 0; i < picWidth; i++) {
            for (int j = 0; j < picHeight; j++) {
                points[i * picHeight + j] = new Point(i, j);
            }
        }

        history.clear();
        redohistory.clear();
        historyIndex.clear();


        pxerLayers.clear();
        Bitmap bitmap = Bitmap.createBitmap(picWidth, picHeight, Bitmap.Config.ARGB_8888);

        history.add(new ArrayList<PxerHistory>());
        redohistory.add(new ArrayList<PxerHistory>());
        historyIndex.add(0);

        PxerLayer layer = new PxerLayer(bitmap);
        layer.visible = true;
        pxerLayers.add(layer);
//            for (int x = 0; x < out.get(i).pxers.size(); x++) {
//                Pxer p = out.get(i).pxers.get(x);
//                pxerLayers.get(i).bitmap.setPixel(p.x, p.y, p.color);
//            }
//        for (int k = 0; k < picWidth; k++) {
//            for (int j = 0; j < picHeight; j++) {
//                pxerLayers.get(0).bitmap.setPixel(k, j, colors[k][j]);
//            }
//        }
        onLayerUpdate();

        mScaleFactor = 1.f;
        drawMatrix.reset();
        initPxerInfo();

        history.clear();
        redohistory.clear();
        historyIndex.clear();

        history.add(new ArrayList<PxerHistory>());
        redohistory.add(new ArrayList<PxerHistory>());
        historyIndex.add(0);

        setCurrentLayer(0);

        reCalBackground();

        freeMemory();
    }

    public boolean loadProject(File file) {
        Gson gson = new Gson();

        ArrayList<PxableLayer> out = new ArrayList<>();
        try {
            JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(new File(file.getPath()))));
            reader.beginArray();
            while (reader.hasNext()) {
                PixelView.PxableLayer layer = gson.fromJson(reader, PixelView.PxableLayer.class);
                out.add(layer);
            }
            reader.endArray();
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();

            return false;
        }

        Bitmap bitmap1 = BitmapFactory.decodeResource(getResources(),
                R.drawable.im56);
        colors = new int[bitmap1.getWidth()][bitmap1.getHeight()];
        for (int i = 0; i < bitmap1.getWidth(); i++) {
            for (int j = 0; j < bitmap1.getHeight(); j++) {
                int p = bitmap1.getPixel(i, j);
                int grayColor = 0x90;
                int a = (p >> 24) & grayColor;
                int r = (p >> 16) & grayColor;
                int g = (p >> 8) & grayColor;
                int b = p & grayColor;
                int avg = (r + g + b) / 3;
                p = (a << 24) | (avg << 16) | (avg << 8) | avg;
                colors[i][j] = p;
            }
        }

        this.picWidth = out.get(0).width;
        this.picHeight = out.get(0).height;

        this.picWidth = bitmap1.getWidth();
        this.picHeight = bitmap1.getHeight();

        points = new Point[picWidth * picHeight];
        for (int i = 0; i < picWidth; i++) {
            for (int j = 0; j < picHeight; j++) {
                points[i * picHeight + j] = new Point(i, j);
            }
        }

        history.clear();
        redohistory.clear();
        historyIndex.clear();


        pxerLayers.clear();
        for (int i = 0; i < out.size(); i++) {
            Bitmap bitmap = Bitmap.createBitmap(picWidth, picHeight, Bitmap.Config.ARGB_8888);

            history.add(new ArrayList<PxerHistory>());
            redohistory.add(new ArrayList<PxerHistory>());
            historyIndex.add(0);

            PxerLayer layer = new PxerLayer(bitmap);
            layer.visible = out.get(i).visible;
            pxerLayers.add(layer);
            for (int k = 0; k < picWidth; k++) {
                for (int j = 0; j < picHeight; j++) {
                    pxerLayers.get(i).bitmap.setPixel(k, j, colors[k][j]);
                }
            }
        }
        onLayerUpdate();

        mScaleFactor = 1.f;
        drawMatrix.reset();
        initPxerInfo();

        setCurrentLayer(0);

        reCalBackground();
        invalidate();

        freeMemory();
        return true;
    }

    private void undoCurrentHistory() {
        Bitmap bitmap = pxerLayers.get(currentLayer).bitmap;
        for (int i = currentHistory.size() -1; i >=0; i--){
            Pxer pxer = currentHistory.get(i);
            bitmap.setPixel(pxer.x, pxer.y, pxer.color);
            mapCurrentNumber[pxer.x][pxer.y] = pxer.number;
        }
        currentHistory.clear();
        invalidate();
    }
    public void undo() {
        if (historyIndex.get(currentLayer) <= 0) {
//            Tool.toast(getContext(), "No more undo");
            return;
        }

        historyIndex.set(currentLayer, historyIndex.get(currentLayer) - 1);
        for (int i = 0; i < history.get(currentLayer).get(historyIndex.get(currentLayer)).pxers.size(); i++) {
            Pxer pxer = history.get(currentLayer).get(historyIndex.get(currentLayer)).pxers.get(i);
            currentHistory.add(new Pxer(pxer.x, pxer.y, pxerLayers.get(currentLayer).bitmap.getPixel(pxer.x, pxer.y), mapCurrentNumber[pxer.x][pxer.y]));

            Pxer coord = history.get(currentLayer).get(historyIndex.get(currentLayer)).pxers.get(i);
            pxerLayers.get(currentLayer).bitmap.setPixel(coord.x, coord.y, coord.color);
        }
        redohistory.get(currentLayer).add(new PxerHistory(cloneList(currentHistory)));
        currentHistory.clear();

        history.get(currentLayer).remove(history.get(currentLayer).size() - 1);
        invalidate();
    }

    public void redo() {
        if (redohistory.get(currentLayer).size() <= 0) {
            Tool.toast(getContext(), "No more redo");
            return;
        }

        for (int i = 0; i < redohistory.get(currentLayer).get(redohistory.get(currentLayer).size() - 1).pxers.size(); i++) {
            Pxer pxer = redohistory.get(currentLayer).get(redohistory.get(currentLayer).size() - 1).pxers.get(i);
            currentHistory.add(new Pxer(pxer.x, pxer.y, pxerLayers.get(currentLayer).bitmap.getPixel(pxer.x, pxer.y), mapCurrentNumber[pxer.x][pxer.y]));

            pxer = redohistory.get(currentLayer).get(redohistory.get(currentLayer).size() - 1).pxers.get(i);
            pxerLayers.get(currentLayer).bitmap.setPixel(pxer.x, pxer.y, pxer.color);
        }
        historyIndex.set(currentLayer, historyIndex.get(currentLayer) + 1);

        history.get(currentLayer).add(new PxerHistory(cloneList(currentHistory)));
        currentHistory.clear();

        redohistory.get(currentLayer).remove(redohistory.get(currentLayer).size() - 1);
        invalidate();
    }

    public void resetViewPort() {
        scaleAtFirst();
    }

    private void init() {
        mScaleDetector = new ScaleGestureDetector(getContext(), this);
        mGestureDetector = new GestureDetector(getContext(), this);
        mGestureDetector.setIsLongpressEnabled(true);

        widthStroke = Tool.convertDpToPixel(2f, getContext()) /5f;
        if (widthStroke < 0.6f) widthStroke = 0.6f;

        setWillNotDraw(false);

        borderPaint = new Paint();
        borderPaint.setAntiAlias(true);
        borderPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        borderPaint.setStrokeJoin(Paint.Join.ROUND);
//        borderPaint.setStrokeWidth(1f);
        borderPaint.setColor(Color.DKGRAY);

//        textPaint.setTextSize(5f);
//        textPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(Tool.convertSpToPixel(6, getContext()));
        textPaint.setTypeface(ResourcesCompat.getFont(getContext(), R.font.roboto_regular));

//        textPaint.setAntiAlias(true);

        pxerPaint = new Paint();
        pxerPaint.setAntiAlias(true);

        picBoundary = new RectF(0, 0, 0, 0);

        //Create a 40 x 40 project
        this.picWidth = 100;
        this.picHeight = 100;

        points = new Point[picWidth * picHeight];
        for (int i = 0; i < picWidth; i++) {
            for (int j = 0; j < picHeight; j++) {
                points[i * picHeight + j] = new Point(i, j);
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(picWidth, picHeight, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(Color.TRANSPARENT);
        pxerLayers.clear();
        pxerLayers.add(new PxerLayer(bitmap));

        history.add(new ArrayList<PxerHistory>());
        redohistory.add(new ArrayList<PxerHistory>());
        historyIndex.add(0);

        reCalBackground();
        resetViewPort();

        //Avoid unknown flicking issue if the user scale the canvas immediately
        long downTime = SystemClock.uptimeMillis(), eventTime = downTime + 100;
        float x = 0.0f, y = 0.0f;
        int metaState = 0;
        MotionEvent motionEvent = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, x, y, metaState);
        mGestureDetector.onTouchEvent(motionEvent);
    }

    public void reCalBackground() {
        preview = Bitmap.createBitmap(picWidth, picHeight, Bitmap.Config.ARGB_8888);

        bgbitmap = Bitmap.createBitmap(picWidth * 2, picHeight * 2, Bitmap.Config.ARGB_8888);
        bgbitmap.eraseColor(ColorUtils.setAlphaComponent(Color.WHITE, 255));

        for (int i = 0; i < picWidth; i++) {
            for (int j = 0; j < picHeight * 2; j++) {
                if (j % 2 != 0) {
                    bgbitmap.setPixel(i * 2 + 1, j, Color.argb(255, 255, 255, 255));
                } else {
                    bgbitmap.setPixel(i * 2, j, Color.argb(255, 255, 255, 255));
                }
            }
        }
        drawNumber();
        new DrawCurrentColor().execute();
    }

    private void drawNumber() {
        new TaskDrawNumberBackground().execute();
        new TaskDrawBorderBackground().execute();
    }

    public void setListColors(ArrayList<ImageColor> listColors) {
        this.listColors = listColors;
        drawNumber();
        setSelectedColor(getSelectedColor());
    }

    public void setMapNumber(int[][] mapNumber) {
        this.mapNumber = mapNumber;
        this.mapCurrentNumber = new int[mapNumber.length][mapNumber[0].length];
    }

    public void setDataOffline(String it) {
        this.mapCurrentNumber = (new Gson()).fromJson(it, int[][].class);
//        reDrawColor(true, true);
//        new DrawColorBackground(true, true).execute();
        if (!reDrawColor(true, true)) {
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    reDrawColor(true, true);
                    getPercentDrawCorrect();
                }
            }, 1000);
        } else  {
            getPercentDrawCorrect();
        }
    }

    @SuppressLint("StaticFieldLeak")
    class DrawColorBackground extends AsyncTask<Void, Void, Boolean> {

        private Boolean isFullAlpha;
        private Boolean isForceFullAlpha;

        private DrawColorBackground(boolean
                                            isFullAlpha, boolean isForceFullAlpha) {
            this.isFullAlpha = isFullAlpha;
            this.isForceFullAlpha = isForceFullAlpha;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                reDrawColor(isFullAlpha, isForceFullAlpha);
            } catch (Exception ex) {
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if (aBoolean) {
                invalidate();
            }
        }
    }

    class TaskDrawNumberBackground extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            if (pxerSize <= 0 || picWidth <= 0 || colors == null) return false;
            float scale = 4f;
            numberBitmap = Bitmap.createBitmap((int) (picWidth * pxerSize * scale), (int) (picHeight * pxerSize * scale), Bitmap.Config.ARGB_8888);
            numberBitmap.eraseColor(Color.TRANSPARENT);
            numberCanvas.setBitmap(numberBitmap);
            numberCanvas.drawColor(Color.TRANSPARENT);
            textPaint.setTextSize(Tool.convertSpToPixel(2 * scale, getContext()));

            for (int x = 0; x < picWidth; x++) {
                for (int y = 0; y < picHeight; y++) {

                    float posx = (picBoundary.left) + pxerSize * x * scale;
                    float posy = (picBoundary.top) + pxerSize * y * scale;
                    if (listColors != null) {
                        int index = mapNumber[x][y];
                        Rect boundText = new Rect();
                        if (index > 0) {
                            String text = "" + index;
                            textPaint.getTextBounds(text, 0, text.length(), boundText);
                            numberCanvas.drawText(text, posx + (pxerSize * scale - boundText.width()) / 2f - boundText.left, posy + (pxerSize * scale - boundText.height()) / 2f - boundText.top, textPaint);
                        }
                    }
                }
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aVoid) {
            super.onPostExecute(aVoid);
            if (aVoid) {
                invalidate();
            }
        }
    }
    class TaskDrawBorderBackground extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            if (pxerSize <= 0 || picWidth <= 0 || colors == null || mapNumber == null) return false;
            float scale = 3f;
            borderBitmap = Bitmap.createBitmap((int) (picWidth * pxerSize * scale), (int) (picHeight * pxerSize * scale), Bitmap.Config.ARGB_8888);
            borderBitmap.eraseColor(Color.TRANSPARENT);
            borderCanvas.setBitmap(borderBitmap);
            borderCanvas.drawColor(Color.TRANSPARENT);

            if (widthStroke < pxerSize / 10f){
                widthStroke = pxerSize / 10f;
            }
            Paint bdPain = new Paint();
            bdPain.setStyle(Paint.Style.STROKE);
            bdPain.setColor(Color.BLACK);
            bdPain.setStrokeWidth(widthStroke);
//            bdPain.setStrokeWidth(2);

            for (int x = 0; x < picWidth; x++) {
                for (int y = 0; y < picHeight; y++) {
                    int index = mapNumber[x][y];
                    if (index > 0) {
                        borderCanvas.drawRect(x * pxerSize * scale /*+ (widthStroke / 2f)*/, y * pxerSize * scale /*+ (widthStroke / 2f)*/, (x + 1) * pxerSize * scale/* - (widthStroke / 2f)*/, (y + 1) * pxerSize * scale/* - (widthStroke / 2f)*/, bdPain);
                    }
                }
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aVoid) {
            super.onPostExecute(aVoid);
            if (aVoid) {
                invalidate();
            }
        }
    }

    class DrawCurrentColor extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            if (pxerSize <= 0 || picWidth <= 0 || colors == null || mapNumber == null) return false;
            if (picWidth != mapNumber.length || picHeight != mapNumber[0].length) return false;
            currentColorBitmap = Bitmap.createBitmap((picWidth), (picHeight), Bitmap.Config.ARGB_8888);
            currentColorBitmap.eraseColor(Color.TRANSPARENT);
            currentColorCanvas.setBitmap(currentColorBitmap);
            currentColorCanvas.drawColor(Color.TRANSPARENT);

            for (int x = 0; x < picWidth; x++) {
                for (int y = 0; y < picHeight; y++) {
                    if (listColors != null) {
                        int index = listColors.indexOf(new ImageColor(1, selectedColor, 0, 0)) + 1;
                        if (index > 0 && index == mapNumber[x][y]) {
                            currentColorBitmap.setPixel(x, y, Color.LTGRAY);
                        }
                    }
                }
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aVoid) {
            super.onPostExecute(aVoid);
            if (aVoid) {
                invalidate();
            }
        }
    }

    public void createResultDraw(CreateResultListener listener) {
        new CreateResultDraw(listener).execute();
    }

    private class CreateResultDraw extends AsyncTask<Void, Void, Bitmap> {
        private WeakReference<CreateResultListener> listener;

        public CreateResultDraw(CreateResultListener listener) {
            this.listener = new WeakReference<>(listener);
        }

        @Override
        protected Bitmap doInBackground(Void... voids) {
            if (pxerSize <= 0 || picWidth <= 0 || colors == null) return null;
            return pxerLayers.get(0).bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            super.onPostExecute(result);
            if (listener != null && listener.get() != null)
                listener.get().finish(result, grayBitmap, pxerSize);
        }
    }

    private float mDownX;
    private float mDownY;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            eventDown = event;
            mDownX = event.getX();
            mDownY = event.getY();
            isMoved = false;
            prePressedTime = System.currentTimeMillis();
            mGestureDetector.onTouchEvent(event);
            if (mScaleFactor > minScaleToDraw) {
                handler.postDelayed(mLongPressed, longTimePress);
            }
            handler.removeCallbacks(checkFinishedRunnable);
        }

        mScaleDetector.onTouchEvent(event);

        if (event.getPointerCount() > 1) {
            mustRedo = true;
            handler.removeCallbacks(mLongPressed);

            if(currentHistory.size() > 0) {
                undoCurrentHistory();
            }
        }

        if(event.getAction() == MotionEvent.ACTION_UP)
            handler.removeCallbacks(mLongPressed);

        if(event.getAction() == MotionEvent.ACTION_MOVE && eventDown != null && (Math.abs(mDownX - event.getX()) > SCROLL_THRESHOLD || Math.abs(mDownY - event.getY()) > SCROLL_THRESHOLD)){
            isMoved = true;
            handler.removeCallbacks(mLongPressed);
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_UP) {
            isLongClick = false;
        }

        if (!typeTouch) {
            isLongClick = true;
        }

        if (event.getPointerCount() > 1 || mScaleFactor <= minScaleToDraw) {
            prePressedTime = -1L;
            mGestureDetector.onTouchEvent(event);
            return true;
        }

        if (!isLongClick && event.getAction() == MotionEvent.ACTION_MOVE) {
            prePressedTime = -1L;
            mGestureDetector.onTouchEvent(event);
            return true;
        }

        //Get the position
        final float mX = event.getX();
        final float mY = event.getY();
        final float[] raw = new float[9];
        drawMatrix.getValues(raw);
        final float scaledWidth = picBoundary.width() * mScaleFactor;
        final float scaledHeight = picBoundary.height() * mScaleFactor;
        picRect.set((int) (raw[Matrix.MTRANS_X])
                , (int) (raw[Matrix.MTRANS_Y])
                , (int) (raw[Matrix.MTRANS_X] + scaledWidth)
                , (int) (raw[Matrix.MTRANS_Y] + scaledHeight)
        );
        if (!picRect.contains((int) mX, (int) mY)) {
            return true;
        }
        //We got x and y
        final int x = (int) (((mX - picRect.left) / (scaledWidth)) * picWidth);
        final int y = (int) (((mY - picRect.top) / (scaledHeight)) * picHeight);

        if (!isValid(x, y)) return true;

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            downY = y;
            downX = x;
            preX = x;
            preY = y;
            mustRedo = false;
        }

        if (getMode() == Mode.ShapeTool && downX != -1 && event.getAction() != MotionEvent.ACTION_UP && event.getAction() != MotionEvent.ACTION_DOWN) {
            return true;
        }

        Pxer pxer;
        Bitmap bitmapToDraw = pxerLayers.get(currentLayer).bitmap;
        Boolean isDrawed = false;

        if (mapNumber == null || mapCurrentNumber == null) return true;
        switch (getMode()) {
            case Normal:
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    break;
                }

                if (mustRedo || (event.getAction() == MotionEvent.ACTION_UP && prePressedTime != -1L
                        && System.currentTimeMillis() - prePressedTime <= pressDelay)
                        || event.getPointerCount() > 1 || (eventDown != null && eventDown.getPointerCount() > 1)) {
                    break;
                }

                final int n = Math.max(Math.abs(x - preX), Math.abs(y - preY));
                final float stepX = n == 0 ? 0 : ((float)(x - preX)) / n;
                final float stepY = n == 0 ? 0 : ((float)(y - preY)) / n;

                for (int i = 0; i<= n; i++) {
                    final int posX = (int)(preX + (stepX * i + 0.5));
                    final int posY = (int)(preY + (stepY * i + 0.5));

                    int rightNumber = mapNumber[posX][posY];
                    if (rightNumber == 0) rightNumber = -1000;
                    int currentNumber = mapCurrentNumber[posX][posY];

                    final boolean isDeleteColor = (event.getAction() == MotionEvent.ACTION_UP && !isMoved) && currentNumber > 0 && selectedNumber != rightNumber && currentNumber != rightNumber;
                    if (selectedNumber == currentNumber && !isDeleteColor) {
                        continue;
                    }
                    if (event.getAction() == MotionEvent.ACTION_UP && isMoved) {
                        continue;
                    }

                    if (event.getAction() != MotionEvent.ACTION_UP && event.getAction() != MotionEvent.ACTION_DOWN) {
                        pxer = new Pxer(posX, posY, bitmapToDraw.getPixel(posX, posY), currentNumber);
                        currentHistory.add(pxer);
                    }

                    float alpha = 1f;
                    if (rightNumber > 0 && showGrid) {
                        if (selectedNumber != rightNumber) {
                            alpha = 0.6f;
                        }
                    } else if (rightNumber <= 0 && showGrid) {
                        alpha = 0.6f;
                    }

                    if (isDeleteColor) {
                        bitmapToDraw.setPixel(posX, posY, Color.TRANSPARENT);
                        mapCurrentNumber[posX][posY] = 0;
                    } else {
                        bitmapToDraw.setPixel(posX, posY, getColorWithAlpha(selectedColor, alpha));
                        mapCurrentNumber[posX][posY] = selectedNumber;
                    }
                    setUnrecordedChanges(true);
                }
                preX = x;
                preY = y;
                break;
            case Dropper: //Select color
                if (event.getAction() == MotionEvent.ACTION_DOWN)
                    break;
                if (x == downX && downY == y) {
                    for (int i = 0; i < pxerLayers.size(); i++) {
                        int pixel = pxerLayers.get(i).bitmap.getPixel(x, y);
                        if (pixel != Color.TRANSPARENT) {
                            setSelectedColor(pxerLayers.get(i).bitmap.getPixel(x, y));
                            if (dropperCallBack != null) {
                                dropperCallBack.onColorDropped(selectedColor);
                            }
                            break;
                        }
                        if (i == pxerLayers.size() - 1) {
                            if (dropperCallBack != null) {
                                dropperCallBack.onColorDropped(Color.TRANSPARENT);
                            }
                        }
                    }
                }
                break;
            case Fill:
                //The fill tool is brought to us with aid by some open source project online :( I forgot the name
                if (event.getAction() == MotionEvent.ACTION_UP && x == downX && downY == y) {
                    Tool.freeMemory();

                    int targetColor = bitmapToDraw.getPixel(x, y);
                    Queue<Point> toExplore = new LinkedList<>();
                    HashSet<Point> explored = new HashSet<>();
                    toExplore.add(new Point(x, y));
                    while (!toExplore.isEmpty()) {
                        Point p = toExplore.remove();
                        //Color it
                        currentHistory.add(new Pxer(p.x, p.y, targetColor, mapCurrentNumber[x][y]));
                        bitmapToDraw.setPixel(p.x, p.y, (ColorUtils.compositeColors(selectedColor, bitmapToDraw.getPixel(p.x, p.y))));
                        //
                        Point cp;
                        if (isValid(p.x, p.y - 1)) {
                            cp = points[p.x * picHeight + p.y - 1];
                            if (!explored.contains(cp)) {
                                if (bitmapToDraw.getPixel(cp.x, cp.y) == targetColor)
                                    toExplore.add(cp);
                                explored.add(cp);
                            }
                        }

                        if (isValid(p.x, p.y + 1)) {
                            cp = points[p.x * picHeight + p.y + 1];
                            if (!explored.contains(cp)) {
                                if (bitmapToDraw.getPixel(cp.x, cp.y) == targetColor)
                                    toExplore.add(cp);
                                explored.add(cp);
                            }
                        }

                        if (isValid(p.x - 1, p.y)) {
                            cp = points[(p.x - 1) * picHeight + p.y];
                            if (!explored.contains(cp)) {
                                if (bitmapToDraw.getPixel(cp.x, cp.y) == targetColor)
                                    toExplore.add(cp);
                                explored.add(cp);
                            }
                        }

                        if (isValid(p.x + 1, p.y)) {
                            cp = points[(p.x + 1) * picHeight + p.y];
                            if (!explored.contains(cp)) {
                                if (bitmapToDraw.getPixel(cp.x, cp.y) == targetColor)
                                    toExplore.add(cp);
                                explored.add(cp);
                            }
                        }
                    }
                    setUnrecordedChanges(true);
                    finishAddHistory();
                }
                break;
        }
        invalidate();

        if (event.getAction() == MotionEvent.ACTION_UP) {
            handler.postDelayed(checkFinishedRunnable, 300);
            if (getMode() != Mode.Fill && getMode() != Mode.Dropper && getMode() != Mode.ShapeTool) {
                finishAddHistory();
            }
        }

        return true;
    }

    private Point getPositionTouchEvent(MotionEvent event) {
        final float mX = event.getX();
        final float mY = event.getY();
        final float[] raw = new float[9];
        drawMatrix.getValues(raw);
        final float scaledWidth = picBoundary.width() * mScaleFactor;
        final float scaledHeight = picBoundary.height() * mScaleFactor;
        picRect.set((int) (raw[Matrix.MTRANS_X])
                , (int) (raw[Matrix.MTRANS_Y])
                , (int) (raw[Matrix.MTRANS_X] + scaledWidth)
                , (int) (raw[Matrix.MTRANS_Y] + scaledHeight)
        );
        if (!picRect.contains((int) mX, (int) mY)) {
            return null;
        }
        final int x = (int) (((mX - picRect.left) / (scaledWidth)) * picWidth);
        final int y = (int) (((mY - picRect.top) / (scaledHeight)) * picHeight);
        return new Point(x, y);
    }

    public void finishAddHistory() {
        if (!(currentHistory.size() <= 0) && isUnrecordedChanges) {
            isUnrecordedChanges = false;
            redohistory.get(currentLayer).clear();
            historyIndex.set(currentLayer, historyIndex.get(currentLayer) + 1);
            history.get(currentLayer).add(new PxerHistory(currentHistory));
            currentHistory = new ArrayList<>();
        }
    }

    private boolean isValid(int x, int y) {
        return x >= 0 && x <= (picWidth - 1) && y >= 0 && y <= (picHeight - 1);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.WHITE);
        canvas.save();
        canvas.concat(drawMatrix);
        pxerPaint.setAlpha(255);
        canvas.drawBitmap(bgbitmap, null, picBoundary, pxerPaint);
        if (grayBitmap != null) {
            if (mScaleFactor >= minScaleToDraw) {
                float sc = 1 - (mScaleFactor - minScaleToDraw) / 4;
                if (sc < 0) sc = 0;
                pxerPaint.setAlpha((int) (sc * 255));
            }
            canvas.drawBitmap(grayBitmap, null, picBoundary, pxerPaint);
        }
        pxerPaint.setAlpha(155);
        if (currentColorBitmap != null && showGrid) {
            canvas.drawBitmap(currentColorBitmap, null, picBoundary, pxerPaint);
        }
        pxerPaint.setAlpha(255);
        if (showGrid) {
//            if (numberBitmap == null) {
//                drawNumber();
//            } else {
//                float sc = 1 - (mScaleFactor - minScaleToDraw) / 4;
//                if (sc < 0) sc = 0;
//                pxerPaint.setAlpha((int) ((1f - sc) * 255));
//                canvas.drawBitmap(numberBitmap, null, picBoundary, pxerPaint);
//            }
        }

        pxerPaint.setAlpha(255);
        if (showGrid) {
            for (int x = 0; x < picWidth + 1; x++) {
                float posx = (picBoundary.left) + pxerSize * x;
                canvas.drawLine(posx, picBoundary.top, posx, picBoundary.bottom, borderPaint);
            }
            for (int y = 0; y < picHeight + 1; y++) {
                float posy = (picBoundary.top) + pxerSize * y;
                canvas.drawLine(picBoundary.left, posy, picBoundary.right, posy, borderPaint);
            }
        }

        if(showGrid) {
            textPaint.setTextSize(Tool.convertDpToPixel(1.5f, getContext()));
            final float[] raw = new float[9];
            drawMatrix.getValues(raw);
            final float scaledWidth = picBoundary.width() * mScaleFactor;
            final float scaledHeight = picBoundary.height() * mScaleFactor;
            picRect.set((int) (raw[Matrix.MTRANS_X])
                    , (int) (raw[Matrix.MTRANS_Y])
                    , (int) (raw[Matrix.MTRANS_X] + scaledWidth)
                    , (int) (raw[Matrix.MTRANS_Y] + scaledHeight)
            );
            Paint bdPain = new Paint();
            bdPain.setStyle(Paint.Style.STROKE);
            bdPain.setColor(Color.BLACK);
            bdPain.setStrokeWidth(0.5f);
            int count = 0;
            //*
            for (int x = 0; x < picWidth; x++) {
                for (int y = 0; y < picHeight; y++) {

                    float positionX = ((float)x / picWidth) * (float) scaledWidth + picRect.left;
                    float positionY = ((float) y / picHeight) * (float) scaledHeight + picRect.top;
                    if(positionX < -(pxerSize * mScaleFactor) || positionY < -(pxerSize * mScaleFactor) || positionX > getWidth() + (pxerSize * mScaleFactor) || positionY > getHeight() + (pxerSize * mScaleFactor)) continue;
//                    float posx = (picBoundary.left) + pxerSize * x * mScaleFactor + picRect.left;
//                    float posy = (picBoundary.top) + pxerSize * y * mScaleFactor + picRect.top;
                    float posx = (picBoundary.left) + pxerSize * x;
                    float posy = (picBoundary.top) + pxerSize * y;
                    if (listColors != null) {
                        int index = mapNumber[x][y];
                        Rect boundText = new Rect();
                        if (index > 0) {
                            String text = "" + index;
                            textPaint.getTextBounds(text, 0, text.length(), boundText);
                            canvas.drawText(text, posx + (pxerSize - boundText.width()) / 2f - boundText.left, posy + (pxerSize - boundText.height()) / 2f - boundText.top, textPaint);
                            canvas.drawRect(x * pxerSize , y * pxerSize , (x + 1) * pxerSize, (y + 1) * pxerSize, bdPain);
//                            canvas.drawText(text, (positionX - picRect.left) / mScaleFactor + (pxerSize - boundText.width()) / 2f - boundText.left, (positionY - picRect.top) / mScaleFactor + (pxerSize - boundText.height()) / 2f - boundText.top, textPaint);
                        }
                        count++;
                    }
                }
            }
            //*/
            Log.d("Number Draw", "" + count);
        }

        for (int i = pxerLayers.size() - 1; i > -1; i--) {
            if (pxerLayers.get(i).visible) {
                canvas.drawBitmap(pxerLayers.get(i).bitmap, null, picBoundary, pxerPaint);
            }
        }

        canvas.restore();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        initPxerInfo();
    }

    private void initPxerInfo() {
        int length = Math.min(getHeight(), getWidth());
        int w = Math.min(picWidth, picHeight);
        pxerSize = length / (w > 100 ? w : 100);
        if (numberBitmap == null) {
            drawNumber();
            picBoundary.set(0, 0, pxerSize * picWidth, pxerSize * picHeight);
            scaleAtFirst();
        }
    }

    @Override
    public boolean onDown(MotionEvent motionEvent) {
        return false;
    }

    final Handler handler = new Handler();
    Runnable mLongPressed = new Runnable() {
        public void run() {
            isLongClick = true;
            Vibrator v = (Vibrator) App.Companion.get().getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null)
                v.vibrate(100);
        }
    };

    Runnable checkFinishedRunnable = new Runnable() {
        @Override
        public void run() {
            float percentCorrect = getPercentDrawCorrect();
            if (percentCorrect >= 100f) {
                if (!isShowFinishedDrawing && finishedDrawingListener != null) {
                    isShowFinishedDrawing = true;
                    finishedDrawingListener.finished();
                }
            }
        }
    };



    @Override
    public void onShowPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        drawMatrix.postTranslate(-v, -v1);
        invalidate();
        return true;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        return false;
    }

    private ScaleGestureDetector lastScaleCorrect;

    @Override
    public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
        float scale = scaleGestureDetector.getScaleFactor();
//        if (scale <= 0.5 || scale >= 20) {
//            return false;
//        }
        mScaleFactor *= scale;
//        if (mScaleFactor >= 1 && mScaleFactor <= 20){
//            lastScaleCorrect = scaleGestureDetector;
//        } else  {
//            return true;
//        }
        Matrix transformationMatrix = new Matrix();
        float focusX = scaleGestureDetector.getFocusX();
        float focusY = scaleGestureDetector.getFocusY();

        transformationMatrix.postTranslate(-focusX, -focusY);
        transformationMatrix.postScale(scaleGestureDetector.getScaleFactor(), scaleGestureDetector.getScaleFactor());

        transformationMatrix.postTranslate(focusX, focusY);
        drawMatrix.postConcat(transformationMatrix);

        if (mScaleFactor > minScaleToDraw) {
            if (!showGrid) {
                showGrid = true;
                reDrawColor(false, false);
            }
            showGrid = true;
        } else {
            if (showGrid) {
                showGrid = false;
                reDrawColor(true, false);
            }
            showGrid = false;

        }

        invalidate();
        return true;
    }

    public boolean reDrawColor(boolean
                                    isFullAlpha, boolean isForceFullAlpha) {
        for (int i = pxerLayers.size() - 1; i > -1; i--) {
            Bitmap bitmap = pxerLayers.get(i).bitmap;
            if (bitmap != null && mapNumber != null && bitmap.getWidth() == mapNumber.length && mapNumber.length > 0 && mapNumber[0].length == bitmap.getHeight()) {
                for (int j = 0; j < bitmap.getWidth(); j++) {
                    for (int k = 0; k < bitmap.getHeight(); k++) {
                        int rightColor = mapNumber[j][k];
                        float alpha = 1f;
                        int numColor = mapCurrentNumber[j][k] - 1;
                        if (numColor < 0) continue;
                        if (isForceFullAlpha || (rightColor > 0 && showGrid)) {
                            if (numColor != rightColor - 1) {
                                alpha = 0.7f;
                            }
                        }
                        Log.d("righ,num", "" + rightColor + "  " + numColor);
                        numColor = listColors.get(numColor).getColor();
                        if (numColor == Color.TRANSPARENT) continue;
                        int colorFullAlpha = getColorWithExactAlpha(numColor, 1f);
                        if (isFullAlpha) {
                            if (colorFullAlpha < 0)
                                pxerLayers.get(i).bitmap.setPixel(j, k, colorFullAlpha);
                        } else {
                            pxerLayers.get(i).bitmap.setPixel(j, k, getColorWithExactAlpha(numColor, alpha));
                        }
                    }
                }
            } else {
                return false;
            }
            invalidate();
        }
        return true;
    }

    private void scaleAtFirst() {
        mScaleFactor = 1.f;
        drawMatrix.reset();

        float scale = (float) getWidth() / picBoundary.width();

        mScaleFactor = scale;
        Matrix transformationMatrix = new Matrix();
        transformationMatrix.postTranslate((getWidth() - picBoundary.width()) / 2, (getHeight() - picBoundary.height()) / 2);

        float focusX = getWidth() / 2;
        float focusY = getHeight() / 2;

        transformationMatrix.postTranslate(-focusX, -focusY);
        transformationMatrix.postScale(scale, scale);

        transformationMatrix.postTranslate(focusX, focusY);
        drawMatrix.postConcat(transformationMatrix);

        invalidate();
    }

    private void scaleTo(float scale, ScaleGestureDetector scaleGestureDetector) {
//        drawMatrix.reset();
        mScaleFactor = scale;
        Matrix transformationMatrix = new Matrix();
        float focusX = scaleGestureDetector.getFocusX();
        float focusY = scaleGestureDetector.getFocusY();

        transformationMatrix.postTranslate(-focusX, -focusY);
        transformationMatrix.postScale(scaleGestureDetector.getScaleFactor(), scaleGestureDetector.getScaleFactor());

        transformationMatrix.postTranslate(focusX, focusY);
        drawMatrix.postConcat(transformationMatrix);

        invalidate();
    }

    public void onLayerUpdate() {
    }

    @Override
    public void invalidate() {
        super.invalidate();
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
        if (mScaleFactor < 1.0f) {
            scaleAtFirst();
        }
    }

    public void setUnrecordedChanges(boolean unrecordedChanges) {
        isUnrecordedChanges = unrecordedChanges;
    }

    public enum Mode {
        Normal, Eraser, Fill, Dropper, ShapeTool
    }

    public interface OnDropperCallBack {
        void onColorDropped(int newColor);
    }

    public static class PxerLayer {
        public Bitmap bitmap;
        public boolean visible = true;

        public PxerLayer() {
        }

        public PxerLayer(Bitmap bitmap) {
            this.bitmap = bitmap;
        }
    }

    public static class PxableLayer {
        public int width, height;
        public boolean visible;
        public ArrayList<Pxer> pxers = new ArrayList<>();

        public PxableLayer() {
        }
    }

    public static class Pxer {
        public int x, y, color, number;

        public Pxer(int x, int y, int c, int number) {
            this.x = x;
            this.y = y;
            this.color = c;
            this.number = number;
        }

        @Override
        protected Pxer clone() {
            return new Pxer(x, y, color, number);
        }

        @Override
        public boolean equals(Object obj) {
            return ((Pxer) obj).x == this.x && ((Pxer) obj).y == this.y;
        }
    }

    public static class PxerHistory {
        public ArrayList<Pxer> pxers;

        public PxerHistory(ArrayList<Pxer> pxers) {
            this.pxers = pxers;
        }

    }

    public static int getColorWithAlpha(int color, float ratio) {
        int newColor = 0;
        int alpha = Math.round(Color.alpha(color) * ratio);
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        newColor = Color.argb(alpha, r, g, b);
        return newColor;
    }

    public static int getColorWithExactAlpha(int color, float alpha) {
        int newColor = 0;
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        newColor = Color.argb((int) (alpha * 255f), r, g, b);
        return newColor;
    }

    public String getDataColorDrawing() {
        if (mapCurrentNumber == null) return null;
        return (new Gson()).toJson(mapCurrentNumber);
    }

    public Bitmap getBitmapDrawing() {
        if (pxerLayers.size() > 0) {
            return pxerLayers.get(0).bitmap;
        } else {
            return null;
        }
    }

    public float getPercentDrawCorrect() {
        if (mapCurrentNumber == null || mapNumber == null) return 0;
        if (mapCurrentNumber.length <= 0 || mapNumber.length <= 0 || mapCurrentNumber.length != mapNumber.length)
            return 0;
        if (mapCurrentNumber[0].length != mapNumber[0].length) return 0;
        for (ImageColor ic: listColors) {
            ic.setCorrectCount(0);
        }
        int correct = 0;
        int sumColor = 0;
        for (int i = 0; i < mapNumber.length; i++) {
            for (int j = 0; j < mapNumber[0].length; j++) {
                if (mapNumber[i][j] > 0) {
                    sumColor++;
                    if (mapNumber[i][j] == mapCurrentNumber[i][j]) {
                        correct++;
                        listColors.get(mapNumber[i][j] - 1).setCorrectCount(listColors.get(mapNumber[i][j] - 1).getCorrectCount() + 1);
                    }
                }
            }
        }
        if (sumColor == 0) return 0;
        if (onDrawChangeBoxWeakReference != null && onDrawChangeBoxWeakReference.get() != null) {
            onDrawChangeBoxWeakReference.get().onChanged();
        }
        return (float) correct * 100f / (float) (sumColor);
    }

    public void fillColor(int position){
        for (int i = 0; i < mapNumber.length; i++) {
            for (int j = 0; j < mapNumber[0].length; j++) {
                if (mapNumber[i][j] > 0) {
                    if (mapNumber[i][j] == position + 1) {
                        mapCurrentNumber[i][j] = position + 1;
                    }
                }
            }
        }
        reDrawColor(!showGrid, false);
        invalidate();
        float percentCorrect = getPercentDrawCorrect();
        if (percentCorrect >= 100f) {
            if (!isShowFinishedDrawing && finishedDrawingListener != null) {
                isShowFinishedDrawing = true;
                finishedDrawingListener.finished();
            }
        }
    }

    public float getPercentCompleteOfColor(int position) {
        return (listColors.get(position).getCorrectCount() * 100f) / (float)listColors.get(position).getCount();
    }
}
