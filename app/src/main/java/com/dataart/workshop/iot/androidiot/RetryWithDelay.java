package com.dataart.workshop.iot.androidiot;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.functions.Function;

public class RetryWithDelay implements Function<Observable<? extends Throwable>, Observable<?>> {
    private final int maxRetries;
    private final int retryDelayMillis;
    private int retryCount;

    public RetryWithDelay(final int maxRetries, final int retryDelayMillis) {
        this.maxRetries = maxRetries;
        this.retryDelayMillis = retryDelayMillis;
        this.retryCount = 0;
    }


    @Override
    public Observable<?> apply(Observable<? extends Throwable> observable) throws Exception {
        return observable.flatMap((Function<Throwable, Observable<?>>) throwable -> {
            if (throwable instanceof NullPointerException) {
                if (++retryCount < maxRetries) {
                    // When this Observable calls onNext, the original
                    // Observable will be retried (i.e. re-subscribed).
                    return Observable.timer(retryDelayMillis,
                            TimeUnit.MILLISECONDS);
                }
            }

            // Max retries hit. Just pass the error along.
            return Observable.error(throwable);
        });
    }
}
