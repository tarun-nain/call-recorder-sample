package com.codes29.call_recorder.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import java.util.Date;

public class CallingBroadReciever extends BroadcastReceiver {
    private static final String TAG = "CallingBroadReciever";
    private static int lastState = TelephonyManager.CALL_STATE_IDLE;
    private static Date callStartTime;
    private static boolean isIncoming, wasRinging;
    private static String savedNumber, number;  //because the passed incoming is only valid in ringing
    private Context context = null;

    @Override
    public void onReceive(Context context, Intent intent) {

        this.context = context;

        //We listen to two intents.  The new outgoing call only tells us of an outgoing call.  We use it to get the number.
        if (intent.getAction().equals("android.intent.action.NEW_OUTGOING_CALL")) {

            savedNumber = intent.getExtras().getString("android.intent.extra.PHONE_NUMBER");
            String info = intent.getExtras().getString("android.intent.extra.NAME");
            String name = intent.getExtras().getString("name");
            callStartTime = new Date();

            Log.e(TAG, "onReceive: " +
                    "Phone Number: " + savedNumber +
                    "info: " + info +
                    "name: " + name +
                    "call start time: " + callStartTime);

//            String stateStr = intent.getExtras().getString(TelephonyManager.EXTRA_STATE);
//            checkState(stateStr);

        } else {
            String stateStr = intent.getExtras().getString(TelephonyManager.EXTRA_STATE);
            number = intent.getExtras().getString(TelephonyManager.EXTRA_INCOMING_NUMBER);

            checkState(stateStr);
        }
    }

    private void checkState(String stateStr) {
        int state = 0;

        if (stateStr.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
            state = TelephonyManager.CALL_STATE_IDLE;
        } else if (stateStr.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
            state = TelephonyManager.CALL_STATE_OFFHOOK;
        } else if (stateStr.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
            state = TelephonyManager.CALL_STATE_RINGING;
        }


        onCallStateChanged(context, state, number);
    }

    //Derived classes should override these to respond to specific events of interest
    protected void onIncomingCallStarted(Context ctx, String number, Date start) {
        Log.e(TAG, "onIncomingCallStarted: ");
    }

    protected void onOutgoingCallStarted(Context ctx, String number, Date start) {
        Log.e(TAG, "onOutgoingCallStarted: ");
    }

    protected void onIncomingCallEnded(Context ctx, String number, Date start, Date end) {
        Log.e(TAG, "onIncomingCallEnded: ");
    }

    protected void onOutgoingCallEnded(Context ctx, String number, Date start, Date end) {
        Log.e(TAG, "onOutgoingCallEnded: ");
    }

    protected void onMissedCall(Context ctx, String number, Date start) {
        Log.e(TAG, "onMissedCall: ");
    }

    //Deals with actual events

    //Incoming call-  goes from IDLE to RINGING when it rings, to OFFHOOK when it's answered, to IDLE when its hung up
    //Outgoing call-  goes from IDLE to OFFHOOK when it dials out, to IDLE when hung up
    public void onCallStateChanged(Context context, int state, String number) {
        if (lastState == state) {
            //No change, debounce extras
            return;
        }
        switch (state) {
            case TelephonyManager.CALL_STATE_RINGING:
                isIncoming = true;
                callStartTime = new Date();
                savedNumber = number;
                onIncomingCallStarted(context, number, callStartTime);
                wasRinging = true;

                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                //Transition of ringing->offhook are pickups of incoming calls.  Nothing done on them
                if (lastState != TelephonyManager.CALL_STATE_RINGING) {
                    isIncoming = false;
                    callStartTime = new Date();

                    onOutgoingCallStarted(context, savedNumber, callStartTime);
                    //todo: start recording here
                }

                break;
            case TelephonyManager.CALL_STATE_IDLE:
                //Went to idle-  this is the end of a call.  What type depends on previous state(s)
                if (lastState == TelephonyManager.CALL_STATE_RINGING) {

                    //Ring but no pickup-  a miss
                    onMissedCall(context, savedNumber, callStartTime);
                    wasRinging = true;

                } else if (isIncoming) {
                    onIncomingCallEnded(context, savedNumber, callStartTime, new Date());

                } else {

                    onOutgoingCallEnded(context, savedNumber, callStartTime, new Date());
                    //todo: stop recording here

                }
                break;


        }

        lastState = state;
    }


}
