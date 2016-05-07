package com.sam_chordas.android.stockhawk.surfaceview;

/**
 * Created by Blues on 17/04/2016.
 */
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class StockDetailView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private SurfaceHolder mHolder;
    private Canvas mCanvas;
    private boolean mIsDrawing,isdraw;
    final private double HeightRatio = 0.8;  //the height ratio of surfaceview to whole screen
    private int viewHeight, viewWidth;
    private int n = 0;
    private Path mPath;
    private Paint mCoorPaint;
    private Paint mRedPaint;
    private Paint mTextPaint;
    private float xstart,xend,ystart,yend,xtextoffset,ytextoffset;
    private float xmaintickinterval,ymaintickinterval,xminorinterval,yminorinterval, majorticklength,minorticklength;
    private int labelnumbersize, xmainticknumber, ymainticknumber;
    private int datasetnumber;
    private Float[] stockprice = new Float[]{1F,2F};
    private String[] stockdate = new String[]{"test"};
    private float datamax, datamin, ymin;



    public StockDetailView(Context context) {
        super(context);
        initView();
    }


    public StockDetailView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public StockDetailView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    public void setData(String[] ss,Float[] dataset)
    {
        stockdate = ss;
        stockprice = dataset;
        //Log.v("test",stockprice[5]+"");
        initView();
        run();
    }


    private void initView() {

        mHolder = getHolder();
        mHolder.addCallback(this);
        setFocusable(true);
        setFocusableInTouchMode(true);
        this.setKeepScreenOn(true);
        mPath = new Path();

        //Button btn = (Button)findViewById(R.id.sixmonthbtn);
        //initial different paint
        mRedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mRedPaint.setColor(Color.RED);
        mRedPaint.setStyle(Paint.Style.STROKE);
        mRedPaint.setStrokeWidth(viewWidth/100f);
        mRedPaint.setStrokeCap(Paint.Cap.ROUND);
        mRedPaint.setStrokeJoin(Paint.Join.ROUND);

        //tick and text painter
        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setStrokeWidth(1);

     //Coordinate painter
        mCoorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCoorPaint.setColor(Color.WHITE);
        mCoorPaint.setStyle(Paint.Style.STROKE);
        mCoorPaint.setStrokeWidth(viewWidth/100f);
        mCoorPaint.setStrokeCap(Paint.Cap.ROUND);
        mCoorPaint.setStrokeJoin(Paint.Join.ROUND);


        //define view size
        viewHeight = (int)(getResources().getDisplayMetrics().heightPixels*HeightRatio);
        viewWidth = getResources().getDisplayMetrics().widthPixels;
        xstart = viewWidth/8.5f;
        xend = viewWidth-viewWidth/20f;
        ystart = viewHeight-viewHeight/10f;
        yend = viewHeight/10f;
        //define axis number size and position offset
        labelnumbersize = (int)(viewWidth/30f);
        mTextPaint.setTextSize(labelnumbersize);
        mTextPaint.setTextAlign(Paint.Align.RIGHT);
        xtextoffset = labelnumbersize;
        ytextoffset =  viewWidth/100f; //yoffset is from x = 0;
        //sefine x axis main tick number here we say 6 as 30 days in a month
        //data = new Float[]{1F,2F};
        datasetnumber = stockprice.length; //for test
        xmainticknumber =4;
        xminorinterval = (xend-xstart)/(float)(datasetnumber);

        xmaintickinterval = (xend-xstart-datasetnumber%xmainticknumber*xminorinterval)/(xmainticknumber);


        ymainticknumber = 5;
        List<Float> list = Arrays.asList(stockprice);
        datamax = Collections.max(list);
        datamin = Collections.min(list);
        Log.v("max",datamax+"");
        Log.v("min",datamin+"");

        ymin = datamin - (datamax-datamin)/(ymainticknumber) ;
        yminorinterval = (datamax-ymin)/(ystart-yend);
        ymaintickinterval = (ystart-yend)/(ymainticknumber+1);


        majorticklength = viewWidth/40;
        minorticklength = viewWidth/80;

        mIsDrawing = true;





    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mHolder.setFixedSize(viewWidth,viewHeight);
        mIsDrawing = true;

        new Thread(this).start();

    }


    @Override
    public void surfaceChanged(SurfaceHolder holder,
                               int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mIsDrawing = false;
    }



    private void draw() {

        try {
            mCanvas = mHolder.lockCanvas();
            // SurfaceView background
            mCanvas.drawColor(Color.BLACK);

            drawCoordinate(mCanvas);
            drawdata(mCanvas);

            // mCanvas.drawPath(mPath, mPaint);
        } catch (Exception e) {
        } finally {
            if (mCanvas != null)
            { mHolder.unlockCanvasAndPost(mCanvas);
                mIsDrawing = false;

            }
        }
    }


    private void drawdata(Canvas mCanvas)
    {
        float oldx,oldy,newx,newy;
        oldx = xstart+xminorinterval;
        oldy = getyaxisfromdata(stockprice[0]);
        for(int i=1;i<datasetnumber;i++)
        {
            newx = xstart+xminorinterval*(i+1);
            newy = getyaxisfromdata(stockprice[i]);
            mCanvas.drawLine(oldx,oldy,newx,newy,mRedPaint);
            oldx = newx;
            oldy = newy;
        }
    }

    private float getyaxisfromdata(float d)
    {
        return ystart-(d-ymin)/yminorinterval;

    }

    private void drawCoordinate(Canvas mCanvas){
        mCanvas.drawLine(xstart,ystart,xend,ystart,mCoorPaint);  //DRAW X AXIS
        mCanvas.drawLine(xstart,ystart,xstart,yend,mCoorPaint);
        float xmaintickposition,ymaintickposition,ymainticklabel;
        //draw x axis
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        ymainticklabel = 0;
        for(int i=0;i<=xmainticknumber;i++)
        {
            xmaintickposition = xstart+datasetnumber%xmainticknumber*xminorinterval+xmaintickinterval*i;
             mCanvas.drawLine(xmaintickposition,ystart,xmaintickposition,ystart-majorticklength,mCoorPaint);
            if(datasetnumber%xmainticknumber+i*(datasetnumber/xmainticknumber)-1>=0)
            mCanvas.drawText(stockdate[datasetnumber%xmainticknumber+i*(datasetnumber/xmainticknumber)-1],xmaintickposition,ystart+xtextoffset,mTextPaint);
        }
        //draw y axis
        mTextPaint.setTextAlign(Paint.Align.RIGHT);
        for(int i = 1;i<=ymainticknumber+1;i++)
        {
            ymaintickposition = ystart-ymaintickinterval*i;
            ymainticklabel = (float)(Math.round(((datamax-datamin)/ymainticknumber*(i-1)+datamin)*100)/100.0);
            mCanvas.drawLine(xstart,ymaintickposition,xstart+majorticklength,ymaintickposition,mCoorPaint);
            mCanvas.drawText(ymainticklabel+"",xstart-ytextoffset,getyaxisfromdata(ymainticklabel)+labelnumbersize/2,mTextPaint);
        }


    }

    @Override
    public void run() {
        while (mIsDrawing) {
            draw();
        }
    }
}