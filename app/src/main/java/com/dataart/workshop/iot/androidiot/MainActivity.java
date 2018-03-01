package com.dataart.workshop.iot.androidiot;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private DeviceViewModel model;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView temp = findViewById(R.id.temp);

        model = ViewModelProviders.of(this).get(DeviceViewModel.class);
        model.initServer(this);

        findViewById(R.id.get)
                .setOnClickListener(v -> model.startPolling(getString(R.string.deviceId))
                        .observe(this, temperature -> temp.setText(
                                String.format(getString(R.string.temperature_format), temperature))
                        ));
        findViewById(R.id.stop).setOnClickListener(v -> model.stopPolling());


        ImageView ledStatus = findViewById(R.id.ledStatus);
        findViewById(R.id.ledOn).setOnClickListener(v -> model.sendOnCommand(getString(R.string.deviceId)).observe(this,
                success -> {
                    if (success != null && success)
                        ledStatus.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_led_lamp_red_on));
                }));
        findViewById(R.id.ledOff).setOnClickListener(v -> model.sendOffCommand(getString(R.string.deviceId)).observe(this,
                success -> {
                    if (success != null && success)
                        ledStatus.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_led_lamp_red_off));
                }));

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (model != null) {
            model.stopPolling();
        }
    }
}
