package com.kraydel.thermalcameraopencv;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

public class DrawView extends View{
    public float currentX=40;
    public float currentY=50;

    public DrawView(Context context){
        super(context);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Paint paint=new Paint();
        paint.setColor(Color.RED);
        canvas.drawCircle(currentX, currentY, 25, paint);
    }
}