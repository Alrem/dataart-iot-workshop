package com.dataart.workshop.iot.androidiot;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.content.Context;

import com.github.devicehive.client.model.DHResponse;
import com.github.devicehive.client.model.Parameter;
import com.github.devicehive.client.service.DeviceCommand;
import com.github.devicehive.client.service.DeviceHive;
import com.google.gson.JsonObject;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.BooleanSupplier;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

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
    private BooleanSupplier supplier = () -> pollingRepeat;

    private MutableLiveData<Float> temperature = new MutableLiveData<>();
    private MutableLiveData<Boolean> ledStatusOn = new MutableLiveData<>();
    private MutableLiveData<Boolean> ledStatusOff = new MutableLiveData<>();
    private int retryCount = 0;
    private int maxRetries = 5;

    public void initServer(Context context) {
        deviceHive = DeviceHive.getInstance().init(
                context.getString(R.string.server_url),
                context.getString(R.string.refresh_token));

    }

    public LiveData<Float> startPolling(String id) {
        pollingRepeat = false;

        Observable.just(id)
                .map(deviceId -> deviceHive.getDevice(deviceId).getData())
                .map(device -> {
                    Parameter parameter = new Parameter(TEMPERATURE_PARAMETER_KEY, TEMPERATURE_PARAMETER_VALUE);
                    return device.sendCommand(TEMPERATURE_COMMAND_NAME,
                            Collections.singletonList(parameter)).getData();
                })
                .delay(1, TimeUnit.SECONDS)
                .map(command -> {
                    Timber.d(command.fetchCommandResult().getData().toString());
                    JsonObject object = command.fetchCommandResult().getData();
                    return object.get(KEY_TEMPERATURE_RESULT).getAsFloat();
                })
                .repeatUntil(supplier)
                .retryWhen(errors -> errors.flatMap(error -> Observable.timer(2, TimeUnit.SECONDS)))
                .compose(applySchedulers())
                .subscribe(temperature::setValue, Throwable::printStackTrace);
        return temperature;

    }


    public void stopPolling() {
        pollingRepeat = true;
    }

    public LiveData<Boolean> sendOffCommand(String id) {
        Observable.just(id).map(deviceId -> deviceHive.getDevice(deviceId).getData())
                .map(device -> {
                    Parameter parameter = new Parameter(LED_PARAMETER_KEY, LED_TURN_OFF_PARAMETER_VALUE);
                    return device.sendCommand(LED_COMMAND_NAME, Collections.singletonList(parameter));

                })
                .flatMap(this::getRetry)
                .map(c -> c.equalsIgnoreCase(OK))
                .compose(applySchedulers())
                .subscribe(success -> ledStatusOff.setValue(success), Throwable::printStackTrace);
        return ledStatusOff;
    }

    public LiveData<Boolean> sendOnCommand(String id) {
        Observable.just(id)
                .map(deviceId -> deviceHive.getDevice(deviceId).getData())
                .map(device -> {
                    Parameter parameter = new Parameter(LED_PARAMETER_KEY, LED_TURN_ON_PARAMETER_VALUE);
                    return device.sendCommand(LED_COMMAND_NAME, Collections.singletonList(parameter));

                })
                .flatMap(this::getRetry)
                .map(c -> c.equalsIgnoreCase(OK))
                .compose(applySchedulers())
                .subscribe(success -> ledStatusOn.setValue(success), Throwable::printStackTrace);
        return ledStatusOn;
    }

    private <T> ObservableTransformer<T, T> applySchedulers() {
        return observable -> observable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private Observable<String> getRetry(DHResponse<DeviceCommand> deviceCommandDHResponse) {
        return Observable.just(deviceCommandDHResponse)
                .map(cmd -> cmd.getData().fetchCommandStatus().getData())
                .retryWhen(errors -> errors.flatMap((Function<Throwable, ObservableSource<?>>) error -> {
                    if (error instanceof NullPointerException) {
                        if (++retryCount < maxRetries) {
                            return Observable.timer(100, TimeUnit.MILLISECONDS);
                        } else {
                            retryCount = 0;
                        }
                    }
                    return Observable.error(error);
                }));
    }
}
