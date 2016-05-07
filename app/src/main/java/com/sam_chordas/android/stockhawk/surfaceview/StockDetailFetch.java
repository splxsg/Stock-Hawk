package com.sam_chordas.android.stockhawk.surfaceview;

import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.support.annotation.IntDef;
import android.util.Log;
import android.view.View;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by Blues on 17/04/2016.
 */
public class StockDetailFetch extends AsyncTask<String, Void, String> {
    private int requestperiod;
    private String stockname;
    private Context mContext;
    private StockDetailView stockdetailview;
    SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd");
    SimpleDateFormat shortdateformat = new SimpleDateFormat("MM-dd");
    Date enddate = new Date(System.currentTimeMillis());
    Date startdate;
    private OkHttpClient client = new OkHttpClient();
    private String[] dateaxislist;
    private Float[] priceaxislist;





    @Retention(RetentionPolicy.SOURCE)
    @IntDef({onemonthperiod, threemonthperiod, sixmonthperiod,  oneyearperiod})
    public @interface Historyduration {

    }
    public static final int onemonthperiod = 1;
    public static final int threemonthperiod = 2;
    public static final int sixmonthperiod = 3;
    public static final int oneyearperiod = 4;



    public StockDetailFetch(Context mcontext, StockDetailView sview){this.mContext=mcontext; this.stockdetailview = sview;}

    private String fetchData(String url) throws IOException{
        Log.v("fetchdata",url);
        Request request = new Request.Builder()
                .url(url)
                .build();
        Response response = client.newCall(request).execute();
        return response.body().string();
    }


    private Date getstartdate(@Historyduration int dateperiod,Date d)
    {
        int diffday;
        switch (dateperiod)
        {
            case onemonthperiod:
                diffday = 32;
                break;
            case threemonthperiod:
                diffday = 93;
                break;
            case sixmonthperiod:
                diffday = 183;
                break;
            case oneyearperiod:
                diffday = 366;
                break;
            default:
                diffday = 32;
                break;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(d);
        calendar.add(Calendar.DATE, -diffday);
        return calendar.getTime();
    }

    @Override
    protected String doInBackground(String[] params){ //@Historyduration int params){//String[] params) {
        String startdatestr,enddatestr;
        if (params.length == 0) {
            return null;
        }
        startdate = getstartdate(Utils.getdateduration(),enddate);



        StringBuilder urlStringBuilder = new StringBuilder();
        if(Utils.checknetwork(mContext)) {
            try {
                // Base URL for the Yahoo query
                urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
                urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.historicaldata where symbol = ","UTF-8"));
                urlStringBuilder.append(URLEncoder.encode("\""+params[0]+"\"", "UTF-8"));
                startdatestr = dateformat.format(startdate);
                enddatestr = dateformat.format(enddate);
                urlStringBuilder.append(URLEncoder.encode(" and startDate = \""+startdatestr+"\"", "UTF-8"));
                urlStringBuilder.append(URLEncoder.encode(" and endDate = \""+enddatestr+"\"", "UTF-8"));
            }
            catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
                    + "org%2Falltableswithkeys&callback=");
            String urlString;
            String getResponse;
            if (urlStringBuilder != null) {
                urlString = urlStringBuilder.toString();
                try {

                    getResponse = fetchData(urlString);
                    //try {
                        ContentValues contentValues = new ContentValues();

                        if (checkResponseValid(getResponse)) {
                           return getResponse;
                        } //else
                       //    return
                   // } catch (RemoteException | OperationApplicationException e) {
                       // Log.e(LOG_TAG, "Error applying batch insert", e);
                       // setAsyncStatus(mContext, STATUS_SERVER_INVALID);
                   // }
                } catch (IOException e) {
                    e.printStackTrace();
                    //setAsyncStatus(mContext, STATUS_SERVER_DOWN);
                }

            }
          //  updateStatus();
           // return result;
        }
        else {
           // setAsyncStatus(mContext, STATUS_NONETWORK);
           // updateStatus();
          //  return GcmNetworkManager.RESULT_FAILURE;
        }

        return null;

    }



    private boolean checkResponseValid(String JSON) {
        try {
            JSONObject jsonObject = new JSONObject(JSON);

            jsonObject = jsonObject.getJSONObject("query");
            int count = Integer.parseInt(jsonObject.getString("count"));
            if (count != 1) {
                return true;
            }
            return false;
        } catch (JSONException e) {
            return false;

        }
    }


    private void AnalysisPriceJson(String stockdetails)
    throws JSONException{
        Log.v("STOCKJSON",stockdetails);
        JSONArray jsonArray = new JSONObject(stockdetails).getJSONObject("query")
                .getJSONObject("results").getJSONArray("quote");
        dateaxislist = new String[jsonArray.length()];
        priceaxislist = new Float[jsonArray.length()];
        String[] tempdateaxislist = new String[jsonArray.length()];
        Float[] temppriceaxislist = new Float[jsonArray.length()];

        for(int i=0;i<jsonArray.length();i++) {
            dateaxislist[i] = jsonArray.getJSONObject(i).getString("Date").toString();
            //ateFormat mmddformat = new SimpleDateFormat("yyyy-MM-dd");
            try{
                Date tdate = dateformat.parse(dateaxislist[i]);
                dateaxislist[i] = shortdateformat.format(tdate);
            } catch(ParseException e)
            {}
            priceaxislist[i] = Float.parseFloat(jsonArray.getJSONObject(i).getString("Close"));
        }
        for(int i=0;i<dateaxislist.length;i++)
        {
         tempdateaxislist[i] = dateaxislist[dateaxislist.length-1-i];
            temppriceaxislist[i] = priceaxislist[priceaxislist.length-1-i];
        }
        dateaxislist = tempdateaxislist;
        priceaxislist = temppriceaxislist;
        Log.v("postanalsys",priceaxislist[6]+"");

    }


    @Override
    protected void onPostExecute(String stockdetails)
    {

        try {
            AnalysisPriceJson(stockdetails);
        }catch(JSONException e)
        {}

        stockdetailview.setData(dateaxislist,priceaxislist);
    }

}
