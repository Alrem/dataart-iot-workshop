package com.dataart.workshop.iot.androidiot;

import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.content.Context;

import com.github.devicehive.client.model.DHResponse;
import com.github.devicehive.client.model.Parameter;
import com.github.devicehive.client.service.DeviceCommand;
import com.github.devicehive.client.service.DeviceHive;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.BooleanSupplier;
import io.reactivex.schedulers.Schedulers;

public class DeviceViewModel extends ViewModel {

    //Temperature Command
    private static final String TEMPERATURE_PARAMETER_KEY = "pin";
    private static final String TEMPERATURE_PARAMETER_VALUE = "0";
    private static final String TEMPERATURE_COMMAND_NAME = "devices/ds18b20/read";
    private static final String KEY_TEMPERATURE_RESULT = "temperature";

    //LED Command
    private static final String LED_PARAMETER_KEY = "5";
    private static final String LED_COMMAND_NAME = "gpio/write";
    private static final String LED_TURN_ON_PARAMETER_VALUE = "1";
    private static final String LED_TURN_OFF_PARAMETER_VALUE = "0";
    public static final String OK = "ok";

    private DeviceHive deviceHive;
    private boolean pollingRepeat = false;
    private BooleanSupplier pollingSupplier = () -> pollingRepeat;

    private MutableLiveData<Float> temperature = new MutableLiveData<>();
    private MutableLiveData<Boolean> ledStatusOn = new MutableLiveData<>();
    private MutableLiveData<Boolean> ledStatusOff = new MutableLiveData<>();
    private int ledDelayInMillis = 100;
    private int tempDelayInMillis = 200;
    private int maxRetries = 5;

    public void initServer(Context context) {
        deviceHive = DeviceHive.getInstance().init(
                context.getString(R.string.server_url),
                context.getString(R.string.refresh_token));

    }

    public LiveData<Float> startPolling(String id) {
        pollingRepeat = false;

        Observable.just(id)
                //This function is getting Device Object by id
                .map(deviceId -> deviceHive.getDevice(deviceId).getData())
                //This function is sending command to the Device witch we've got before
                .map(device -> {
                    Parameter parameter = new Parameter(TEMPERATURE_PARAMETER_KEY, TEMPERATURE_PARAMETER_VALUE);
                    return device.sendCommand(TEMPERATURE_COMMAND_NAME,
                            Collections.singletonList(parameter));
                })
                //Getting command Result and retry if Device didn't send result to server
                .flatMap(this::getRetryForTemperature)
                //If pollingSupplier returns true we repeat actions (Get Device, Send Command, Read Result,Send Result to UI)
                .repeatUntil(pollingSupplier)
                //Retry if we've got error
                .retryWhen(errors ->
                        errors.flatMap(error -> Observable.timer(500, TimeUnit.MILLISECONDS)))
                .compose(applySchedulers())
                //Send temperature value to the UI
                .subscribe(temperature::setValue, Throwable::printStackTrace);
        return temperature;

    }


    public void stopPolling(LifecycleOwner owner) {
        temperature.removeObservers(owner);
        pollingRepeat = true;
    }

    public LiveData<Boolean> sendOffCommand(String id) {
        Observable.just(id)
                //This function is getting Device Object by id
                .map(deviceId -> deviceHive.getDevice(deviceId).getData())
                //This function is sending command to the Device that we got before
                .map(device -> {
                    Parameter parameter = new Parameter(LED_PARAMETER_KEY, LED_TURN_OFF_PARAMETER_VALUE);
                    return device.sendCommand(LED_COMMAND_NAME, Collections.singletonList(parameter));

                })
                //Getting command Status and Retry if Device didn't send status to server
                .flatMap(this::getRetryForLED)
                //Check if status is OK
                .map(c -> c.equalsIgnoreCase(OK))
                .compose(applySchedulers())
                //Send temperature value to the UI
                .subscribe(success -> ledStatusOff.setValue(success), Throwable::printStackTrace);
        return ledStatusOff;
    }

    public LiveData<Boolean> sendOnCommand(String id) {
        Observable.just(id)
                //This function is getting Device Object by id
                .map(deviceId -> deviceHive.getDevice(deviceId).getData())
                //This function is sending command to the Device that we got before
                .map(device -> {
                    Parameter parameter = new Parameter(LED_PARAMETER_KEY, LED_TURN_ON_PARAMETER_VALUE);
                    return device.sendCommand(LED_COMMAND_NAME, Collections.singletonList(parameter));

                })
                //Getting command Status and Retry if Device didn't send status to server
                .flatMap(this::getRetryForLED)
                //Check if status is OK
                .map(c -> c.equalsIgnoreCase(OK))
                .compose(applySchedulers())
                //Send temperature value to the UI
                .subscribe(success -> ledStatusOn.setValue(success), Throwable::printStackTrace);
        return ledStatusOn;
    }

    //Define threads where the logic will start and end
    private <T> ObservableTransformer<T, T> applySchedulers() {
        return observable -> observable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    //Fetch command status and retry if status is null
    private Observable<String> getRetryForLED(DHResponse<DeviceCommand> deviceCommandDHResponse) {
        return Observable.just(deviceCommandDHResponse)
                .map(cmd -> cmd.getData().fetchCommandStatus().getData())
                .retryWhen(new RetryWithDelay(maxRetries, ledDelayInMillis));
    }

    //Fetch command result and retry if result is null
    private Observable<Float> getRetryForTemperature(DHResponse<DeviceCommand> commandDHResponse) {
        return Observable.just(commandDHResponse)
                .map(cmd -> cmd.getData().fetchCommandResult().getData().get(KEY_TEMPERATURE_RESULT).getAsFloat())
                .retryWhen(new RetryWithDelay(maxRetries, tempDelayInMillis));
    }
}
