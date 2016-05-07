package com.sam_chordas.android.stockhawk.service;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import com.google.android.gms.gcm.TaskParams;

/**
 * Created by sam_chordas on 10/1/15.
 */
public class StockIntentService extends IntentService {
    StockTaskService stockTaskService;
    Intent intent;
  //////////add alarmmanager, pendingintent
 /* AlarmManager alarmManager = null;
  PendingIntent alarmIntent = null;

  @Override public void onCreate(){
    super.onCreate();
    alarmManager = (AlarmManager)this.getSystemService(Context.ALARM_SERVICE);
    Intent intentTo = new Intent(this,AlarmReceiver.class);
    alarmIntent = PendingIntent.getBroadcast(this,0,intentTo,0);

  }*/

  /////////////////////



  public StockIntentService(){
    super(StockIntentService.class.getName());
  }

  public StockIntentService(String name) {
    super(name);
  }

    public void updateManually(){
    Bundle args = new Bundle();
    if (intent.getStringExtra("tag").equals("add")){
        args.putString("symbol", intent.getStringExtra("symbol"));
    }
    stockTaskService.onRunTask(new TaskParams("init", args));
}


  @Override protected void onHandleIntent(Intent intent) {
      this.intent = intent;
    Log.d(StockIntentService.class.getSimpleName(), "Stock Intent Service");
    stockTaskService = new StockTaskService(this);
    Bundle args = new Bundle();
    if (intent.getStringExtra("tag").equals("add")){
      args.putString("symbol", intent.getStringExtra("symbol"));
    }
    // We can call OnRunTask from the intent service to force it to run immediately instead of
    // scheduling a task.

    //alarmManager.setInexactRepeating(alarmType, triggerAtMillis, intervalMillis, alarmIntent);

    stockTaskService.onRunTask(new TaskParams(intent.getStringExtra("tag"), args));
  }
  /*public static class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
      Intent sendIntent = new Intent(context, StockIntentService.class);
      //sendIntent.putExtra(, intent.getStringExtra(SunshineService.LOCATION_QUERY_EXTRA));
      context.startService(sendIntent);

    }
  }*/
}
