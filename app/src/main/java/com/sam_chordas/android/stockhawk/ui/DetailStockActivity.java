package com.sam_chordas.android.stockhawk.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.surfaceview.StockDetailFetch;
import com.sam_chordas.android.stockhawk.surfaceview.StockDetailView;

import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

/**
 * Created by Blues on 10/04/2016.
 */
public class DetailStockActivity extends AppCompatActivity{
    StockDetailView stockDetailView;
    Button oneyearbtn;
    Button onemonthbtn;
    Button threemonthsbtn;
    Button sixmonthsbtn;
    Context mContext;
    String symbol;


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        setContentView(R.layout.details_my_stock);
        Intent intent = getIntent();
        symbol = intent.getStringExtra("position");
        setTitle(symbol.toUpperCase()+" history");

        mContext = this;
        stockDetailView = (StockDetailView) findViewById(R.id.SurfaceView01);


        oneyearbtn = (Button) this.findViewById(R.id.oneyearbtn);
        onemonthbtn = (Button) this.findViewById(R.id.onemonthbtn);
        threemonthsbtn = (Button) this.findViewById(R.id.threemonthbtn);
        sixmonthsbtn = (Button) this.findViewById(R.id.sixmonthbtn);

        onemonthbtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                buttonClick(onemonthbtn);
                Utils.setdateduration(StockDetailFetch.onemonthperiod);

            }
        });
        threemonthsbtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                buttonClick(threemonthsbtn);
                Utils.setdateduration(StockDetailFetch.threemonthperiod);
            }
        });
        sixmonthsbtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                buttonClick(sixmonthsbtn);
                Utils.setdateduration(StockDetailFetch.sixmonthperiod);
            }
        });
        oneyearbtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                buttonClick(oneyearbtn);
                Utils.setdateduration(StockDetailFetch.oneyearperiod);
            }
        });


        onemonthbtn.callOnClick();

    }

    private void buttonClick(Button btn)
    {
        btn.setBackgroundColor(getResources().getColor(R.color.colorGrey));
        if(btn != oneyearbtn)
            oneyearbtn.setBackgroundColor(getResources().getColor(R.color.colorWhite));
        if(btn != onemonthbtn)
            onemonthbtn.setBackgroundColor(getResources().getColor(R.color.colorWhite));
        if(btn != threemonthsbtn)
            threemonthsbtn.setBackgroundColor(getResources().getColor(R.color.colorWhite));
        if(btn != sixmonthsbtn)
            sixmonthsbtn.setBackgroundColor(getResources().getColor(R.color.colorWhite));
        StockDetailFetch stocktask = new StockDetailFetch(mContext,stockDetailView);
        stocktask.execute(symbol);
    }




}
