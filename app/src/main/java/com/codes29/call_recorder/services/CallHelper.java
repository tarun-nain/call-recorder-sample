package com.codes29.call_recorder.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaRecorder;
import android.os.Environment;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.aykuttasil.callrecord.CallRecord;

import java.util.Date;

import timber.log.Timber;

public class CallHelper {

    private static final String TAG = "CallHelper";

    /**
     * Call recorder
     */
    private CallRecord callRecord = null;

    /**
     * Context of the view
     */
    private Context ctx;

    /**
     * Listner of Call broadcasts
     */
    private CallingBroadReciever outgoingReceiver;


    /**
     * constructor
     *
     * @param ctx context of calling Act/Frag
     */
    public CallHelper(Context ctx) {
        this.ctx = ctx;

        //Initialize the Broadcast receiver
        outgoingReceiver = new CallingBroadReciever();
    }


    /**
     * Broadcast receiver to detect the outgoing calls and phone state.
     */
    public class CallingBroadReciever extends BroadcastReceiver {
        private static final String TAG = "CallingBroadReciever";
        private int lastState = TelephonyManager.CALL_STATE_IDLE;
        private Date callStartTime;
        private boolean isIncoming, wasRinging;
        private String savedNumber, number;  //because the passed incoming is only valid in ringing
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

            //start call recording
            callRecord.startCallReceiver();
        }

        protected void onIncomingCallEnded(Context ctx, String number, Date start, Date end) {
            Log.e(TAG, "onIncomingCallEnded: ");
            // Stop call recording
            callRecord.stopCallReceiver();
        }

        protected void onOutgoingCallEnded(Context ctx, String number, Date start, Date end) {
            Log.e(TAG, "onOutgoingCallEnded: ");

            // Stop call recording
            callRecord.stopCallReceiver();
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


    /**
     * Start calls detection.
     */
    public void start() {

        Log.e(TAG, "start: register receiver");

        // Start listening of broadcasts
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_NEW_OUTGOING_CALL);
        ctx.registerReceiver(outgoingReceiver, intentFilter);

        // start call recorder
        callRecord = new CallRecord.Builder(ctx)
                .setRecordFileName("RecordFileName")
                .setRecordDirName("RecordDirName")
                .setRecordDirPath(Environment.getExternalStorageDirectory().getPath()) // optional & default value
                .setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB) // optional & default value
                .setOutputFormat(MediaRecorder.OutputFormat.AMR_NB) // optional & default value
                    .setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION) // optional & default value
                .setShowSeed(true) // optional & default value ->Ex: RecordFileName_incoming.amr || RecordFileName_outgoing.amr
                .build();

        // Start call recording
        callRecord.startCallReceiver();
    }

    /**
     * Stop calls detection.
     */
    public void stop() {

        Log.e(TAG, "stop: unregister receiver & stop call recording");

        // Stop further listening of broadcast
        ctx.unregisterReceiver(outgoingReceiver);
    }
}
