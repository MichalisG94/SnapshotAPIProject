package com.unipi.mgiavris.awarenessproject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class AutoStart extends BroadcastReceiver {

    private static final String TAG = "AwarenessAutoStart";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "AutoStart");
        //context.startService(new Intent(context, DataCollectionService.class));
        if (intent.getAction().equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED) || intent.getAction().matches("android.location.PROVIDERS_CHANGED")) {
            Intent dataCollectServiceIntent = new Intent(context, DataCollectionService.class);
            //serviceIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startService(dataCollectServiceIntent);

        }
    }
}