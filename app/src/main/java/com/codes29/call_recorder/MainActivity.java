package com.codes29.call_recorder;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.codes29.call_recorder.services.CallDetectService;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    /**
     * View to enter phone number
     */
    EditText etPhoneNumber;

    /**
     * Button to start the call and events to handle the call
     */
    Button btnCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

        askPermission();

        startService(new Intent(MainActivity.this, CallDetectService.class));
    }

    /**
     * Ask for the required permissions
     */
    private void askPermission() {
        CheckPermission checkPermission = new CheckPermission();
        checkPermission.checkPermission(this);
    }

    /**
     * initialize the views
     */
    private void init() {

        etPhoneNumber = findViewById(R.id.et_phone_number);
        btnCall = findViewById(R.id.btn_call);

        btnCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                openDialer(etPhoneNumber.getText().toString());
            }
        });
    }

    /**
     * opens the default phone dialer
     *
     * @param phoneNumber entered phone number
     */
    private void openDialer(String phoneNumber) {
        Intent dialIntent = new Intent(Intent.ACTION_DIAL);
        dialIntent.setData(Uri.parse("tel:" + phoneNumber));
        startActivity(dialIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}
