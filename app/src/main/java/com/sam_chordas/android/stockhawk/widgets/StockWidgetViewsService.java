package com.sam_chordas.android.stockhawk.widgets;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import android.widget.TextView;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.QuoteCursorAdapter;
import com.sam_chordas.android.stockhawk.touch_helper.ItemTouchHelperViewHolder;
import com.sam_chordas.android.stockhawk.ui.DetailStockActivity;
import com.sam_chordas.android.stockhawk.ui.MyStocksActivity;

import java.util.Collections;
import java.util.concurrent.ExecutionException;

/**
 * Created by Blues on 30/04/2016.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class StockWidgetViewsService extends RemoteViewsService {
    public final String LOG_TAG = StockWidgetViewsService.class.getSimpleName();
    private QuoteCursorAdapter mCursorAdapter;
    private Context mContext;


    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        mContext = this.getApplicationContext();
        return new RemoteViewsFactory() {
            private Cursor data = null;
            private DataSetObserver mDataSetObserver;


            @Override
            public void onCreate() {


            }

            @Override
            public void onDataSetChanged() {
                if (data != null) {
                    data.close();
                }
                final long identityToken = Binder.clearCallingIdentity();
                //data.moveToFirst();
                data = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                        null, QuoteColumns.ISCURRENT + "= ?",
                        new String[]{"1"}, null);
                data.moveToFirst();
                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy() {
                if (data != null) {
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {

                if (position == AdapterView.INVALID_POSITION ||
                        data == null || !data.moveToPosition(position)) {// position>data.getCount()) {
                    return null;
                }
                RemoteViews views = new RemoteViews(getPackageName(),
                        R.layout.widget_stock_list_item);
                views.setTextViewText(R.id.widget_stock_symbol, data.getString(data.getColumnIndex("symbol")));
                views.setTextViewText(R.id.widget_bid_price, data.getString(data.getColumnIndex("bid_price")));
                views.setTextViewText(R.id.widget_change, data.getString(data.getColumnIndex("percent_change")));
                if (data.getInt(data.getColumnIndex("is_up")) == 1)
                    views.setInt(R.id.widget_change, "setBackgroundResource", R.color.material_red_700);
                else
                    views.setInt(R.id.widget_change, "setBackgroundResource", R.color.material_green_700);
                String symbol = data.getString(data.getColumnIndex(QuoteColumns.SYMBOL));
                final Intent fillInIntent = new Intent(mContext, DetailStockActivity.class).putExtra("position", symbol);
                views.setOnClickFillInIntent(R.id.widget_stock_item, fillInIntent);
                return views;
            }




            /*@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
            private void setRemoteContentDescription(RemoteViews views, String description) {
                views.setContentDescription(R.id.widget_icon, description);
            }*/

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.widget_stock_list_item);
                // return null;
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}
