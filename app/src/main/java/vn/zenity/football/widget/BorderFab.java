package vn.zenity.football.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import vn.zenity.football.R;
import vn.zenity.football.extensions.Tool;

/**
 * Created by BennyKok on 10/9/2016.
 */

public class BorderFab extends View{
    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint colorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    float three,one, padding, radius, four;

    Bitmap bg;

    int color;

    public void setIsSelected(boolean selected) {
        isSelected = selected;
        invalidate();
    }

    boolean isSelected = false;

    public void setNumber(int number) {
        this.number = number;
        invalidate();
    }

    private int number = 0;
    Paint textPaint;

    public BorderFab(Context context) {
        super(context);
        init();
    }

    public BorderFab(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BorderFab(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

//    public BorderFab(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
//        super(context, attrs, defStyleAttr, defStyleRes);
//        init();
//    }

    public void setColor(int color){
        this.color = color;
        invalidate();
    }

    private void init() {
        bg = Bitmap.createBitmap(2,2, Bitmap.Config.ARGB_8888);
        bg.eraseColor(Color.WHITE);
        bg.setPixel(0,0, Color.GRAY);
        bg.setPixel(1,1, Color.GRAY);

        three = Tool.convertDpToPixel(2,getContext());
        one = Tool.convertDpToPixel(1,getContext());
        padding = Tool.convertDpToPixel(6,getContext());
        four = Tool.convertDpToPixel(4,getContext());
        radius = Tool.convertDpToPixel(10,getContext());

        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(getContext().getResources().getColor(R.color.colorAccent));
        paint.setStrokeWidth(Tool.convertDpToPixel(2,getContext()));

        textPaint = new TextPaint();
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(Tool.convertSpToPixel(20, getContext()));
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        colorPaint.setColor(Color.WHITE);

        canvas.save();
        Path p = new Path();
        p.addCircle(getWidth() / 2, getHeight()/ 2, radius, Path.Direction.CCW);
        canvas.clipPath(p);
        canvas.drawBitmap(bg,null,new Rect(0,0,getWidth(),getHeight()),colorPaint);
        canvas.restore();

        colorPaint.setColor(color);
        canvas.drawRoundRect(new RectF(four,four,getWidth() - four * 2, getHeight() - four * 2), radius, radius, colorPaint);

        if (isSelected)
            canvas.drawRoundRect(new RectF(four,four,getWidth() - four * 2, getHeight() - four * 2), radius, radius, paint);
//        canvas.drawCircle(getWidth() / 2,  getHeight()/ 2, getWidth() / 3 + one, colorPaint);
//        canvas.drawCircle(getWidth() / 2,  getHeight() / 2, getWidth() / 3 + one, paint);
        if (number > 0) {
            Rect boundText = new Rect();
            String text = "" + number;

            textPaint.getTextBounds(text, 0, text.length(), boundText);
            canvas.drawText(text,getWidth() / 2f - boundText.width() / 2f - boundText.left - four/2f,  getHeight()/ 2f + boundText.height()/ 2f - four/2f , textPaint);
        }
    }

}
