package com.sam_chordas.android.stockhawk.service;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.ui.MyStocksActivity;
import com.sam_chordas.android.stockhawk.widgets.NewStockWidget;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;

/**
 * Created by sam_chordas on 9/30/15.
 * The GCMTask service is primarily for periodic tasks. However, OnRunTask can be called directly
 * and is used for the initialization and adding task as well.
 */
public class StockTaskService extends GcmTaskService{
  private String LOG_TAG = StockTaskService.class.getSimpleName();

  public static final String ACTION_DATA_UPDATED =
          "com.sam_chordas.android.stockhawk.service.ACTION_DATA_UPDATED";
  private OkHttpClient client = new OkHttpClient();
  private Context mContext;
  private StringBuilder mStoredSymbols = new StringBuilder();
  private boolean isUpdate;
  static final public String broadcastpairkey = "com.app.stockhawk.broadcast";
  LocalBroadcastManager broadcaster;
  //define async status
  @Retention(RetentionPolicy.SOURCE)
  @IntDef({STATUS_UPDATED, STATUS_SERVER_DOWN, STATUS_SERVER_INVALID,  STATUS_UNKNOWN,STATUS_INPUT_INVALID,STATUS_NONETWORK})
  public @interface ServerStatus {}

  public static final int STATUS_UPDATED = 0;
  public static final int STATUS_SERVER_DOWN = 1;
  public static final int STATUS_SERVER_INVALID = 2;
  public static final int STATUS_UNKNOWN = 3;
  public static final int STATUS_INPUT_INVALID = 4;
  public static final int STATUS_NONETWORK = 5;


  public StockTaskService(){}
  public StockTaskService(Context context){
    mContext = context;
  }
  String fetchData(String url) throws IOException{
    Log.v("fetchdata",url);
    Request request = new Request.Builder()
        .url(url)
        .build();

    Response response = client.newCall(request).execute();
    return response.body().string();
  }

  @Override
  public int onRunTask(TaskParams params){
    broadcaster = LocalBroadcastManager.getInstance(this);
    Cursor initQueryCursor;

    if (mContext == null){
      mContext = this;
    }
    StringBuilder urlStringBuilder = new StringBuilder();
    if(Utils.checknetwork(mContext)) {
      try {
        // Base URL for the Yahoo query
        urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
        urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.quotes where symbol "
                + "in (", "UTF-8"));
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
        setAsyncStatus(mContext, STATUS_INPUT_INVALID);
      }
      if (params.getTag().equals("init") || params.getTag().equals("periodic") || params.getTag().equals("manually")) {
        isUpdate = true;
        initQueryCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                new String[]{"Distinct " + QuoteColumns.SYMBOL}, null,
                null, null);
        if (initQueryCursor.getCount() == 0 || initQueryCursor == null) {
          // Init task. Populates DB with quotes for the symbols seen below
          try {

            urlStringBuilder.append(
                    URLEncoder.encode("\"YHOO\",\"AAPL\",\"GOOG\",\"MSFT\")", "UTF-8"));
          } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            setAsyncStatus(mContext, STATUS_INPUT_INVALID);
          }
        } else if (initQueryCursor != null) {
          DatabaseUtils.dumpCursor(initQueryCursor);
          initQueryCursor.moveToFirst();
          for (int i = 0; i < initQueryCursor.getCount(); i++) {
            mStoredSymbols.append("\"" +
                    initQueryCursor.getString(initQueryCursor.getColumnIndex("symbol")) + "\",");
            initQueryCursor.moveToNext();
          }
          mStoredSymbols.replace(mStoredSymbols.length() - 1, mStoredSymbols.length(), ")");
          try {
            urlStringBuilder.append(URLEncoder.encode(mStoredSymbols.toString(), "UTF-8"));
          } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            setAsyncStatus(mContext, STATUS_INPUT_INVALID);
          }
        }
      } else if (params.getTag().equals("add")) {
        isUpdate = false;
        // get symbol from params.getExtra and build query
        String stockInput = params.getExtras().getString("symbol");
        try {
          urlStringBuilder.append(URLEncoder.encode("\"" + stockInput + "\")", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
          setAsyncStatus(mContext, STATUS_INPUT_INVALID);
        }
      }
      // finalize the URL for the API query.
      urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
              + "org%2Falltableswithkeys&callback=");

      String urlString;
      String getResponse;
      int result = GcmNetworkManager.RESULT_FAILURE;

      if (urlStringBuilder != null) {
        urlString = urlStringBuilder.toString();
        try {
          // Log.v("urlstring",urlString);
          getResponse = fetchData(urlString);
          //getResponse = fetchData("https://query.yahooapis.com/v1/public/yql?q=select+*+from+yahoo.finance.quotes+where+symbol+in+%28%22YHOO%22%2C%22AAPL%22%2C%22GOOG%22%2C%22MSFT%22%29&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys&callback=");
          result = GcmNetworkManager.RESULT_SUCCESS;
          try {
            ContentValues contentValues = new ContentValues();
            // update ISCURRENT to 0 (false) so new data is current
            if (isUpdate) {
              contentValues.put(QuoteColumns.ISCURRENT, 0);
              mContext.getContentResolver().update(QuoteProvider.Quotes.CONTENT_URI, contentValues,
                      null, null);
            }

            if (checkResponseValid(getResponse)) {
              mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
                      Utils.quoteJsonToContentVals(getResponse));
              if (isUpdate)
                setAsyncStatus(mContext, STATUS_UPDATED);
            } else
              setAsyncStatus(mContext, STATUS_INPUT_INVALID);
          } catch (RemoteException | OperationApplicationException e) {
            Log.e(LOG_TAG, "Error applying batch insert", e);
            setAsyncStatus(mContext, STATUS_SERVER_INVALID);
          }//catch(JSONException e){}
        } catch (IOException e) {
          e.printStackTrace();
          setAsyncStatus(mContext, STATUS_SERVER_DOWN);
        }

      }
      updateStatus();
      updateWidgets();
      //MyStocksActivity.updateStatus(getAsyncStatus(mContext));
      return result;
    }
    else {
      setAsyncStatus(mContext, STATUS_NONETWORK);
      updateStatus();
      updateWidgets();
      return GcmNetworkManager.RESULT_FAILURE;
    }

  }

  private void updateWidgets() {
    Log.v("sendbroadcast","test");
    AppWidgetManager widgetmanager = AppWidgetManager.getInstance(mContext);
    int[] ids = widgetmanager.getAppWidgetIds(
            new ComponentName(mContext, NewStockWidget.class));
    // Setting the package ensures that only components in our app will receive the broadcast
    Intent dataUpdatedIntent = new Intent();
    dataUpdatedIntent.setAction(widgetmanager.ACTION_APPWIDGET_UPDATE);
    dataUpdatedIntent.putExtra(ACTION_DATA_UPDATED,ids);
    mContext.sendBroadcast(dataUpdatedIntent);
  }


  private void updateStatus()
  {
    Intent intent = new Intent(broadcastpairkey);
    long nowtime = System.currentTimeMillis();
    SimpleDateFormat timeformat = new SimpleDateFormat("MM-dd HH:mm:ss");
    String time = timeformat.format(nowtime);
    String statustext;
    Log.v("statuscode", getAsyncStatus(mContext)+"");
    switch (getAsyncStatus(mContext))
    {
      case STATUS_UPDATED:
        statustext = mContext.getString(R.string.status_updated_text);
        break;
      case STATUS_SERVER_DOWN:
        statustext = mContext.getString(R.string.status_server_down_text);
        break;
      case STATUS_SERVER_INVALID:
        statustext = mContext.getString(R.string.status_server_invalid_text);
        break;
      case STATUS_INPUT_INVALID:
        statustext = mContext.getString(R.string.status_input_invalid_text);
        break;
      case STATUS_NONETWORK:
        statustext = mContext.getString(R.string.status_nonetwork_text);
        break;
      default:
        statustext = mContext.getString(R.string.status_unknow_text);
        break;
    }
      intent.putExtra("Server-Status", statustext+" "+time);
    broadcaster.sendBroadcast(intent);
  }
  private boolean checkResponseValid(String JSON)
  {
    try
    {
      JSONObject jsonObject = new JSONObject(JSON);

      jsonObject = jsonObject.getJSONObject("query");
      int count = Integer.parseInt(jsonObject.getString("count"));
      if (count == 1) {
        jsonObject = jsonObject.getJSONObject("results").getJSONObject("quote");
        if (jsonObject.getString("Bid") != "null")
          return true;
        else
          return false;
      }
    }
    catch(JSONException e){
      return false;
    }
    return true;
  }
  static public void setAsyncStatus(Context mContext, @ServerStatus int serverStatus){
    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
    SharedPreferences.Editor spe = sp.edit();
    spe.putInt("Server-Status",serverStatus);
    spe.commit();
  }

  @SuppressWarnings("ResourceType")
  static public @ServerStatus
  int getAsyncStatus(Context mContext)
  {
    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
    return sp.getInt("Server-Status", STATUS_UNKNOWN);
  }

}
